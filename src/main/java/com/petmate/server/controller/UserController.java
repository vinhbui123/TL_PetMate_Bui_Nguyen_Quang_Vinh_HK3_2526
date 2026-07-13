package com.petmate.server.controller;

import com.petmate.server.entity.User;
import com.petmate.server.enums.RoleType;
import com.petmate.server.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.petmate.server.dto.UserProfileDto;

import java.util.Optional;

import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import com.petmate.server.service.CloudinaryService;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final CloudinaryService cloudinaryService;

    @PostMapping("/sync")
    public ResponseEntity<User> syncUser(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody(required = false) java.util.Map<String, String> body) {
        if (jwt == null) {
            return ResponseEntity.status(401).build();
        }

        String uid = jwt.getSubject();
        String provider = jwt.getClaimAsString("firebase.sign_in_provider");

        String email = jwt.getClaimAsString("email");
        String name = jwt.getClaimAsString("name");
        String avatarUrl = jwt.getClaimAsString("picture");

        boolean emailVerified = Boolean.TRUE.equals(jwt.getClaimAsBoolean("email_verified"));
        boolean isSocialLogin = provider != null && !provider.equals("password");
        String currentStatus = (emailVerified || isSocialLogin) ? "ACTIVED" : "PENDING";

        if ((email == null || email.isEmpty()) && body != null && body.get("email") != null) {
            email = body.get("email");
        }
        if ((name == null || name.isEmpty()) && body != null && body.get("fullName") != null) {
            name = body.get("fullName");
        }
        if ((avatarUrl == null || avatarUrl.isEmpty()) && body != null && body.get("avatarUrl") != null) {
            avatarUrl = body.get("avatarUrl");
        }

        // 1. Tìm theo email trước (để trùng email thì dùng tài khoản cũ)
        if (email != null && !email.isEmpty()) {
            Optional<User> existingByEmail = userRepository.findByEmail(email);
            if (existingByEmail.isPresent()) {
                User user = existingByEmail.get();
                // Cập nhật providerId nếu chưa có (liên kết Facebook vào tài khoản cũ)
                if (user.getProviderId() == null || !user.getProviderId().equals(uid)) {
                    user.setProviderId(uid);
                }
                if (provider != null)
                    user.setProvider(provider);
                if (name != null && !name.isEmpty())
                    user.setFullName(name);
                // Chỉ cập nhật avatar nếu user chưa có (không ghi đè ảnh Cloudinary đã upload)
                if (avatarUrl != null && !avatarUrl.isEmpty()
                        && (user.getAvatarUrl() == null || user.getAvatarUrl().isEmpty())) {
                    user.setAvatarUrl(avatarUrl);
                }
                if (user.getStatus() == null || "PENDING".equals(user.getStatus())) {
                    user.setStatus(currentStatus);
                }
                return ResponseEntity.ok(userRepository.save(user));
            }
        }

        // 2. Tìm theo providerId (Firebase UID)
        Optional<User> existingByUid = userRepository.findByProviderId(uid);
        if (existingByUid.isPresent()) {
            User user = existingByUid.get();
            if (email != null && !email.isEmpty())
                user.setEmail(email);
            if (name != null && !name.isEmpty())
                user.setFullName(name);
            // Chỉ cập nhật avatar nếu user chưa có
            if (avatarUrl != null && !avatarUrl.isEmpty()
                    && (user.getAvatarUrl() == null || user.getAvatarUrl().isEmpty())) {
                user.setAvatarUrl(avatarUrl);
            }
            if (user.getStatus() == null || "PENDING".equals(user.getStatus())) {
                user.setStatus(currentStatus);
            }
            return ResponseEntity.ok(userRepository.save(user));
        }

        // 3. Không tìm thấy → Tạo mới
        if (email == null || email.isEmpty()) {
            email = (provider != null ? provider : "unknown") + "_" + uid + "@petmate.com";
        }
        User newUser = User.builder()
                .email(email)
                .fullName(name != null ? name : "Unknown User")
                .provider(provider != null ? provider : "firebase")
                .providerId(uid)
                .avatarUrl(avatarUrl)
                .role(RoleType.MEMBER)
                .status(currentStatus)
                .build();
        return ResponseEntity.ok(userRepository.save(newUser));
    }

    @GetMapping("/me")
    public ResponseEntity<User> getCurrentUser(@AuthenticationPrincipal Jwt jwt) {
        if (jwt == null)
            return ResponseEntity.status(401).build();
        Optional<User> user = findCurrentUser(jwt);
        return user.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/me")
    public ResponseEntity<User> updateProfile(@AuthenticationPrincipal Jwt jwt, @RequestBody UserProfileDto dto) {
        if (jwt == null)
            return ResponseEntity.status(401).build();
        Optional<User> existingUser = findCurrentUser(jwt);

        if (existingUser.isPresent()) {
            User user = existingUser.get();
            if (dto.getFullName() != null)
                user.setFullName(dto.getFullName());
            if (dto.getPhone() != null)
                user.setPhone(dto.getPhone());
            if (dto.getAddress() != null)
                user.setAddress(dto.getAddress());
            if (dto.getAvatarUrl() != null)
                user.setAvatarUrl(dto.getAvatarUrl());
            return ResponseEntity.ok(userRepository.save(user));
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/avatar")
    public ResponseEntity<User> uploadAvatar(@AuthenticationPrincipal Jwt jwt,
            @RequestParam("image") MultipartFile file) {
        if (jwt == null)
            return ResponseEntity.status(401).build();
        Optional<User> existingUser = findCurrentUser(jwt);

        if (existingUser.isPresent()) {
            try {
                String avatarUrl = cloudinaryService.uploadImage(file);
                User user = existingUser.get();
                user.setAvatarUrl(avatarUrl);
                return ResponseEntity.ok(userRepository.save(user));
            } catch (Exception e) {
                e.printStackTrace();
                return ResponseEntity.internalServerError().build();
            }
        }
        return ResponseEntity.notFound().build();
    }

    private Optional<User> findCurrentUser(Jwt jwt) {
        String uid = jwt.getSubject();
        Optional<User> userByUid = userRepository.findByProviderId(uid);
        if (userByUid.isPresent()) {
            return userByUid;
        }

        // Fallback to email if providerId is not set yet (e.g., legacy accounts)
        String email = jwt.getClaimAsString("email");
        if (email != null && !email.isEmpty()) {
            return userRepository.findByEmail(email);
        }

        return Optional.empty();
    }
}

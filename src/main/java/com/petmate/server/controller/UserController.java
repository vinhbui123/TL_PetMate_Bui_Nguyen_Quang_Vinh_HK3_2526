package com.petmate.server.controller;

import com.petmate.server.entity.FcmToken;
import com.petmate.server.entity.User;
import com.petmate.server.enums.RoleType;
import com.petmate.server.repository.FcmTokenRepository;
import com.petmate.server.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.petmate.server.dto.UserProfileDto;

import java.util.Optional;

import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import com.petmate.server.service.CloudinaryService;
import com.petmate.server.repository.PetRepository;
import com.petmate.server.repository.UserBlockRepository;
import com.petmate.server.repository.UserFollowRepository;
import com.petmate.server.entity.Pet;
import com.petmate.server.entity.UserBlock;
import com.petmate.server.entity.UserFollow;
import com.petmate.server.enums.AdStatus;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.HashMap;
import org.springframework.web.bind.annotation.PathVariable;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final CloudinaryService cloudinaryService;
    private final FcmTokenRepository fcmTokenRepository;
    private final PetRepository petRepository;
    private final UserBlockRepository userBlockRepository;
    private final UserFollowRepository userFollowRepository;

    @PostMapping("/sync")
    public ResponseEntity<User> syncUser(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody(required = false) java.util.Map<String, String> body) {
        if (jwt == null) {
            return ResponseEntity.status(401).build();
        }

        String uid = jwt.getSubject();

        // === DEBUG: In ra TOÀN BỘ claims trong JWT ===
        System.out.println("===== [SyncUser DEBUG] =====");
        System.out.println("[SyncUser] uid = " + uid);
        System.out.println("[SyncUser] ALL JWT Claims:");
        jwt.getClaims().forEach((key, value) -> {
            System.out.println("  claim[" + key + "] = " + value + " (type: " + (value != null ? value.getClass().getSimpleName() : "null") + ")");
        });
        System.out.println("[SyncUser] Request body = " + body);
        System.out.println("===========================");
        
        // Trích xuất claim "firebase" là một Map
        java.util.Map<String, Object> firebaseClaim = jwt.getClaimAsMap("firebase");
        String provider = null;
        String identitiesEmail = null;
        if (firebaseClaim != null) {
            provider = (String) firebaseClaim.get("sign_in_provider");
            try {
                Map<String, Object> identities = (Map<String, Object>) firebaseClaim.get("identities");
                if (identities != null && identities.get("email") != null) {
                    List<String> emails = (List<String>) identities.get("email");
                    if (emails != null && !emails.isEmpty()) {
                        identitiesEmail = emails.get(0);
                    }
                }
            } catch (Exception e) {
                // Ignore
            }
        }

        String email = jwt.getClaimAsString("email");
        if (email == null && identitiesEmail != null) {
            email = identitiesEmail;
        }
        String name = jwt.getClaimAsString("name");
        String avatarUrl = jwt.getClaimAsString("picture");

        boolean emailVerified = Boolean.TRUE.equals(jwt.getClaimAsBoolean("email_verified"));
        boolean isSocialLogin = provider != null && !provider.equals("password");
        String currentStatus = (emailVerified || isSocialLogin) ? "ACTIVED" : "PENDING";

        if ((email == null || email.isEmpty() || email.equals("null")) && body != null && body.get("email") != null && !body.get("email").equals("null")) {
            email = body.get("email");
        }
        if ((name == null || name.isEmpty() || name.equals("null")) && body != null && body.get("fullName") != null && !body.get("fullName").equals("null")) {
            name = body.get("fullName");
        }
        if ((avatarUrl == null || avatarUrl.isEmpty() || avatarUrl.equals("null")) && body != null && body.get("avatarUrl") != null && !body.get("avatarUrl").equals("null")) {
            avatarUrl = body.get("avatarUrl");
        }

        // 1. Tìm theo email trước (để trùng email thì dùng tài khoản cũ)
        if (email != null && !email.isEmpty()) {
            Optional<User> existingByEmail = userRepository.findByEmail(email);
            if (existingByEmail.isPresent()) {
                User user = existingByEmail.get();
                // Cập nhật providerId nếu chưa có (liên kết Facebook vào tài khoản cũ)
                if (user.getProviderId() == null || !user.getProviderId().equals(uid)) {
                    Optional<User> conflictUser = userRepository.findByProviderId(uid);
                    if (conflictUser.isPresent() && !conflictUser.get().getId().equals(user.getId())) {
                        User conflict = conflictUser.get();
                        conflict.setProviderId(null);
                        userRepository.save(conflict);
                    }
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
                if ("DELETED".equals(user.getStatus()) || "BANNED".equals(user.getStatus())) {
                    return ResponseEntity.status(403).build();
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
            if ("DELETED".equals(user.getStatus()) || "BANNED".equals(user.getStatus())) {
                return ResponseEntity.status(403).build();
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

    @PutMapping("/location")
    public ResponseEntity<User> updateLocation(@AuthenticationPrincipal Jwt jwt,
            @RequestBody java.util.Map<String, Double> body) {
        if (jwt == null)
            return ResponseEntity.status(401).build();
        Optional<User> existingUser = findCurrentUser(jwt);

        if (existingUser.isPresent()) {
            User user = existingUser.get();
            Double lat = body.get("latitude");
            Double lng = body.get("longitude");
            if (lat != null && lng != null) {
                user.setLatitude(lat);
                user.setLongitude(lng);
                return ResponseEntity.ok(userRepository.save(user));
            }
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.notFound().build();
    }

    @PutMapping("/request-rescue-org")
    public ResponseEntity<User> requestRescueOrg(@AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) return ResponseEntity.status(401).build();
        Optional<User> existingUser = findCurrentUser(jwt);
        if (existingUser.isPresent()) {
            User user = existingUser.get();
            if (user.getRole() == RoleType.MEMBER) {
                user.setRole(RoleType.PENDING_RESCUE);
                return ResponseEntity.ok(userRepository.save(user));
            }
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/fcm-token")
    public ResponseEntity<?> registerFcmToken(@AuthenticationPrincipal Jwt jwt,
            @RequestBody java.util.Map<String, String> body) {
        if (jwt == null) {
            return ResponseEntity.status(401).build();
        }
        Optional<User> existingUser = findCurrentUser(jwt);
        if (existingUser.isPresent()) {
            String token = body.get("token");
            if (token != null && !token.isEmpty()) {
                User user = existingUser.get();
                FcmToken fcmToken = FcmToken.builder()
                        .token(token)
                        .user(user)
                        .build();
                fcmTokenRepository.save(fcmToken);
                return ResponseEntity.ok().build();
            }
            return ResponseEntity.badRequest().body("Token is missing");
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/fcm-token")
    public ResponseEntity<?> removeFcmToken(@AuthenticationPrincipal Jwt jwt,
            @RequestParam String token) {
        if (jwt == null) return ResponseEntity.status(401).build();
        Optional<User> existingUser = findCurrentUser(jwt);
        if (existingUser.isPresent()) {
            if (fcmTokenRepository.existsById(token)) {
                fcmTokenRepository.deleteById(token);
            }
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/account")
    public ResponseEntity<Void> deleteAccount(@AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) return ResponseEntity.status(401).build();
        Optional<User> existingUser = findCurrentUser(jwt);
        
        if (existingUser.isPresent()) {
            User user = existingUser.get();
            // Soft delete user
            user.setStatus("DELETED");
            userRepository.save(user);
            
            // Soft delete pets
            List<Pet> userPets = petRepository.findByUserId(user.getId());
            for (Pet pet : userPets) {
                pet.setStatus(AdStatus.HIDDEN);
            }
            petRepository.saveAll(userPets);
            
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/blocks/{blockedId}")
    public ResponseEntity<Void> blockUser(@AuthenticationPrincipal Jwt jwt, @PathVariable Long blockedId) {
        if (jwt == null) return ResponseEntity.status(401).build();
        Optional<User> existingUser = findCurrentUser(jwt);
        if (existingUser.isPresent()) {
            User blocker = existingUser.get();
            if (blocker.getId().equals(blockedId)) {
                return ResponseEntity.badRequest().build();
            }
            Optional<User> blockedUser = userRepository.findById(blockedId);
            if (blockedUser.isPresent()) {
                if (!userBlockRepository.existsByBlockerIdAndBlockedId(blocker.getId(), blockedId)) {
                    UserBlock block = UserBlock.builder()
                            .blocker(blocker)
                            .blocked(blockedUser.get())
                            .build();
                    userBlockRepository.save(block);
                }
                return ResponseEntity.ok().build();
            }
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/blocks/{blockedId}")
    public ResponseEntity<Void> unblockUser(@AuthenticationPrincipal Jwt jwt, @PathVariable Long blockedId) {
        if (jwt == null) return ResponseEntity.status(401).build();
        Optional<User> existingUser = findCurrentUser(jwt);
        if (existingUser.isPresent()) {
            User blocker = existingUser.get();
            Optional<UserBlock> block = userBlockRepository.findByBlockerIdAndBlockedId(blocker.getId(), blockedId);
            block.ifPresent(userBlockRepository::delete);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/blocks")
    public ResponseEntity<List<Long>> getBlockedUsers(@AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) return ResponseEntity.status(401).build();
        Optional<User> existingUser = findCurrentUser(jwt);
        if (existingUser.isPresent()) {
            User current = existingUser.get();
            
            // Những người mình chặn
            List<Long> blockedByMe = userBlockRepository.findByBlockerId(current.getId())
                .stream().map(b -> b.getBlocked().getId()).collect(Collectors.toList());
                
            // Những người chặn mình
            List<Long> blockedMe = userBlockRepository.findByBlockedId(current.getId())
                .stream().map(b -> b.getBlocker().getId()).collect(Collectors.toList());
                
            List<Long> mutuallyBlocked = new ArrayList<>();
            mutuallyBlocked.addAll(blockedByMe);
            mutuallyBlocked.addAll(blockedMe);
            
            // Remove duplicates
            List<Long> distinctBlocked = mutuallyBlocked.stream().distinct().collect(Collectors.toList());
            return ResponseEntity.ok(distinctBlocked);
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/blocks/details")
    public ResponseEntity<List<User>> getBlockedUserDetails(@AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) return ResponseEntity.status(401).build();
        Optional<User> existingUser = findCurrentUser(jwt);
        if (existingUser.isPresent()) {
            User current = existingUser.get();
            List<User> blockedUsers = userBlockRepository.findByBlockerId(current.getId())
                .stream().map(UserBlock::getBlocked).collect(Collectors.toList());
            return ResponseEntity.ok(blockedUsers);
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/follows/{followedId}")
    public ResponseEntity<Void> followUser(@AuthenticationPrincipal Jwt jwt, @PathVariable Long followedId) {
        if (jwt == null) return ResponseEntity.status(401).build();
        Optional<User> existingUser = findCurrentUser(jwt);
        if (existingUser.isPresent()) {
            User follower = existingUser.get();
            if (follower.getId().equals(followedId)) {
                return ResponseEntity.badRequest().build(); // Cannot follow yourself
            }
            Optional<User> followedUser = userRepository.findById(followedId);
            if (followedUser.isPresent()) {
                if (!userFollowRepository.existsByFollowerIdAndFollowedId(follower.getId(), followedId)) {
                    UserFollow follow = UserFollow.builder()
                            .follower(follower)
                            .followed(followedUser.get())
                            .build();
                    userFollowRepository.save(follow);
                }
                return ResponseEntity.ok().build();
            }
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/follows/{followedId}")
    public ResponseEntity<Void> unfollowUser(@AuthenticationPrincipal Jwt jwt, @PathVariable Long followedId) {
        if (jwt == null) return ResponseEntity.status(401).build();
        Optional<User> existingUser = findCurrentUser(jwt);
        if (existingUser.isPresent()) {
            User follower = existingUser.get();
            Optional<UserFollow> follow = userFollowRepository.findByFollowerIdAndFollowedId(follower.getId(), followedId);
            follow.ifPresent(userFollowRepository::delete);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/follows/status/{followedId}")
    public ResponseEntity<Boolean> checkFollowStatus(@AuthenticationPrincipal Jwt jwt, @PathVariable Long followedId) {
        if (jwt == null) return ResponseEntity.status(401).build();
        Optional<User> existingUser = findCurrentUser(jwt);
        if (existingUser.isPresent()) {
            User follower = existingUser.get();
            boolean isFollowing = userFollowRepository.existsByFollowerIdAndFollowedId(follower.getId(), followedId);
            return ResponseEntity.ok(isFollowing);
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/follows/{userId}/stats")
    public ResponseEntity<Map<String, Long>> getUserFollowStats(@PathVariable Long userId) {
        Optional<User> user = userRepository.findById(userId);
        if (user.isPresent()) {
            long followers = userFollowRepository.countByFollowedId(userId);
            long following = userFollowRepository.countByFollowerId(userId);
            Map<String, Long> stats = new HashMap<>();
            stats.put("followers", followers);
            stats.put("following", following);
            return ResponseEntity.ok(stats);
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/follows/{userId}/followers")
    public ResponseEntity<List<User>> getFollowers(@PathVariable Long userId) {
        List<User> followers = userFollowRepository.findFollowersByUserId(userId);
        return ResponseEntity.ok(followers);
    }

    @GetMapping("/follows/{userId}/following")
    public ResponseEntity<List<User>> getFollowing(@PathVariable Long userId) {
        List<User> following = userFollowRepository.findFollowingByUserId(userId);
        return ResponseEntity.ok(following);
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

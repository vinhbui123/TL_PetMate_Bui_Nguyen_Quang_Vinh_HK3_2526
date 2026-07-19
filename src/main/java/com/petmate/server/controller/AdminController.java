package com.petmate.server.controller;

import com.petmate.server.entity.User;
import com.petmate.server.enums.RoleType;
import com.petmate.server.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import com.petmate.server.service.FirebaseService;
import com.petmate.server.service.SystemLogService;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserRepository userRepository;
    private final FirebaseService firebaseService;
    private final SystemLogService systemLogService;

    private boolean isAdmin(Jwt jwt) {
        if (jwt == null) return false;
        String uid = jwt.getSubject();
        Optional<User> user = userRepository.findByProviderId(uid);
        return user.isPresent() && user.get().getRole() == RoleType.ADMIN;
    }

    @GetMapping("/users/pending-rescue")
    public ResponseEntity<List<User>> getPendingRescueOrgs(@AuthenticationPrincipal Jwt jwt) {
        if (!isAdmin(jwt)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        
        List<User> pendingUsers = userRepository.findByRole(RoleType.PENDING_RESCUE);
        return ResponseEntity.ok(pendingUsers);
    }

    @PutMapping("/users/{id}/approve-rescue")
    public ResponseEntity<User> approveRescueOrg(
            @AuthenticationPrincipal Jwt jwt, 
            @PathVariable Long id,
            @RequestParam boolean approve) {
        
        if (!isAdmin(jwt)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) return ResponseEntity.notFound().build();

        User user = userOpt.get();
        if (user.getRole() != RoleType.PENDING_RESCUE) {
            return ResponseEntity.badRequest().build();
        }

        if (approve) {
            user.setRole(RoleType.RESCUE_ORG);
        } else {
            user.setRole(RoleType.MEMBER); // fallback to member if rejected
        }

        userRepository.save(user);

        systemLogService.info(
                approve ? "APPROVE_RESCUE" : "REJECT_RESCUE",
                jwt.getSubject(),
                "Processed rescue org request for user ID: " + id
        );

        return ResponseEntity.ok(user);
    }

    @PostMapping("/broadcast")
    public ResponseEntity<Void> sendBroadcast(
            @AuthenticationPrincipal Jwt jwt, 
            @RequestBody Map<String, String> payload) {
        
        if (!isAdmin(jwt)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        
        String title = payload.get("title");
        String body = payload.get("body");
        
        if (title == null || title.isEmpty() || body == null || body.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        firebaseService.broadcastNotification(title, body);

        systemLogService.info(
                "BROADCAST_NOTIFICATION",
                jwt.getSubject(),
                "Title: " + title + " | Body: " + body
        );

        return ResponseEntity.ok().build();
    }

    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers(@AuthenticationPrincipal Jwt jwt) {
        if (!isAdmin(jwt)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        
        List<User> users = userRepository.findAllByOrderByIdDesc();
        return ResponseEntity.ok(users);
    }

    @PutMapping("/users/{id}/status")
    public ResponseEntity<User> updateUserStatus(
            @AuthenticationPrincipal Jwt jwt, 
            @PathVariable Long id,
            @RequestParam String status) {
        
        if (!isAdmin(jwt)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) return ResponseEntity.notFound().build();

        User user = userOpt.get();
        
        // Prevent admin from banning themselves
        if (user.getProviderId().equals(jwt.getSubject()) && "BANNED".equals(status)) {
            return ResponseEntity.badRequest().build();
        }

        user.setStatus(status);
        userRepository.save(user);

        systemLogService.info(
                "UPDATE_USER_STATUS",
                jwt.getSubject(),
                "Changed user ID " + id + " status to " + status
        );

        return ResponseEntity.ok(user);
    }

    @PutMapping("/users/{id}/role")
    public ResponseEntity<User> updateUserRole(
            @AuthenticationPrincipal Jwt jwt, 
            @PathVariable Long id,
            @RequestParam RoleType role) {
        
        if (!isAdmin(jwt)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) return ResponseEntity.notFound().build();

        User user = userOpt.get();
        
        // Prevent admin from changing their own role
        if (user.getProviderId().equals(jwt.getSubject())) {
            return ResponseEntity.badRequest().build();
        }

        user.setRole(role);
        userRepository.save(user);

        systemLogService.info(
                "UPDATE_USER_ROLE",
                jwt.getSubject(),
                "Changed user ID " + id + " role to " + role
        );

        return ResponseEntity.ok(user);
    }
}

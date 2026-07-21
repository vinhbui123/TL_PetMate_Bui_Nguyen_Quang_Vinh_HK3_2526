package com.petmate.server.service;

import com.petmate.server.entity.User;
import com.petmate.server.enums.RoleType;
import com.petmate.server.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminService {

    private final UserRepository userRepository;
    private final FirebaseService firebaseService;
    private final SystemLogService systemLogService;

    private boolean isAdmin(Jwt jwt) {
        return Optional.ofNullable(jwt)
                .map(Jwt::getSubject)
                .flatMap(userRepository::findByProviderId)
                .map(u -> u.getRole() == RoleType.ADMIN)
                .orElse(false);
    }

    private void checkAdminAccess(Jwt jwt) {
        if (!isAdmin(jwt)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }
    }

    public List<User> getPendingRescueOrgs(Jwt jwt) {
        checkAdminAccess(jwt);
        return userRepository.findByRole(RoleType.PENDING_RESCUE);
    }

    public User approveRescueOrg(Jwt jwt, Long id, boolean approve) {
        checkAdminAccess(jwt);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (user.getRole() != RoleType.PENDING_RESCUE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User is not a pending rescue org");
        }

        user.setRole(approve ? RoleType.RESCUE_ORG : RoleType.MEMBER);

        userRepository.save(user);

        systemLogService.info(
                approve ? "APPROVE_RESCUE" : "REJECT_RESCUE",
                jwt.getSubject(),
                "Processed rescue org request for user ID: " + id
        );

        return user;
    }

    public void sendBroadcast(Jwt jwt, Map<String, String> payload) {
        checkAdminAccess(jwt);
        
        String title = payload.get("title");
        String body = payload.get("body");
        
        Optional.ofNullable(title).filter(t -> !t.isEmpty())
                .flatMap(t -> Optional.ofNullable(body).filter(b -> !b.isEmpty()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Title and body are required"));

        firebaseService.broadcastNotification(title, body);

        systemLogService.info(
                "BROADCAST_NOTIFICATION",
                jwt.getSubject(),
                "Title: " + title + " | Body: " + body
        );
    }

    public List<User> getAllUsers(Jwt jwt) {
        checkAdminAccess(jwt);
        return userRepository.findAllByOrderByIdDesc();
    }

    public User updateUserStatus(Jwt jwt, Long id, String status) {
        checkAdminAccess(jwt);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        
        if (user.getProviderId() != null && user.getProviderId().equals(jwt.getSubject()) && "BANNED".equals(status)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot ban yourself");
        }

        user.setStatus(status);
        userRepository.save(user);

        systemLogService.info(
                "UPDATE_USER_STATUS",
                jwt.getSubject(),
                "Changed user ID " + id + " status to " + status
        );

        return user;
    }

    public User updateUserRole(Jwt jwt, Long id, RoleType role) {
        checkAdminAccess(jwt);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        
        if (user.getProviderId() != null && user.getProviderId().equals(jwt.getSubject())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot change your own role");
        }

        user.setRole(role);
        userRepository.save(user);

        systemLogService.info(
                "UPDATE_USER_ROLE",
                jwt.getSubject(),
                "Changed user ID " + id + " role to " + role
        );

        return user;
    }
}

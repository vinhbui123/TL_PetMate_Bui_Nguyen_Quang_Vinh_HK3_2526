package com.petmate.server.service;

import com.petmate.server.entity.User;
import com.petmate.server.enums.RoleType;
import com.petmate.server.repository.UserRepository;
import com.petmate.server.repository.OrganizationProfileRepository;
import com.petmate.server.repository.PetRepository;
import com.petmate.server.repository.AdoptionApplicationRepository;
import com.petmate.server.repository.ReportRepository;
import com.petmate.server.repository.SystemLogRepository;
import com.petmate.server.dto.SystemStatsDto;
import com.petmate.server.entity.SystemLog;
import com.petmate.server.enums.AdoptionStatus;
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
    private final OrganizationProfileRepository orgRepository;
    private final PetRepository petRepository;
    private final AdoptionApplicationRepository adoptionRepository;
    private final ReportRepository reportRepository;
    private final SystemLogRepository systemLogRepository;

    private boolean isAdmin(Jwt jwt) {
        return Optional.ofNullable(jwt)
                .map(Jwt::getSubject)
                .flatMap(userRepository::findByProviderId)
                .map(u -> u.getRole() == RoleType.ADMIN)
                .orElse(false);
    }

    private void checkAdminAccess(Jwt jwt) {
        if (!isAdmin(jwt)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Từ chối truy cập");
        }
    }

    public List<User> getPendingRescueOrgs(Jwt jwt) {
        checkAdminAccess(jwt);
        return userRepository.findByRole(RoleType.PENDING_RESCUE);
    }

    public User approveRescueOrg(Jwt jwt, Long id, boolean approve) {
        checkAdminAccess(jwt);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng"));

        if (user.getRole() != RoleType.PENDING_RESCUE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User is not a pending rescue org");
        }

        user.setRole(approve ? RoleType.RESCUE_ORG : RoleType.MEMBER);

        userRepository.save(user);

        String title = approve ? "Yêu cầu nâng cấp thành công" : "Yêu cầu nâng cấp bị từ chối";
        String body = approve ? "Chúc mừng! Tài khoản của bạn đã được nâng cấp lên Trạm cứu hộ." : "Rất tiếc, yêu cầu nâng cấp lên Trạm cứu hộ của bạn đã bị từ chối.";
        firebaseService.sendNotification(user.getId(), title, body, null);

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
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng"));
        
        try {
            com.petmate.server.enums.UserStatus newStatus = com.petmate.server.enums.UserStatus.valueOf(status.toUpperCase());
            if (user.getProviderId() != null && user.getProviderId().equals(jwt.getSubject()) && newStatus == com.petmate.server.enums.UserStatus.BANNED) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot ban yourself");
            }
            user.setStatus(newStatus);

            if (newStatus == com.petmate.server.enums.UserStatus.BANNED) {
                firebaseService.sendNotification(user.getId(), "Tài khoản bị khóa", "Tài khoản của bạn đã bị Admin khóa vĩnh viễn.", null);
            } else if (newStatus == com.petmate.server.enums.UserStatus.ACTIVE) {
                firebaseService.sendNotification(user.getId(), "Tài khoản được mở khóa", "Tài khoản của bạn đã được Admin mở khóa và có thể hoạt động bình thường.", null);
            }
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid user status: " + status);
        }
        
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
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng"));
        
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

    public SystemStatsDto getSystemStats(Jwt jwt) {
        checkAdminAccess(jwt);
        long totalUsers = userRepository.count();
        long totalPets = petRepository.count();
        long totalOrgs = orgRepository.count();
        long totalReports = reportRepository.count();

        return SystemStatsDto.builder()
                .totalUsers(totalUsers)
                .totalPets(totalPets)
                .totalOrganizations(totalOrgs)
                .totalReports(totalReports)
                .build();
    }

    public List<SystemLog> getSystemLogs(Jwt jwt) {
        checkAdminAccess(jwt);
        return systemLogService.getRecentLogs(100);
    }
}


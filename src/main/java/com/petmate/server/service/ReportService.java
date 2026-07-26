package com.petmate.server.service;

import com.petmate.server.dto.ReportRequestDto;
import com.petmate.server.entity.Pet;
import com.petmate.server.entity.ChatMessage;
import com.petmate.server.entity.Report;
import com.petmate.server.entity.User;
import com.petmate.server.entity.OrganizationProfile;
import com.petmate.server.enums.ReportStatus;
import com.petmate.server.enums.UserStatus;
import com.petmate.server.repository.ChatMessageRepository;
import com.petmate.server.repository.PetRepository;
import com.petmate.server.repository.ReportRepository;
import com.petmate.server.repository.UserRepository;
import com.petmate.server.repository.OrganizationProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class ReportService {

    private final ReportRepository reportRepository;
    private final PetRepository petRepository;
    private final UserRepository userRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final OrganizationProfileRepository organizationProfileRepository;
    private final FirebaseService firebaseService;
    private final UserService userService;

    public Report submitReport(Jwt jwt, ReportRequestDto dto) {
        User reporter = userService.getCurrentUserAndUpdateActivity(jwt);

        Report.ReportBuilder reportBuilder = Report.builder()
                .reporter(reporter)
                .reason(dto.getReason())
                .description(dto.getDescription())
                .status(ReportStatus.PENDING);

        Optional.ofNullable(dto.getReportedPetId())
                .map(id -> petRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không tìm thấy thú cưng bị báo cáo")))
                .ifPresent(reportBuilder::reportedPet);

        Optional.ofNullable(dto.getReportedUserId())
                .map(id -> userRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không tìm thấy người dùng bị báo cáo")))
                .ifPresent(reportBuilder::reportedUser);

        Optional.ofNullable(dto.getReportedMessageId())
                .map(id -> chatMessageRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không tìm thấy tin nhắn bị báo cáo")))
                .ifPresent(reportBuilder::reportedMessage);

        Optional.ofNullable(dto.getReportedOrgId())
                .map(id -> organizationProfileRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không tìm thấy tổ chức bị báo cáo")))
                .ifPresent(reportBuilder::reportedOrg);

        if (dto.getReportedPetId() == null && dto.getReportedUserId() == null && dto.getReportedMessageId() == null && dto.getReportedOrgId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Phải báo cáo ít nhất một đối tượng (thú cưng, người dùng, tin nhắn, hoặc tổ chức)");
        }

        if (dto.getReportedPetId() != null) {
            reportBuilder.reportType(com.petmate.server.enums.ReportType.PET);
        } else if (dto.getReportedUserId() != null) {
            reportBuilder.reportType(com.petmate.server.enums.ReportType.USER);
        } else if (dto.getReportedMessageId() != null) {
            reportBuilder.reportType(com.petmate.server.enums.ReportType.MESSAGE);
        } else if (dto.getReportedOrgId() != null) {
            reportBuilder.reportType(com.petmate.server.enums.ReportType.ORGANIZATION);
        }

        return reportRepository.save(reportBuilder.build());
    }

    public List<Report> getAllReports() {
        return reportRepository.findAll();
    }

    public Report updateReportStatus(Long id, ReportStatus status) {
        Report report = reportRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy báo cáo"));

        report.setStatus(status);
        
        Optional.of(status)
                .filter(s -> s == ReportStatus.RESOLVED)
                .flatMap(s -> Optional.ofNullable(report.getReportedUser())
                        .or(() -> Optional.ofNullable(report.getReportedPet()).map(Pet::getUser))
                        .or(() -> Optional.ofNullable(report.getReportedMessage()).map(ChatMessage::getSender))
                        .or(() -> Optional.ofNullable(report.getReportedOrg()).map(OrganizationProfile::getUser)))
                .ifPresent(offendingUser -> {
                    if (report.getReportedPet() != null) {
                        firebaseService.sendNotification(offendingUser.getId(), "Cảnh báo bài đăng", "Bài đăng thú cưng của bạn đã bị báo cáo và được xác nhận vi phạm nội quy.", null);
                    } else if (report.getReportedMessage() != null) {
                        firebaseService.sendNotification(offendingUser.getId(), "Cảnh báo tin nhắn", "Tin nhắn của bạn đã bị báo cáo và được xác nhận vi phạm nội quy.", null);
                    } else if (report.getReportedOrg() != null) {
                        firebaseService.sendNotification(offendingUser.getId(), "Cảnh báo trạm cứu hộ", "Trạm cứu hộ của bạn đã bị báo cáo và được xác nhận vi phạm nội quy.", null);
                    } else if (report.getReportedUser() != null) {
                        firebaseService.sendNotification(offendingUser.getId(), "Cảnh báo tài khoản", "Tài khoản của bạn đã bị báo cáo và được xác nhận vi phạm nội quy.", null);
                    }

                    int currentViolations = Optional.ofNullable(offendingUser.getViolationCount()).orElse(0) + 1;
                    offendingUser.setViolationCount(currentViolations);

                    if (currentViolations == 3) {
                        firebaseService.sendNotification(offendingUser.getId(), "Cảnh báo hệ thống", "Tài khoản của bạn đã vi phạm tiêu chuẩn cộng đồng 3 lần. Xin lưu ý nếu vi phạm 5 lần, tài khoản sẽ bị khóa vĩnh viễn.", null);
                    } else if (currentViolations >= 5) {
                        offendingUser.setStatus(UserStatus.BANNED);
                        firebaseService.sendNotification(offendingUser.getId(), "Tài khoản bị khóa", "Tài khoản của bạn đã bị khóa vĩnh viễn do vi phạm tiêu chuẩn cộng đồng 5 lần.", null);
                    }
                    userRepository.save(offendingUser);
                });

        return reportRepository.save(report);
    }
}


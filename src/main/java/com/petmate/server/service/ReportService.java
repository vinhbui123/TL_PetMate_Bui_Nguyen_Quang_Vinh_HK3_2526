package com.petmate.server.service;

import com.petmate.server.dto.ReportRequestDto;
import com.petmate.server.entity.Pet;
import com.petmate.server.entity.ChatMessage;
import com.petmate.server.entity.Report;
import com.petmate.server.entity.User;
import com.petmate.server.enums.ReportStatus;
import com.petmate.server.repository.ChatMessageRepository;
import com.petmate.server.repository.PetRepository;
import com.petmate.server.repository.ReportRepository;
import com.petmate.server.repository.UserRepository;
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
    private final FirebaseService firebaseService;
    private final UserService userService;

    public Report submitReport(Jwt jwt, ReportRequestDto dto) {
        User reporter = userService.getCurrentUserOrThrow(jwt);

        Report.ReportBuilder reportBuilder = Report.builder()
                .reporter(reporter)
                .reason(dto.getReason())
                .description(dto.getDescription())
                .status(ReportStatus.PENDING);

        Optional.ofNullable(dto.getReportedPetId())
                .map(id -> petRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reported pet not found")))
                .ifPresent(reportBuilder::reportedPet);

        Optional.ofNullable(dto.getReportedUserId())
                .map(id -> userRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reported user not found")))
                .ifPresent(reportBuilder::reportedUser);

        Optional.ofNullable(dto.getReportedMessageId())
                .map(id -> chatMessageRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reported message not found")))
                .ifPresent(reportBuilder::reportedMessage);

        if (dto.getReportedPetId() == null && dto.getReportedUserId() == null && dto.getReportedMessageId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Must report at least a pet, user, or message");
        }

        return reportRepository.save(reportBuilder.build());
    }

    public List<Report> getAllReports() {
        return reportRepository.findAll();
    }

    public Report updateReportStatus(Long id, ReportStatus status) {
        Report report = reportRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Report not found"));

        report.setStatus(status);
        
        Optional.of(status)
                .filter(s -> s == ReportStatus.RESOLVED)
                .flatMap(s -> Optional.ofNullable(report.getReportedUser())
                        .or(() -> Optional.ofNullable(report.getReportedPet()).map(Pet::getUser))
                        .or(() -> Optional.ofNullable(report.getReportedMessage()).map(ChatMessage::getSender)))
                .ifPresent(offendingUser -> {
                    int currentViolations = Optional.ofNullable(offendingUser.getViolationCount()).orElse(0) + 1;
                    offendingUser.setViolationCount(currentViolations);

                    if (currentViolations == 3) {
                        firebaseService.sendNotification(offendingUser.getId(), "Cảnh báo hệ thống", "Tài khoản của bạn đã vi phạm tiêu chuẩn cộng đồng 3 lần. Xin lưu ý nếu vi phạm 5 lần, tài khoản sẽ bị khóa vĩnh viễn.", null);
                    } else if (currentViolations >= 5) {
                        offendingUser.setStatus("BANNED");
                        firebaseService.sendNotification(offendingUser.getId(), "Tài khoản bị khóa", "Tài khoản của bạn đã bị khóa vĩnh viễn do vi phạm tiêu chuẩn cộng đồng 5 lần.", null);
                    }
                    userRepository.save(offendingUser);
                });

        return reportRepository.save(report);
    }
}

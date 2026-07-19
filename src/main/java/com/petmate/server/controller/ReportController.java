package com.petmate.server.controller;

import com.petmate.server.dto.ReportRequestDto;
import com.petmate.server.entity.Pet;
import com.petmate.server.entity.Report;
import com.petmate.server.entity.User;
import com.petmate.server.enums.ReportStatus;
import com.petmate.server.repository.PetRepository;
import com.petmate.server.repository.ReportRepository;
import com.petmate.server.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/reports")
@CrossOrigin("*")
public class ReportController {

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PetRepository petRepository;

    @PostMapping
    public ResponseEntity<Report> submitReport(@AuthenticationPrincipal Jwt jwt, @RequestBody ReportRequestDto dto) {
        if (jwt == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        String uid = jwt.getSubject();
        Optional<User> reporterOpt = userRepository.findByProviderId(uid);
        if (reporterOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        User reporter = reporterOpt.get();

        Report.ReportBuilder reportBuilder = Report.builder()
                .reporter(reporter)
                .reason(dto.getReason())
                .description(dto.getDescription())
                .status(ReportStatus.PENDING);

        if (dto.getReportedPetId() != null) {
            Optional<Pet> petOpt = petRepository.findById(dto.getReportedPetId());
            if (petOpt.isPresent()) {
                reportBuilder.reportedPet(petOpt.get());
                // Optional: Automatically flag the owner as reported user
                // reportBuilder.reportedUser(petOpt.get().getUser());
            } else {
                return ResponseEntity.badRequest().build();
            }
        }

        if (dto.getReportedUserId() != null) {
            Optional<User> userOpt = userRepository.findById(dto.getReportedUserId());
            if (userOpt.isPresent()) {
                reportBuilder.reportedUser(userOpt.get());
            } else {
                return ResponseEntity.badRequest().build();
            }
        }

        if (dto.getReportedPetId() == null && dto.getReportedUserId() == null) {
            return ResponseEntity.badRequest().build(); // Must report at least a pet or a user
        }

        Report savedReport = reportRepository.save(reportBuilder.build());
        return ResponseEntity.ok(savedReport);
    }

    @GetMapping
    public ResponseEntity<List<Report>> getAllReports() {
        return ResponseEntity.ok(reportRepository.findAll());
    }
}

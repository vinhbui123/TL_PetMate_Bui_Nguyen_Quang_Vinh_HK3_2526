package com.petmate.server.controller;

import com.petmate.server.dto.AdoptionRequest;
import com.petmate.server.dto.AdoptionResponse;
import com.petmate.server.entity.AdoptionApplication;
import com.petmate.server.entity.Pet;
import com.petmate.server.entity.User;
import com.petmate.server.enums.AdoptionStatus;
import com.petmate.server.repository.AdoptionApplicationRepository;
import com.petmate.server.repository.PetRepository;
import com.petmate.server.repository.UserRepository;
import com.petmate.server.service.ChatService;
import com.petmate.server.dto.ChatRoomResponse;
import com.petmate.server.dto.ChatMessagePayload;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/adoptions")
@RequiredArgsConstructor
public class AdoptionController {

    private final AdoptionApplicationRepository adoptionRepo;
    private final PetRepository petRepository;
    private final UserRepository userRepository;
    private final ChatService chatService;

    @PostMapping("/apply")
    public ResponseEntity<AdoptionResponse> applyForAdoption(
            @AuthenticationPrincipal Jwt jwt, 
            @RequestBody AdoptionRequest request) {
        
        if (jwt == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        String uid = jwt.getSubject();
        
        Optional<User> applicantOpt = userRepository.findByProviderId(uid);
        if (applicantOpt.isEmpty()) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        
        Optional<Pet> petOpt = petRepository.findById(request.getPetId());
        if (petOpt.isEmpty()) return ResponseEntity.notFound().build();

        AdoptionApplication app = new AdoptionApplication();
        app.setApplicant(applicantOpt.get());
        app.setPet(petOpt.get());
        app.setMessage(request.getMessage());
        app.setExperience(request.getExperience());
        app.setStatus(AdoptionStatus.PENDING);

        AdoptionApplication savedApp = adoptionRepo.save(app);

        // Tự động tạo phòng chat và gửi tin nhắn
        try {
            Long applicantId = applicantOpt.get().getId();
            Long sellerId = petOpt.get().getUser().getId();
            Long petId = petOpt.get().getId();

            if (!applicantId.equals(sellerId)) {
                ChatRoomResponse room = chatService.getOrCreateRoom(applicantId, sellerId, petId);

                String autoMessage = "Xin chào, tôi vừa nộp đơn xin nhận nuôi bé " + petOpt.get().getName() + ".\n\n" +
                                     "🐾 Lời giới thiệu: " + request.getMessage() + "\n" +
                                     "⭐ Kinh nghiệm: " + request.getExperience() + "\n\n" +
                                     "Mong bạn xem xét nhé!";

                ChatMessagePayload payload = ChatMessagePayload.builder()
                        .type("CHAT")
                        .roomId(room.getId())
                        .senderId(applicantId)
                        .recipientId(sellerId)
                        .content(autoMessage)
                        .senderName(applicantOpt.get().getFullName())
                        .build();

                chatService.saveMessage(payload);
            }
        } catch (Exception e) {
            System.err.println("Lỗi tự động gửi tin nhắn nhận nuôi: " + e.getMessage());
        }

        return ResponseEntity.ok(AdoptionResponse.fromEntity(savedApp));
    }

    @GetMapping("/my-applications")
    public ResponseEntity<List<AdoptionResponse>> getMyApplications(@AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        String uid = jwt.getSubject();

        List<AdoptionResponse> responses = adoptionRepo.findByApplicant_ProviderId(uid)
                .stream()
                .map(AdoptionResponse::fromEntity)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    @GetMapping("/received")
    public ResponseEntity<List<AdoptionResponse>> getReceivedApplications(@AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        String uid = jwt.getSubject();

        List<AdoptionResponse> responses = adoptionRepo.findByPet_User_ProviderId(uid)
                .stream()
                .map(AdoptionResponse::fromEntity)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<AdoptionResponse> updateApplicationStatus(
            @AuthenticationPrincipal Jwt jwt, 
            @PathVariable Long id, 
            @RequestParam AdoptionStatus status) {
        
        if (jwt == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        String uid = jwt.getSubject();
        
        Optional<AdoptionApplication> appOpt = adoptionRepo.findById(id);
        if (appOpt.isEmpty()) return ResponseEntity.notFound().build();
        
        AdoptionApplication app = appOpt.get();
        // Only the pet owner can update the status
        if (!app.getPet().getUser().getProviderId().equals(uid)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        app.setStatus(status);
        AdoptionApplication updatedApp = adoptionRepo.save(app);
        
        return ResponseEntity.ok(AdoptionResponse.fromEntity(updatedApp));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelAdoptionApplication(
            @AuthenticationPrincipal Jwt jwt, 
            @PathVariable Long id) {
        if (jwt == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        String uid = jwt.getSubject();
        
        Optional<AdoptionApplication> appOpt = adoptionRepo.findById(id);
        if (appOpt.isEmpty()) return ResponseEntity.notFound().build();
        
        AdoptionApplication app = appOpt.get();
        // Only the applicant can cancel their own application
        if (!app.getApplicant().getProviderId().equals(uid)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        // Cannot cancel if already processed (not PENDING)
        if (app.getStatus() != AdoptionStatus.PENDING) {
            return ResponseEntity.badRequest().build();
        }

        adoptionRepo.delete(app);
        return ResponseEntity.ok().build();
    }
}

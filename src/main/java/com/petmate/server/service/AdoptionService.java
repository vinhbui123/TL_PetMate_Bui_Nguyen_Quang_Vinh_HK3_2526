package com.petmate.server.service;

import com.petmate.server.dto.AdoptionRequest;
import com.petmate.server.dto.AdoptionResponse;
import com.petmate.server.dto.ChatMessagePayload;
import com.petmate.server.dto.ChatRoomResponse;
import com.petmate.server.entity.AdoptionApplication;
import com.petmate.server.entity.Pet;
import com.petmate.server.entity.User;
import com.petmate.server.enums.AdoptionStatus;
import com.petmate.server.repository.AdoptionApplicationRepository;
import com.petmate.server.repository.PetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class AdoptionService {

    private final AdoptionApplicationRepository adoptionRepo;
    private final PetRepository petRepository;
    private final UserService userService;
    private final ChatService chatService;
    private final FirebaseService firebaseService;

    public AdoptionResponse applyForAdoption(Jwt jwt, AdoptionRequest request) {
        User applicant = userService.getCurrentUserOrThrow(jwt);
        
        Pet pet = petRepository.findById(request.getPetId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pet not found"));

        AdoptionApplication app = new AdoptionApplication();
        app.setApplicant(applicant);
        app.setPet(pet);
        app.setMessage(request.getMessage());
        app.setExperience(request.getExperience());
        app.setStatus(AdoptionStatus.PENDING);

        AdoptionApplication savedApp = adoptionRepo.save(app);

        try {
            Optional.of(applicant.getId())
                    .filter(id -> !id.equals(pet.getUser().getId()))
                    .ifPresent(applicantId -> {
                        ChatRoomResponse room = chatService.getOrCreateRoom(applicantId, pet.getUser().getId(), pet.getId());

                        String autoMessage = "Xin chào, tôi vừa nộp đơn xin nhận nuôi bé " + pet.getName() + ".\n\n" +
                                             "Lời giới thiệu: " + request.getMessage() + "\n" +
                                             "Kinh nghiệm: " + request.getExperience() + "\n\n" +
                                             "Mong bạn xem xét nhé!";

                        chatService.saveMessage(ChatMessagePayload.builder()
                                .type("CHAT")
                                .roomId(room.getId())
                                .senderId(applicantId)
                                .recipientId(pet.getUser().getId())
                                .content(autoMessage)
                                .senderName(applicant.getFullName())
                                .build());
                    });
        } catch (Exception e) {
            System.err.println("Lỗi tự động gửi tin nhắn nhận nuôi: " + e.getMessage());
        }

        return AdoptionResponse.fromEntity(savedApp);
    }

    public List<AdoptionResponse> getMyApplications(Jwt jwt) {
        String uid = jwt.getSubject();
        return adoptionRepo.findByApplicant_ProviderId(uid)
                .stream()
                .map(AdoptionResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public List<AdoptionResponse> getReceivedApplications(Jwt jwt) {
        String uid = jwt.getSubject();
        return adoptionRepo.findByPet_User_ProviderId(uid)
                .stream()
                .map(AdoptionResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public AdoptionResponse updateApplicationStatus(Jwt jwt, Long id, AdoptionStatus status) {
        String uid = jwt.getSubject();
        
        AdoptionApplication app = adoptionRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Application not found"));
        
        if (!app.getPet().getUser().getProviderId().equals(uid)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        app.setStatus(status);
        AdoptionApplication updatedApp = adoptionRepo.save(app);

        // Gửi thông báo đến người nhận nuôi
        try {
            String title = "Cập nhật đơn nhận nuôi";
            String body = "";
            if (status == AdoptionStatus.APPROVED) {
                body = "Chúc mừng! Đơn xin nhận nuôi bé " + app.getPet().getName() + " của bạn đã được duyệt.";
            } else if (status == AdoptionStatus.REJECTED) {
                body = "Rất tiếc, đơn xin nhận nuôi bé " + app.getPet().getName() + " của bạn đã bị từ chối.";
            }
            
            if (!body.isEmpty()) {
                firebaseService.sendNotification(
                    app.getApplicant().getId(),
                    title,
                    body,
                    Map.of("type", "adoption_update", "petId", String.valueOf(app.getPet().getId()))
                );
            }
        } catch (Exception e) {
            System.err.println("Lỗi gửi thông báo: " + e.getMessage());
        }
        
        return AdoptionResponse.fromEntity(updatedApp);
    }

    public void cancelAdoptionApplication(Jwt jwt, Long id) {
        String uid = jwt.getSubject();
        
        AdoptionApplication app = adoptionRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Application not found"));
        
        if (!app.getApplicant().getProviderId().equals(uid)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }
        
        if (app.getStatus() != AdoptionStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Application is not pending");
        }

        adoptionRepo.delete(app);
    }
}

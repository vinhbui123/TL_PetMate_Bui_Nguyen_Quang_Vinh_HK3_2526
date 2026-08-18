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
import com.petmate.server.repository.OrganizationMemberRepository;
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
    private final OrganizationMemberRepository memberRepository;

    public AdoptionResponse applyForAdoption(Jwt jwt, AdoptionRequest request) {
        User applicant = userService.getCurrentUserAndUpdateActivity(jwt);
        
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

                        String autoMessage = "Xin chÃ o, tÃ´i vá»«a ná»™p Ä‘Æ¡n xin nháº­n nuÃ´i bÃ© " + pet.getName() + ".\n\n" +
                                             "Lá»i giá»›i thiá»‡u: " + request.getMessage() + "\n" +
                                             "Kinh nghiá»‡m: " + request.getExperience() + "\n\n" +
                                             "Mong báº¡n xem xÃ©t nhÃ©!";

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
            System.err.println("Lá»—i tá»± Ä‘á»™ng gá»­i tin nháº¯n nháº­n nuÃ´i: " + e.getMessage());
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

    public List<AdoptionResponse> getOrgAdoptions(Long orgId, Jwt jwt) {
        return adoptionRepo.findByPet_Organization_Id(orgId)
                .stream()
                .map(AdoptionResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public AdoptionResponse updateApplicationStatus(Jwt jwt, Long id, AdoptionStatus status) {
        String uid = jwt.getSubject();
        User currentUser = userService.getCurrentUserOrThrow(jwt);
        
        AdoptionApplication app = adoptionRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Application not found"));
        
        boolean isOwner = app.getPet().getUser() != null && app.getPet().getUser().getProviderId().equals(uid);
        boolean isOrgMember = app.getPet().getOrganization() != null && memberRepository.existsByOrganizationIdAndUserId(app.getPet().getOrganization().getId(), currentUser.getId());
        boolean isOrgOwner = app.getPet().getOrganization() != null && app.getPet().getOrganization().getUser().getId().equals(currentUser.getId());

        if (!isOwner && !isOrgMember && !isOrgOwner) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        app.setStatus(status);
        AdoptionApplication updatedApp = adoptionRepo.save(app);

        // Gá»­i thÃ´ng bÃ¡o Ä‘áº¿n ngÆ°á»i nháº­n nuÃ´i
        try {
            String title = "Cáº­p nháº­t Ä‘Æ¡n nháº­n nuÃ´i";
            String body = "";
            if (status == AdoptionStatus.APPROVED) {
                body = "ChÃºc má»«ng! ÄÆ¡n xin nháº­n nuÃ´i bÃ© " + app.getPet().getName() + " cá»§a báº¡n Ä‘Ã£ Ä‘Æ°á»£c duyá»‡t.";
            } else if (status == AdoptionStatus.REJECTED) {
                body = "Ráº¥t tiáº¿c, Ä‘Æ¡n xin nháº­n nuÃ´i bÃ© " + app.getPet().getName() + " cá»§a báº¡n Ä‘Ã£ bá»‹ tá»« chá»‘i.";
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
            System.err.println("Lá»—i gá»­i thÃ´ng bÃ¡o: " + e.getMessage());
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
    
    public boolean hasApprovedAdoption(Jwt jwt, Long petId) {
        User user = userService.getCurrentUserOrThrow(jwt);
        return adoptionRepo.existsByApplicantIdAndPetIdAndStatus(user.getId(), petId, AdoptionStatus.APPROVED);
    }
}

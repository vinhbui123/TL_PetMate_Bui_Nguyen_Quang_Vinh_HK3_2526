package com.petmate.server.service;

import com.petmate.server.dto.InteractionDto.*;
import com.petmate.server.entity.Pet;
import com.petmate.server.entity.SavedPet;
import com.petmate.server.entity.User;
import com.petmate.server.repository.SavedPetRepository;
import com.petmate.server.repository.PetRepository;
import com.petmate.server.repository.PetLikeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class InteractionService {

    private final PetRepository petRepository;
    private final SavedPetRepository savedPetRepository;
    private final PetLikeRepository petLikeRepository;
    private final UserService userService;

    public SaveStatusResponse getSaveStatus(Long petId, Jwt jwt) {
        boolean isSaved = Optional.ofNullable(jwt)
                .map(Jwt::getSubject)
                .map(uid -> savedPetRepository.existsByPetIdAndUser_ProviderId(petId, uid))
                .orElse(false);

        return new SaveStatusResponse(isSaved);
    }

    public SaveStatusResponse toggleSave(Long petId, Jwt jwt) {
        String uid = jwt.getSubject();

        User user = userService.getCurrentUserAndUpdateActivity(jwt);

        Pet pet = petRepository.findById(petId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pet not found"));

        boolean isCurrentlySaved = savedPetRepository.existsByPetIdAndUser_ProviderId(petId, uid);
        
        if (isCurrentlySaved) {
            savedPetRepository.deleteByPetIdAndUser_ProviderId(petId, uid);
        } else {
            savedPetRepository.save(SavedPet.builder().pet(pet).user(user).build());
        }

        return new SaveStatusResponse(!isCurrentlySaved);
    }

    public LikeStatusResponse getLikeStatus(Long petId, Jwt jwt) {
        boolean isLiked = Optional.ofNullable(jwt)
                .map(Jwt::getSubject)
                .map(uid -> petLikeRepository.existsByPetIdAndUser_ProviderId(petId, uid))
                .orElse(false);

        Pet pet = petRepository.findById(petId).orElse(null);
        int likeCount = (pet != null && pet.getLikeCount() != null) ? pet.getLikeCount() : 0;

        return new LikeStatusResponse(isLiked, likeCount);
    }

    public LikeStatusResponse toggleLike(Long petId, Jwt jwt) {
        String uid = jwt.getSubject();

        User user = userService.getCurrentUserAndUpdateActivity(jwt);

        Pet pet = petRepository.findById(petId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pet not found"));

        boolean isCurrentlyLiked = petLikeRepository.existsByPetIdAndUser_ProviderId(petId, uid);
        
        if (isCurrentlyLiked) {
            petLikeRepository.deleteByPetIdAndUser_ProviderId(petId, uid);
            pet.setLikeCount(Math.max(0, (pet.getLikeCount() != null ? pet.getLikeCount() : 1) - 1));
        } else {
            petLikeRepository.save(com.petmate.server.entity.PetLike.builder().pet(pet).user(user).build());
            pet.setLikeCount((pet.getLikeCount() != null ? pet.getLikeCount() : 0) + 1);
        }
        
        petRepository.save(pet);

        return new LikeStatusResponse(!isCurrentlyLiked, pet.getLikeCount());
    }
}

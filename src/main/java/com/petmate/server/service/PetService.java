package com.petmate.server.service;

import com.petmate.server.dto.PetRequestDto;
import com.petmate.server.entity.Pet;
import com.petmate.server.entity.User;
import com.petmate.server.enums.AdStatus;
import com.petmate.server.enums.RoleType;
import com.petmate.server.repository.PetRepository;
import com.petmate.server.repository.SavedPetRepository;
import com.petmate.server.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class PetService {

    private final PetRepository petRepository;
    private final UserRepository userRepository;
    private final CloudinaryService cloudinaryService;
    private final FirebaseService firebaseService;
    private final SavedPetRepository savedPetRepository;
    private final UserService userService; // To reuse findCurrentUser

    public List<Pet> getAllPets(String category) {
        if (category != null && !category.isEmpty()) {
            return petRepository.findByCategoryAndStatus(category, AdStatus.AVAILABLE);
        }
        return petRepository.findByStatus(AdStatus.AVAILABLE);
    }

    public Pet getPetById(Long id) {
        return petRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pet not found"));
    }

    public List<Pet> getPetsByUserId(Long userId) {
        return petRepository.findByUserId(userId);
    }

    public List<Pet> getMyPets(Jwt jwt) {
        String uid = jwt.getSubject();
        return petRepository.findByUser_ProviderId(uid);
    }

    public List<Pet> getSavedPets(Jwt jwt) {
        String uid = jwt.getSubject();
        return savedPetRepository.findByUser_ProviderIdOrderByCreatedAtDesc(uid)
                .stream()
                .map(com.petmate.server.entity.SavedPet::getPet)
                .toList();
    }

    public Pet createPet(Jwt jwt, PetRequestDto dto) {
        User owner = userService.getCurrentUserOrThrow(jwt);
        Pet pet = Pet.builder()
                .name(dto.getName())
                .breed(dto.getBreed())
                .age(dto.getAge())
                .weight(dto.getWeight())
                .gender(dto.getGender())
                .price(dto.getPrice())
                .description(dto.getDescription())
                .category(dto.getCategory())
                .status(AdStatus.PENDING)
                .latitude(owner.getLatitude())
                .longitude(owner.getLongitude())
                .user(owner)
                .build();
        
        return petRepository.save(pet);
    }

    public List<Pet> getPendingPets(Jwt jwt) {
        User user = userService.getCurrentUserOrThrow(jwt);
        if (user.getRole() != RoleType.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }
        return petRepository.findByStatus(AdStatus.PENDING);
    }

    public List<Pet> getAdminAllPets(Jwt jwt) {
        User user = userService.getCurrentUserOrThrow(jwt);
        if (user.getRole() != RoleType.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }
        return petRepository.findAll();
    }

    public Pet updatePetStatus(Jwt jwt, Long id, AdStatus status) {
        User currentUser = userService.getCurrentUserOrThrow(jwt);
        String uid = jwt.getSubject();

        Pet pet = petRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pet not found"));

        boolean isAdmin = currentUser.getRole() == RoleType.ADMIN;
        boolean isOwner = pet.getUser() != null && uid.equals(pet.getUser().getProviderId());

        if (!isAdmin && !isOwner) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        pet.setStatus(status);
        petRepository.save(pet);

        Optional.of(pet)
                .map(Pet::getUser)
                .filter(u -> isAdmin && !isOwner)
                .ifPresent(u -> {
                    String title = "Cập nhật Tin đăng";
                    String body = status == AdStatus.AVAILABLE 
                        ? "Tin đăng thú cưng '" + pet.getName() + "' của bạn đã được duyệt!" 
                        : (status == AdStatus.REJECTED ? "Tin đăng thú cưng '" + pet.getName() + "' của bạn đã bị từ chối." : "Tin đăng của bạn đã được cập nhật.");
                    firebaseService.sendNotification(u.getId(), title, body, null);
                });

        return pet;
    }

    public Pet updatePet(Jwt jwt, Long id, PetRequestDto dto) {
        String uid = jwt.getSubject();
        
        Pet pet = petRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pet not found"));
        
        if (pet.getUser() == null || !uid.equals(pet.getUser().getProviderId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        Optional.ofNullable(dto.getName()).ifPresent(pet::setName);
        Optional.ofNullable(dto.getBreed()).ifPresent(pet::setBreed);
        Optional.ofNullable(dto.getAge()).ifPresent(pet::setAge);
        Optional.ofNullable(dto.getWeight()).ifPresent(pet::setWeight);
        Optional.ofNullable(dto.getGender()).ifPresent(pet::setGender);
        Optional.ofNullable(dto.getPrice()).ifPresent(pet::setPrice);
        Optional.ofNullable(dto.getDescription()).ifPresent(pet::setDescription);
        Optional.ofNullable(dto.getCategory()).ifPresent(pet::setCategory);
        Optional.ofNullable(dto.getStatus()).ifPresent(pet::setStatus);

        return petRepository.save(pet);
    }

    public void deletePet(Jwt jwt, Long id) {
        User currentUser = userService.getCurrentUserOrThrow(jwt);
        String uid = jwt.getSubject();

        Pet pet = petRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pet not found"));
        
        if (pet.getUser() == null || (!uid.equals(pet.getUser().getProviderId()) && currentUser.getRole() != RoleType.ADMIN)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        petRepository.deleteById(id);
    }

    public Pet uploadPetImage(Jwt jwt, Long id, MultipartFile file) {
        String uid = jwt.getSubject();

        Pet pet = petRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pet not found"));

        if (pet.getUser() == null || !uid.equals(pet.getUser().getProviderId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        try {
            String imageUrl = cloudinaryService.uploadImage(file);
            pet.setImageUrl(imageUrl);
            return petRepository.save(pet);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error uploading image", e);
        }
    }
}

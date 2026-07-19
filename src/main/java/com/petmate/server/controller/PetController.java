package com.petmate.server.controller;

import com.petmate.server.dto.PetRequestDto;
import com.petmate.server.entity.Pet;
import com.petmate.server.entity.User;
import com.petmate.server.enums.AdStatus;
import com.petmate.server.repository.PetRepository;
import com.petmate.server.repository.UserRepository;
import com.petmate.server.service.CloudinaryService;
import com.petmate.server.service.FirebaseService;
import com.petmate.server.enums.RoleType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/pets")
@RequiredArgsConstructor
public class PetController {

    private final PetRepository petRepository;
    private final UserRepository userRepository;
    private final CloudinaryService cloudinaryService;
    private final FirebaseService firebaseService;

    @GetMapping
    public ResponseEntity<List<Pet>> getAllPets(@RequestParam(required = false) String category) {
        if (category != null && !category.isEmpty()) {
            return ResponseEntity.ok(petRepository.findByCategoryAndStatus(category, AdStatus.AVAILABLE));
        }
        return ResponseEntity.ok(petRepository.findByStatus(AdStatus.AVAILABLE));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Pet> getPetById(@PathVariable Long id) {
        return petRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Pet>> getPetsByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(petRepository.findByUserId(userId));
    }

    @GetMapping("/my-pets")
    public ResponseEntity<List<Pet>> getMyPets(@AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        String uid = jwt.getSubject();
        return ResponseEntity.ok(petRepository.findByUser_ProviderId(uid));
    }

    @PostMapping
    public ResponseEntity<Pet> createPet(@AuthenticationPrincipal Jwt jwt, @RequestBody PetRequestDto dto) {
        if (jwt == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        String uid = jwt.getSubject();
        Optional<User> userOpt = userRepository.findByProviderId(uid);
        if (userOpt.isEmpty()) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

        User owner = userOpt.get();
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
        
        return ResponseEntity.ok(petRepository.save(pet));
    }

    @GetMapping("/pending")
    public ResponseEntity<List<Pet>> getPendingPets(@AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        String uid = jwt.getSubject();
        Optional<User> userOpt = userRepository.findByProviderId(uid);
        if (userOpt.isEmpty() || userOpt.get().getRole() != RoleType.ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(petRepository.findByStatus(AdStatus.PENDING));
    }

    @GetMapping("/admin/all")
    public ResponseEntity<List<Pet>> getAdminAllPets(@AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        String uid = jwt.getSubject();
        Optional<User> userOpt = userRepository.findByProviderId(uid);
        if (userOpt.isEmpty() || userOpt.get().getRole() != RoleType.ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(petRepository.findAll());
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Pet> updatePetStatus(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id, @RequestParam AdStatus status) {
        if (jwt == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        String uid = jwt.getSubject();
        Optional<User> userOpt = userRepository.findByProviderId(uid);
        if (userOpt.isEmpty() || userOpt.get().getRole() != RoleType.ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Optional<Pet> petOpt = petRepository.findById(id);
        if (petOpt.isEmpty()) return ResponseEntity.notFound().build();

        Pet pet = petOpt.get();
        pet.setStatus(status);
        petRepository.save(pet);

        // Send FCM Notification
        if (pet.getUser() != null) {
            String title = "Cập nhật Tin đăng";
            String body = status == AdStatus.AVAILABLE 
                ? "Tin đăng thú cưng '" + pet.getName() + "' của bạn đã được duyệt!" 
                : (status == AdStatus.REJECTED ? "Tin đăng thú cưng '" + pet.getName() + "' của bạn đã bị từ chối." : "Tin đăng của bạn đã được cập nhật.");
            firebaseService.sendNotification(pet.getUser().getId(), title, body, null);
        }

        return ResponseEntity.ok(pet);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Pet> updatePet(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id, @RequestBody PetRequestDto dto) {
        if (jwt == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        String uid = jwt.getSubject();
        
        Optional<Pet> petOpt = petRepository.findById(id);
        if (petOpt.isEmpty()) return ResponseEntity.notFound().build();
        
        Pet pet = petOpt.get();
        if (pet.getUser() == null || !uid.equals(pet.getUser().getProviderId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        if (dto.getName() != null) pet.setName(dto.getName());
        if (dto.getBreed() != null) pet.setBreed(dto.getBreed());
        if (dto.getAge() != null) pet.setAge(dto.getAge());
        if (dto.getWeight() != null) pet.setWeight(dto.getWeight());
        if (dto.getGender() != null) pet.setGender(dto.getGender());
        if (dto.getPrice() != null) pet.setPrice(dto.getPrice());
        if (dto.getDescription() != null) pet.setDescription(dto.getDescription());
        if (dto.getCategory() != null) pet.setCategory(dto.getCategory());
        if (dto.getStatus() != null) pet.setStatus(dto.getStatus());

        return ResponseEntity.ok(petRepository.save(pet));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePet(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        if (jwt == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        String uid = jwt.getSubject();
        
        Optional<User> userOpt = userRepository.findByProviderId(uid);
        if (userOpt.isEmpty()) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        User currentUser = userOpt.get();

        Optional<Pet> petOpt = petRepository.findById(id);
        if (petOpt.isEmpty()) return ResponseEntity.notFound().build();
        
        Pet pet = petOpt.get();
        if (pet.getUser() == null || (!uid.equals(pet.getUser().getProviderId()) && currentUser.getRole() != RoleType.ADMIN)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        petRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/image")
    public ResponseEntity<Pet> uploadPetImage(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id, @RequestParam("image") MultipartFile file) {
        if (jwt == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        String uid = jwt.getSubject();

        Optional<Pet> petOpt = petRepository.findById(id);
        if (petOpt.isEmpty()) return ResponseEntity.notFound().build();

        Pet pet = petOpt.get();
        if (pet.getUser() == null || !uid.equals(pet.getUser().getProviderId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            String imageUrl = cloudinaryService.uploadImage(file);
            pet.setImageUrl(imageUrl);
            return ResponseEntity.ok(petRepository.save(pet));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}

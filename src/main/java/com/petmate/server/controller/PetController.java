package com.petmate.server.controller;

import com.petmate.server.dto.PetRequestDto;
import com.petmate.server.entity.Pet;
import com.petmate.server.entity.User;
import com.petmate.server.enums.AdStatus;
import com.petmate.server.repository.PetRepository;
import com.petmate.server.repository.UserRepository;
import com.petmate.server.service.CloudinaryService;
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

    @GetMapping
    public ResponseEntity<List<Pet>> getAllPets(@RequestParam(required = false) String category) {
        if (category != null && !category.isEmpty()) {
            return ResponseEntity.ok(petRepository.findByCategory(category));
        }
        return ResponseEntity.ok(petRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Pet> getPetById(@PathVariable Long id) {
        return petRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
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

        Pet pet = Pet.builder()
                .name(dto.getName())
                .breed(dto.getBreed())
                .age(dto.getAge())
                .weight(dto.getWeight())
                .gender(dto.getGender())
                .distance(dto.getDistance())
                .price(dto.getPrice())
                .description(dto.getDescription())
                .category(dto.getCategory())
                .status(dto.getStatus() != null ? dto.getStatus() : AdStatus.AVAILABLE)
                .user(userOpt.get())
                .build();
        
        return ResponseEntity.ok(petRepository.save(pet));
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
        if (dto.getDistance() != null) pet.setDistance(dto.getDistance());
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
        
        Optional<Pet> petOpt = petRepository.findById(id);
        if (petOpt.isEmpty()) return ResponseEntity.notFound().build();
        
        Pet pet = petOpt.get();
        if (pet.getUser() == null || !uid.equals(pet.getUser().getProviderId())) {
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

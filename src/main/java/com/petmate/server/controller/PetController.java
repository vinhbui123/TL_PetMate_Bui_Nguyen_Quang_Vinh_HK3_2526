package com.petmate.server.controller;

import com.petmate.server.dto.PetRequestDto;
import com.petmate.server.entity.Pet;
import com.petmate.server.enums.AdStatus;
import com.petmate.server.service.PetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/pets")
@RequiredArgsConstructor
public class PetController {

    private final PetService petService;

    @GetMapping
    public ResponseEntity<List<Pet>> getAllPets(@RequestParam(required = false) String category) {
        return ResponseEntity.ok(petService.getAllPets(category));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Pet> getPetById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(petService.getPetById(id));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Pet>> getPetsByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(petService.getPetsByUserId(userId));
    }

    @GetMapping("/my-pets")
    public ResponseEntity<List<Pet>> getMyPets(@AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            return ResponseEntity.ok(petService.getMyPets(jwt));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }

    @GetMapping("/org/{orgId}")
    public ResponseEntity<List<Pet>> getOrgPets(@AuthenticationPrincipal Jwt jwt, @PathVariable Long orgId) {
        if (jwt == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            return ResponseEntity.ok(petService.getPetsByOrganizationId(orgId, jwt));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }

    @GetMapping("/saved")
    public ResponseEntity<List<Pet>> getSavedPets(@AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            return ResponseEntity.ok(petService.getSavedPets(jwt));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }

    @PostMapping
    public ResponseEntity<Pet> createPet(@AuthenticationPrincipal Jwt jwt, @jakarta.validation.Valid @RequestBody PetRequestDto dto) {
        if (jwt == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            return ResponseEntity.ok(petService.createPet(jwt, dto));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }

    @GetMapping("/pending")
    public ResponseEntity<List<Pet>> getPendingPets(@AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            return ResponseEntity.ok(petService.getPendingPets(jwt));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }

    @GetMapping("/admin/all")
    public ResponseEntity<List<Pet>> getAdminAllPets(@AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            return ResponseEntity.ok(petService.getAdminAllPets(jwt));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Pet> updatePetStatus(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id, @RequestParam AdStatus status) {
        if (jwt == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            return ResponseEntity.ok(petService.updatePetStatus(jwt, id, status));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Pet> updatePet(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id, @jakarta.validation.Valid @RequestBody PetRequestDto dto) {
        if (jwt == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            return ResponseEntity.ok(petService.updatePet(jwt, id, dto));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePet(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        if (jwt == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            petService.deletePet(jwt, id);
            return ResponseEntity.ok().build();
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }

    @PostMapping("/{id}/image")
    public ResponseEntity<Pet> uploadPetImage(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id, @RequestParam("image") MultipartFile file) {
        if (jwt == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            return ResponseEntity.ok(petService.uploadPetImage(jwt, id, file));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }
}

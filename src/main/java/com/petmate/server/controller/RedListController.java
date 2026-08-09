package com.petmate.server.controller;

import com.petmate.server.dto.RedListSpeciesDto;
import com.petmate.server.entity.Pet;
import com.petmate.server.entity.RedListSpecies;
import com.petmate.server.enums.AdStatus;
import com.petmate.server.service.AdminService;
import com.petmate.server.service.PetService;
import com.petmate.server.service.RedListService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/admin/red-list")
@RequiredArgsConstructor
public class RedListController {

    private final RedListService redListService;
    private final PetService petService;
    private final AdminService adminService;

    private void checkAdmin(Jwt jwt) {
        adminService.getAllUsers(jwt); // throws if not admin
    }

    @GetMapping
    public ResponseEntity<List<RedListSpecies>> getAllSpecies(@AuthenticationPrincipal Jwt jwt) {
        try {
            checkAdmin(jwt);
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
        try {
            return ResponseEntity.ok(redListService.getAllSpecies());
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }

    @PostMapping
    public ResponseEntity<RedListSpecies> addSpecies(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody RedListSpeciesDto dto) {
        try {
            checkAdmin(jwt);
            return ResponseEntity.ok(redListService.addSpecies(dto));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<RedListSpecies> updateSpecies(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id,
            @Valid @RequestBody RedListSpeciesDto dto) {
        try {
            checkAdmin(jwt);
            return ResponseEntity.ok(redListService.updateSpecies(id, dto));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSpecies(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        try {
            checkAdmin(jwt);
            redListService.deleteSpecies(id);
            return ResponseEntity.ok().build();
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }

    @GetMapping("/pending")
    public ResponseEntity<List<Pet>> getPendingRedListPets(@AuthenticationPrincipal Jwt jwt) {
        try {
            checkAdmin(jwt);
            return ResponseEntity.ok(redListService.getPendingRedListPets());
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }

    @PutMapping("/pets/{petId}/approve")
    public ResponseEntity<Pet> approveRedListPet(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long petId) {
        try {
            checkAdmin(jwt);
            return ResponseEntity.ok(petService.updatePetStatus(jwt, petId, AdStatus.AVAILABLE));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }

    @PutMapping("/pets/{petId}/reject")
    public ResponseEntity<Pet> rejectRedListPet(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long petId) {
        try {
            checkAdmin(jwt);
            return ResponseEntity.ok(petService.updatePetStatus(jwt, petId, AdStatus.REJECTED));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }
}

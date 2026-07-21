package com.petmate.server.controller;

import com.petmate.server.dto.AdoptionRequest;
import com.petmate.server.dto.AdoptionResponse;
import com.petmate.server.enums.AdoptionStatus;
import com.petmate.server.service.AdoptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/adoptions")
@RequiredArgsConstructor
public class AdoptionController {

    private final AdoptionService adoptionService;

    @PostMapping("/apply")
    public ResponseEntity<AdoptionResponse> applyForAdoption(
            @AuthenticationPrincipal Jwt jwt, 
            @RequestBody AdoptionRequest request) {
        if (jwt == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            return ResponseEntity.ok(adoptionService.applyForAdoption(jwt, request));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }

    @GetMapping("/my-applications")
    public ResponseEntity<List<AdoptionResponse>> getMyApplications(@AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            return ResponseEntity.ok(adoptionService.getMyApplications(jwt));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }

    @GetMapping("/received")
    public ResponseEntity<List<AdoptionResponse>> getReceivedApplications(@AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            return ResponseEntity.ok(adoptionService.getReceivedApplications(jwt));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<AdoptionResponse> updateApplicationStatus(
            @AuthenticationPrincipal Jwt jwt, 
            @PathVariable Long id, 
            @RequestParam AdoptionStatus status) {
        if (jwt == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            return ResponseEntity.ok(adoptionService.updateApplicationStatus(jwt, id, status));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelAdoptionApplication(
            @AuthenticationPrincipal Jwt jwt, 
            @PathVariable Long id) {
        if (jwt == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            adoptionService.cancelAdoptionApplication(jwt, id);
            return ResponseEntity.ok().build();
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }
}

package com.petmate.server.controller;

import com.petmate.server.dto.InteractionDto.*;
import com.petmate.server.service.InteractionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/pets/{petId}")
@RequiredArgsConstructor
public class InteractionController {

    private final InteractionService interactionService;

    // --- SAVES ---

    @GetMapping("/save-status")
    public ResponseEntity<SaveStatusResponse> getSaveStatus(
            @PathVariable Long petId,
            @AuthenticationPrincipal Jwt jwt) {
        try {
            return ResponseEntity.ok(interactionService.getSaveStatus(petId, jwt));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }

    @PostMapping("/save")
    public ResponseEntity<SaveStatusResponse> toggleSave(
            @PathVariable Long petId,
            @AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            return ResponseEntity.ok(interactionService.toggleSave(petId, jwt));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }

    // --- LIKES ---

    @GetMapping("/like-status")
    public ResponseEntity<LikeStatusResponse> getLikeStatus(
            @PathVariable Long petId,
            @AuthenticationPrincipal Jwt jwt) {
        try {
            return ResponseEntity.ok(interactionService.getLikeStatus(petId, jwt));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }

    @PostMapping("/like")
    public ResponseEntity<LikeStatusResponse> toggleLike(
            @PathVariable Long petId,
            @AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            return ResponseEntity.ok(interactionService.toggleLike(petId, jwt));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }
}

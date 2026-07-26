package com.petmate.server.controller;

import com.petmate.server.entity.User;
import com.petmate.server.entity.SystemLog;
import com.petmate.server.dto.SystemStatsDto;
import com.petmate.server.enums.RoleType;
import com.petmate.server.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/users/pending-rescue")
    public ResponseEntity<List<User>> getPendingRescueOrgs(@AuthenticationPrincipal Jwt jwt) {
        try {
            return ResponseEntity.ok(adminService.getPendingRescueOrgs(jwt));
        } catch (ResponseStatusException e) { throw e; }
    }

    @PutMapping("/users/{id}/approve-rescue")
    public ResponseEntity<User> approveRescueOrg(
            @AuthenticationPrincipal Jwt jwt, 
            @PathVariable Long id,
            @RequestParam boolean approve) {
        try {
            return ResponseEntity.ok(adminService.approveRescueOrg(jwt, id, approve));
        } catch (ResponseStatusException e) { throw e; }
    }

    @PostMapping("/broadcast")
    public ResponseEntity<Void> sendBroadcast(
            @AuthenticationPrincipal Jwt jwt, 
            @RequestBody Map<String, String> payload) {
        try {
            adminService.sendBroadcast(jwt, payload);
            return ResponseEntity.ok().build();
        } catch (ResponseStatusException e) { throw e; }
    }

    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers(@AuthenticationPrincipal Jwt jwt) {
        try {
            return ResponseEntity.ok(adminService.getAllUsers(jwt));
        } catch (ResponseStatusException e) { throw e; }
    }

    @PutMapping("/users/{id}/status")
    public ResponseEntity<User> updateUserStatus(
            @AuthenticationPrincipal Jwt jwt, 
            @PathVariable Long id,
            @RequestParam String status) {
        try {
            return ResponseEntity.ok(adminService.updateUserStatus(jwt, id, status));
        } catch (ResponseStatusException e) { throw e; }
    }

    @PutMapping("/users/{id}/role")
    public ResponseEntity<User> updateUserRole(
            @AuthenticationPrincipal Jwt jwt, 
            @PathVariable Long id,
            @RequestParam RoleType role) {
        try {
            return ResponseEntity.ok(adminService.updateUserRole(jwt, id, role));
        } catch (ResponseStatusException e) { throw e; }
    }

    @GetMapping("/stats")
    public ResponseEntity<SystemStatsDto> getSystemStats(@AuthenticationPrincipal Jwt jwt) {
        try {
            return ResponseEntity.ok(adminService.getSystemStats(jwt));
        } catch (ResponseStatusException e) { throw e; }
    }

    @GetMapping("/logs")
    public ResponseEntity<List<SystemLog>> getSystemLogs(@AuthenticationPrincipal Jwt jwt) {
        try {
            return ResponseEntity.ok(adminService.getSystemLogs(jwt));
        } catch (ResponseStatusException e) { throw e; }
    }
}


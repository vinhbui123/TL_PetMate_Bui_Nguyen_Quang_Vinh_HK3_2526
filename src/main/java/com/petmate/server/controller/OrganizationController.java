package com.petmate.server.controller;

import com.petmate.server.dto.*;
import com.petmate.server.service.OrganizationService;
import com.petmate.server.service.UserService;
import com.petmate.server.entity.User;
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
@RequestMapping("/api/orgs")
@RequiredArgsConstructor
public class OrganizationController {

    private final OrganizationService organizationService;
    private final UserService userService;

    @PostMapping
    public ResponseEntity<OrganizationProfileDto> registerOrg(@AuthenticationPrincipal Jwt jwt,
                                                              @RequestBody OrganizationProfileDto dto) {
        if (jwt == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            User user = userService.getCurrentUserAndUpdateActivity(jwt);
            OrganizationProfileDto created = organizationService.createOrganization(user.getId(), dto);
            return ResponseEntity.ok(created);
        } catch (ResponseStatusException e) { throw e; }
    }

    @PutMapping("/{id}")
    public ResponseEntity<OrganizationProfileDto> updateOrg(@AuthenticationPrincipal Jwt jwt,
                                                             @PathVariable Long id,
                                                             @RequestBody OrganizationProfileDto dto) {
        if (jwt == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            User user = userService.getCurrentUserAndUpdateActivity(jwt);
            return ResponseEntity.ok(organizationService.updateOrganization(id, user.getId(), dto));
        } catch (ResponseStatusException e) { throw e; }
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrganizationProfileDto> getOrg(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(organizationService.getOrganization(id));
        } catch (ResponseStatusException e) { throw e; }
    }

    @GetMapping("/my")
    public ResponseEntity<OrganizationProfileDto> getMyOrg(@AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            User user = userService.getCurrentUserAndUpdateActivity(jwt);
            return ResponseEntity.ok(organizationService.getMyOrganization(user.getId()));
        } catch (ResponseStatusException e) { throw e; }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<OrganizationProfileDto> getOrgByUserId(@PathVariable Long userId) {
        try {
            return ResponseEntity.ok(organizationService.getMyOrganization(userId));
        } catch (ResponseStatusException e) { throw e; }
    }

    @PostMapping("/{id}/documents")
    public ResponseEntity<OrgDocumentDto> uploadDocument(@AuthenticationPrincipal Jwt jwt,
                                                         @PathVariable Long id,
                                                         @RequestParam String docType,
                                                         @RequestParam("file") MultipartFile file) {
        if (jwt == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            User user = userService.getCurrentUserAndUpdateActivity(jwt);
            return ResponseEntity.ok(organizationService.uploadDocument(id, user.getId(), docType, file));
        } catch (ResponseStatusException e) { throw e; }
    }

    @PostMapping("/{id}/logo")
    public ResponseEntity<OrganizationProfileDto> uploadLogo(@AuthenticationPrincipal Jwt jwt,
                                                             @PathVariable Long id,
                                                             @RequestParam("file") MultipartFile file) {
        if (jwt == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            User user = userService.getCurrentUserAndUpdateActivity(jwt);
            return ResponseEntity.ok(organizationService.uploadLogo(id, user.getId(), file));
        } catch (ResponseStatusException e) { throw e; }
    }

    @DeleteMapping("/{id}/documents/{docId}")
    public ResponseEntity<Void> deleteDocument(@AuthenticationPrincipal Jwt jwt,
                                               @PathVariable Long id,
                                               @PathVariable Long docId) {
        if (jwt == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            User user = userService.getCurrentUserAndUpdateActivity(jwt);
            organizationService.deleteDocument(id, docId, user.getId());
            return ResponseEntity.ok().build();
        } catch (ResponseStatusException e) { throw e; }
    }

    @PostMapping("/{id}/members")
    public ResponseEntity<OrgMemberDto> inviteMember(@AuthenticationPrincipal Jwt jwt,
                                                     @PathVariable Long id,
                                                     @RequestBody InviteMemberDto dto) {
        if (jwt == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            User user = userService.getCurrentUserAndUpdateActivity(jwt);
            return ResponseEntity.ok(organizationService.inviteMember(id, user.getId(), dto));
        } catch (ResponseStatusException e) { throw e; }
    }

    @PostMapping("/{id}/members/{memberId}/accept")
    public ResponseEntity<OrgMemberDto> acceptInvitation(@AuthenticationPrincipal Jwt jwt,
                                                         @PathVariable Long id,
                                                         @PathVariable Long memberId) {
        if (jwt == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            User user = userService.getCurrentUserAndUpdateActivity(jwt);
            return ResponseEntity.ok(organizationService.acceptInvitation(id, memberId, user.getId()));
        } catch (ResponseStatusException e) { throw e; }
    }

    @PostMapping("/{id}/members/{memberId}/reject")
    public ResponseEntity<Void> rejectInvitation(@AuthenticationPrincipal Jwt jwt,
                                                 @PathVariable Long id,
                                                 @PathVariable Long memberId) {
        if (jwt == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            User user = userService.getCurrentUserAndUpdateActivity(jwt);
            organizationService.rejectInvitation(id, memberId, user.getId());
            return ResponseEntity.ok().build();
        } catch (ResponseStatusException e) { throw e; }
    }

    @DeleteMapping("/{id}/members/{memberId}")
    public ResponseEntity<Void> removeMember(@AuthenticationPrincipal Jwt jwt,
                                             @PathVariable Long id,
                                             @PathVariable Long memberId) {
        if (jwt == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            User user = userService.getCurrentUserAndUpdateActivity(jwt);
            organizationService.removeMember(id, user.getId(), memberId);
            return ResponseEntity.ok().build();
        } catch (ResponseStatusException e) { throw e; }
    }

    @GetMapping("/{id}/members")
    public ResponseEntity<List<OrgMemberDto>> getMembers(@AuthenticationPrincipal Jwt jwt,
                                                         @PathVariable Long id) {
        if (jwt == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return ResponseEntity.ok(organizationService.getMembers(id));
    }

    @DeleteMapping("/{id}/leave")
    public ResponseEntity<Void> leaveOrganization(@AuthenticationPrincipal Jwt jwt,
                                                  @PathVariable Long id) {
        if (jwt == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        User user = userService.getCurrentUserAndUpdateActivity(jwt);
        organizationService.leaveOrganization(id, user.getId());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> dissolveOrganization(@AuthenticationPrincipal Jwt jwt,
                                                     @PathVariable Long id) {
        if (jwt == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        User user = userService.getCurrentUserAndUpdateActivity(jwt);
        organizationService.dissolveOrganization(id, user.getId());
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/transfer-ownership/{newOwnerId}")
    public ResponseEntity<Void> transferOwnership(@AuthenticationPrincipal Jwt jwt,
                                                  @PathVariable Long id,
                                                  @PathVariable Long newOwnerId) {
        if (jwt == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        User user = userService.getCurrentUserAndUpdateActivity(jwt);
        organizationService.transferOwnershipAndLeave(id, user.getId(), newOwnerId);
        return ResponseEntity.ok().build();
    }

    // Admin: list by status
    @GetMapping
    public ResponseEntity<List<OrganizationProfileDto>> listByStatus(@RequestParam(required = false) String status) {
        try {
            String s = (status == null) ? "PENDING" : status;
            return ResponseEntity.ok(organizationService.listByStatus(s));
        } catch (ResponseStatusException e) { throw e; }
    }

    @PutMapping("/{id}/review")
    public ResponseEntity<OrganizationProfileDto> reviewOrg(@PathVariable Long id, 
                                                            @RequestBody OrgReviewRequestDto dto) {
        try {
            return ResponseEntity.ok(organizationService.reviewOrganization(id, dto));
        } catch (ResponseStatusException e) { throw e; }
    }
}


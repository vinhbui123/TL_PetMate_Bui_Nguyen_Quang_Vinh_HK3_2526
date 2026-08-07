package com.petmate.server.controller;

import com.petmate.server.dto.ChangePasswordDto;
import com.petmate.server.dto.RatingRequestDto;
import com.petmate.server.dto.RatingResponseDto;
import com.petmate.server.dto.SellerRatingSummaryDto;
import com.petmate.server.dto.UserProfileDto;
import jakarta.validation.Valid;
import com.petmate.server.entity.User;
import com.petmate.server.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/sync")
    public ResponseEntity<User> syncUser(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody(required = false) Map<String, String> body) {
        if (jwt == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            return ResponseEntity.ok(userService.syncUser(jwt, body));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }

    @GetMapping("/me")
    public ResponseEntity<User> getCurrentUser(@AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            return ResponseEntity.ok(userService.getCurrentUserAndUpdateActivity(jwt));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }

    @PostMapping("/revoke-tokens")
    public ResponseEntity<Void> revokeTokens(@AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            userService.revokeTokens(jwt);
            return ResponseEntity.ok().build();
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }

    @PutMapping("/me")
    public ResponseEntity<User> updateProfile(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody UserProfileDto dto) {
        if (jwt == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            return ResponseEntity.ok(userService.updateProfile(jwt, dto));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }

    @PostMapping("/avatar")
    public ResponseEntity<User> uploadAvatar(@AuthenticationPrincipal Jwt jwt,
            @RequestParam("image") MultipartFile file) {
        if (jwt == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            return ResponseEntity.ok(userService.uploadAvatar(jwt, file));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }

    @PutMapping("/location")
    public ResponseEntity<User> updateLocation(@AuthenticationPrincipal Jwt jwt,
            @RequestBody Map<String, Double> body) {
        if (jwt == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            return ResponseEntity.ok(userService.updateLocation(jwt, body));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }

    @PutMapping("/request-rescue-org")
    public ResponseEntity<User> requestRescueOrg(@AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            return ResponseEntity.ok(userService.requestRescueOrg(jwt));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }

    @PostMapping("/fcm-token")
    public ResponseEntity<?> registerFcmToken(@AuthenticationPrincipal Jwt jwt,
            @RequestBody Map<String, String> body) {
        if (jwt == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            userService.registerFcmToken(jwt, body);
            return ResponseEntity.ok().build();
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getReason());
        }
    }

    @DeleteMapping("/fcm-token")
    public ResponseEntity<?> removeFcmToken(@AuthenticationPrincipal Jwt jwt,
            @RequestParam String token) {
        if (jwt == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            userService.removeFcmToken(jwt, token);
            return ResponseEntity.ok().build();
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }

    @DeleteMapping("/fcm-token/all")
    public ResponseEntity<?> removeAllFcmTokens(@AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            userService.removeAllFcmTokens(jwt);
            return ResponseEntity.ok().build();
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }

    @DeleteMapping("/account")
    public ResponseEntity<Void> deleteAccount(@AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            userService.deleteAccount(jwt);
            return ResponseEntity.ok().build();
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }

    @PostMapping("/blocks/{blockedId}")
    public ResponseEntity<Void> blockUser(@AuthenticationPrincipal Jwt jwt, @PathVariable Long blockedId) {
        if (jwt == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            userService.blockUser(jwt, blockedId);
            return ResponseEntity.ok().build();
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }

    @DeleteMapping("/blocks/{blockedId}")
    public ResponseEntity<Void> unblockUser(@AuthenticationPrincipal Jwt jwt, @PathVariable Long blockedId) {
        if (jwt == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            userService.unblockUser(jwt, blockedId);
            return ResponseEntity.ok().build();
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }

    @GetMapping("/blocks")
    public ResponseEntity<List<Long>> getBlockedUsers(@AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            return ResponseEntity.ok(userService.getBlockedUsers(jwt));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }

    @GetMapping("/blocks/details")
    public ResponseEntity<List<User>> getBlockedUserDetails(@AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            return ResponseEntity.ok(userService.getBlockedUserDetails(jwt));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }

    @PostMapping("/follows/{followedId}")
    public ResponseEntity<Void> followUser(@AuthenticationPrincipal Jwt jwt, @PathVariable Long followedId) {
        if (jwt == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            userService.followUser(jwt, followedId);
            return ResponseEntity.ok().build();
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }

    @DeleteMapping("/follows/{followedId}")
    public ResponseEntity<Void> unfollowUser(@AuthenticationPrincipal Jwt jwt, @PathVariable Long followedId) {
        if (jwt == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            userService.unfollowUser(jwt, followedId);
            return ResponseEntity.ok().build();
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }

    @GetMapping("/follows/status/{followedId}")
    public ResponseEntity<Boolean> checkFollowStatus(@AuthenticationPrincipal Jwt jwt, @PathVariable Long followedId) {
        if (jwt == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            return ResponseEntity.ok(userService.checkFollowStatus(jwt, followedId));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }

    @GetMapping("/follows/{userId}/stats")
    public ResponseEntity<Map<String, Long>> getUserFollowStats(@PathVariable Long userId) {
        try {
            return ResponseEntity.ok(userService.getUserFollowStats(userId));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }

    @GetMapping("/follows/{userId}/followers")
    public ResponseEntity<List<User>> getFollowers(@PathVariable Long userId) {
        return ResponseEntity.ok(userService.getFollowers(userId));
    }

    @GetMapping("/follows/{userId}/following")
    public ResponseEntity<List<User>> getFollowing(@PathVariable Long userId) {
        return ResponseEntity.ok(userService.getFollowing(userId));
    }

    @PostMapping("/{userId}/rate")
    public ResponseEntity<RatingResponseDto> rateUser(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long userId,
            @Valid @RequestBody RatingRequestDto dto) {
        if (jwt == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            return ResponseEntity.ok(userService.rateUser(jwt, userId, dto));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }

    @GetMapping("/{userId}/ratings/summary")
    public ResponseEntity<SellerRatingSummaryDto> getSellerRatingSummary(
            @PathVariable Long userId,
            @AuthenticationPrincipal Jwt jwt) {
        try {
            return ResponseEntity.ok(userService.getSellerRatingSummary(jwt, userId));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }

    @GetMapping("/{userId}/ratings")
    public ResponseEntity<List<RatingResponseDto>> getSellerReviews(@PathVariable Long userId) {
        try {
            return ResponseEntity.ok(userService.getSellerReviews(userId));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }

    @GetMapping("/{userId}/rating-status")
    public ResponseEntity<Boolean> checkRatingStatus(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long userId) {
        if (jwt == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            return ResponseEntity.ok(userService.checkRatingStatus(jwt, userId));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }

    @DeleteMapping("/ratings/{ratingId}")
    public ResponseEntity<Void> deleteRating(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long ratingId) {
        if (jwt == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            userService.deleteRating(jwt, ratingId);
            return ResponseEntity.ok().build();
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }

    @PostMapping("/change-password")
    public ResponseEntity<Map<String, String>> changePassword(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ChangePasswordDto dto) {
        if (jwt == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            userService.changePassword(jwt, dto);
            return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(Map.of("error", e.getReason()));
        }
    }
}

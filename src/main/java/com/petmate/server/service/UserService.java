package com.petmate.server.service;

import com.petmate.server.dto.RatingRequestDto;
import com.petmate.server.dto.RatingResponseDto;
import com.petmate.server.dto.SellerRatingSummaryDto;
import com.petmate.server.dto.UserProfileDto;
import com.petmate.server.entity.FcmToken;
import com.petmate.server.entity.Pet;
import com.petmate.server.entity.User;
import com.petmate.server.entity.UserBlock;
import com.petmate.server.entity.UserFollow;
import com.petmate.server.entity.UserRating;
import com.petmate.server.enums.AdStatus;
import com.petmate.server.enums.AdoptionStatus;
import com.petmate.server.enums.RoleType;
import com.petmate.server.repository.AdoptionApplicationRepository;
import com.petmate.server.repository.FcmTokenRepository;
import com.petmate.server.repository.PetRepository;
import com.petmate.server.repository.UserBlockRepository;
import com.petmate.server.repository.UserFollowRepository;
import com.petmate.server.repository.UserRatingRepository;
import com.petmate.server.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final CloudinaryService cloudinaryService;
    private final FcmTokenRepository fcmTokenRepository;
    private final PetRepository petRepository;
    private final UserBlockRepository userBlockRepository;
    private final UserFollowRepository userFollowRepository;
    private final UserRatingRepository userRatingRepository;
    private final AdoptionApplicationRepository adoptionApplicationRepository;

    public Optional<User> findCurrentUser(Jwt jwt) {
        String uid = jwt.getSubject();
        return userRepository.findByProviderId(uid)
                .or(() -> Optional.ofNullable(jwt.getClaimAsString("email"))
                        .filter(e -> !e.isEmpty())
                        .flatMap(userRepository::findByEmail));
    }

    public User getCurrentUserOrThrow(Jwt jwt) {
        return findCurrentUser(jwt)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private String extractValidString(String... values) {
        return Stream.of(values)
                .filter(v -> v != null && !v.trim().isEmpty() && !v.equals("null"))
                .findFirst()
                .orElse(null);
    }

    public User syncUser(Jwt jwt, Map<String, String> body) {
        String uid = jwt.getSubject();
        
        Map<String, Object> firebaseClaim = Optional.ofNullable(jwt.getClaimAsMap("firebase")).orElse(new HashMap<>());
        String provider = (String) firebaseClaim.get("sign_in_provider");
        
        String identitiesEmail = null;
        try {
            Map<String, Object> identities = (Map<String, Object>) firebaseClaim.get("identities");
            List<String> emails = (List<String>) identities.get("email");
            identitiesEmail = (emails != null && !emails.isEmpty()) ? emails.get(0) : null;
        } catch (Exception ignored) {}

        Map<String, String> safeBody = Optional.ofNullable(body).orElse(new HashMap<>());
        
        String email = extractValidString(jwt.getClaimAsString("email"), identitiesEmail, safeBody.get("email"), provider + "_" + uid + "@petmate.com");
        String name = extractValidString(jwt.getClaimAsString("name"), safeBody.get("fullName"), "Unknown User");
        String avatarUrl = extractValidString(jwt.getClaimAsString("picture"), safeBody.get("avatarUrl"));

        boolean emailVerified = Boolean.TRUE.equals(jwt.getClaimAsBoolean("email_verified"));
        boolean isSocialLogin = provider != null && !provider.equals("password");
        String currentStatus = (emailVerified || isSocialLogin) ? "ACTIVED" : "PENDING";

        User user = userRepository.findByProviderId(uid)
                .or(() -> userRepository.findByEmail(email))
                .orElseGet(() -> User.builder()
                        .email(email)
                        .fullName(name)
                        .provider(provider != null ? provider : "firebase")
                        .providerId(uid)
                        .role(RoleType.MEMBER)
                        .build());

        // Handle providerId update & conflict resolution
        if (user.getId() != null && !uid.equals(user.getProviderId())) {
            userRepository.findByProviderId(uid)
                    .filter(conflictUser -> !conflictUser.getId().equals(user.getId()))
                    .ifPresent(conflictUser -> {
                        conflictUser.setProviderId(null);
                        userRepository.save(conflictUser);
                    });
            user.setProviderId(uid);
        }

        Optional.ofNullable(provider).ifPresent(user::setProvider);
        Optional.ofNullable(name).ifPresent(user::setFullName);
        Optional.ofNullable(email).ifPresent(user::setEmail);
        
        // Update avatar only if not present
        if (user.getAvatarUrl() == null || user.getAvatarUrl().trim().isEmpty()) {
            Optional.ofNullable(avatarUrl).ifPresent(user::setAvatarUrl);
        }

        if (user.getStatus() == null || "PENDING".equals(user.getStatus())) {
            user.setStatus(currentStatus);
        }

        if ("DELETED".equals(user.getStatus()) || "BANNED".equals(user.getStatus())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account is disabled");
        }

        return userRepository.save(user);
    }

    public User updateProfile(Jwt jwt, UserProfileDto dto) {
        User user = getCurrentUserOrThrow(jwt);
        Optional.ofNullable(dto.getFullName()).ifPresent(user::setFullName);
        Optional.ofNullable(dto.getPhone()).ifPresent(user::setPhone);
        Optional.ofNullable(dto.getAddress()).ifPresent(user::setAddress);
        Optional.ofNullable(dto.getAvatarUrl()).ifPresent(user::setAvatarUrl);
        return userRepository.save(user);
    }

    public User uploadAvatar(Jwt jwt, MultipartFile file) {
        User user = getCurrentUserOrThrow(jwt);
        try {
            user.setAvatarUrl(cloudinaryService.uploadImage(file));
            return userRepository.save(user);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error uploading avatar", e);
        }
    }

    public User updateLocation(Jwt jwt, Map<String, Double> body) {
        User user = getCurrentUserOrThrow(jwt);
        return Optional.ofNullable(body.get("latitude"))
                .flatMap(lat -> Optional.ofNullable(body.get("longitude")).map(lng -> {
                    user.setLatitude(lat);
                    user.setLongitude(lng);
                    return userRepository.save(user);
                }))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing latitude or longitude"));
    }

    public User requestRescueOrg(Jwt jwt) {
        User user = getCurrentUserOrThrow(jwt);
        return Optional.of(user)
                .filter(u -> u.getRole() == RoleType.MEMBER)
                .map(u -> {
                    u.setRole(RoleType.PENDING_RESCUE);
                    return userRepository.save(u);
                })
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "User is not a MEMBER"));
    }

    public void registerFcmToken(Jwt jwt, Map<String, String> body) {
        User user = getCurrentUserOrThrow(jwt);
        Optional.ofNullable(body.get("token"))
                .filter(token -> !token.trim().isEmpty())
                .map(token -> FcmToken.builder().token(token).user(user).build())
                .ifPresentOrElse(fcmTokenRepository::save, 
                        () -> { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token is missing"); });
    }

    public void removeFcmToken(Jwt jwt, String token) {
        getCurrentUserOrThrow(jwt);
        fcmTokenRepository.findById(token).ifPresent(fcmTokenRepository::delete);
    }

    public void deleteAccount(Jwt jwt) {
        User user = getCurrentUserOrThrow(jwt);
        user.setStatus("DELETED");
        userRepository.save(user);
        
        List<Pet> userPets = petRepository.findByUserId(user.getId());
        userPets.forEach(pet -> pet.setStatus(AdStatus.HIDDEN));
        petRepository.saveAll(userPets);
    }

    public void blockUser(Jwt jwt, Long blockedId) {
        User blocker = getCurrentUserOrThrow(jwt);
        if (blocker.getId().equals(blockedId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot block yourself");
        }
        User blockedUser = userRepository.findById(blockedId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Target user not found"));
        
        if (!userBlockRepository.existsByBlockerIdAndBlockedId(blocker.getId(), blockedId)) {
            userBlockRepository.save(UserBlock.builder().blocker(blocker).blocked(blockedUser).build());
        }
    }

    public void unblockUser(Jwt jwt, Long blockedId) {
        User blocker = getCurrentUserOrThrow(jwt);
        userBlockRepository.findByBlockerIdAndBlockedId(blocker.getId(), blockedId)
                .ifPresent(userBlockRepository::delete);
    }

    public List<Long> getBlockedUsers(Jwt jwt) {
        User current = getCurrentUserOrThrow(jwt);
        List<Long> blockedByMe = userBlockRepository.findByBlockerId(current.getId())
            .stream().map(b -> b.getBlocked().getId()).collect(Collectors.toList());
        List<Long> blockedMe = userBlockRepository.findByBlockedId(current.getId())
            .stream().map(b -> b.getBlocker().getId()).collect(Collectors.toList());
            
        return Stream.concat(blockedByMe.stream(), blockedMe.stream())
                .distinct()
                .collect(Collectors.toList());
    }

    public List<User> getBlockedUserDetails(Jwt jwt) {
        User current = getCurrentUserOrThrow(jwt);
        return userBlockRepository.findByBlockerId(current.getId())
            .stream().map(UserBlock::getBlocked).collect(Collectors.toList());
    }

    public void followUser(Jwt jwt, Long followedId) {
        User follower = getCurrentUserOrThrow(jwt);
        if (follower.getId().equals(followedId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot follow yourself");
        }
        User followedUser = userRepository.findById(followedId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Target user not found"));
                
        if (!userFollowRepository.existsByFollowerIdAndFollowedId(follower.getId(), followedId)) {
            userFollowRepository.save(UserFollow.builder().follower(follower).followed(followedUser).build());
        }
    }

    public void unfollowUser(Jwt jwt, Long followedId) {
        User follower = getCurrentUserOrThrow(jwt);
        userFollowRepository.findByFollowerIdAndFollowedId(follower.getId(), followedId)
                .ifPresent(userFollowRepository::delete);
    }

    public boolean checkFollowStatus(Jwt jwt, Long followedId) {
        User follower = getCurrentUserOrThrow(jwt);
        return userFollowRepository.existsByFollowerIdAndFollowedId(follower.getId(), followedId);
    }

    public Map<String, Long> getUserFollowStats(Long userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        Map<String, Long> stats = new HashMap<>();
        stats.put("followers", userFollowRepository.countByFollowedId(userId));
        stats.put("following", userFollowRepository.countByFollowerId(userId));
        return stats;
    }

    public List<User> getFollowers(Long userId) {
        return userFollowRepository.findFollowersByUserId(userId);
    }

    public List<User> getFollowing(Long userId) {
        return userFollowRepository.findFollowingByUserId(userId);
    }

    public RatingResponseDto rateUser(Jwt jwt, Long userId, RatingRequestDto dto) {
        if (dto.getScore() == null || dto.getScore() < 1 || dto.getScore() > 5) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Score must be between 1 and 5");
        }

        User rater = getCurrentUserOrThrow(jwt);
        User ratedUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rated user not found"));
        
        if (rater.getId().equals(ratedUser.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot rate yourself");
        }

        Pet pet = petRepository.findById(dto.getPetId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pet not found"));

        if (!pet.getUser().getId().equals(ratedUser.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pet does not belong to the rated user");
        }

        // Kiểm tra: chỉ người mua/nhận nuôi đã được duyệt (APPROVED) mới có thể đánh giá người bán
        boolean hasApprovedAdoption = adoptionApplicationRepository
                .existsByApplicantIdAndPetIdAndStatus(rater.getId(), pet.getId(), AdoptionStatus.APPROVED);
        if (!hasApprovedAdoption) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn chỉ có thể đánh giá sau khi nhận nuôi thú cưng này thành công");
        }

        UserRating rating = userRatingRepository.findByRaterIdAndRatedUserId(rater.getId(), ratedUser.getId())
                .orElseGet(() -> UserRating.builder().rater(rater).ratedUser(ratedUser).build());
                
        rating.setScore(dto.getScore());
        rating.setComment(dto.getComment());
        rating.setPet(pet);
        rating = userRatingRepository.save(rating);

        Double newAvg = Optional.ofNullable(userRatingRepository.getAverageScoreByRatedUserId(ratedUser.getId())).orElse(0.0);
        Integer newCount = Optional.ofNullable(userRatingRepository.countByRatedUserId(ratedUser.getId())).orElse(0);
        
        ratedUser.setAverageRating(Math.round(newAvg * 10.0) / 10.0);
        ratedUser.setRatingCount(newCount);
        userRepository.save(ratedUser);
        
        return mapToRatingResponseDto(rating);
    }

    private RatingResponseDto mapToRatingResponseDto(UserRating rating) {
        RatingResponseDto dto = RatingResponseDto.builder()
                .id(rating.getId())
                .raterId(rating.getRater().getId())
                .raterName(rating.getRater().getFullName())
                .raterAvatarUrl(rating.getRater().getAvatarUrl())
                .score(rating.getScore())
                .comment(rating.getComment())
                .createdAt(rating.getCreatedAt())
                .build();
                
        if (rating.getPet() != null) {
            dto.setPetId(rating.getPet().getId());
            dto.setPetName(rating.getPet().getName());
            dto.setPetPrice(rating.getPet().getPrice());
            dto.setPetImageUrl(rating.getPet().getImageUrl());
        }
        return dto;
    }

    public SellerRatingSummaryDto getSellerRatingSummary(Jwt jwt, Long sellerId) {
        User seller = userRepository.findById(sellerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Seller not found"));
        
        List<Object[]> distributionResult = userRatingRepository.countByRatedUserIdGroupByScore(sellerId);
        Map<Integer, Integer> distribution = new HashMap<>();
        for (int i = 1; i <= 5; i++) distribution.put(i, 0);
        
        for (Object[] row : distributionResult) {
            Double score = (Double) row[0];
            Long count = (Long) row[1];
            if (score != null) {
                int roundedScore = (int) Math.round(score);
                if (roundedScore >= 1 && roundedScore <= 5) {
                    distribution.put(roundedScore, distribution.get(roundedScore) + count.intValue());
                }
            }
        }
        
        Boolean currentUserHasRated = false;
        RatingResponseDto currentUserRating = null;
        
        if (jwt != null) {
            Optional<User> currentUserOpt = findCurrentUser(jwt);
            if (currentUserOpt.isPresent()) {
                Long currentUserId = currentUserOpt.get().getId();
                currentUserHasRated = userRatingRepository.existsByRaterIdAndRatedUserId(currentUserId, sellerId);
                if (currentUserHasRated) {
                    Optional<UserRating> ratingOpt = userRatingRepository.findByRaterIdAndRatedUserId(currentUserId, sellerId);
                    if (ratingOpt.isPresent()) {
                        currentUserRating = mapToRatingResponseDto(ratingOpt.get());
                    }
                }
            }
        }
        
        List<UserRating> recentRatings = userRatingRepository.findByRatedUserIdOrderByCreatedAtDesc(sellerId);
        List<RatingResponseDto> recentReviews = recentRatings.stream()
                .limit(50)
                .map(this::mapToRatingResponseDto)
                .collect(Collectors.toList());
                
        return SellerRatingSummaryDto.builder()
                .sellerId(seller.getId())
                .sellerName(seller.getFullName())
                .averageRating(seller.getAverageRating())
                .totalReviews(seller.getRatingCount())
                .ratingDistribution(distribution)
                .currentUserHasRated(currentUserHasRated)
                .currentUserRating(currentUserRating)
                .recentReviews(recentReviews)
                .build();
    }

    public List<RatingResponseDto> getSellerReviews(Long sellerId) {
        if (!userRepository.existsById(sellerId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Seller not found");
        }
        return userRatingRepository.findByRatedUserIdOrderByCreatedAtDesc(sellerId)
                .stream()
                .map(this::mapToRatingResponseDto)
                .collect(Collectors.toList());
    }

    public boolean checkRatingStatus(Jwt jwt, Long sellerId) {
        User user = getCurrentUserOrThrow(jwt);
        return userRatingRepository.existsByRaterIdAndRatedUserId(user.getId(), sellerId);
    }

    public void deleteRating(Jwt jwt, Long ratingId) {
        User user = getCurrentUserOrThrow(jwt);
        UserRating rating = userRatingRepository.findById(ratingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rating not found"));
                
        if (!rating.getRater().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only delete your own ratings");
        }
        
        Long ratedUserId = rating.getRatedUser().getId();
        userRatingRepository.delete(rating);
        
        Double newAvg = Optional.ofNullable(userRatingRepository.getAverageScoreByRatedUserId(ratedUserId)).orElse(0.0);
        Integer newCount = Optional.ofNullable(userRatingRepository.countByRatedUserId(ratedUserId)).orElse(0);
        
        User ratedUser = userRepository.findById(ratedUserId).orElse(null);
        if (ratedUser != null) {
            ratedUser.setAverageRating(Math.round(newAvg * 10.0) / 10.0);
            ratedUser.setRatingCount(newCount);
            userRepository.save(ratedUser);
        }
    }
}

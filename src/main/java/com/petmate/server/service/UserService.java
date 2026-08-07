package com.petmate.server.service;

import java.time.Instant;

import com.petmate.server.dto.ChangePasswordDto;
import com.petmate.server.dto.RatingRequestDto;
import com.petmate.server.dto.RatingResponseDto;
import com.petmate.server.dto.SellerRatingSummaryDto;
import com.petmate.server.dto.UserProfileDto;
import com.petmate.server.entity.DeviceToken;
import com.petmate.server.entity.Pet;
import com.petmate.server.entity.User;
import com.petmate.server.entity.UserBlock;
import com.petmate.server.entity.UserFollow;
import com.petmate.server.entity.UserRating;
import com.petmate.server.entity.OrganizationMember;
import com.petmate.server.enums.AdStatus;
import com.petmate.server.enums.AdoptionStatus;
import com.petmate.server.enums.RoleType;
import com.petmate.server.enums.UserStatus;
import com.petmate.server.enums.OrgMemberRole;
import com.petmate.server.repository.AdoptionApplicationRepository;
import com.petmate.server.repository.DeviceTokenRepository;
import com.petmate.server.repository.OrganizationMemberRepository;
import com.petmate.server.repository.PetRepository;
import com.petmate.server.repository.UserBlockRepository;
import com.petmate.server.repository.UserFollowRepository;
import com.petmate.server.repository.UserRatingRepository;
import com.petmate.server.repository.UserRepository;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import com.google.firebase.auth.UserRecord.UpdateRequest;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
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
    private final DeviceTokenRepository deviceTokenRepository;
    private final PetRepository petRepository;
    private final UserBlockRepository userBlockRepository;
    private final UserFollowRepository userFollowRepository;
    private final UserRatingRepository userRatingRepository;
    private final AdoptionApplicationRepository adoptionApplicationRepository;
    private final OrganizationMemberRepository memberRepository;

    public Optional<User> findCurrentUser(Jwt jwt) {
        String uid = jwt.getSubject();
        return userRepository.findByProviderId(uid)
                .or(() -> Optional.ofNullable(jwt.getClaimAsString("email"))
                        .filter(e -> !e.isEmpty())
                        .flatMap(userRepository::findByEmail));
    }

    public User getCurrentUserOrThrow(Jwt jwt) {
        return findCurrentUser(jwt)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "KhÃ´ng tÃ¬m tháº¥y ngÆ°á»i dÃ¹ng"));
    }

    public User getCurrentUserAndUpdateActivity(Jwt jwt) {
        User user = getCurrentUserOrThrow(jwt);
        touchLastActive(user);
        return userRepository.save(user);
    }

    public void revokeTokens(Jwt jwt) {
        User user = getCurrentUserOrThrow(jwt);
        user.setTokensValidAfter(Instant.now());
        userRepository.save(user);
    }

    private void touchLastActive(User user) {
        user.setLastActiveAt(LocalDateTime.now());
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
        
        String realEmail = extractValidString(jwt.getClaimAsString("email"), identitiesEmail, safeBody.get("email"));
        String fallbackEmail = provider + "_" + uid + "@petmate.com";
        String email = realEmail != null ? realEmail : fallbackEmail;
        String name = extractValidString(jwt.getClaimAsString("name"), safeBody.get("fullName"), "Unknown User");
        String avatarUrl = extractValidString(jwt.getClaimAsString("picture"), safeBody.get("avatarUrl"));

        boolean emailVerified = Boolean.TRUE.equals(jwt.getClaimAsBoolean("email_verified"));
        boolean isSocialLogin = provider != null && !provider.equals("password");
        UserStatus currentStatus = (emailVerified || isSocialLogin) ? UserStatus.ACTIVE : UserStatus.PENDING;

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
        // Only update email if we have a REAL email (not fallback)
        // This prevents overwriting a real email with a fake fallback on re-login
        if (realEmail != null) {
            user.setEmail(realEmail);
        }
        
        // Update avatar only if not present
        if (user.getAvatarUrl() == null || user.getAvatarUrl().trim().isEmpty()) {
            Optional.ofNullable(avatarUrl).ifPresent(user::setAvatarUrl);
        }

        if (user.getStatus() == null || user.getStatus() == UserStatus.PENDING) {
            user.setStatus(currentStatus);
        }

        if (user.getStatus() == UserStatus.DELETED || user.getStatus() == UserStatus.BANNED) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "TÃ i khoáº£n Ä‘Ã£ bá»‹ khÃ³a");
        }

        touchLastActive(user);
        return userRepository.save(user);
    }

    public User updateProfile(Jwt jwt, UserProfileDto dto) {
        User user = getCurrentUserOrThrow(jwt);
        Optional.ofNullable(dto.getFullName()).ifPresent(user::setFullName);
        Optional.ofNullable(dto.getPhone()).ifPresent(user::setPhone);
        Optional.ofNullable(dto.getAddress()).ifPresent(user::setAddress);
        Optional.ofNullable(dto.getAvatarUrl()).ifPresent(user::setAvatarUrl);
        Optional.ofNullable(dto.getCccd()).ifPresent(user::setCccd);
        return userRepository.save(user);
    }

    public User uploadAvatar(Jwt jwt, MultipartFile file) {
        User user = getCurrentUserOrThrow(jwt);
        try {
            user.setAvatarUrl(cloudinaryService.uploadImage(file));
            touchLastActive(user);
            return userRepository.save(user);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Lá»—i khi táº£i lÃªn áº£nh Ä‘áº¡i diá»‡n", e);
        }
    }

    public User updateLocation(Jwt jwt, Map<String, Double> body) {
        User user = getCurrentUserOrThrow(jwt);
        return Optional.ofNullable(body.get("latitude"))
                .flatMap(lat -> Optional.ofNullable(body.get("longitude")).map(lng -> {
                    user.setLatitude(lat);
                    user.setLongitude(lng);
                    touchLastActive(user);
                    return userRepository.save(user);
                }))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thiáº¿u thÃ´ng tin tá»a Ä‘á»™ (vÄ© Ä‘á»™/kinh Ä‘á»™)"));
    }

    public User requestRescueOrg(Jwt jwt) {
        User user = getCurrentUserOrThrow(jwt);

        return Optional.of(user)
                .filter(u -> u.getRole() == RoleType.MEMBER)
                .map(u -> {
                    u.setRole(RoleType.PENDING_RESCUE);
                    touchLastActive(u);
                    return userRepository.save(u);
                })
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "NgÆ°á»i dÃ¹ng khÃ´ng pháº£i lÃ  thÃ nh viÃªn"));
    }

    @org.springframework.transaction.annotation.Transactional
    public void registerFcmToken(Jwt jwt, Map<String, String> body) {
        User user = getCurrentUserOrThrow(jwt);
        String token = body.get("token");
        if (token == null || token.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thiếu token xác thực");
        }
        // Xóa tất cả token cũ của user này trước khi lưu token mới
        deviceTokenRepository.deleteByUserId(user.getId());
        deviceTokenRepository.save(DeviceToken.builder().token(token).user(user).build());
        touchLastActive(user);
        userRepository.save(user);
    }

    public void removeFcmToken(Jwt jwt, String token) {
        getCurrentUserOrThrow(jwt);
        deviceTokenRepository.findById(token).ifPresent(deviceTokenRepository::delete);
    }

    @org.springframework.transaction.annotation.Transactional
    public void removeAllFcmTokens(Jwt jwt) {
        User user = getCurrentUserOrThrow(jwt);
        deviceTokenRepository.deleteByUserId(user.getId());
    }

    public void deleteAccount(Jwt jwt) {
        User user = getCurrentUserOrThrow(jwt);
        user.setStatus(UserStatus.DELETED);
        touchLastActive(user);
        userRepository.save(user);
        
        List<Pet> userPets = petRepository.findByUserId(user.getId());
        userPets.forEach(pet -> pet.setStatus(AdStatus.HIDDEN));
        petRepository.saveAll(userPets);
    }

    public void blockUser(Jwt jwt, Long blockedId) {
        User blocker = getCurrentUserOrThrow(jwt);
        if (blocker.getId().equals(blockedId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "KhÃ´ng thá»ƒ tá»± cháº·n chÃ­nh mÃ¬nh");
        }
        User blockedUser = userRepository.findById(blockedId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "KhÃ´ng tÃ¬m tháº¥y ngÆ°á»i dÃ¹ng Ä‘Ã­ch"));
        
        if (!userBlockRepository.existsByBlockerIdAndBlockedId(blocker.getId(), blockedId)) {
            userBlockRepository.save(UserBlock.builder().blocker(blocker).blocked(blockedUser).build());
            touchLastActive(blocker);
            userRepository.save(blocker);
        }
    }

    public void unblockUser(Jwt jwt, Long blockedId) {
        User blocker = getCurrentUserOrThrow(jwt);
        userBlockRepository.findByBlockerIdAndBlockedId(blocker.getId(), blockedId)
                .ifPresent(block -> {
                    userBlockRepository.delete(block);
                    touchLastActive(blocker);
                    userRepository.save(blocker);
                });
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
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "KhÃ´ng thá»ƒ tá»± theo dÃµi chÃ­nh mÃ¬nh");
        }
        User followedUser = userRepository.findById(followedId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "KhÃ´ng tÃ¬m tháº¥y ngÆ°á»i dÃ¹ng Ä‘Ã­ch"));
                
        if (!userFollowRepository.existsByFollowerIdAndFollowedId(follower.getId(), followedId)) {
            userFollowRepository.save(UserFollow.builder().follower(follower).followed(followedUser).build());
            touchLastActive(follower);
            userRepository.save(follower);
        }
    }

    public void unfollowUser(Jwt jwt, Long followedId) {
        User follower = getCurrentUserOrThrow(jwt);
        userFollowRepository.findByFollowerIdAndFollowedId(follower.getId(), followedId)
                .ifPresent(follow -> {
                    userFollowRepository.delete(follow);
                    touchLastActive(follower);
                    userRepository.save(follower);
                });
    }

    public boolean checkFollowStatus(Jwt jwt, Long followedId) {
        User follower = getCurrentUserOrThrow(jwt);
        return userFollowRepository.existsByFollowerIdAndFollowedId(follower.getId(), followedId);
    }

    public Map<String, Long> getUserFollowStats(Long userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "KhÃ´ng tÃ¬m tháº¥y ngÆ°á»i dÃ¹ng"));
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
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Äiá»ƒm Ä‘Ã¡nh giÃ¡ pháº£i tá»« 1 Ä‘áº¿n 5");
        }

        User rater = getCurrentUserOrThrow(jwt);
        touchLastActive(rater);
        userRepository.save(rater);
        User ratedUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "KhÃ´ng tÃ¬m tháº¥y ngÆ°á»i dÃ¹ng Ä‘Æ°á»£c Ä‘Ã¡nh giÃ¡"));
        
        if (rater.getId().equals(ratedUser.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "KhÃ´ng thá»ƒ tá»± Ä‘Ã¡nh giÃ¡ chÃ­nh mÃ¬nh");
        }

        Pet pet = petRepository.findById(dto.getPetId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "KhÃ´ng tÃ¬m tháº¥y thÃº cÆ°ng"));

        if (!pet.getUser().getId().equals(ratedUser.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ThÃº cÆ°ng khÃ´ng thuá»™c vá» ngÆ°á»i dÃ¹ng Ä‘Æ°á»£c Ä‘Ã¡nh giÃ¡");
        }

        // Kiá»ƒm tra: chá»‰ ngÆ°á»i mua/nháº­n nuÃ´i Ä‘Ã£ Ä‘Æ°á»£c duyá»‡t (APPROVED) má»›i cÃ³ thá»ƒ Ä‘Ã¡nh giÃ¡ ngÆ°á»i bÃ¡n
        boolean hasApprovedAdoption = adoptionApplicationRepository
                .existsByApplicantIdAndPetIdAndStatus(rater.getId(), pet.getId(), AdoptionStatus.APPROVED);
        if (!hasApprovedAdoption) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Báº¡n chá»‰ cÃ³ thá»ƒ Ä‘Ã¡nh giÃ¡ sau khi nháº­n nuÃ´i thÃº cÆ°ng nÃ y thÃ nh cÃ´ng");
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
        
        Double systemAvg = Optional.ofNullable(userRatingRepository.getSystemAverageScore()).orElse(5.0);
        calculateAndSaveTrustScore(ratedUser, systemAvg);
        
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

    public void calculateAndSaveTrustScore(User user, Double systemAverageM) {
        List<UserRating> ratings = userRatingRepository.findByRatedUserId(user.getId());
        Double m = systemAverageM != null ? systemAverageM : 5.0;
        if (m.isNaN() || m == 0.0) m = 5.0; // fallback if no ratings in system at all
        
        if (ratings.isEmpty()) {
            user.setTrustScore(m);
            return;
        }

        double sumRw = 0.0;
        double sumW = 0.0;
        double lambda = Math.log(2) / 180.0;
        java.time.LocalDateTime now = java.time.LocalDateTime.now();

        for (UserRating rating : ratings) {
            double r_i = rating.getScore();
            long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(rating.getCreatedAt(), now);
            if (daysBetween < 0) daysBetween = 0;
            
            double w_time = Math.exp(-lambda * daysBetween);
            
            double w_verify = 0.5;
            User rater = rating.getRater();
            if (rater.isIdentityVerified()) {
                w_verify = 1.5;
            } else if (rater.getProvider() != null && !rater.getProvider().equals("local")) {
                w_verify = 1.0;
            }
            
            double w_i = w_time * w_verify;
            sumRw += r_i * w_i;
            sumW += w_i;
        }

        double C = 5.0;
        double sFinal = (C * m + sumRw) / (C + sumW);
        sFinal = Math.round(sFinal * 100.0) / 100.0; // round to 2 decimal places
        
        user.setTrustScore(sFinal);
    }

    public SellerRatingSummaryDto getSellerRatingSummary(Jwt jwt, Long sellerId) {
        User seller = userRepository.findById(sellerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "KhÃ´ng tÃ¬m tháº¥y ngÆ°á»i bÃ¡n"));
        
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
                .trustScore(seller.getTrustScore() != null ? seller.getTrustScore() : seller.getAverageRating())
                .totalReviews(seller.getRatingCount())
                .ratingDistribution(distribution)
                .currentUserHasRated(currentUserHasRated)
                .currentUserRating(currentUserRating)
                .recentReviews(recentReviews)
                .build();
    }

    public List<RatingResponseDto> getSellerReviews(Long sellerId) {
        if (!userRepository.existsById(sellerId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "KhÃ´ng tÃ¬m tháº¥y ngÆ°á»i bÃ¡n");
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
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "KhÃ´ng tÃ¬m tháº¥y Ä‘Ã¡nh giÃ¡"));
                
        if (!rating.getRater().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Báº¡n chá»‰ cÃ³ thá»ƒ xÃ³a Ä‘Ã¡nh giÃ¡ cá»§a chÃ­nh mÃ¬nh");
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

    /**
     * Change password for users who registered with email/password provider.
     * Uses Firebase Admin SDK to update the user's password.
     */
    public void changePassword(Jwt jwt, ChangePasswordDto dto) {
        // Validate that new password matches confirm password
        if (!dto.getNewPassword().equals(dto.getConfirmPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mật khẩu mới và xác nhận mật khẩu không khớp");
        }

        // Validate that new password is different from current password
        if (dto.getCurrentPassword().equals(dto.getNewPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mật khẩu mới phải khác mật khẩu hiện tại");
        }

        User user = getCurrentUserOrThrow(jwt);
        String uid = jwt.getSubject();

        // Check if user is using password provider (email/password login)
        Map<String, Object> firebaseClaim = Optional.ofNullable(jwt.getClaimAsMap("firebase")).orElse(new HashMap<>());
        String provider = (String) firebaseClaim.get("sign_in_provider");

        if (provider == null || !provider.equals("password")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                "Chỉ người dùng đăng ký bằng email/mật khẩu mới có thể đổi mật khẩu. " +
                "Người dùng đăng nhập bằng mạng xã hội vui lòng đổi mật khẩu qua nhà cung cấp tương ứng.");
        }

        try {
            // Update password in Firebase using Admin SDK
            UpdateRequest request = new UpdateRequest(uid)
                    .setPassword(dto.getNewPassword());

            FirebaseAuth.getInstance().updateUser(request);

            // Revoke all tokens to force re-login with new password
            user.setTokensValidAfter(Instant.now());
            userRepository.save(user);

        } catch (FirebaseAuthException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                "Lỗi khi cập nhật mật khẩu: " + e.getMessage());
        }
    }
}


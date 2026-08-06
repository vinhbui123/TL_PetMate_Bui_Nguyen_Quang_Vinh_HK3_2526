package com.petmate.server.service;

import com.petmate.server.entity.User;
import com.petmate.server.repository.UserRepository;
import com.petmate.server.repository.UserRatingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrustScoreScheduler {

    private final UserRepository userRepository;
    private final UserService userService;
    private final UserRatingRepository userRatingRepository;

    // Run every day at 2 AM
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void calculateTrustScoreForAllUsers() {
        log.info("Starting TrustScore batch calculation...");
        
        List<User> users = userRepository.findAll();
        Double m = Optional.ofNullable(userRatingRepository.getSystemAverageScore()).orElse(5.0);
        if (m.isNaN() || m == 0.0) m = 5.0;
        
        for (User user : users) {
            try {
                if (user.getRatingCount() != null && user.getRatingCount() > 0) {
                    userService.calculateAndSaveTrustScore(user, m);
                } else {
                    // For users with no ratings, set to system average to avoid cold start problem
                    user.setTrustScore(m);
                }
            } catch (Exception e) {
                log.error("Failed to calculate TrustScore for user ID " + user.getId(), e);
            }
        }
        
        userRepository.saveAll(users);
        log.info("Finished TrustScore batch calculation.");
    }
}

package com.petmate.server.repository;

import com.petmate.server.entity.DeviceToken;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DeviceTokenRepository extends JpaRepository<DeviceToken, String> {

    List<DeviceToken> findByUserId(Long userId);

    void deleteByUserId(Long userId);

    @Modifying
    @Query(value = "INSERT INTO fcm_tokens (token, user_id, updated_at) VALUES (?1, ?2, CURRENT_TIMESTAMP) ON DUPLICATE KEY UPDATE user_id = ?2, updated_at = CURRENT_TIMESTAMP", nativeQuery = true)
    void upsertToken(String token, Long userId);
}

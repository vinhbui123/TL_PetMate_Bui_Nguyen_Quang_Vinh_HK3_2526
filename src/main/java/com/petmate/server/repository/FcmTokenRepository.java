package com.petmate.server.repository;

import com.petmate.server.entity.FcmToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FcmTokenRepository extends JpaRepository<FcmToken, String> {
    List<FcmToken> findByUserId(Long userId);
    void deleteByUserId(Long userId);
}

package com.petmate.server.repository;

import com.petmate.server.entity.OrganizationProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrganizationProfileRepository extends JpaRepository<OrganizationProfile, Long> {
    List<OrganizationProfile> findByStatus(String status);
    Optional<OrganizationProfile> findByUserId(Long userId);
    List<OrganizationProfile> findByOrgType(String orgType);
    boolean existsByUserId(Long userId);
    List<OrganizationProfile> findByVerifiedUntilBeforeAndStatus(java.time.LocalDateTime date, String status);
    List<OrganizationProfile> findByStatusAndCreatedAtBefore(String status, java.time.LocalDateTime date);
}

package com.petmate.server.repository;

import com.petmate.server.entity.AdoptionApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.petmate.server.enums.AdoptionStatus;

import java.util.List;

@Repository
public interface AdoptionApplicationRepository extends JpaRepository<AdoptionApplication, Long> {
    List<AdoptionApplication> findByApplicant_ProviderId(String providerId);
    List<AdoptionApplication> findByPet_User_ProviderId(String ownerProviderId);
    boolean existsByApplicantIdAndPet_UserIdAndStatus(Long applicantId, Long petOwnerId, AdoptionStatus status);
    boolean existsByApplicantIdAndPetIdAndStatus(Long applicantId, Long petId, AdoptionStatus status);
    List<AdoptionApplication> findByPet_Organization_Id(Long orgId);
    long countByStatus(AdoptionStatus status);
    long countByPet_Organization_IdAndStatus(Long orgId, AdoptionStatus status);
    long countByCreatedAtBetween(java.time.LocalDateTime start, java.time.LocalDateTime end);
}

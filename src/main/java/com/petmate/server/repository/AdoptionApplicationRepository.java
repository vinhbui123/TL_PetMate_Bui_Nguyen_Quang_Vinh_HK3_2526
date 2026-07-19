package com.petmate.server.repository;

import com.petmate.server.entity.AdoptionApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdoptionApplicationRepository extends JpaRepository<AdoptionApplication, Long> {
    List<AdoptionApplication> findByApplicant_ProviderId(String providerId);
    List<AdoptionApplication> findByPet_User_ProviderId(String ownerProviderId);
}

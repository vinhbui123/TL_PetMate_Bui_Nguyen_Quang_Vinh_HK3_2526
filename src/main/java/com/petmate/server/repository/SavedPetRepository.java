package com.petmate.server.repository;

import com.petmate.server.entity.SavedPet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SavedPetRepository extends JpaRepository<SavedPet, Long> {
    boolean existsByPetIdAndUser_ProviderId(Long petId, String providerId);
    void deleteByPetIdAndUser_ProviderId(Long petId, String providerId);
    List<SavedPet> findByUser_ProviderIdOrderByCreatedAtDesc(String providerId);
}

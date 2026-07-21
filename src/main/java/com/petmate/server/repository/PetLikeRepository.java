package com.petmate.server.repository;

import com.petmate.server.entity.PetLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PetLikeRepository extends JpaRepository<PetLike, Long> {
    boolean existsByPetIdAndUser_ProviderId(Long petId, String providerId);
    void deleteByPetIdAndUser_ProviderId(Long petId, String providerId);
    long countByPetId(Long petId);
}

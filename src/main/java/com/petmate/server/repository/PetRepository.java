package com.petmate.server.repository;

import com.petmate.server.entity.Pet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PetRepository extends JpaRepository<Pet, Long> {
    List<Pet> findByCategory(String category);
    List<Pet> findByCategoryAndStatus(String category, com.petmate.server.enums.AdStatus status);
    List<Pet> findByStatus(com.petmate.server.enums.AdStatus status);
    List<Pet> findByUserId(Long userId);
    List<Pet> findByUser_ProviderId(String providerId);
}

package com.petmate.server.repository;

import com.petmate.server.entity.Pet;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PetRepository extends JpaRepository<Pet, Long> {
    @EntityGraph(attributePaths = {"user"})
    List<Pet> findByCategory(String category);

    @EntityGraph(attributePaths = {"user"})
    List<Pet> findByCategoryAndStatus(String category, com.petmate.server.enums.AdStatus status);

    @EntityGraph(attributePaths = {"user"})
    List<Pet> findByStatus(com.petmate.server.enums.AdStatus status);

    @EntityGraph(attributePaths = {"user"})
    List<Pet> findByUserId(Long userId);

    @EntityGraph(attributePaths = {"user"})
    List<Pet> findByUser_ProviderId(String providerId);

    @EntityGraph(attributePaths = {"user", "organization"})
    List<Pet> findByOrganizationId(Long organizationId);

    @EntityGraph(attributePaths = {"user"})
    List<Pet> findByListingTypeAndStatus(com.petmate.server.enums.ListingType listingType, com.petmate.server.enums.AdStatus status);

    @EntityGraph(attributePaths = {"user"})
    List<Pet> findByCategoryAndListingTypeAndStatus(String category, com.petmate.server.enums.ListingType listingType, com.petmate.server.enums.AdStatus status);

    long countByOrganization_Id(Long orgId);
    
    long countByOrganization_IdAndStatus(Long orgId, com.petmate.server.enums.AdStatus status);
}

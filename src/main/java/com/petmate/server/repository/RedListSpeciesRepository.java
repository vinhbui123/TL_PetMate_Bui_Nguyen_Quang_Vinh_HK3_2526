package com.petmate.server.repository;

import com.petmate.server.entity.RedListSpecies;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RedListSpeciesRepository extends JpaRepository<RedListSpecies, Long> {

    List<RedListSpecies> findByCategory(String category);

    boolean existsByCategoryAndBreedKeywordIgnoreCase(String category, String breedKeyword);
}

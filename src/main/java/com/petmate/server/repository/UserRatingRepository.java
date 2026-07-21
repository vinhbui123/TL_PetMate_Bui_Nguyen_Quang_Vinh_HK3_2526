package com.petmate.server.repository;

import java.util.List;

import com.petmate.server.entity.UserRating;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRatingRepository extends JpaRepository<UserRating, Long> {
    Optional<UserRating> findByRaterIdAndRatedUserId(Long raterId, Long ratedUserId);

    @Query("SELECT AVG(r.score) FROM UserRating r WHERE r.ratedUser.id = :ratedUserId")
    Double getAverageScoreByRatedUserId(@Param("ratedUserId") Long ratedUserId);

    @Query("SELECT COUNT(r) FROM UserRating r WHERE r.ratedUser.id = :ratedUserId")
    Integer countByRatedUserId(@Param("ratedUserId") Long ratedUserId);

    List<UserRating> findByRatedUserIdOrderByCreatedAtDesc(Long ratedUserId);

    @Query("SELECT r.score, COUNT(r) FROM UserRating r WHERE r.ratedUser.id = :ratedUserId GROUP BY r.score")
    List<Object[]> countByRatedUserIdGroupByScore(@Param("ratedUserId") Long ratedUserId);

    boolean existsByRaterIdAndRatedUserId(Long raterId, Long ratedUserId);
}

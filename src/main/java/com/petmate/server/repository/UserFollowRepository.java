package com.petmate.server.repository;

import com.petmate.server.entity.UserFollow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserFollowRepository extends JpaRepository<UserFollow, Long> {
    boolean existsByFollowerIdAndFollowedId(Long followerId, Long followedId);
    Optional<UserFollow> findByFollowerIdAndFollowedId(Long followerId, Long followedId);
    long countByFollowedId(Long followedId);
    long countByFollowerId(Long followerId);

    @org.springframework.data.jpa.repository.Query("SELECT uf.follower FROM UserFollow uf WHERE uf.followed.id = :followedId")
    java.util.List<com.petmate.server.entity.User> findFollowersByUserId(@org.springframework.data.repository.query.Param("followedId") Long followedId);

    @org.springframework.data.jpa.repository.Query("SELECT uf.followed FROM UserFollow uf WHERE uf.follower.id = :followerId")
    java.util.List<com.petmate.server.entity.User> findFollowingByUserId(@org.springframework.data.repository.query.Param("followerId") Long followerId);
}

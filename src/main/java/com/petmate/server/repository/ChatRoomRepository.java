package com.petmate.server.repository;

import com.petmate.server.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    @Query("SELECT r FROM ChatRoom r WHERE r.buyer.id = :userId OR r.seller.id = :userId ORDER BY r.updatedAt DESC")
    List<ChatRoom> findByUserIdOrderByUpdatedAtDesc(@Param("userId") Long userId);

    @Query("SELECT r FROM ChatRoom r WHERE (r.buyer.id = :buyerId AND r.seller.id = :sellerId AND r.pet.id = :petId) OR (r.buyer.id = :sellerId AND r.seller.id = :buyerId AND r.pet.id = :petId)")
    Optional<ChatRoom> findExistingRoom(@Param("buyerId") Long buyerId, @Param("sellerId") Long sellerId, @Param("petId") Long petId);
}

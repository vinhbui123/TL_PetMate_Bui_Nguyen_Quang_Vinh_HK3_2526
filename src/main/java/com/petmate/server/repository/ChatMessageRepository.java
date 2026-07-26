package com.petmate.server.repository;

import com.petmate.server.entity.ChatMessage;
import com.petmate.server.enums.MessageStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByRoomIdOrderByCreatedAtAsc(Long roomId);
    ChatMessage findTopByRoomIdOrderByCreatedAtDesc(Long roomId);

    int countByRoomIdAndSenderIdNotAndStatusNot(Long roomId, Long currentUserId, MessageStatus status);

    @Query("SELECT COUNT(m) FROM ChatMessage m WHERE (m.room.buyer.id = :userId OR m.room.seller.id = :userId) AND m.sender.id != :userId AND m.status != com.petmate.server.enums.MessageStatus.READ")
    int countTotalUnreadForUser(@Param("userId") Long userId);
}

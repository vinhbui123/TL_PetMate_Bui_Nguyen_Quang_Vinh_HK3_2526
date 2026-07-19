package com.petmate.server.dto;

import com.petmate.server.entity.Pet;
import com.petmate.server.entity.User;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoomResponse {
    private Long id;
    private User otherUser; // Information about the person they are chatting with
    private Pet pet;
    private MessageResponse lastMessage;
    private int unreadCount;
    private LocalDateTime updatedAt;
}

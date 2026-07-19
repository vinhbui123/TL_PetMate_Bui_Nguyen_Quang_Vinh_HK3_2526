package com.petmate.server.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessagePayload {
    private String type; // CHAT, JOIN, LEAVE
    private Long roomId;
    private Long senderId;
    private Long recipientId; // the person receiving this message
    private String content;
    private String senderName;
    private String timestamp;
}

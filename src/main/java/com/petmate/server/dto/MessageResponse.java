package com.petmate.server.dto;

import com.petmate.server.enums.MessageStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageResponse {
    private Long id;
    private Long roomId;
    private Long senderId;
    private String content;
    private LocalDateTime createdAt;
    private MessageStatus status;
}

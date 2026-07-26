package com.petmate.server.dto;

import lombok.Data;

@Data
public class ReportRequestDto {
    private Long reportedPetId;
    private Long reportedUserId;
    private Long reportedMessageId;
    private Long reportedOrgId;
    private String reason;
    private String description;
}

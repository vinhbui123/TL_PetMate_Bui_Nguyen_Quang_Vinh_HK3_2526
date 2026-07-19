package com.petmate.server.dto;

import lombok.Data;

@Data
public class ReportRequestDto {
    private Long reportedPetId;
    private Long reportedUserId;
    private String reason;
    private String description;
}

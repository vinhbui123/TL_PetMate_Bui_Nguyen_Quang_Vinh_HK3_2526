package com.petmate.server.dto;

import lombok.Data;

@Data
public class OrgReviewRequestDto {
    private String status;
    private String adminNote;
    private String rejectionReason;
}

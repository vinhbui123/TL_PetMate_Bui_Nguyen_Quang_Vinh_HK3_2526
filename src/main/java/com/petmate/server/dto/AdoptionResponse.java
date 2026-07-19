package com.petmate.server.dto;

import com.petmate.server.entity.AdoptionApplication;
import com.petmate.server.enums.AdoptionStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdoptionResponse {
    private Long id;
    private Long petId;
    private String petName;
    private String petImageUrl;
    private Long applicantId;
    private String applicantName;
    private String applicantAvatarUrl;
    private String applicantPhone;
    private String message;
    private String experience;
    private AdoptionStatus status;
    private LocalDateTime createdAt;

    public static AdoptionResponse fromEntity(AdoptionApplication app) {
        AdoptionResponse dto = new AdoptionResponse();
        dto.setId(app.getId());
        dto.setPetId(app.getPet().getId());
        dto.setPetName(app.getPet().getName());
        dto.setPetImageUrl(app.getPet().getImageUrl());
        dto.setApplicantId(app.getApplicant().getId());
        dto.setApplicantName(app.getApplicant().getFullName());
        dto.setApplicantAvatarUrl(app.getApplicant().getAvatarUrl());
        dto.setApplicantPhone(app.getApplicant().getPhone());
        dto.setMessage(app.getMessage());
        dto.setExperience(app.getExperience());
        dto.setStatus(app.getStatus());
        dto.setCreatedAt(app.getCreatedAt());
        return dto;
    }
}

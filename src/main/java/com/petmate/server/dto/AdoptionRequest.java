package com.petmate.server.dto;

import lombok.Data;

@Data
public class AdoptionRequest {
    private Long petId;
    private String message;
    private String experience;
}

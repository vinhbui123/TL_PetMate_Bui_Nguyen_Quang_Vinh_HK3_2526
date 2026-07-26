package com.petmate.server.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RatingResponseDto {
    private Long id;
    private Long raterId;
    private String raterName;
    private String raterAvatarUrl;
    private Double score;
    private String comment;
    private Long petId;
    private String petName;
    private BigDecimal petPrice;
    private String petImageUrl;
    private LocalDateTime createdAt;
}

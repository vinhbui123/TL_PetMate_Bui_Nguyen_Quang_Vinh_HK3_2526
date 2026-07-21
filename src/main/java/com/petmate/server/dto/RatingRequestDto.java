package com.petmate.server.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RatingRequestDto {
    @NotNull(message = "Score is required")
    @Min(value = 1, message = "Score must be at least 1")
    @Max(value = 5, message = "Score must be at most 5")
    private Double score;

    @NotNull(message = "Pet ID is required")
    private Long petId;

    @Size(max = 500, message = "Comment must not exceed 500 characters")
    private String comment;
}

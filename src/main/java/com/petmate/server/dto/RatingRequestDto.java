package com.petmate.server.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RatingRequestDto {
    @NotNull(message = "Vui lòng chọn số sao đánh giá")
    @Min(value = 1, message = "Số sao tối thiểu là 1")
    @Max(value = 5, message = "Số sao tối đa là 5")
    private Double score;

    @NotNull(message = "Thiếu ID thú cưng")
    private Long petId;

    @Size(max = 500, message = "Nhận xét không được vượt quá 500 ký tự")
    private String comment;
}

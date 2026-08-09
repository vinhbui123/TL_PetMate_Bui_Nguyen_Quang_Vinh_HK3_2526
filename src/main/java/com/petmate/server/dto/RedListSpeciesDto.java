package com.petmate.server.dto;

import com.petmate.server.enums.ProtectionLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RedListSpeciesDto {

    private Long id;

    private String category;

    @NotBlank(message = "Vui lòng nhập từ khóa loài")
    private String breedKeyword;

    private String synonyms;

    @NotNull(message = "Vui lòng chọn mức độ bảo vệ")
    private ProtectionLevel protectionLevel;

    private String description;
}

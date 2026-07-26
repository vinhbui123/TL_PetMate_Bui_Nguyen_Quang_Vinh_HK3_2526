package com.petmate.server.dto;

import lombok.Data;
import com.petmate.server.enums.AdStatus;
import com.petmate.server.enums.Gender;
import com.petmate.server.enums.ListingType;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

@Data
public class PetRequestDto {
    private ListingType listingType;
    
    @NotBlank(message = "Vui lòng nhập tên thú cưng")
    private String name;
    
    @NotBlank(message = "Vui lòng nhập giống")
    private String breed;
    
    @Pattern(regexp = "^[0-9]*$", message = "Tuổi chỉ được nhập số")
    private String age;

    @Pattern(regexp = "^[0-9]*(?:\\.[0-9]+)?$", message = "Cân nặng chỉ được nhập số")
    private String weight;
    private Gender gender;
    private BigDecimal price;
    private String description;
    private String category;
    private String address;
    private Double latitude;
    private Double longitude;
    private AdStatus status;
    private Boolean isVaccinated;
    private Boolean isNeutered;
    private Long organizationId;
}

package com.petmate.server.dto;

import lombok.Data;
import com.petmate.server.enums.AdStatus;

@Data
public class PetRequestDto {
    private String name;
    private String breed;
    private String age;
    private String weight;
    private String gender;
    private String price;
    private String description;
    private String category;
    private AdStatus status;
}

package com.petmate.server.dto;

import lombok.Data;

@Data
public class UserProfileDto {
    private String fullName;
    private String phone;
    private String address;
    private String avatarUrl;
}

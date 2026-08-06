package com.petmate.server.dto;

import lombok.Data;

@Data
public class OrgMemberDto {
    private Long id;
    private Long userId;
    private String userName;
    private String userEmail;
    private String userAvatarUrl;
    private String memberRole;
    private String status;
}

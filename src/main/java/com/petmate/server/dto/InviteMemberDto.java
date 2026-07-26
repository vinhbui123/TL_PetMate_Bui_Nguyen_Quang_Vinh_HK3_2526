package com.petmate.server.dto;

import lombok.Data;

@Data
public class InviteMemberDto {
    private String email;
    private String memberRole;
}

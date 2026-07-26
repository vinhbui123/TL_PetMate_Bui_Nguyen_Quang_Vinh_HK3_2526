package com.petmate.server.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrgStatsDto {
    private long totalPets;
    private long adoptedPets;
    private long pendingAdoptionApps;
    private long totalMembers;
}

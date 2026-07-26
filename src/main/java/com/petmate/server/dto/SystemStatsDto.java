package com.petmate.server.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemStatsDto {
    private long totalUsers;
    private long totalOrganizations;
    private long totalPets;
    private long totalAdoptions;
    private long pendingAdoptions;
    private long approvedAdoptions;
    private long totalReports;
}

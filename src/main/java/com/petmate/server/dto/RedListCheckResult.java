package com.petmate.server.dto;

import com.petmate.server.entity.RedListSpecies;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RedListCheckResult {

    private boolean matched;
    private RedListSpecies species;
    private String matchedKeyword;
    private String matchType;

    public static RedListCheckResult noMatch() {
        return RedListCheckResult.builder()
                .matched(false)
                .build();
    }
}

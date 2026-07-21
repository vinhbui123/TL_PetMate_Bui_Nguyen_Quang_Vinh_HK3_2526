package com.petmate.server.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public class InteractionDto {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SaveStatusResponse {
        private boolean isSaved;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LikeStatusResponse {
        private boolean liked;
        private int likeCount;
    }
}

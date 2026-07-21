package com.petmate.server.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SellerRatingSummaryDto {
    private Long sellerId;
    private String sellerName;
    private Double averageRating;
    private Integer totalReviews;
    private Map<Integer, Integer> ratingDistribution;
    private Boolean currentUserHasRated;
    private RatingResponseDto currentUserRating;
    private List<RatingResponseDto> recentReviews;
}

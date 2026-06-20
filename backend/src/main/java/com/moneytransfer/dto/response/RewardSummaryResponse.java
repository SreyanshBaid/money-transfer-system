package com.moneytransfer.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RewardSummaryResponse {
    private Long userId;
    private String username;
    private int totalPoints;
    private int totalRewards;
    private int totalEarned;
    private int totalRedeemed;
    private List<RewardResponse> recentRewards;
}

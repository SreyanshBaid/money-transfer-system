package com.moneytransfer.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RedeemResponse {
    private Long rewardId;
    private Long userId;
    private String depositTxnId;
    private int pointsRedeemed;
    private BigDecimal amountCredited;
    private Long accountId;
    private String status;
    private int remainingPoints;
    private LocalDateTime createdAt;
}

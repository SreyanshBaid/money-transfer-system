package com.moneytransfer.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RewardResponse {
    private Long id;
    private Long userId;
    private String transactionLogId;
    private Integer points;
    private String reason;
    private LocalDateTime createdAt;
}

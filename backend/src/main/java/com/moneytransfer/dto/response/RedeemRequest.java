package com.moneytransfer.dto.response;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RedeemRequest {

    @Min(value = 1, message = "Points to redeem must be at least 1")
    private int points;

    @NotNull(message = "Account ID is required")
    private Long accountId;
}

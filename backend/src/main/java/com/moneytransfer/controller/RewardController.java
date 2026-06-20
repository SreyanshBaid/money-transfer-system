package com.moneytransfer.controller;

import com.moneytransfer.dto.response.RedeemRequest;
import com.moneytransfer.dto.response.RedeemResponse;
import com.moneytransfer.dto.response.RewardResponse;
import com.moneytransfer.dto.response.RewardSummaryResponse;
import com.moneytransfer.service.RewardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/rewards")
@RequiredArgsConstructor
@Tag(name = "Rewards", description = "User reward points and history")
public class RewardController {

    private final RewardService rewardService;

    @GetMapping
    @Operation(summary = "Get my rewards", description = "Retrieve all reward records for the current user")
    @ApiResponse(responseCode = "200", description = "Rewards retrieved")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @SecurityRequirement(name = "Bearer")
    public ResponseEntity<List<RewardResponse>> getMyRewards() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        log.debug("Fetching rewards for user: {}", username);
        return ResponseEntity.ok(rewardService.getUserRewards(username));
    }

    @GetMapping("/summary")
    @Operation(summary = "Get reward summary", description = "Get total reward points and recent rewards for the current user")
    @ApiResponse(responseCode = "200", description = "Summary retrieved")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @SecurityRequirement(name = "Bearer")
    public ResponseEntity<RewardSummaryResponse> getRewardSummary() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        log.debug("Fetching reward summary for user: {}", username);
        return ResponseEntity.ok(rewardService.getUserRewardSummary(username));
    }

    @PostMapping("/redeem")
    @Operation(summary = "Redeem reward points", description = "Convert reward points to USD and deposit into a user account (1 point = $1)")
    @ApiResponse(responseCode = "201", description = "Points redeemed successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "409", description = "Insufficient points")
    @SecurityRequirement(name = "Bearer")
    public ResponseEntity<RedeemResponse> redeemPoints(@Valid @RequestBody RedeemRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        log.info("Redeeming {} points to account {} for user {}", request.getPoints(), request.getAccountId(), username);
        RedeemResponse response = rewardService.redeemPoints(username, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}

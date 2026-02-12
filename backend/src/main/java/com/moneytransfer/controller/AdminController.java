package com.moneytransfer.controller;

import com.moneytransfer.dto.response.AccountBalanceResponse;
import com.moneytransfer.dto.response.AccountResponse;
import com.moneytransfer.dto.response.TransactionLogResponse;
import com.moneytransfer.dto.request.CreateAccountRequest;
import com.moneytransfer.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;

/**
 * AdminController: Admin-only endpoints for operational and support functions.
 * 
 * Design principle: Admins observe the system - they do not move money.
 * 
 * ADMIN can:
 * - View any account balance
 * - View any account transaction history
 * - Access system-wide data
 * 
 * ADMIN cannot:
 * - Initiate money transfers (prevents insider abuse)
 * - Act "as" a user
 * - Bypass transaction rules
 * 
 * Access control:
 * - URL-based: /api/v1/admin/** requires ADMIN role (SecurityConfig)
 * - Method-based: @PreAuthorize adds defense-in-depth
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Administrative operations (ADMIN role required)")
public class AdminController {

    private final AccountService accountService;

    /**
     * Admin endpoint to view any account balance.
     * No ownership checks - admins can view all accounts.
     * 
     * @param accountId account ID to view
     * @return account balance
     */
    @GetMapping("/accounts/{accountId}/balance")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "[ADMIN] Get any account balance", 
        description = "Admin-only: View balance of any account without ownership restrictions"
    )
    @ApiResponse(responseCode = "200", description = "Balance retrieved")
    @ApiResponse(responseCode = "403", description = "Forbidden - ADMIN role required")
    @ApiResponse(responseCode = "404", description = "Account not found")
    @SecurityRequirement(name = "Bearer")
    public ResponseEntity<AccountBalanceResponse> getAccountBalance(@PathVariable Long accountId) {
        log.info("[ADMIN] Viewing balance for account: {}", accountId);
        AccountBalanceResponse balance = accountService.getAccountBalanceAdmin(accountId);
        return ResponseEntity.ok(balance);
    }

    /**
     * Admin endpoint to view any account's full details.
     * No ownership checks - admins can view all accounts.
     * 
     * @param accountId account ID to view
     * @return full account details
     */
    @GetMapping("/accounts/{accountId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "[ADMIN] Get any account details", 
        description = "Admin-only: View full details of any account"
    )
    @ApiResponse(responseCode = "200", description = "Account details retrieved")
    @ApiResponse(responseCode = "403", description = "Forbidden - ADMIN role required")
    @ApiResponse(responseCode = "404", description = "Account not found")
    @SecurityRequirement(name = "Bearer")
    public ResponseEntity<AccountResponse> getAccount(@PathVariable Long accountId) {
        log.info("[ADMIN] Viewing account details: {}", accountId);
        AccountResponse account = accountService.getAccountAdmin(accountId);
        return ResponseEntity.ok(account);
    }

    /**
     * Admin endpoint to view any account's transaction history.
     * No ownership checks - admins can view all transactions.
     * 
     * @param accountId account ID to view
     * @return transaction history
     */
    @GetMapping("/accounts/{accountId}/transactions")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "[ADMIN] Get any account's transaction history", 
        description = "Admin-only: View transaction history of any account"
    )
    @ApiResponse(responseCode = "200", description = "Transaction history retrieved")
    @ApiResponse(responseCode = "403", description = "Forbidden - ADMIN role required")
    @ApiResponse(responseCode = "404", description = "Account not found")
    @SecurityRequirement(name = "Bearer")
    public ResponseEntity<List<TransactionLogResponse>> getAccountTransactions(@PathVariable Long accountId) {
        log.info("[ADMIN] Viewing transaction history for account: {}", accountId);
        List<TransactionLogResponse> transactions = accountService.getTransactionHistoryAdmin(accountId);
        return ResponseEntity.ok(transactions);
    }

    /**
     * Admin endpoint to create an account for a specific user.
     * Admins can attach accounts to any user by user ID.
     *
     * @param userId target user ID
     * @param request account creation request
     * @return created account details
     */
    @PostMapping("/users/{userId}/accounts")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "[ADMIN] Create account for user",
        description = "Admin-only: Create a new account for a specific user ID"
    )
    @ApiResponse(responseCode = "201", description = "Account created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request or user not found")
    @ApiResponse(responseCode = "403", description = "Forbidden - ADMIN role required")
    @SecurityRequirement(name = "Bearer")
    public ResponseEntity<AccountResponse> createAccountForUser(
            @PathVariable Long userId,
            @Valid @RequestBody CreateAccountRequest request) {
        log.info("[ADMIN] Creating account for userId: {}", userId);
        AccountResponse createdAccount = accountService.createAccountForUser(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdAccount);
    }

    /**
     * Admin system health check.
     * 
     * @return simple health response
     */
    @GetMapping("/health")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "[ADMIN] Admin system health", 
        description = "Admin-only health check endpoint"
    )
    @ApiResponse(responseCode = "200", description = "System healthy")
    @ApiResponse(responseCode = "403", description = "Forbidden - ADMIN role required")
    @SecurityRequirement(name = "Bearer")
    public ResponseEntity<String> healthCheck() {
        log.info("[ADMIN] Health check accessed");
        return ResponseEntity.ok("Admin system operational");
    }
}

package com.moneytransfer.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for creating a new account.
 * The account will automatically be owned by the authenticated user.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateAccountRequest {

    @NotBlank(message = "Account number is required")
    @Size(min = 5, max = 20, message = "Account number must be between 5 and 20 characters")
    private String accountNumber;

    @NotBlank(message = "Account holder name is required")
    @Size(min = 2, max = 100, message = "Account holder name must be between 2 and 100 characters")
    private String accountHolder;

    @Positive(message = "Initial balance must be greater than zero")
    private BigDecimal balance;

    @NotBlank(message = "Account type is required")
    @Pattern(regexp = "CHECKING|SAVINGS|BUSINESS", 
            message = "Account type must be CHECKING, SAVINGS, or BUSINESS")
    private String accountType;

    @NotBlank(message = "Account status is required")
    @Pattern(regexp = "ACTIVE|INACTIVE|SUSPENDED", 
            message = "Account status must be ACTIVE, INACTIVE, or SUSPENDED")
    private String status;
}

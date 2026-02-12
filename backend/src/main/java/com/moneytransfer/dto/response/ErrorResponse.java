package com.moneytransfer.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Data Transfer Object for error responses.
 * Standard error output format with error codes.
 * 
 * Error Code Format:
 * - ACC-XXX: Account-related errors
 * - TRX-XXX: Transaction-related errors
 * - VAL-XXX: Validation errors
 * - AUTH-XXX: Authentication/Authorization errors
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErrorResponse {

    private int status;

    private String code;

    private String message;

    private String error;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;

    private String path;
}

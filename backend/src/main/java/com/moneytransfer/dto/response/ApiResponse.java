package com.moneytransfer.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Standardized API response envelope for consistent response structure.
 * Provides a uniform interface for both success and error responses.
 * 
 * Usage:
 * - Success: ApiResponse.success(data)
 * - Error: ApiResponse.error(errorResponse)
 * 
 * Example success response:
 * {
 *   "data": { "id": 1001, "balance": 5000.00 },
 *   "meta": { "timestamp": "2026-02-13T10:30:00" },
 *   "error": null
 * }
 * 
 * Example error response:
 * {
 *   "data": null,
 *   "meta": { "timestamp": "2026-02-13T10:30:00" },
 *   "error": { "code": "ACC-404", "message": "Account not found" }
 * }
 * 
 * @param <T> the type of data payload
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    /**
     * The actual data payload for successful responses
     */
    private T data;

    /**
     * Error details when request fails
     */
    private ErrorResponse error;

    /**
     * Metadata about the response
     */
    private ResponseMetadata meta;

    /**
     * Metadata about the API response
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ResponseMetadata {
        private LocalDateTime timestamp;
        private String version;
        private String requestId;
        
        public static ResponseMetadata now() {
            return ResponseMetadata.builder()
                    .timestamp(LocalDateTime.now())
                    .version("1.0.0")
                    .build();
        }
    }

    /**
     * Create a successful response with data
     * 
     * @param data the response data
     * @param <T> type of data
     * @return ApiResponse with data and no error
     */
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .data(data)
                .meta(ResponseMetadata.now())
                .build();
    }

    /**
     * Create an error response
     * 
     * @param error the error details
     * @param <T> type of data (will be null)
     * @return ApiResponse with error and no data
     */
    public static <T> ApiResponse<T> error(ErrorResponse error) {
        return ApiResponse.<T>builder()
                .error(error)
                .meta(ResponseMetadata.now())
                .build();
    }

    /**
     * Create a successful response with data and custom metadata
     * 
     * @param data the response data
     * @param requestId optional request ID for tracking
     * @param <T> type of data
     * @return ApiResponse with data, metadata, and no error
     */
    public static <T> ApiResponse<T> success(T data, String requestId) {
        ResponseMetadata metadata = ResponseMetadata.now();
        metadata.setRequestId(requestId);
        
        return ApiResponse.<T>builder()
                .data(data)
                .meta(metadata)
                .build();
    }
}

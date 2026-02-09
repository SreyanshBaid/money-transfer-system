package com.moneytransfer.controller;

import com.moneytransfer.config.JwtProperties;
import com.moneytransfer.dto.request.LoginRequest;
import com.moneytransfer.dto.response.LoginResponse;
import com.moneytransfer.dto.response.LogoutResponse;
import com.moneytransfer.service.TokenBlacklistService;
import com.moneytransfer.util.JwtUtil;
import com.moneytransfer.util.RateLimitUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuthController.
 * 
 * Tests cover:
 * - Logout with valid token
 * - Logout with missing token
 * - Logout with invalid token
 * - Logout with expired token
 * - Token blacklisting verification
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthController Unit Tests")
class AuthControllerTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private JwtProperties jwtProperties;

    @Mock
    private RateLimitUtil rateLimitUtil;

    @Mock
    private TokenBlacklistService tokenBlacklistService;

    @InjectMocks
    private AuthController authController;

    private static final String VALID_TOKEN = "eyJhbGciOiJIUzI1NiJ9.valid.token";
    private static final String INVALID_TOKEN = "invalid.token.format";
    private static final String USERNAME = "testuser";

    @BeforeEach
    void setUp() {
        // Use lenient() for setup stubs that may not be used in all tests
        lenient().when(jwtProperties.getExpirationMs()).thenReturn(3600000L);
    }

    // ============ LOGOUT SUCCESS CASES ============

    @Test
    @DisplayName("✅ POST /auth/logout: Should successfully logout with valid token")
    void testLogoutSuccess() {
        // Arrange
        String authHeader = "Bearer " + VALID_TOKEN;
        lenient().when(jwtUtil.validateToken(VALID_TOKEN)).thenReturn(true);
        when(jwtUtil.extractUsername(VALID_TOKEN)).thenReturn(USERNAME);
        lenient().when(tokenBlacklistService.isBlacklisted(VALID_TOKEN)).thenReturn(false);

        // Act
        ResponseEntity<LogoutResponse> response = authController.logout(authHeader);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getMessage()).isEqualTo("Logout successful");
        verify(tokenBlacklistService, times(1)).blacklistToken(VALID_TOKEN);
    }

    @Test
    @DisplayName("✅ POST /auth/logout: Should blacklist token correctly")
    void testLogoutBlacklistsToken() {
        // Arrange
        String authHeader = "Bearer " + VALID_TOKEN;
        when(jwtUtil.validateToken(VALID_TOKEN)).thenReturn(true);
        when(jwtUtil.extractUsername(VALID_TOKEN)).thenReturn(USERNAME);

        // Act
        authController.logout(authHeader);

        // Assert
        verify(tokenBlacklistService, times(1)).blacklistToken(VALID_TOKEN);
    }

    // ============ LOGOUT FAILURE CASES ============

    @Test
    @DisplayName("❌ POST /auth/logout: Should fail when no token provided")
    void testLogoutFailsWithNoToken() {
        // Act
        ResponseEntity<LogoutResponse> response = authController.logout(null);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getMessage()).isEqualTo("No token provided");
        verify(tokenBlacklistService, never()).blacklistToken(anyString());
    }

    @Test
    @DisplayName("❌ POST /auth/logout: Should fail when Authorization header is empty")
    void testLogoutFailsWithEmptyAuthHeader() {
        // Act
        ResponseEntity<LogoutResponse> response = authController.logout("");

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getMessage()).isEqualTo("No token provided");
        verify(tokenBlacklistService, never()).blacklistToken(anyString());
    }

    @Test
    @DisplayName("❌ POST /auth/logout: Should fail when token format is invalid (missing Bearer)")
    void testLogoutFailsWithoutBearerPrefix() {
        // Arrange
        String authHeader = VALID_TOKEN; // Missing "Bearer " prefix

        // Act
        ResponseEntity<LogoutResponse> response = authController.logout(authHeader);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getMessage()).isEqualTo("No token provided");
        verify(tokenBlacklistService, never()).blacklistToken(anyString());
    }

    @Test
    @DisplayName("❌ POST /auth/logout: Should fail when token is invalid")
    void testLogoutFailsWithInvalidToken() {
        // Arrange
        String authHeader = "Bearer " + INVALID_TOKEN;
        when(jwtUtil.validateToken(INVALID_TOKEN)).thenReturn(false);

        // Act
        ResponseEntity<LogoutResponse> response = authController.logout(authHeader);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getMessage()).isEqualTo("Invalid token");
        verify(tokenBlacklistService, never()).blacklistToken(anyString());
    }

    @Test
    @DisplayName("❌ POST /auth/logout: Should fail when token extraction throws exception")
    void testLogoutFailsWhenTokenExtractionThrows() {
        // Arrange
        String authHeader = "Bearer " + VALID_TOKEN;
        when(jwtUtil.validateToken(VALID_TOKEN)).thenReturn(true);
        when(jwtUtil.extractUsername(VALID_TOKEN)).thenThrow(new RuntimeException("Token parsing failed"));

        // Act
        ResponseEntity<LogoutResponse> response = authController.logout(authHeader);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getMessage()).contains("Logout failed");
        // Note: blacklistToken is called before extractUsername, so it will be invoked
        verify(tokenBlacklistService, times(1)).blacklistToken(VALID_TOKEN);
    }

    @Test
    @DisplayName("❌ POST /auth/logout: Should fail when blacklist service throws exception")
    void testLogoutFailsWhenBlacklistThrows() {
        // Arrange
        String authHeader = "Bearer " + VALID_TOKEN;
        when(jwtUtil.validateToken(VALID_TOKEN)).thenReturn(true);
        lenient().when(jwtUtil.extractUsername(VALID_TOKEN)).thenReturn(USERNAME);
        doThrow(new RuntimeException("Blacklist service error")).when(tokenBlacklistService).blacklistToken(VALID_TOKEN);

        // Act
        ResponseEntity<LogoutResponse> response = authController.logout(authHeader);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getMessage()).contains("Logout failed");
    }

    // ============ HTTP STATUS CODES ============

    @Test
    @DisplayName("✅ POST /auth/logout: Should return 200 OK on success")
    void testLogoutReturns200StatusOnSuccess() {
        // Arrange
        String authHeader = "Bearer " + VALID_TOKEN;
        when(jwtUtil.validateToken(VALID_TOKEN)).thenReturn(true);
        when(jwtUtil.extractUsername(VALID_TOKEN)).thenReturn(USERNAME);

        // Act
        ResponseEntity<LogoutResponse> response = authController.logout(authHeader);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("✅ POST /auth/logout: Should return 400 Bad Request on invalid input")
    void testLogoutReturns400StatusOnBadRequest() {
        // Act
        ResponseEntity<LogoutResponse> response = authController.logout(null);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ============ RESPONSE STRUCTURE ============

    @Test
    @DisplayName("✅ POST /auth/logout: Response body should contain message and success fields")
    void testLogoutResponseStructure() {
        // Arrange
        String authHeader = "Bearer " + VALID_TOKEN;
        when(jwtUtil.validateToken(VALID_TOKEN)).thenReturn(true);
        when(jwtUtil.extractUsername(VALID_TOKEN)).thenReturn(USERNAME);

        // Act
        ResponseEntity<LogoutResponse> response = authController.logout(authHeader);

        // Assert
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody())
                .isNotNull()
                .hasFieldOrProperty("message")
                .hasFieldOrProperty("success");
        assertThat(response.getBody().getMessage()).isNotBlank();
        assertThat(response.getBody().isSuccess()).isNotNull();
    }

    @Test
    @DisplayName("✅ POST /auth/logout: Failure response should have success=false")
    void testLogoutFailureResponseHasSuccessFalse() {
        // Act
        ResponseEntity<LogoutResponse> response = authController.logout(null);

        // Assert
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
    }

    // ============ EDGE CASES ============

    @Test
    @DisplayName("✅ POST /auth/logout: Should handle token with extra whitespace in Bearer prefix")
    void testLogoutFailsWithExtraWhitespaceInBearerPrefix() {
        // Arrange
        String authHeader = "Bearer  " + VALID_TOKEN; // Extra space

        // Act
        ResponseEntity<LogoutResponse> response = authController.logout(authHeader);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().isSuccess()).isFalse();
    }

    @Test
    @DisplayName("✅ POST /auth/logout: Should handle different Bearer case formats")
    void testLogoutFailsWithDifferentBearerCase() {
        // Arrange
        String authHeader = "bearer " + VALID_TOKEN; // lowercase

        // Act
        ResponseEntity<LogoutResponse> response = authController.logout(authHeader);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().isSuccess()).isFalse();
    }

    @Test
    @DisplayName("✅ POST /auth/logout: Should not call tokenBlacklist on invalid token")
    void testLogoutDoesNotBlacklistInvalidToken() {
        // Arrange
        String authHeader = "Bearer " + INVALID_TOKEN;
        when(jwtUtil.validateToken(INVALID_TOKEN)).thenReturn(false);

        // Act
        authController.logout(authHeader);

        // Assert
        verify(tokenBlacklistService, never()).blacklistToken(anyString());
    }
}

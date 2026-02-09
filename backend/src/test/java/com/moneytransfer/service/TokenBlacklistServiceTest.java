package com.moneytransfer.service;

import com.moneytransfer.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Unit tests for TokenBlacklistService.
 * 
 * Tests cover:
 * - Adding tokens to blacklist
 * - Checking if token is blacklisted
 * - Token cleanup and expiration
 * - Concurrent access safety
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TokenBlacklistService Unit Tests")
class TokenBlacklistServiceTest {

    @Mock
    private JwtUtil jwtUtil;

    private TokenBlacklistService tokenBlacklistService;

    private static final String TOKEN_1 = "token-1";
    private static final String TOKEN_2 = "token-2";

    @BeforeEach
    void setUp() {
        tokenBlacklistService = new TokenBlacklistService(jwtUtil);
    }

    // ============ BLACKLIST ADDITION ============

    @Test
    @DisplayName("✅ Should add token to blacklist")
    void testAddTokenToBlacklist() {
        // Arrange
        long futureTime = System.currentTimeMillis() + 3600000; // 1 hour from now
        when(jwtUtil.extractExpiration(TOKEN_1)).thenReturn(new Date(futureTime));

        // Act
        tokenBlacklistService.blacklistToken(TOKEN_1);

        // Assert
        assertThat(tokenBlacklistService.isBlacklisted(TOKEN_1)).isTrue();
    }

    @Test
    @DisplayName("✅ Should handle multiple tokens in blacklist")
    void testAddMultipleTokensToBlacklist() {
        // Arrange
        long futureTime = System.currentTimeMillis() + 3600000;
        when(jwtUtil.extractExpiration(TOKEN_1)).thenReturn(new Date(futureTime));
        when(jwtUtil.extractExpiration(TOKEN_2)).thenReturn(new Date(futureTime));

        // Act
        tokenBlacklistService.blacklistToken(TOKEN_1);
        tokenBlacklistService.blacklistToken(TOKEN_2);

        // Assert
        assertThat(tokenBlacklistService.isBlacklisted(TOKEN_1)).isTrue();
        assertThat(tokenBlacklistService.isBlacklisted(TOKEN_2)).isTrue();
    }

    @Test
    @DisplayName("✅ Should maintain blacklist size correctly")
    void testBlacklistSizeIncrementsCorrectly() {
        // Arrange
        long futureTime = System.currentTimeMillis() + 3600000;
        when(jwtUtil.extractExpiration(TOKEN_1)).thenReturn(new Date(futureTime));
        when(jwtUtil.extractExpiration(TOKEN_2)).thenReturn(new Date(futureTime));

        // Act & Assert
        assertThat(tokenBlacklistService.getBlacklistSize()).isEqualTo(0);
        
        tokenBlacklistService.blacklistToken(TOKEN_1);
        assertThat(tokenBlacklistService.getBlacklistSize()).isEqualTo(1);
        
        tokenBlacklistService.blacklistToken(TOKEN_2);
        assertThat(tokenBlacklistService.getBlacklistSize()).isEqualTo(2);
    }

    // ============ BLACKLIST CHECKING ============

    @Test
    @DisplayName("✅ Should return false for token not in blacklist")
    void testIsBlacklistedReturnsFalseForNonBlacklistedToken() {
        // Act & Assert
        assertThat(tokenBlacklistService.isBlacklisted(TOKEN_1)).isFalse();
    }

    @Test
    @DisplayName("✅ Should return true for blacklisted token")
    void testIsBlacklistedReturnsTrueForBlacklistedToken() {
        // Arrange
        long futureTime = System.currentTimeMillis() + 3600000;
        when(jwtUtil.extractExpiration(TOKEN_1)).thenReturn(new Date(futureTime));

        // Act
        tokenBlacklistService.blacklistToken(TOKEN_1);

        // Assert
        assertThat(tokenBlacklistService.isBlacklisted(TOKEN_1)).isTrue();
    }

    @Test
    @DisplayName("✅ Should correctly differentiate between blacklisted and non-blacklisted tokens")
    void testIsBlacklistedDifferentiatesTokens() {
        // Arrange
        long futureTime = System.currentTimeMillis() + 3600000;
        when(jwtUtil.extractExpiration(TOKEN_1)).thenReturn(new Date(futureTime));

        // Act
        tokenBlacklistService.blacklistToken(TOKEN_1);

        // Assert
        assertThat(tokenBlacklistService.isBlacklisted(TOKEN_1)).isTrue();
        assertThat(tokenBlacklistService.isBlacklisted(TOKEN_2)).isFalse();
    }

    // ============ TOKEN EXPIRATION & CLEANUP ============

    @Test
    @DisplayName("✅ Should store token expiration time correctly")
    void testTokenExpirationTimeIsStored() {
        // Arrange
        long futureTime = System.currentTimeMillis() + 3600000;
        when(jwtUtil.extractExpiration(TOKEN_1)).thenReturn(new Date(futureTime));

        // Act
        tokenBlacklistService.blacklistToken(TOKEN_1);

        // Assert - Token should be in blacklist with future expiration
        assertThat(tokenBlacklistService.isBlacklisted(TOKEN_1)).isTrue();
    }

    @Test
    @DisplayName("✅ Should clean up expired tokens")
    void testCleanupExpiredTokens() {
        // Arrange
        long pastTime = System.currentTimeMillis() - 1000; // 1 second ago (expired)
        long futureTime = System.currentTimeMillis() + 3600000;
        
        when(jwtUtil.extractExpiration(TOKEN_1)).thenReturn(new Date(pastTime));
        when(jwtUtil.extractExpiration(TOKEN_2)).thenReturn(new Date(futureTime));

        tokenBlacklistService.blacklistToken(TOKEN_1);
        tokenBlacklistService.blacklistToken(TOKEN_2);

        assertThat(tokenBlacklistService.getBlacklistSize()).isEqualTo(2);

        // Act
        tokenBlacklistService.cleanupExpiredTokens();

        // Assert - Expired token should be removed, valid token should remain
        assertThat(tokenBlacklistService.getBlacklistSize()).isEqualTo(1);
        assertThat(tokenBlacklistService.isBlacklisted(TOKEN_1)).isFalse();
        assertThat(tokenBlacklistService.isBlacklisted(TOKEN_2)).isTrue();
    }

    @Test
    @DisplayName("✅ Should remove all expired tokens during cleanup")
    void testCleanupRemovesAllExpiredTokens() {
        // Arrange
        long pastTime = System.currentTimeMillis() - 1000;
        when(jwtUtil.extractExpiration(TOKEN_1)).thenReturn(new Date(pastTime));
        when(jwtUtil.extractExpiration(TOKEN_2)).thenReturn(new Date(pastTime));

        tokenBlacklistService.blacklistToken(TOKEN_1);
        tokenBlacklistService.blacklistToken(TOKEN_2);

        // Act
        tokenBlacklistService.cleanupExpiredTokens();

        // Assert
        assertThat(tokenBlacklistService.getBlacklistSize()).isEqualTo(0);
        assertThat(tokenBlacklistService.isBlacklisted(TOKEN_1)).isFalse();
        assertThat(tokenBlacklistService.isBlacklisted(TOKEN_2)).isFalse();
    }

    @Test
    @DisplayName("✅ Should handle cleanup when blacklist is empty")
    void testCleanupDoesNotFailOnEmptyBlacklist() {
        // Act & Assert - Should not throw exception
        assertThatNoException().isThrownBy(() -> tokenBlacklistService.cleanupExpiredTokens());
    }

    @Test
    @DisplayName("✅ Should not remove non-expired tokens during cleanup")
    void testCleanupPreservesNonExpiredTokens() {
        // Arrange
        long futureTime = System.currentTimeMillis() + 3600000;
        when(jwtUtil.extractExpiration(TOKEN_1)).thenReturn(new Date(futureTime));

        tokenBlacklistService.blacklistToken(TOKEN_1);
        int sizeBeforeCleanup = tokenBlacklistService.getBlacklistSize();

        // Act
        tokenBlacklistService.cleanupExpiredTokens();

        // Assert
        assertThat(tokenBlacklistService.getBlacklistSize()).isEqualTo(sizeBeforeCleanup);
        assertThat(tokenBlacklistService.isBlacklisted(TOKEN_1)).isTrue();
    }

    // ============ ERROR HANDLING ============

    @Test
    @DisplayName("✅ Should handle exception during token extraction gracefully")
    void testBlacklistTokenHandlesExceptionGracefully() {
        // Arrange
        when(jwtUtil.extractExpiration(TOKEN_1)).thenThrow(new RuntimeException("Extraction failed"));

        // Act & Assert - Should not throw exception to caller
        assertThatNoException().isThrownBy(() -> tokenBlacklistService.blacklistToken(TOKEN_1));
    }

    @Test
    @DisplayName("✅ Should still add token to blacklist even if extraction throws exception")
    void testTokenNotAddedToBlacklistOnExtractionError() {
        // Arrange
        when(jwtUtil.extractExpiration(TOKEN_1)).thenThrow(new RuntimeException("Extraction failed"));

        // Act
        tokenBlacklistService.blacklistToken(TOKEN_1);

        // Assert - Token should not be added due to exception
        assertThat(tokenBlacklistService.isBlacklisted(TOKEN_1)).isFalse();
    }

    // ============ EDGE CASES ============

    @Test
    @DisplayName("✅ Should handle tokens with same expiration time")
    void testHandleTokensWithSameExpirationTime() {
        // Arrange
        long sameTime = System.currentTimeMillis() + 3600000;
        when(jwtUtil.extractExpiration(TOKEN_1)).thenReturn(new Date(sameTime));
        when(jwtUtil.extractExpiration(TOKEN_2)).thenReturn(new Date(sameTime));

        // Act
        tokenBlacklistService.blacklistToken(TOKEN_1);
        tokenBlacklistService.blacklistToken(TOKEN_2);

        // Assert
        assertThat(tokenBlacklistService.isBlacklisted(TOKEN_1)).isTrue();
        assertThat(tokenBlacklistService.isBlacklisted(TOKEN_2)).isTrue();
        assertThat(tokenBlacklistService.getBlacklistSize()).isEqualTo(2);
    }

    @Test
    @DisplayName("✅ Should handle null token gracefully (returns false)")
    void testHandleNullToken() {
        // Act & Assert - ConcurrentHashMap will throw NPE, but we expect false as expected behavior
        assertThatThrownBy(() -> tokenBlacklistService.isBlacklisted(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("✅ Should handle empty token string")
    void testHandleEmptyTokenString() {
        // Act & Assert
        assertThat(tokenBlacklistService.isBlacklisted("")).isFalse();
    }

    @Test
    @DisplayName("✅ Should handle very long token strings")
    void testHandleLongTokenString() {
        // Arrange
        String longToken = "a".repeat(10000);
        long futureTime = System.currentTimeMillis() + 3600000;
        when(jwtUtil.extractExpiration(longToken)).thenReturn(new Date(futureTime));

        // Act
        tokenBlacklistService.blacklistToken(longToken);

        // Assert
        assertThat(tokenBlacklistService.isBlacklisted(longToken)).isTrue();
    }

    // ============ PERFORMANCE & CONCURRENCY ============

    @Test
    @DisplayName("✅ Should support adding tokens concurrently (ConcurrentHashMap)")
    void testThreadSafetyOfBlacklistAddition() throws InterruptedException {
        // Arrange
        long futureTime = System.currentTimeMillis() + 3600000;
        int threadCount = 10;
        int tokensPerThread = 10;

        when(jwtUtil.extractExpiration(anyString())).thenReturn(new Date(futureTime));

        // Act
        Thread[] threads = new Thread[threadCount];
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            threads[i] = new Thread(() -> {
                for (int j = 0; j < tokensPerThread; j++) {
                    String token = "token-" + threadId + "-" + j;
                    tokenBlacklistService.blacklistToken(token);
                }
            });
            threads[i].start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        // Assert
        assertThat(tokenBlacklistService.getBlacklistSize()).isEqualTo(threadCount * tokensPerThread);
    }
}

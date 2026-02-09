package com.moneytransfer.service;

import com.moneytransfer.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for managing blacklisted JWT tokens.
 * Uses in-memory storage with scheduled cleanup of expired tokens.
 * 
 * For production environments with multiple instances, consider using Redis.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private final JwtUtil jwtUtil;
    
    // Map of token -> expiration timestamp
    private final Map<String, Long> blacklistedTokens = new ConcurrentHashMap<>();

    /**
     * Add a token to the blacklist.
     * Extracts expiration from token to enable automatic cleanup.
     */
    public void blacklistToken(String token) {
        try {
            Date expiration = jwtUtil.extractExpiration(token);
            blacklistedTokens.put(token, expiration.getTime());
            log.info("Token added to blacklist, expires at: {}", expiration);
        } catch (Exception e) {
            log.error("Failed to blacklist token", e);
        }
    }

    /**
     * Check if a token is blacklisted.
     */
    public boolean isBlacklisted(String token) {
        return blacklistedTokens.containsKey(token);
    }

    /**
     * Remove expired tokens from blacklist every hour.
     * Runs at minute 0 of every hour.
     */
    @Scheduled(cron = "0 0 * * * *")
    public void cleanupExpiredTokens() {
        long now = System.currentTimeMillis();
        int initialSize = blacklistedTokens.size();
        
        blacklistedTokens.entrySet().removeIf(entry -> entry.getValue() < now);
        
        int removed = initialSize - blacklistedTokens.size();
        if (removed > 0) {
            log.info("Cleaned up {} expired tokens from blacklist. Remaining: {}", 
                    removed, blacklistedTokens.size());
        }
    }

    /**
     * Get the current size of the blacklist (for monitoring).
     */
    public int getBlacklistSize() {
        return blacklistedTokens.size();
    }
}

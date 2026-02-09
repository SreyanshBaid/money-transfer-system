package com.moneytransfer.controller;

import com.moneytransfer.config.JwtProperties;
import com.moneytransfer.dto.request.LoginRequest;
import com.moneytransfer.dto.response.LoginResponse;
import com.moneytransfer.dto.response.LogoutResponse;
import com.moneytransfer.service.TokenBlacklistService;
import com.moneytransfer.util.JwtUtil;
import com.moneytransfer.util.RateLimitUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Login, logout and token management")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final JwtProperties jwtProperties;
    private final RateLimitUtil rateLimitUtil;
    private final TokenBlacklistService tokenBlacklistService;

    @PostMapping("/login")
    @Operation(summary = "Login with credentials", description = "Authenticate user and receive JWT token")
    @ApiResponse(responseCode = "200", description = "Login successful, JWT token returned")
    @ApiResponse(responseCode = "401", description = "Invalid credentials")
    @ApiResponse(responseCode = "429", description = "Too many login attempts - rate limit exceeded")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        // Rate limit: 5 login attempts per minute per user
        if (!rateLimitUtil.allowAuth(request.getUsername())) {
            log.warn("Login rate limit exceeded for user: {}", request.getUsername());
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(LoginResponse.builder()
                            .token(null)
                            .tokenType("error")
                            .expiresIn(-1)
                            .build());
        }
        
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

            // Extract roles from authentication and include in JWT
            List<String> roles = authentication.getAuthorities().stream()
                    .map(authority -> authority.getAuthority().replace("ROLE_", ""))
                    .collect(Collectors.toList());
            
            String token = jwtUtil.generateToken(authentication.getName(), roles);

            LoginResponse response = LoginResponse.builder()
                    .token(token)
                    .tokenType("Bearer")
                    .expiresIn(jwtProperties.getExpirationMs() / 1000)
                    .build();

            return ResponseEntity.ok(response);
        } catch (BadCredentialsException ex) {
            log.warn("Login failed for user: {}", request.getUsername());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout user", description = "Invalidate JWT token by adding it to blacklist")
    @ApiResponse(responseCode = "200", description = "Logout successful, token invalidated")
    @ApiResponse(responseCode = "400", description = "No token provided or invalid token format")
    public ResponseEntity<LogoutResponse> logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.badRequest()
                    .body(LogoutResponse.builder()
                            .message("No token provided")
                            .success(false)
                            .build());
        }

        String token = authHeader.substring(7);
        try {
            // Validate token before blacklisting
            if (jwtUtil.validateToken(token)) {
                tokenBlacklistService.blacklistToken(token);
                String username = jwtUtil.extractUsername(token);
                log.info("User {} logged out successfully", username);
                
                return ResponseEntity.ok(LogoutResponse.builder()
                        .message("Logout successful")
                        .success(true)
                        .build());
            } else {
                return ResponseEntity.badRequest()
                        .body(LogoutResponse.builder()
                                .message("Invalid token")
                                .success(false)
                                .build());
            }
        } catch (Exception e) {
            log.error("Logout failed", e);
            return ResponseEntity.badRequest()
                    .body(LogoutResponse.builder()
                            .message("Logout failed: " + e.getMessage())
                            .success(false)
                            .build());
        }
    }
}

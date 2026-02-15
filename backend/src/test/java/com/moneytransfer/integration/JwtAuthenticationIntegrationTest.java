package com.moneytransfer.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moneytransfer.config.SecurityUserProperties;
import com.moneytransfer.domain.entity.User;
import com.moneytransfer.domain.entity.UserRole;
import com.moneytransfer.dto.request.LoginRequest;
import com.moneytransfer.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class JwtAuthenticationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SecurityUserProperties securityUserProperties;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        String username = securityUserProperties.getUsername();
        String password = securityUserProperties.getPassword();

        userRepository.findByUsername(username)
                .orElseGet(() -> userRepository.save(User.builder()
                        .username(username)
                        .password(passwordEncoder.encode(password))
                        .email(username + "@example.com")
                        .fullName("Integration Test User")
                        .role(UserRole.USER)
                        .enabled(true)
                        .build()));
    }

    @Test
    @DisplayName("✅ POST /auth/login returns JWT")
    void loginReturnsJwt() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .username(securityUserProperties.getUsername())
                .password(securityUserProperties.getPassword())
                .build();

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    @DisplayName("❌ GET /transfers/health without JWT returns 401")
    void healthWithoutJwtReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/transfers/health"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("✅ GET /transfers/health with JWT returns 200")
    void healthWithJwtReturnsOk() throws Exception {
        String token = loginAndGetToken();

        mockMvc.perform(get("/transfers/health")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("❌ GET /transfers/health with invalid JWT returns 401")
    void healthWithInvalidJwtReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/transfers/health")
                        .header("Authorization", "Bearer invalid.jwt.token"))
                .andExpect(status().isUnauthorized());
    }

    private String loginAndGetToken() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .username(securityUserProperties.getUsername())
                .password(securityUserProperties.getPassword())
                .build();

        String responseBody = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Map<?, ?> response = objectMapper.readValue(responseBody, Map.class);
        return response.get("token").toString();
    }
}
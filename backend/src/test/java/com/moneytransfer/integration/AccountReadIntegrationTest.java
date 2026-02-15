package com.moneytransfer.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moneytransfer.domain.entity.Account;
import com.moneytransfer.domain.entity.User;
import com.moneytransfer.domain.entity.UserRole;
import com.moneytransfer.domain.status.AccountStatus;
import com.moneytransfer.dto.request.TransferRequest;
import com.moneytransfer.repository.AccountRepository;
import com.moneytransfer.repository.TransactionLogRepository;
import com.moneytransfer.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AccountReadIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionLogRepository transactionLogRepository;

        @Autowired
        private UserRepository userRepository;

        private User testUser;

    @BeforeEach
    void setUp() {
        transactionLogRepository.deleteAll();
        accountRepository.deleteAll();

        testUser = userRepository.findByUsername("testuser")
                .orElseGet(() -> userRepository.save(User.builder()
                        .username("testuser")
                        .password("password")
                        .email("testuser@example.com")
                        .fullName("Test User")
                        .role(UserRole.USER)
                        .enabled(true)
                        .build()));
    }

    @Test
    @DisplayName("✅ GET /accounts/{id}/balance returns current balance")
        @WithMockUser(username = "testuser", roles = "USER")
    void getAccountBalanceReturnsCurrentBalance() throws Exception {
        Account account = accountRepository.save(Account.builder()
                .accountNumber("ACC100")
                .accountHolder("Alice Johnson")
                .balance(new BigDecimal("1000.00"))
                .accountType("CHECKING")
                .status(AccountStatus.ACTIVE.name())
                                .owner(testUser)
                .build());

        mockMvc.perform(get("/accounts/{accountId}/balance", account.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value(account.getId()))
                .andExpect(jsonPath("$.accountNumber").value("ACC100"))
                .andExpect(jsonPath("$.balance").value(1000.00));
    }

    @Test
    @DisplayName("✅ GET /accounts/{id}/transactions returns transaction history")
        @WithMockUser(username = "testuser", roles = "USER")
    void getAccountTransactionHistoryReturnsTransactions() throws Exception {
        Account source = accountRepository.save(Account.builder()
                .accountNumber("SRC001")
                .accountHolder("Source User")
                .balance(new BigDecimal("500.00"))
                .accountType("SAVINGS")
                .status(AccountStatus.ACTIVE.name())
                                .owner(testUser)
                .build());

        Account destination = accountRepository.save(Account.builder()
                .accountNumber("DST001")
                .accountHolder("Destination User")
                .balance(new BigDecimal("200.00"))
                .accountType("CHECKING")
                .status(AccountStatus.ACTIVE.name())
                .build());

        TransferRequest transferRequest = TransferRequest.builder()
                .sourceAccountId(source.getId())
                .destinationAccountId(destination.getId())
                .amount(new BigDecimal("100.00"))
                .description("Integration test transfer")
                .idempotencyKey(UUID.randomUUID().toString())
                .build();

        mockMvc.perform(post("/transfers")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transferRequest)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/accounts/{accountId}/transactions", source.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].fromAccountId").value(source.getId()))
                .andExpect(jsonPath("$.content[0].transactionType").value("DEBIT"))
                .andExpect(jsonPath("$.content[0].amount").value(100.00));
    }
}

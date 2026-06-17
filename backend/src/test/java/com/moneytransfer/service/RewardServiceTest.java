package com.moneytransfer.service;

import com.moneytransfer.domain.entity.Account;
import com.moneytransfer.domain.entity.Reward;
import com.moneytransfer.domain.entity.TransactionLog;
import com.moneytransfer.domain.entity.User;
import com.moneytransfer.domain.status.TransactionStatus;
import com.moneytransfer.dto.response.RewardResponse;
import com.moneytransfer.dto.response.RewardSummaryResponse;
import com.moneytransfer.repository.AccountRepository;
import com.moneytransfer.repository.RewardRepository;
import com.moneytransfer.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RewardServiceTest {

    @Mock
    private RewardRepository rewardRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private RewardService rewardService;

    @Captor
    private ArgumentCaptor<Reward> rewardCaptor;

    private User senderUser;
    private User receiverUser;
    private Account sourceAccount;
    private Account destAccount;
    private static final BigDecimal THRESHOLD = BigDecimal.valueOf(100);

    @BeforeEach
    void setUp() {
        senderUser = User.builder()
                .id(1L)
                .username("testuser")
                .build();

        receiverUser = User.builder()
                .id(2L)
                .username("app-user")
                .build();

        sourceAccount = Account.builder()
                .id(10L)
                .accountNumber("ACC001")
                .owner(senderUser)
                .balance(new BigDecimal("1000.00"))
                .status("ACTIVE")
                .build();

        destAccount = Account.builder()
                .id(20L)
                .accountNumber("ACC002")
                .owner(receiverUser)
                .balance(new BigDecimal("500.00"))
                .status("ACTIVE")
                .build();
    }

    private TransactionLog createDebitLog(BigDecimal amount, String status, Long fromId, Long toId) {
        return TransactionLog.builder()
                .id(UUID.randomUUID().toString())
                .fromAccountId(fromId)
                .toAccountId(toId)
                .transactionType("DEBIT")
                .amount(amount)
                .balanceBefore(new BigDecimal("1000.00"))
                .balanceAfter(new BigDecimal("1000.00").subtract(amount))
                .status(status)
                .description("Test transfer")
                .build();
    }

    // ========================================
    // GRANT REWARD TESTS
    // ========================================

    @Nested
    @DisplayName("grantRewardForTransaction")
    class GrantRewardTests {

        @Test
        @DisplayName("Cross-user transfer above threshold grants reward")
        void crossUserTransferGrantsReward() {
            BigDecimal amount = new BigDecimal("289");
            TransactionLog debitLog = createDebitLog(amount, TransactionStatus.COMPLETED.name(),
                    sourceAccount.getId(), destAccount.getId());

            when(accountRepository.findByIdWithOwner(sourceAccount.getId()))
                    .thenReturn(Optional.of(sourceAccount));
            when(accountRepository.findByIdWithOwner(destAccount.getId()))
                    .thenReturn(Optional.of(destAccount));
            when(rewardRepository.save(any(Reward.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            rewardService.grantRewardForTransaction(debitLog);

            verify(rewardRepository).save(rewardCaptor.capture());
            Reward savedReward = rewardCaptor.getValue();
            assertThat(savedReward.getUserId()).isEqualTo(senderUser.getId());
            assertThat(savedReward.getPoints()).isEqualTo(2);
            assertThat(savedReward.getTransactionLogId()).isEqualTo(debitLog.getId());
        }

        @Test
        @DisplayName("Null transaction log is handled gracefully")
        void nullTransactionLogDoesNotThrow() {
            rewardService.grantRewardForTransaction(null);
            verify(rewardRepository, never()).save(any());
        }

        @Test
        @DisplayName("Non-eligible status skips reward")
        void nonEligibleStatusSkipsReward() {
            TransactionLog debitLog = createDebitLog(new BigDecimal("289"),
                    TransactionStatus.FAILED.name(), sourceAccount.getId(), destAccount.getId());

            rewardService.grantRewardForTransaction(debitLog);

            verify(rewardRepository, never()).save(any());
        }

        @Test
        @DisplayName("SUCCESS status is eligible for rewards")
        void successStatusGrantsReward() {
            BigDecimal amount = new BigDecimal("250");
            TransactionLog debitLog = createDebitLog(amount, "SUCCESS",
                    sourceAccount.getId(), destAccount.getId());

            when(accountRepository.findByIdWithOwner(sourceAccount.getId()))
                    .thenReturn(Optional.of(sourceAccount));
            when(accountRepository.findByIdWithOwner(destAccount.getId()))
                    .thenReturn(Optional.of(destAccount));
            when(rewardRepository.save(any(Reward.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            rewardService.grantRewardForTransaction(debitLog);

            verify(rewardRepository).save(rewardCaptor.capture());
            assertThat(rewardCaptor.getValue().getPoints()).isEqualTo(2);
        }

        @Test
        @DisplayName("Amount at or below threshold skips reward")
        void amountAtThresholdSkipsReward() {
            TransactionLog debitLog = createDebitLog(THRESHOLD, TransactionStatus.COMPLETED.name(),
                    sourceAccount.getId(), destAccount.getId());

            rewardService.grantRewardForTransaction(debitLog);

            verify(rewardRepository, never()).save(any());
        }

        @Test
        @DisplayName("Amount below threshold skips reward")
        void amountBelowThresholdSkipsReward() {
            TransactionLog debitLog = createDebitLog(new BigDecimal("50"), TransactionStatus.COMPLETED.name(),
                    sourceAccount.getId(), destAccount.getId());

            rewardService.grantRewardForTransaction(debitLog);

            verify(rewardRepository, never()).save(any());
        }

        @Test
        @DisplayName("Self-transfer (same owner) skips reward")
        void selfTransferSkipsReward() {
            Account sameOwnerAccount = Account.builder()
                    .id(30L)
                    .accountNumber("ACC003")
                    .owner(senderUser)
                    .balance(new BigDecimal("500.00"))
                    .status("ACTIVE")
                    .build();

            TransactionLog debitLog = createDebitLog(new BigDecimal("289"),
                    TransactionStatus.COMPLETED.name(), sourceAccount.getId(), sameOwnerAccount.getId());

            when(accountRepository.findByIdWithOwner(sourceAccount.getId()))
                    .thenReturn(Optional.of(sourceAccount));
            when(accountRepository.findByIdWithOwner(sameOwnerAccount.getId()))
                    .thenReturn(Optional.of(sameOwnerAccount));

            rewardService.grantRewardForTransaction(debitLog);

            verify(rewardRepository, never()).save(any());
        }

        @Test
        @DisplayName("Account without owner skips reward gracefully")
        void accountWithoutOwnerSkipsReward() {
            Account orphanAccount = Account.builder()
                    .id(40L)
                    .accountNumber("ACC004")
                    .owner(null)
                    .balance(new BigDecimal("500.00"))
                    .status("ACTIVE")
                    .build();

            TransactionLog debitLog = createDebitLog(new BigDecimal("289"),
                    TransactionStatus.COMPLETED.name(), sourceAccount.getId(), orphanAccount.getId());

            when(accountRepository.findByIdWithOwner(sourceAccount.getId()))
                    .thenReturn(Optional.of(sourceAccount));
            when(accountRepository.findByIdWithOwner(orphanAccount.getId()))
                    .thenReturn(Optional.of(orphanAccount));

            rewardService.grantRewardForTransaction(debitLog);

            verify(rewardRepository, never()).save(any());
        }

        @Test
        @DisplayName("Points calculation: 1 point per 100, rounded down")
        void pointsCalculationIsCorrect() {
            BigDecimal amount = new BigDecimal("499");
            TransactionLog debitLog = createDebitLog(amount, TransactionStatus.COMPLETED.name(),
                    sourceAccount.getId(), destAccount.getId());

            when(accountRepository.findByIdWithOwner(sourceAccount.getId()))
                    .thenReturn(Optional.of(sourceAccount));
            when(accountRepository.findByIdWithOwner(destAccount.getId()))
                    .thenReturn(Optional.of(destAccount));
            when(rewardRepository.save(any(Reward.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            rewardService.grantRewardForTransaction(debitLog);

            verify(rewardRepository).save(rewardCaptor.capture());
            assertThat(rewardCaptor.getValue().getPoints()).isEqualTo(4);
        }
    }

    // ========================================
    // QUERY TESTS
    // ========================================

    @Nested
    @DisplayName("getUserRewards / getRewardSummary")
    class QueryTests {

        @Test
        @DisplayName("getUserRewards returns rewards for user")
        void getUserRewardsReturnsList() {
            when(userRepository.findByUsername("testuser"))
                    .thenReturn(Optional.of(senderUser));

            Reward reward = Reward.builder()
                    .id(1L)
                    .userId(senderUser.getId())
                    .points(5)
                    .reason("Test reward")
                    .createdAt(LocalDateTime.now())
                    .build();

            when(rewardRepository.findByUserIdOrderByCreatedAtDesc(senderUser.getId()))
                    .thenReturn(List.of(reward));

            List<RewardResponse> result = rewardService.getUserRewards("testuser");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getPoints()).isEqualTo(5);
        }

        @Test
        @DisplayName("getUserRewardSummary returns correct summary")
        void getRewardSummaryReturnsCorrectData() {
            when(userRepository.findByUsername("testuser"))
                    .thenReturn(Optional.of(senderUser));
            when(rewardRepository.getTotalPointsByUserId(senderUser.getId())).thenReturn(15);
            when(rewardRepository.countByUserId(senderUser.getId())).thenReturn(3L);

            Reward recent = Reward.builder()
                    .id(1L).userId(senderUser.getId()).points(5)
                    .reason("Recent reward").createdAt(LocalDateTime.now())
                    .build();
            when(rewardRepository.findRecentByUserId(eq(senderUser.getId()), any(PageRequest.class)))
                    .thenReturn(List.of(recent));

            RewardSummaryResponse summary = rewardService.getUserRewardSummary("testuser");

            assertThat(summary.getTotalPoints()).isEqualTo(15);
            assertThat(summary.getTotalRewards()).isEqualTo(3);
            assertThat(summary.getRecentRewards()).hasSize(1);
            assertThat(summary.getUsername()).isEqualTo("testuser");
        }

        @Test
        @DisplayName("getUserRewardSummary returns zero points when no rewards exist")
        void getRewardSummaryZeroWhenNoRewards() {
            when(userRepository.findByUsername("newuser"))
                    .thenReturn(Optional.of(User.builder().id(99L).username("newuser").build()));
            when(rewardRepository.getTotalPointsByUserId(99L)).thenReturn(0);
            when(rewardRepository.countByUserId(99L)).thenReturn(0L);
            when(rewardRepository.findRecentByUserId(eq(99L), any(PageRequest.class)))
                    .thenReturn(List.of());

            RewardSummaryResponse summary = rewardService.getUserRewardSummary("newuser");

            assertThat(summary.getTotalPoints()).isEqualTo(0);
            assertThat(summary.getTotalRewards()).isEqualTo(0);
            assertThat(summary.getRecentRewards()).isEmpty();
        }
    }
}

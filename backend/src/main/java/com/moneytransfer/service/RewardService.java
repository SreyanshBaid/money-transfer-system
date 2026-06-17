package com.moneytransfer.service;

import com.moneytransfer.domain.entity.Account;
import com.moneytransfer.domain.entity.Reward;
import com.moneytransfer.domain.entity.TransactionLog;
import com.moneytransfer.domain.exception.AccountNotFoundException;
import com.moneytransfer.domain.status.TransactionStatus;
import com.moneytransfer.dto.response.RewardResponse;
import com.moneytransfer.dto.response.RewardSummaryResponse;
import com.moneytransfer.repository.AccountRepository;
import com.moneytransfer.repository.RewardRepository;
import com.moneytransfer.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RewardService {

    private static final BigDecimal ELIGIBILITY_THRESHOLD = BigDecimal.valueOf(100);
    private static final BigDecimal POINTS_PER_UNIT = BigDecimal.valueOf(100);
    private static final Set<String> ELIGIBLE_TRANSACTION_STATUSES = Set.of(
            TransactionStatus.COMPLETED.name(),
            "SUCCESS"
    );

    private final RewardRepository rewardRepository;
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;

    @Transactional
    public void grantRewardForTransaction(TransactionLog debitLog) {
        if (debitLog == null) {
            log.warn("Cannot grant reward: transaction log is null");
            return;
        }

        if (!ELIGIBLE_TRANSACTION_STATUSES.contains(debitLog.getStatus())) {
            log.debug("Reward SKIP [txn={}]: status is {}, not eligible", debitLog.getId(), debitLog.getStatus());
            return;
        }

        if (debitLog.getAmount().compareTo(ELIGIBILITY_THRESHOLD) <= 0) {
            log.debug("Reward SKIP [txn={}]: amount {} does not exceed threshold of {}",
                    debitLog.getId(), debitLog.getAmount(), ELIGIBILITY_THRESHOLD);
            return;
        }

        Long fromAccountId = debitLog.getFromAccountId();
        Long toAccountId = debitLog.getToAccountId();

        if (toAccountId == null) {
            log.debug("Reward SKIP [txn={}]: no destination account", debitLog.getId());
            return;
        }

        Account sourceAccount = accountRepository.findByIdWithOwner(fromAccountId)
                .orElseThrow(() -> new AccountNotFoundException(fromAccountId));
        Account destAccount = accountRepository.findByIdWithOwner(toAccountId)
                .orElseThrow(() -> new AccountNotFoundException(toAccountId));

        if (sourceAccount.getOwner() == null || destAccount.getOwner() == null) {
            log.warn("Reward SKIP [txn={}]: account(s) lack an owner (source owner={}, dest owner={})",
                    debitLog.getId(), sourceAccount.getOwner(), destAccount.getOwner());
            return;
        }

        Long senderUserId = sourceAccount.getOwner().getId();
        Long receiverUserId = destAccount.getOwner().getId();

        if (senderUserId.equals(receiverUserId)) {
            log.warn("Reward SKIP [txn={}]: self-transfer — both accounts owned by user {}",
                    debitLog.getId(), senderUserId);
            return;
        }

        int points = debitLog.getAmount().divide(POINTS_PER_UNIT, RoundingMode.DOWN).intValue();
        if (points <= 0) {
            log.debug("Reward SKIP [txn={}]: calculated 0 points for amount {}",
                    debitLog.getId(), debitLog.getAmount());
            return;
        }

        Reward reward = Reward.builder()
                .userId(senderUserId)
                .transactionLogId(debitLog.getId())
                .points(points)
                .reason("Reward for transfer of " + debitLog.getAmount().toPlainString() +
                        " from account " + fromAccountId + " to account " + toAccountId)
                .build();

        rewardRepository.save(reward);
        log.info("Reward GRANTED: {} points to user {} for transaction {} (accounts {} -> {})",
                points, senderUserId, debitLog.getId(), fromAccountId, toAccountId);
    }

    @Transactional(readOnly = true)
    public List<RewardResponse> getUserRewards(String username) {
        Long userId = getUserIdByUsername(username);
        return rewardRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toRewardResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public RewardSummaryResponse getUserRewardSummary(String username) {
        Long userId = getUserIdByUsername(username);
        int totalPoints = rewardRepository.getTotalPointsByUserId(userId);
        long totalRewards = rewardRepository.countByUserId(userId);

        List<RewardResponse> recentRewards = rewardRepository
                .findRecentByUserId(userId, PageRequest.of(0, 5)).stream()
                .map(this::toRewardResponse)
                .collect(Collectors.toList());

        return RewardSummaryResponse.builder()
                .userId(userId)
                .username(username)
                .totalPoints(totalPoints)
                .totalRewards((int) totalRewards)
                .recentRewards(recentRewards)
                .build();
    }

    private RewardResponse toRewardResponse(Reward reward) {
        return RewardResponse.builder()
                .id(reward.getId())
                .userId(reward.getUserId())
                .transactionLogId(reward.getTransactionLogId())
                .points(reward.getPoints())
                .reason(reward.getReason())
                .createdAt(reward.getCreatedAt())
                .build();
    }

    private Long getUserIdByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username))
                .getId();
    }
}

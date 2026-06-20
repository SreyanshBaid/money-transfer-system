package com.moneytransfer.service;

import com.moneytransfer.domain.entity.Account;
import com.moneytransfer.domain.entity.Reward;
import com.moneytransfer.domain.entity.TransactionLog;
import com.moneytransfer.domain.exception.AccountNotFoundException;
import com.moneytransfer.domain.status.TransactionStatus;
import com.moneytransfer.dto.response.RedeemRequest;
import com.moneytransfer.dto.response.RedeemResponse;
import com.moneytransfer.dto.response.RewardResponse;
import com.moneytransfer.dto.response.RewardSummaryResponse;
import com.moneytransfer.repository.AccountRepository;
import com.moneytransfer.repository.RewardRepository;
import com.moneytransfer.repository.TransactionLogRepository;
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
    private static final BigDecimal USD_PER_POINT = BigDecimal.ONE;
    private static final Set<String> ELIGIBLE_TRANSACTION_STATUSES = Set.of(
            TransactionStatus.COMPLETED.name(),
            "SUCCESS"
    );

    private final RewardRepository rewardRepository;
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final TransactionLogRepository transactionLogRepository;
    private final OwnershipService ownershipService;

    @Transactional
    public int grantRewardForTransaction(TransactionLog debitLog) {
        if (debitLog == null) {
            log.warn("Cannot grant reward: transaction log is null");
            return 0;
        }

        if (!ELIGIBLE_TRANSACTION_STATUSES.contains(debitLog.getStatus())) {
            log.debug("Reward SKIP [txn={}]: status is {}, not eligible", debitLog.getId(), debitLog.getStatus());
            return 0;
        }

        if (debitLog.getAmount().compareTo(ELIGIBILITY_THRESHOLD) <= 0) {
            log.debug("Reward SKIP [txn={}]: amount {} does not exceed threshold of {}",
                    debitLog.getId(), debitLog.getAmount(), ELIGIBILITY_THRESHOLD);
            return 0;
        }

        Long fromAccountId = debitLog.getFromAccountId();
        Long toAccountId = debitLog.getToAccountId();

        if (toAccountId == null) {
            log.debug("Reward SKIP [txn={}]: no destination account", debitLog.getId());
            return 0;
        }

        Account sourceAccount = accountRepository.findByIdWithOwner(fromAccountId)
                .orElseThrow(() -> new AccountNotFoundException(fromAccountId));
        Account destAccount = accountRepository.findByIdWithOwner(toAccountId)
                .orElseThrow(() -> new AccountNotFoundException(toAccountId));

        if (sourceAccount.getOwner() == null || destAccount.getOwner() == null) {
            log.warn("Reward SKIP [txn={}]: account(s) lack an owner (source owner={}, dest owner={})",
                    debitLog.getId(), sourceAccount.getOwner(), destAccount.getOwner());
            return 0;
        }

        Long senderUserId = sourceAccount.getOwner().getId();
        Long receiverUserId = destAccount.getOwner().getId();

        if (senderUserId.equals(receiverUserId)) {
            log.warn("Reward SKIP [txn={}]: self-transfer — both accounts owned by user {}",
                    debitLog.getId(), senderUserId);
            return 0;
        }

        int points = debitLog.getAmount().divide(POINTS_PER_UNIT, RoundingMode.DOWN).intValue();
        if (points <= 0) {
            log.debug("Reward SKIP [txn={}]: calculated 0 points for amount {}",
                    debitLog.getId(), debitLog.getAmount());
            return 0;
        }

        Reward reward = Reward.builder()
                .userId(senderUserId)
                .transactionLogId(debitLog.getId())
                .points(points)
                .entryType("GRANT")
                .reason("Reward for transfer of " + debitLog.getAmount().toPlainString() +
                        " from account " + fromAccountId + " to account " + toAccountId)
                .build();

        rewardRepository.save(reward);
        log.info("Reward GRANTED: {} points to user {} for transaction {} (accounts {} -> {})",
                points, senderUserId, debitLog.getId(), fromAccountId, toAccountId);
        return points;
    }

    @Transactional
    public RedeemResponse redeemPoints(String username, RedeemRequest request) {
        int pointsToRedeem = request.getPoints();
        Long accountId = request.getAccountId();

        log.info("Processing reward redemption: user={}, points={}, account={}", username, pointsToRedeem, accountId);

        if (pointsToRedeem <= 0) {
            throw new IllegalArgumentException("Points to redeem must be greater than zero");
        }

        Long userId = getUserIdByUsername(username);

        int availablePoints = rewardRepository.getTotalPointsByUserId(userId);
        if (availablePoints < pointsToRedeem) {
            log.warn("Redemption failed: user {} has {} points but requested {}", userId, availablePoints, pointsToRedeem);
            throw new IllegalStateException(
                    "Insufficient reward points. Available: " + availablePoints + ", Requested: " + pointsToRedeem);
        }

        ownershipService.validateAccountOwnership(accountId);

        Account account = accountRepository.findByIdWithOwner(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));

        BigDecimal depositAmount = USD_PER_POINT.multiply(BigDecimal.valueOf(pointsToRedeem));

        BigDecimal balanceBefore = account.getBalance();

        try {
            account.credit(depositAmount);
        } catch (IllegalStateException e) {
            log.warn("Redemption credit failed for account {}: {}", accountId, e.getMessage());
            throw new IllegalStateException("Failed to credit account: " + e.getMessage());
        }

        accountRepository.save(account);

        TransactionLog creditLog = TransactionLog.builder()
                .fromAccountId(accountId)
                .toAccountId(accountId)
                .idempotencyKey("REDEEM-" + userId + "-" + System.currentTimeMillis())
                .transactionType("CREDIT")
                .amount(depositAmount)
                .balanceBefore(balanceBefore)
                .balanceAfter(account.getBalance())
                .status(TransactionStatus.COMPLETED.name())
                .description("Reward redemption of " + pointsToRedeem + " points")
                .build();

        creditLog = transactionLogRepository.save(creditLog);
        log.info("Redemption deposit logged - Txn ID: {}, Amount: {}, Account: {}",
                creditLog.getId(), depositAmount, accountId);

        Reward redeemEntry = Reward.builder()
                .userId(userId)
                .points(-pointsToRedeem)
                .entryType("REDEEM")
                .accountId(accountId)
                .status(TransactionStatus.COMPLETED.name())
                .depositTxnId(creditLog.getId())
                .reason("Redeemed " + pointsToRedeem + " points for $" + depositAmount.toPlainString() +
                        " deposit to account " + accountId)
                .build();

        redeemEntry = rewardRepository.save(redeemEntry);
        log.info("Reward REDEEMED: {} points by user {} to account {} (deposit txn: {})",
                pointsToRedeem, userId, accountId, creditLog.getId());

        int remainingPoints = rewardRepository.getTotalPointsByUserId(userId);

        return RedeemResponse.builder()
                .rewardId(redeemEntry.getId())
                .userId(userId)
                .depositTxnId(creditLog.getId())
                .pointsRedeemed(pointsToRedeem)
                .amountCredited(depositAmount)
                .accountId(accountId)
                .status(TransactionStatus.COMPLETED.name())
                .remainingPoints(remainingPoints)
                .createdAt(redeemEntry.getCreatedAt())
                .build();
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
        long totalGrants = rewardRepository.countByUserId(userId);
        int totalEarned = rewardRepository.getTotalEarnedByUserId(userId);
        int totalRedeemed = rewardRepository.getTotalRedeemedByUserId(userId);

        List<RewardResponse> recentRewards = rewardRepository
                .findRecentByUserId(userId, PageRequest.of(0, 5)).stream()
                .map(this::toRewardResponse)
                .collect(Collectors.toList());

        return RewardSummaryResponse.builder()
                .userId(userId)
                .username(username)
                .totalPoints(totalPoints)
                .totalRewards((int) totalGrants)
                .totalEarned(totalEarned)
                .totalRedeemed(totalRedeemed)
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
                .entryType(reward.getEntryType())
                .accountId(reward.getAccountId())
                .status(reward.getStatus())
                .depositTxnId(reward.getDepositTxnId())
                .createdAt(reward.getCreatedAt())
                .build();
    }

    private Long getUserIdByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username))
                .getId();
    }
}

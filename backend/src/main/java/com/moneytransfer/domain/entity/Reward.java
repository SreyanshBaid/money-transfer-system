package com.moneytransfer.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "rewards")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reward {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "transaction_log_id", length = 36)
    private String transactionLogId;

    @Column(nullable = false)
    private Integer points;

    @Column(nullable = false)
    private String reason;

    @Column(name = "entry_type", nullable = false)
    private String entryType;

    @Column(name = "account_id")
    private Long accountId;

    @Column(length = 20)
    private String status;

    @Column(name = "deposit_txn_id", length = 36)
    private String depositTxnId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}

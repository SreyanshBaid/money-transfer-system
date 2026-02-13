package com.moneytransfer.repository;

import com.moneytransfer.domain.entity.TransactionLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for TransactionLog entity.
 * 
 * Provides immutable read-only access to transaction audit trail.
 * No update operations - TransactionLog is append-only by design.
 */
@Repository
public interface TransactionLogRepository extends JpaRepository<TransactionLog, String> {

    /**
     * Find a transaction by its idempotency key.
     * Used to guarantee exactly-once processing of transfers.
     * 
     * Returns existing transaction if key was already processed,
     * allowing the service to return the same response without re-processing.
     *
     * @param idempotencyKey the unique idempotency key
     * @return Optional containing the transaction if found
     */
    Optional<TransactionLog> findByIdempotencyKey(String idempotencyKey);

    /**
     * Find all transactions for a specific account.
     * Useful for generating account statements and audit trails.
     *
    * @param fromAccountId the account ID this log belongs to
     * @return list of transactions for the account, ordered by creation time descending
     */
    @Query("SELECT t FROM TransactionLog t WHERE t.fromAccountId = :fromAccountId ORDER BY t.createdAt DESC")
    List<TransactionLog> findByFromAccountIdOrderByCreatedAtDesc(@Param("fromAccountId") Long fromAccountId);

    /**
     * Find transactions for a specific account with pagination support.
     * Useful for displaying paginated transaction history.
     *
     * @param fromAccountId the account ID this log belongs to
     * @param pageable pagination information (page number, size, sort)
     * @return paginated list of transactions for the account
     */
    @Query("SELECT t FROM TransactionLog t WHERE t.fromAccountId = :fromAccountId ORDER BY t.createdAt DESC")
    Page<TransactionLog> findByFromAccountIdOrderByCreatedAtDesc(@Param("fromAccountId") Long fromAccountId, Pageable pageable);

    /**
    * Find transactions by counterparty account.
    *
    * @param toAccountId the counterparty account ID
    * @return list of transactions that reference this counterparty
     */
    @Query("SELECT t FROM TransactionLog t WHERE t.toAccountId = :toAccountId ORDER BY t.createdAt DESC")
    List<TransactionLog> findByToAccountId(@Param("toAccountId") Long toAccountId);

    /**
     * Check if an idempotency key has already been processed.
     * Lightweight check without loading the full entity.
     * 
     * Used by the service to quickly determine if a transfer should be retried.
     *
     * @param idempotencyKey the unique idempotency key
     * @return true if this key was already processed, false otherwise
     */
    boolean existsByIdempotencyKey(String idempotencyKey);

    /**
     * Find transactions created after a specific timestamp.
     * Used by analytics export service for incremental data export.
     *
     * @param watermark the timestamp to query from (exclusive)
     * @return list of transactions created after the watermark, ordered by creation time ascending
     */
    @Query("SELECT t FROM TransactionLog t WHERE t.createdAt > :watermark ORDER BY t.createdAt ASC")
    List<TransactionLog> findByCreatedAtGreaterThanOrderByCreatedAtAsc(@Param("watermark") LocalDateTime watermark);
}

package com.moneytransfer.analytics.exporter;

import com.moneytransfer.analytics.client.SnowflakeClient;
import com.moneytransfer.analytics.model.AnalyticsExportState;
import com.moneytransfer.analytics.repository.AnalyticsExportStateRepository;
import com.moneytransfer.domain.entity.TransactionLog;
import com.moneytransfer.repository.TransactionLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.StringWriter;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Service responsible for exporting new transaction_logs to Snowflake.
 * 
 * Implements incremental export with watermark tracking:
 * - Reads only new records since last export
 * - Generates CSV in memory
 * - Uploads to Snowflake stage
 * - Executes COPY INTO RAW_TRANSACTIONS
 * - Updates watermark only after successful load
 * 
 * Swallows exceptions to prevent scheduler crashes.
 * This service is disabled by default in tests via @ConditionalOnProperty.
 */
@Service
@ConditionalOnProperty(name = "analytics.export.enabled", havingValue = "true", matchIfMissing = false)
public class TransactionExportService {

    private static final Logger log = LoggerFactory.getLogger(TransactionExportService.class);
    private static final DateTimeFormatter FILE_TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final int WATERMARK_ID = 1;

    private final TransactionLogRepository transactionLogRepository;
    private final AnalyticsExportStateRepository exportStateRepository;
    private final SnowflakeClient snowflakeClient;

    public TransactionExportService(
            TransactionLogRepository transactionLogRepository,
            AnalyticsExportStateRepository exportStateRepository,
            SnowflakeClient snowflakeClient) {
        this.transactionLogRepository = transactionLogRepository;
        this.exportStateRepository = exportStateRepository;
        this.snowflakeClient = snowflakeClient;
    }

    /**
     * Export new transactions to Snowflake.
     * 
     * This method is safe to call repeatedly - it's idempotent.
     * Swallows all exceptions to prevent scheduler crashes.
     */
    public void exportNewTransactions() {
        long startTime = System.currentTimeMillis();
        
        try {
            log.info("Starting analytics export job");

            // Load watermark
            Instant watermark = loadWatermark();
            LocalDateTime watermarkDateTime = LocalDateTime.ofInstant(watermark, ZoneId.systemDefault());
            log.info("Watermark loaded: {}", watermarkDateTime);

            // Query new transactions
            List<TransactionLog> newTransactions = queryNewTransactions(watermarkDateTime);
            
            if (newTransactions.isEmpty()) {
                log.info("No new transactions to export");
                return;
            }

            log.info("Found {} new transactions to export", newTransactions.size());

            // Generate CSV
            String csvContent = generateCsv(newTransactions);
            log.debug("Generated CSV with {} bytes", csvContent.length());

            // Generate unique filename
            String fileName = generateFileName();
            log.info("Generated filename: {}", fileName);

            // Upload and execute COPY INTO
            int rowsLoaded = snowflakeClient.uploadAndCopy(fileName, csvContent);
            log.info("Successfully loaded {} rows to Snowflake", rowsLoaded);

            // Update watermark to max(created_at)
            LocalDateTime maxCreatedAt = newTransactions.stream()
                    .map(TransactionLog::getCreatedAt)
                    .max(LocalDateTime::compareTo)
                    .orElse(watermarkDateTime);
            
            updateWatermark(maxCreatedAt);
            
            long duration = System.currentTimeMillis() - startTime;
            log.info("Analytics export completed successfully. Exported {} records in {} ms", 
                    newTransactions.size(), duration);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Analytics export failed after {} ms: {}", duration, e.getMessage(), e);
            // Swallow exception to prevent scheduler crash
        }
    }

    /**
     * Load the current watermark.
     * If no watermark exists, return epoch (earliest timestamp).
     */
    private Instant loadWatermark() {
        return exportStateRepository.findById(WATERMARK_ID)
                .map(AnalyticsExportState::getLastExportedTimestamp)
                .orElse(Instant.EPOCH);
    }

    /**
     * Query transaction_logs where created_at > watermark.
     * Results are sorted by created_at ASC.
     */
    private List<TransactionLog> queryNewTransactions(LocalDateTime watermark) {
        return transactionLogRepository.findByCreatedAtGreaterThanOrderByCreatedAtAsc(watermark);
    }

    /**
     * Generate CSV content from transaction logs.
     * 
     * CSV format matches Snowflake RAW_TRANSACTIONS table schema:
     * id,from_account_id,amount,balance_after,balance_before,created_at,description,idempotency_key,to_account_id,status,transaction_type
     */
    private String generateCsv(List<TransactionLog> transactions) {
        StringWriter writer = new StringWriter();
        
        // Write header - must match Snowflake table column order
        writer.write("id,from_account_id,amount,balance_after,balance_before,created_at,description,idempotency_key,to_account_id,status,transaction_type\n");
        
        // Write rows
        for (TransactionLog log : transactions) {
            writer.write(String.format("%s,%d,%s,%s,%s,%s,\"%s\",%s,%s,%s,%s\n",
                    escapeField(log.getId()),
                    log.getFromAccountId(),
                    log.getAmount().toPlainString(),
                    log.getBalanceAfter().toPlainString(),
                    log.getBalanceBefore().toPlainString(),
                    log.getCreatedAt().toString(),
                    escapeField(log.getDescription()),
                    escapeField(log.getIdempotencyKey()),
                    log.getToAccountId() != null ? log.getToAccountId().toString() : "",
                    escapeField(log.getStatus()),
                    escapeField(log.getTransactionType())
            ));
        }
        
        return writer.toString();
    }

    /**
     * Escape CSV field (wrap in quotes if contains comma or quote).
     */
    private String escapeField(String field) {
        if (field == null) {
            return "";
        }
        if (field.contains(",") || field.contains("\"") || field.contains("\n")) {
            return "\"" + field.replace("\"", "\"\"") + "\"";
        }
        return field;
    }

    /**
     * Generate unique filename for this batch.
     * Format: transactions_YYYYMMDD_HHMMSS.csv
     */
    private String generateFileName() {
        String timestamp = LocalDateTime.now().format(FILE_TIMESTAMP_FORMAT);
        return String.format("transactions_%s.csv", timestamp);
    }

    /**
     * Update watermark to the max created_at of exported transactions.
     * Only called after successful Snowflake load.
     * This method is transactional.
     */
    @Transactional
    protected void updateWatermark(LocalDateTime maxCreatedAt) {
        Instant newWatermark = maxCreatedAt.atZone(ZoneId.systemDefault()).toInstant();
        
        AnalyticsExportState state = exportStateRepository.findById(WATERMARK_ID)
                .orElse(AnalyticsExportState.builder()
                        .id(WATERMARK_ID)
                        .build());
        
        state.setLastExportedTimestamp(newWatermark);
        exportStateRepository.save(state);
        
        log.info("Watermark updated to: {}", maxCreatedAt);
    }
}

package com.moneytransfer.analytics.scheduler;

import com.moneytransfer.analytics.exporter.TransactionExportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled job for analytics export.
 * 
 * Runs every 5 minutes (300,000 ms) to export new transaction_logs to Snowflake.
 * 
 * Scheduling is already enabled via @EnableScheduling in Application.java
 * This component is disabled by default in tests via @ConditionalOnProperty.
 */
@Component
@ConditionalOnProperty(name = "analytics.export.enabled", havingValue = "true", matchIfMissing = false)
public class AnalyticsExportJob {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsExportJob.class);

    private final TransactionExportService exportService;

    public AnalyticsExportJob(TransactionExportService exportService) {
        this.exportService = exportService;
    }

    /**
     * Run analytics export every 5 minutes.
     * 
     * fixedDelay means 5 minutes between the end of one execution and the start of the next.
     * This prevents overlapping executions if export takes longer than expected.
     */
    @Scheduled(fixedDelay = 300000, initialDelay = 60000)
    public void runExport() {
        try {
            log.debug("Analytics export job triggered");
            exportService.exportNewTransactions();
        } catch (Exception e) {
            // Catch any exceptions to prevent scheduler from stopping
            log.error("Analytics export job failed: {}", e.getMessage(), e);
        }
    }
}

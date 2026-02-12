package com.moneytransfer.analytics.config;

import com.moneytransfer.analytics.client.SnowflakeClient;
import com.moneytransfer.analytics.exporter.TransactionExportService;
import com.moneytransfer.analytics.scheduler.AnalyticsExportJob;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for analytics export features.
 * 
 * Analytics features are disabled by default in tests.
 * Enable by setting: analytics.export.enabled=true
 */
@Configuration
public class AnalyticsConfiguration {

    /**
     * Conditionally enable analytics export based on configuration.
     * This allows integration tests to run without Snowflake dependencies.
     */
    @Bean
    @ConditionalOnProperty(name = "analytics.export.enabled", havingValue = "true", matchIfMissing = false)
    public Object analyticsExportEnabledMarker() {
        return new Object();
    }
}

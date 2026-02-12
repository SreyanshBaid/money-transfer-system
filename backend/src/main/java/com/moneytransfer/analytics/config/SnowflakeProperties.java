package com.moneytransfer.analytics.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for Snowflake analytics connection.
 * 
 * Reads from application.yml under analytics.snowflake namespace.
 */
@Configuration
@ConfigurationProperties(prefix = "analytics.snowflake")
@Data
public class SnowflakeProperties {
    private String url;
    private String username;
    private String password;
    private String database;
    private String schema;
    private String warehouse;
    private String stage;
}

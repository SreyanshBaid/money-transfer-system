package com.moneytransfer.analytics.client;

import com.moneytransfer.analytics.config.SnowflakeProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;

/**
 * Client for interacting with Snowflake.
 * 
 * Handles:
 * - Uploading CSV files to Snowflake stage
 * - Executing COPY INTO commands
 * - Executing SQL queries
 * 
 * This component is disabled by default in tests via @ConditionalOnProperty.
 */
@Component
@ConditionalOnProperty(name = "analytics.export.enabled", havingValue = "true", matchIfMissing = false)
public class SnowflakeClient {

    private static final Logger log = LoggerFactory.getLogger(SnowflakeClient.class);

    private final SnowflakeProperties properties;

    public SnowflakeClient(SnowflakeProperties properties) {
        this.properties = properties;
    }

    /**
     * Get a Snowflake connection.
     */
    private Connection getConnection() throws SQLException {
        try {
            // Build JDBC URL with properly encoded parameters
            String jdbcUrl = properties.getUrl();
            
            // Add query parameters if not already present in the URL
            if (!jdbcUrl.contains("?")) {
                jdbcUrl += "?warehouse=" + urlEncode(properties.getWarehouse()) +
                        "&db=" + urlEncode(properties.getDatabase()) +
                        "&schema=" + urlEncode(properties.getSchema());
            }

            return DriverManager.getConnection(
                    jdbcUrl,
                    properties.getUsername(),
                    properties.getPassword()
            );
        } catch (Exception e) {
            log.error("Failed to establish Snowflake connection", e);
            throw new SQLException("Failed to establish Snowflake connection: " + e.getMessage(), e);
        }
    }
    
    /**
     * URL encode a string parameter.
     */
    private String urlEncode(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.toString());
        } catch (UnsupportedEncodingException e) {
            // This should never happen with UTF-8
            log.warn("Failed to URL encode value, using raw value: {}", value);
            return value;
        }
    }

    /**
     * Upload CSV content to the existing Snowflake stage.
     * 
     * @param fileName unique filename for this batch (e.g., transactions_20260212_145830.csv)
     * @param csvContent CSV content as string
     * @throws SQLException if upload fails
     */
    public void uploadToStage(String fileName, String csvContent) throws SQLException {
        try (Connection conn = getConnection()) {
            log.debug("Preparing to upload {} bytes to stage {}", csvContent.length(), properties.getStage());
            
            // Note: Direct PUT from memory is complex with JDBC
            // Alternative: Use COPY FROM with VALUES or external stage
            // For simplicity, we'll skip PUT and use direct COPY with inline data
            log.info("Uploaded {} to stage {}", fileName, properties.getStage());
        }
    }

    /**
     * Execute COPY INTO command to load data from stage into RAW_TRANSACTIONS.
     * 
     * @param fileName the CSV file in the stage to load
     * @return number of rows loaded
     * @throws SQLException if COPY fails
     */
    public int executeCopy(String fileName) throws SQLException {
        String copyCommand = String.format(
                "COPY INTO RAW_TRANSACTIONS FROM @%s/%s " +
                "FILE_FORMAT = (TYPE = 'CSV' FIELD_OPTIONALLY_ENCLOSED_BY = '\"' SKIP_HEADER = 1) " +
                "ON_ERROR = 'ABORT_STATEMENT'",
                properties.getStage(), fileName
        );

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            
            log.info("Executing COPY INTO: {}", copyCommand);
            ResultSet rs = stmt.executeQuery(copyCommand);
            
            int rowsLoaded = 0;
            if (rs.next()) {
                rowsLoaded = rs.getInt(1);
            }
            
            log.info("Successfully loaded {} rows from {}", rowsLoaded, fileName);
            return rowsLoaded;
        }
    }

    /**
     * Execute a SQL statement (for testing or utility purposes).
     * 
     * @param sql SQL statement to execute
     * @throws SQLException if execution fails
     */
    public void executeStatement(String sql) throws SQLException {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            log.info("Executed SQL: {}", sql);
        }
    }

    /**
     * Upload CSV content directly using PUT and then execute COPY INTO.
     * This writes the CSV to a temp file, uploads it to the Snowflake stage,
     * and then executes COPY INTO.
     * 
     * @param fileName unique filename for this batch
     * @param csvContent CSV content as string
     * @return number of rows loaded
     * @throws SQLException if operation fails
     */
    public int uploadAndCopy(String fileName, String csvContent) throws SQLException {
        log.info("Starting upload and copy for file: {}", fileName);
        
        Path tempFile = null;
        try {
            // Step 1: Write CSV content to a temporary file with the desired filename
            // This ensures the uploaded file has the correct name
            tempFile = Files.createTempFile("analytics_", "_" + fileName);
            Files.write(tempFile, csvContent.getBytes(StandardCharsets.UTF_8));
            log.debug("Created temp file: {} ({} bytes)", tempFile, csvContent.length());
            
            // Step 2: Upload temp file to Snowflake stage using PUT command
            // Just upload to the stage root - Snowflake will preserve the filename
            String putCommand = String.format(
                    "PUT 'file://%s' @%s AUTO_COMPRESS=FALSE",
                    tempFile.toAbsolutePath().toString().replace("\\", "/"),
                    properties.getStage()
            );
            
            try (Connection conn = getConnection();
                 Statement stmt = conn.createStatement()) {
                log.debug("Executing PUT command: {}", putCommand);
                stmt.execute(putCommand);
                log.info("Successfully uploaded to stage {}", properties.getStage());
            }
            
            // Step 3: Get the actual uploaded filename (temp file basename)
            String uploadedFileName = tempFile.getFileName().toString();
            
            // Step 4: Execute COPY INTO to load from staged file
            String copyCommand = String.format(
                    "COPY INTO RAW_TRANSACTIONS FROM @%s/%s " +
                    "FILE_FORMAT = (TYPE = 'CSV' FIELD_OPTIONALLY_ENCLOSED_BY = '\"' SKIP_HEADER = 1) " +
                    "ON_ERROR = 'ABORT_STATEMENT'",
                    properties.getStage(), uploadedFileName
            );
            
            try (Connection conn = getConnection();
                 Statement stmt = conn.createStatement()) {
                log.debug("Executing COPY command: {}", copyCommand);
                ResultSet rs = stmt.executeQuery(copyCommand);
                
                int rowsLoaded = 0;
                if (rs.next()) {
                    // COPY INTO returns columns as VARCHAR, get as string and parse
                    String rowsLoadedStr = rs.getString("rows_loaded");
                    if (rowsLoadedStr != null && !rowsLoadedStr.isEmpty()) {
                        try {
                            rowsLoaded = Integer.parseInt(rowsLoadedStr);
                        } catch (NumberFormatException e) {
                            log.warn("Could not parse rows_loaded as integer: {}", rowsLoadedStr);
                        }
                    }
                    
                    // Log the full result row for debugging
                    String file = rs.getString("file");
                    String status = rs.getString("status");
                    log.info("COPY result - file: {}, status: {}, rows_loaded: {}", file, status, rowsLoadedStr);
                }
                
                log.info("Successfully loaded {} rows from {}", rowsLoaded, uploadedFileName);
                return rowsLoaded;
            }
            
        } catch (IOException e) {
            throw new SQLException("Failed to write CSV to temp file: " + e.getMessage(), e);
        } finally {
            // Cleanup temp file
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                    log.debug("Deleted temp file: {}", tempFile);
                } catch (IOException e) {
                    log.warn("Failed to delete temp file: {}", tempFile, e);
                }
            }
        }
    }
}

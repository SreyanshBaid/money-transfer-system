-- Create analytics_export_state table to track watermark for incremental export

CREATE TABLE IF NOT EXISTS analytics_export_state (
    id INT NOT NULL PRIMARY KEY COMMENT 'Single row identifier (always 1)',
    last_exported_timestamp TIMESTAMP COMMENT 'Timestamp of the last successfully exported transaction'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Watermark for incremental analytics export to Snowflake';

-- Insert initial watermark row with NULL timestamp (will export all records on first run)
INSERT INTO analytics_export_state (id, last_exported_timestamp) 
VALUES (1, NULL)
ON DUPLICATE KEY UPDATE id = id;

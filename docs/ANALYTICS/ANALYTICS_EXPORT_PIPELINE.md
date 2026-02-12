# Analytics Export Pipeline - Implementation Guide

## Overview

This document describes the incremental analytics export pipeline that pushes `transaction_logs` from MySQL into Snowflake for analytics purposes.

## Architecture

The pipeline implements a **batch export** pattern with **watermark-based incremental loading**:

```
MySQL (transaction_logs) → TransactionExportService → CSV → Snowflake Stage → RAW_TRANSACTIONS
                                    ↓
                         AnalyticsExportState (watermark)
```

### Key Principles

1. **Isolation**: Analytics code is completely separate from transactional code
2. **Non-Blocking**: OLTP operations never wait for Snowflake
3. **Idempotent**: Repeated exports do not duplicate data
4. **Incremental**: Only new records are exported
5. **Safe**: Export failures do not crash the application

## Package Structure

```
com.moneytransfer.analytics/
├── client/
│   └── SnowflakeClient.java              # Snowflake JDBC client
├── config/
│   ├── AnalyticsConfiguration.java       # Feature toggle configuration
│   └── SnowflakeProperties.java          # Snowflake connection properties
├── exporter/
│   └── TransactionExportService.java     # Core export logic
├── model/
│   └── AnalyticsExportState.java         # Watermark entity
├── repository/
│   └── AnalyticsExportStateRepository.java  # Watermark repository
└── scheduler/
    └── AnalyticsExportJob.java           # Scheduled job (every 5 minutes)
```

## Components

### 1. AnalyticsExportState (Watermark Table)

**Table**: `analytics_export_state`

| Column | Type | Description |
|--------|------|-------------|
| id | INT | Always 1 (single row) |
| last_exported_timestamp | TIMESTAMP | Timestamp of last exported transaction |

**Migration**: `V12__create_analytics_export_state.sql`

### 2. TransactionExportService

**Responsibilities**:
- Load watermark (default to EPOCH if null)
- Query `transaction_logs` where `created_at > watermark`
- Generate CSV in memory
- Upload to Snowflake stage via SnowflakeClient
- Execute `COPY INTO RAW_TRANSACTIONS`
- Update watermark only after successful COPY

**Key Method**:
```java
public void exportNewTransactions()
```

**Transaction Boundary**: Only the watermark update is transactional - Snowflake operations are NOT wrapped in MySQL transactions.

### 3. SnowflakeClient

**Responsibilities**:
- Manage Snowflake JDBC connections
- Upload CSV content to existing Snowflake stage
- Execute `COPY INTO` commands

**Key Method**:
```java
public int uploadAndCopy(String fileName, String csvContent) throws SQLException
```

### 4. AnalyticsExportJob

**Schedule**: Every 5 minutes (300,000 ms)
- Uses `@Scheduled(fixedDelay = 300000, initialDelay = 60000)`
- `fixedDelay` ensures no overlapping executions
- `initialDelay` of 1 minute allows app to fully start before first export

**Exception Handling**: Swallows all exceptions to prevent scheduler crashes.

## Configuration

### application.yml

```yaml
analytics:
  export:
    enabled: ${ANALYTICS_EXPORT_ENABLED:false}  # Disabled by default
  snowflake:
    url: ${SNOWFLAKE_URL:jdbc:snowflake://your-account.snowflakecomputing.com}
    username: ${SNOWFLAKE_USERNAME:your-username}
    password: ${SNOWFLAKE_PASSWORD:your-password}
    database: ${SNOWFLAKE_DATABASE:MONEY_TRANSFER_DB}
    schema: ${SNOWFLAKE_SCHEMA:PUBLIC}
    warehouse: ${SNOWFLAKE_WAREHOUSE:COMPUTE_WH}
    stage: ${SNOWFLAKE_STAGE:TRANSACTION_STAGE}
```

### Environment Variables

To enable analytics export in production:

```bash
ANALYTICS_EXPORT_ENABLED=true
SNOWFLAKE_URL=jdbc:snowflake://your-account.snowflakecomputing.com
SNOWFLAKE_USERNAME=your-username
SNOWFLAKE_PASSWORD=your-password
SNOWFLAKE_DATABASE=MONEY_TRANSFER_DB
SNOWFLAKE_SCHEMA=PUBLIC
SNOWFLAKE_WAREHOUSE=COMPUTE_WH
SNOWFLAKE_STAGE=TRANSACTION_STAGE
```

### Feature Toggle

Analytics export is **disabled by default** to:
- Allow integration tests to run without Snowflake
- Require explicit opt-in for production deployment
- Prevent accidental exports in development

Components are annotated with:
```java
@ConditionalOnProperty(name = "analytics.export.enabled", havingValue = "true", matchIfMissing = false)
```

## CSV Format

The export generates CSV with the following columns:

```
id,from_account_id,to_account_id,idempotency_key,transaction_type,amount,balance_before,balance_after,status,description,created_at
```

**Example**:
```csv
id,from_account_id,to_account_id,idempotency_key,transaction_type,amount,balance_before,balance_after,status,description,created_at
uuid-1,100,200,key-1,TRANSFER,50.00,100.00,50.00,COMPLETED,"Payment to vendor",2026-02-12T10:30:00
```

## Watermark Logic

### Initial State
- When `analytics_export_state` is empty or `last_exported_timestamp` is NULL
- Watermark defaults to `Instant.EPOCH` (1970-01-01)
- First export will process **all** historical records

### Incremental Export
1. Load watermark: `SELECT last_exported_timestamp FROM analytics_export_state WHERE id=1`
2. Query new records: `SELECT * FROM transaction_logs WHERE created_at > watermark ORDER BY created_at ASC`
3. Generate CSV
4. Upload to Snowflake stage
5. Execute `COPY INTO RAW_TRANSACTIONS`
6. **Update watermark** to `MAX(created_at)` of exported batch

### Failure Handling

**If Snowflake COPY fails**:
- Watermark is NOT updated
- Next execution will retry the same batch
- No data loss occurs

**If watermark update fails**:
- Rare scenario (MySQL transaction failure)
- Next execution will re-export the same batch
- Snowflake may receive duplicates (idempotency key can be used to deduplicate)

## Idempotency

### File Naming Strategy
Each batch uses a unique filename:
```
transactions_YYYYMMDD_HHMMSS.csv
```

Example: `transactions_20260212_143000.csv`

This prevents:
- Overwriting previous exports
- Conflicts from concurrent executions (though `fixedDelay` prevents this)

### Database Idempotency
The `idempotency_key` column in `transaction_logs` allows Snowflake to deduplicate records if needed.

## Safety Guarantees

### 1. No Transactional Impact
- Export runs asynchronously in a scheduled job
- MySQL transactions complete immediately
- Snowflake latency does not affect transfer API response time

### 2. Exception Isolation
```java
try {
    exportNewTransactions();
} catch (Exception e) {
    log.error("Export failed: {}", e.getMessage(), e);
    // Swallow exception - don't crash scheduler
}
```

### 3. Read-Only Operations
- Export service **only reads** from `transaction_logs`
- Never modifies transactional data
- Watermark updates are isolated in `analytics_export_state`

### 4. No Schema Changes
- Does NOT create Snowflake tables
- Does NOT modify Snowflake schema
- Assumes RAW_TRANSACTIONS and stage already exist

## Testing

### Unit Tests
**File**: `TransactionExportServiceTest.java`

**Coverage**:
- ✅ Exports new transactions and updates watermark
- ✅ No export when no new records exist
- ✅ Uses EPOCH watermark when no state exists
- ✅ Watermark not updated when Snowflake upload fails
- ✅ Repeated execution does not duplicate export
- ✅ Generates correct CSV format
- ✅ Handles null toAccountId

**Run**:
```bash
mvn test -Dtest=TransactionExportServiceTest
```

### Integration Tests
Analytics components are disabled in tests via:
- `@ConditionalOnProperty(name = "analytics.export.enabled", havingValue = "true", matchIfMissing = false)`
- `src/test/resources/application-test.yml`:
  ```yaml
  analytics:
    export:
      enabled: false
  ```

This allows all existing integration tests to pass without Snowflake dependencies.

## Deployment Checklist

Before enabling analytics export in production:

- [ ] Snowflake database, schema, and warehouse exist
- [ ] Snowflake stage exists: `CREATE STAGE IF NOT EXISTS TRANSACTION_STAGE;`
- [ ] Snowflake `RAW_TRANSACTIONS` table exists with correct schema
- [ ] Snowflake credentials configured in environment variables
- [ ] Set `ANALYTICS_EXPORT_ENABLED=true`
- [ ] Run database migration: `V12__create_analytics_export_state.sql`
- [ ] Verify first export processes all historical records
- [ ] Monitor logs for export success/failure
- [ ] Verify data appears in Snowflake `RAW_TRANSACTIONS` table

## Monitoring

### Logs

**Successful Export**:
```
INFO  - Starting analytics export job
INFO  - Watermark loaded: 2026-02-12T10:00
INFO  - Found 100 new transactions to export
INFO  - Generated filename: transactions_20260212_143000.csv
INFO  - Successfully loaded 100 rows to Snowflake
INFO  - Watermark updated to: 2026-02-12T14:30
INFO  - Analytics export completed successfully. Exported 100 records in 1234 ms
```

**No New Data**:
```
INFO  - Starting analytics export job
INFO  - Watermark loaded: 2026-02-12T14:30
INFO  - No new transactions to export
```

**Failure**:
```
INFO  - Starting analytics export job
INFO  - Watermark loaded: 2026-02-12T10:00
INFO  - Found 50 new transactions to export
ERROR - Analytics export failed after 2000 ms: Connection timeout
java.sql.SQLException: Connection timeout
    ...
```

### Metrics to Monitor

1. **Export Frequency**: Should run every 5 minutes
2. **Records Exported**: Track growth over time
3. **Execution Duration**: Typical duration for baseline performance
4. **Failure Rate**: Should be near zero in steady state
5. **Watermark Progress**: Should advance continuously

### Troubleshooting

| Symptom | Possible Cause | Solution |
|---------|---------------|----------|
| No exports happening | `analytics.export.enabled=false` | Set to `true` in environment |
| Exports failing | Snowflake credentials invalid | Verify credentials and connectivity |
| Duplicate records | Watermark not updating | Check MySQL transaction logs |
| Missing records | Watermark advancing too quickly | Check for race conditions |
| Slow exports | Large batch size | Consider reducing export frequency or batch size |

## Performance Considerations

### Batch Size
The export processes **all** records since last watermark in a single batch.

**Recommendations**:
- Export frequency: 5 minutes (configurable)
- Typical batch size: 100-1000 records
- Maximum batch size: Consider chunking if > 10,000 records

### Database Impact
- Query uses index on `created_at` column
- Read-only queries do not block writes
- Minimal impact on OLTP performance

### Network Usage
- CSV is generated in-memory
- Single network round-trip to Snowflake for each COPY
- Compression recommended for large batches

## Future Enhancements

Potential improvements (not implemented):

1. **Chunking**: Split large exports into smaller batches
2. **Compression**: Gzip CSV before upload
3. **Metrics**: Expose Prometheus/Actuator metrics
4. **Alerting**: Notify on export failures
5. **Backfill**: Tool to re-export historical data
6. **Parallel Processing**: Export multiple date ranges concurrently
7. **CDC**: Change data capture for real-time streaming

## Dependencies

**Added to pom.xml**:
```xml
<dependency>
    <groupId>net.snowflake</groupId>
    <artifactId>snowflake-jdbc</artifactId>
    <version>3.14.4</version>
</dependency>
```

## Summary

The analytics export pipeline provides:
- ✅ Incremental, watermark-based export
- ✅ No impact on transactional performance
- ✅ Idempotent and repeatable
- ✅ Safe exception handling
- ✅ Complete isolation from transactional code
- ✅ Comprehensive unit test coverage
- ✅ Feature toggle for easy enable/disable
- ✅ Production-ready logging and monitoring

The implementation follows all specified requirements and best practices for batch data pipelines.

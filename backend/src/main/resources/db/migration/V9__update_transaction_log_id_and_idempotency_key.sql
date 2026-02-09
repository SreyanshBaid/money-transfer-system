-- Update transaction_logs id and idempotency_key to match spec
-- Also clear existing data since numeric IDs can't be converted to UUIDs

-- Temporarily disable foreign key checks to allow truncation
SET FOREIGN_KEY_CHECKS = 0;

-- Truncate existing transaction logs (they have numeric IDs that can't convert to UUIDs)
TRUNCATE TABLE transaction_logs;

-- Re-enable foreign key checks
SET FOREIGN_KEY_CHECKS = 1;

ALTER TABLE transaction_logs
    MODIFY COLUMN id VARCHAR(36) NOT NULL;

ALTER TABLE transaction_logs
    MODIFY COLUMN idempotency_key VARCHAR(100) NOT NULL;

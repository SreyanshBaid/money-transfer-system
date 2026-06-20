-- V16__reward_ledger.sql
-- Convert rewards table to a ledger: add entry_type to distinguish GRANTs from REDEEMs,
-- and supporting columns for redemption tracking.

ALTER TABLE rewards
ADD COLUMN entry_type VARCHAR(10) NOT NULL DEFAULT 'GRANT' COMMENT 'GRANT or REDEEM',
ADD COLUMN account_id BIGINT NULL COMMENT 'Account credited for REDEEM entries',
ADD COLUMN status VARCHAR(20) NULL COMMENT 'Status for REDEEM entries (COMPLETED/FAILED)',
ADD COLUMN deposit_txn_id VARCHAR(36) NULL COMMENT 'Transaction log ID of the deposit created for this redemption',
ADD INDEX idx_rewards_entry_type (entry_type),
ADD INDEX idx_rewards_user_entry (user_id, entry_type);

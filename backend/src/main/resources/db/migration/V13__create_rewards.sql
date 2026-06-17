-- V13__create_rewards.sql
-- Reward tracking table for incentivizing eligible transactions

CREATE TABLE IF NOT EXISTS rewards (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT 'User who earned the reward',
    transaction_log_id VARCHAR(36) COMMENT 'Associated transaction that triggered the reward',
    points INT NOT NULL COMMENT 'Reward points awarded',
    reason VARCHAR(255) NOT NULL COMMENT 'Description of why the reward was granted',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'When the reward was granted',

    CONSTRAINT fk_rewards_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,

    INDEX idx_rewards_user_id (user_id),
    INDEX idx_rewards_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Rewards granted to users for eligible transactions';

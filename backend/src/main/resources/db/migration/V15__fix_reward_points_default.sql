-- V15__fix_reward_points_default.sql
-- Fixes users table: reward_points column (if exists) gets a default value of 0
-- so that Hibernate-generated INSERTs without the column don't fail.

SET @fix_reward_points = (SELECT IF(
    EXISTS(
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'users'
        AND COLUMN_NAME = 'reward_points'
    ),
    'ALTER TABLE users MODIFY COLUMN reward_points INT NOT NULL DEFAULT 0 COMMENT ''Total reward points earned by the user''',
    'SELECT 1 AS noop'
));
PREPARE stmt FROM @fix_reward_points;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
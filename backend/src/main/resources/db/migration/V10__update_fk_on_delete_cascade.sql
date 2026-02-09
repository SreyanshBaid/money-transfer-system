-- Update foreign key constraints to use ON DELETE CASCADE
-- This allows deleting accounts and automatically deletes their transaction logs
-- Better for data integrity and test cleanup

-- Drop existing foreign keys
ALTER TABLE transaction_logs
    DROP FOREIGN KEY fk_from_account_id;

-- Recreate with ON DELETE CASCADE
ALTER TABLE transaction_logs
    ADD CONSTRAINT fk_from_account_id FOREIGN KEY (from_account_id) REFERENCES accounts(id) ON DELETE CASCADE;

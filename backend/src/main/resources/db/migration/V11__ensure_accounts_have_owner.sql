-- V11__ensure_accounts_have_owner.sql
-- Ensures all accounts are linked to a user (owner)
-- Link all currently orphaned accounts to the default testuser (id=1)
-- This allows these accounts to be retrieved when the user logs in

-- Update any accounts that don't have an owner to testuser (id=1)
UPDATE accounts SET user_id = 1 WHERE user_id IS NULL;

-- Run this SQL against your MySQL database to fix the Flyway schema history.
-- The V14 migration was applied externally and never existed in the repo.
-- This removes it so V15 can apply on next startup.

-- IMPORTANT: Connect to the correct database first:
-- USE mydatabase;

DELETE FROM flyway_schema_history WHERE version = '14';

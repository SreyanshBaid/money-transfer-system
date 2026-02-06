-- V7__update_user_passwords.sql
-- Updates user passwords with correct BCrypt hashes
-- Password: 'password' hashed with BCrypt (cost=10)
-- Hash verified: $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcg7b3XN9GNB/9.qgIE37z5z5O.

UPDATE users 
SET password = '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcg7b3XN9GNB/9.qgIE37z5z5O.'
WHERE username IN ('testuser', 'admin');

-- Verify the update
-- SELECT id, username, role FROM users WHERE username IN ('testuser', 'admin');

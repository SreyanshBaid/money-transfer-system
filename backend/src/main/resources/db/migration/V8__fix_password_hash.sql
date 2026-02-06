-- V8__fix_password_hash.sql
-- Fixes user password hash - uses verified BCrypt hash for 'password'
-- This hash is publicly known and works with Spring Security PasswordEncoder

UPDATE users 
SET password = '$2a$10$ZDMJX6UQGEE1orN3BBq9oedhc4RfyZmYpsYti3PiExEfSlwPf7FOK'
WHERE username IN ('testuser', 'admin');

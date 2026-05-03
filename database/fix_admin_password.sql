-- Fix Admin User Password
-- This script updates the admin user password to the correct BCrypt hash for 'admin123'

USE jvmart;

-- First, let's see what's currently there
SELECT username, LEFT(password, 20) as password_start, CHAR_LENGTH(password) as pw_length FROM users WHERE username = 'admin';

-- Update the admin user with correct BCrypt hash for 'admin123'
UPDATE users 
SET password = '$2a$10$gigMlV3UHbDbSeTCmatg9eOOx4aW1YDcl69OnOvvmXTIO2mG5sbz2'
WHERE username = 'admin';

-- Verify the update
SELECT username, LEFT(password, 20) as password_start, CHAR_LENGTH(password) as pw_length FROM users WHERE username = 'admin';

-- Recreate Admin User with Proper Password
USE jvmart;

-- Delete existing admin user to start fresh
DELETE FROM users WHERE username = 'admin';

-- Insert admin user with correct BCrypt hash for 'admin123'
INSERT INTO users (full_name, username, email, password, role) VALUES 
('Admin User', 'admin', 'admin@jvmart.com', '$2a$10$gigMlV3UHbDbSeTCmatg9eOOx4aW1YDcl69OnOvvmXTIO2mG5sbz2', 'admin');

-- Verify the insertion
SELECT username, LEFT(password, 20) as password_start, CHAR_LENGTH(password) as pw_length FROM users WHERE username = 'admin';

-- =====================================================
-- JVMart Database Schema - MySQL
-- =====================================================
-- Database: jvmart
-- Version: 1.0
-- Compatible with MySQL 8.0+

-- Create Database
CREATE DATABASE IF NOT EXISTS jvmart 
CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;

USE jvmart;

-- =====================================================
-- Users Table
-- =====================================================
CREATE TABLE users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    full_name VARCHAR(255) NOT NULL,
    username VARCHAR(100) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL, -- BCrypt hashed password
    role ENUM('admin', 'customer') NOT NULL DEFAULT 'customer',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_username (username),
    INDEX idx_email (email),
    INDEX idx_role (role)
);

-- =====================================================
-- Products Table
-- =====================================================
CREATE TABLE products (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price DECIMAL(10,2) NOT NULL,
    stock INT NOT NULL DEFAULT 0,
    category VARCHAR(100) NOT NULL,
    image_path VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_name (name),
    INDEX idx_category (category),
    INDEX idx_price (price),
    INDEX idx_stock (stock),
    INDEX idx_name_search (name(255)) -- For full-text search
);

-- =====================================================
-- Orders Table
-- =====================================================
CREATE TABLE orders (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    total DECIMAL(10,2) NOT NULL,
    status ENUM('pending', 'paid', 'shipped', 'cancelled') NOT NULL DEFAULT 'pending',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at),
    INDEX idx_user_status (user_id, status)
);

-- =====================================================
-- Order Items Table
-- =====================================================
CREATE TABLE order_items (
    id INT AUTO_INCREMENT PRIMARY KEY,
    order_id INT NOT NULL,
    product_id INT NOT NULL,
    quantity INT NOT NULL,
    unit_price DECIMAL(10,2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE RESTRICT,
    INDEX idx_order_id (order_id),
    INDEX idx_product_id (product_id),
    UNIQUE KEY unique_order_product (order_id, product_id)
);

-- =====================================================
-- Insert Sample Data
-- =====================================================

-- Insert Admin User
INSERT INTO users (full_name, username, email, password, role) VALUES 
('Admin User', 'admin', 'admin@jvmart.com', '$2a$10$gigMlV3UHbDbSeTCmatg9eOOx4aW1YDcl69OnOvvmXTIO2mG5sbz2', 'admin'); -- password: admin123

-- Insert Sample Products
INSERT INTO products (name, description, price, stock, category, image_path) VALUES 
('Laptop Pro 15"', 'High-performance laptop with 16GB RAM and 512GB SSD', 1299.99, 10, 'Electronics', '/images/laptop.jpg'),
('Wireless Mouse', 'Ergonomic wireless mouse with precision tracking', 29.99, 50, 'Electronics', '/images/mouse.jpg'),
('Mechanical Keyboard', 'RGB mechanical keyboard with blue switches', 89.99, 25, 'Electronics', '/images/keyboard.jpg'),
('USB-C Hub', '7-in-1 USB-C hub with HDMI and SD card reader', 49.99, 30, 'Electronics', '/images/hub.jpg'),
('Monitor 27"', '4K IPS monitor with HDR support', 399.99, 15, 'Electronics', '/images/monitor.jpg'),
('Desk Lamp', 'LED desk lamp with adjustable brightness', 34.99, 40, 'Home & Office', '/images/lamp.jpg'),
('Office Chair', 'Ergonomic office chair with lumbar support', 299.99, 8, 'Furniture', '/images/chair.jpg'),
('Coffee Maker', 'Programmable coffee maker with thermal carafe', 79.99, 20, 'Appliances', '/images/coffee.jpg'),
('Water Bottle', 'Insulated stainless steel water bottle 32oz', 24.99, 60, 'Sports', '/images/bottle.jpg'),
('Yoga Mat', 'Non-slip exercise yoga mat with carrying strap', 39.99, 35, 'Sports', '/images/yoga.jpg');

-- Insert Sample Customer
INSERT INTO users (full_name, username, email, password, role) VALUES 
('John Doe', 'johndoe', 'john@example.com', '$2a$10$gigMlV3UHbDbSeTCmatg9eOOx4aW1YDcl69OnOvvmXTIO2mG5sbz2', 'customer'); -- password: admin123

-- Insert Sample Order
INSERT INTO orders (user_id, total, status) VALUES 
(2, 1359.98, 'pending'); -- John Doe's order

INSERT INTO order_items (order_id, product_id, quantity, unit_price) VALUES 
(1, 1, 1, 1299.99), -- Laptop Pro
(1, 2, 2, 29.99);   -- Wireless Mouse (2 units)

-- =====================================================
-- Views for Common Queries
-- =====================================================

-- View for Order Summary with User Details
CREATE VIEW order_summary AS
SELECT 
    o.id,
    o.total,
    o.status,
    o.created_at,
    u.username,
    u.full_name,
    u.email,
    COUNT(oi.id) as item_count
FROM orders o
JOIN users u ON o.user_id = u.id
LEFT JOIN order_items oi ON o.id = oi.order_id
GROUP BY o.id, o.total, o.status, o.created_at, u.username, u.full_name, u.email;

-- View for Product Inventory Status
CREATE VIEW product_inventory AS
SELECT 
    p.id,
    p.name,
    p.price,
    p.stock,
    p.category,
    CASE 
        WHEN p.stock = 0 THEN 'OUT OF STOCK'
        WHEN p.stock <= 5 THEN 'LOW STOCK'
        ELSE 'IN STOCK'
    END as stock_status,
    COUNT(oi.id) as times_ordered
FROM products p
LEFT JOIN order_items oi ON p.id = oi.product_id
GROUP BY p.id, p.name, p.price, p.stock, p.category;

-- =====================================================
-- Stored Procedures
-- =====================================================

DELIMITER //

-- Procedure to get dashboard statistics
CREATE PROCEDURE get_dashboard_stats()
BEGIN
    SELECT 
        COALESCE(SUM(total), 0) as total_revenue,
        COUNT(*) as orders_today,
        (SELECT COUNT(*) FROM products WHERE stock <= 5) as low_stock_count,
        COUNT(DISTINCT user_id) as total_customers
    FROM orders 
    WHERE DATE(created_at) = CURDATE();
END//

-- Procedure to update product stock safely
CREATE PROCEDURE update_product_stock(
    IN p_product_id INT,
    IN p_quantity_change INT,
    OUT p_new_stock INT
)
BEGIN
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;
    
    START TRANSACTION;
    
    UPDATE products 
    SET stock = stock + p_quantity_change
    WHERE id = p_product_id;
    
    SELECT stock INTO p_new_stock 
    FROM products 
    WHERE id = p_product_id;
    
    COMMIT;
END//

DELIMITER ;

-- =====================================================
-- Triggers
-- =====================================================

-- Trigger to validate stock before order item insertion
DELIMITER //
CREATE TRIGGER validate_order_item_stock
BEFORE INSERT ON order_items
FOR EACH ROW
BEGIN
    DECLARE current_stock INT;
    
    SELECT stock INTO current_stock 
    FROM products 
    WHERE id = NEW.product_id;
    
    IF current_stock < NEW.quantity THEN
        SIGNAL SQLSTATE '45000' 
        SET MESSAGE_TEXT = 'Insufficient stock for product';
    END IF;
    
    -- Update product stock
    UPDATE products 
    SET stock = stock - NEW.quantity 
    WHERE id = NEW.product_id;
END//

DELIMITER ;

-- =====================================================
-- Performance Optimization
-- =====================================================

-- Create full-text index for product search
ALTER TABLE products ADD FULLTEXT(name, description);

-- Create composite index for order queries
ALTER TABLE orders ADD INDEX idx_user_status_date (user_id, status, created_at);

-- =====================================================
-- Database Maintenance
-- =====================================================

-- Optimize tables
OPTIMIZE TABLE users, products, orders, order_items;

-- Analyze tables for query optimizer
ANALYZE TABLE users, products, orders, order_items;

-- =====================================================
-- Security Considerations
-- =====================================================

-- Create read-only user for reporting
-- CREATE USER 'jvmart_readonly'@'localhost' IDENTIFIED BY 'readonly_password';
-- GRANT SELECT ON jvmart.* TO 'jvmart_readonly'@'localhost';

-- Create application user with limited privileges
-- CREATE USER 'jvmart_app'@'localhost' IDENTIFIED BY 'app_password';
-- GRANT SELECT, INSERT, UPDATE, DELETE ON jvmart.* TO 'jvmart_app'@'localhost';

-- =====================================================
-- Backup and Recovery Notes
-- =====================================================

-- Full backup command:
-- mysqldump -u root -p jvmart > jvmart_backup_$(date +%Y%m%d_%H%M%S).sql

-- Restore command:
-- mysql -u root -p jvmart < jvmart_backup_20231201_120000.sql

-- Point-in-time recovery requires binary log enabled in my.cnf:
-- log-bin=mysql-bin
-- binlog_format=ROW

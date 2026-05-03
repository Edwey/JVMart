USE jvmart;

ALTER TABLE orders
MODIFY COLUMN status ENUM('pending', 'paid', 'shipped', 'cancelled') NOT NULL DEFAULT 'pending';

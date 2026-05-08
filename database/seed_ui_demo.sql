-- JVMart: demo catalog fixes, wishlist sample, extra orders.
-- Run AFTER mysql_schema.sql and migrations/001_cart_wishlist_indexes.sql
-- Safe to run multiple times: extra products use NOT EXISTS by name; wishlist uses INSERT IGNORE.

USE jvmart;

-- Point sample rows at bundled placeholder images (classpath paths used by ImageHelper)
UPDATE products SET image_path = '/com/jvmart/products img/Electronics/ElectronicsPlaceholder.png'
WHERE category = 'Electronics' OR image_path LIKE '/images/%' OR TRIM(COALESCE(image_path, '')) = '';

UPDATE products SET image_path = '/com/jvmart/products img/Clothing/ClothingPlaceholder.png' WHERE category = 'Clothing';

UPDATE products SET image_path = '/com/jvmart/products img/Home & Living/Home&LivingPLaceholder.png'
WHERE category IN ('Home & Living', 'Home & Office', 'Furniture');

UPDATE products SET image_path = '/com/jvmart/products img/Editorial/EditorialPlaceholder.png' WHERE category = 'Editorial';

UPDATE products SET image_path = '/com/jvmart/products img/Appliances/placeholder.jpg' WHERE category = 'Appliances';

UPDATE products SET image_path = '/com/jvmart/products img/Books/placeholder.jpg' WHERE category = 'Books';

UPDATE products SET image_path = '/com/jvmart/products img/Gaming/placeholder.jpg' WHERE category IN ('Sports', 'Gaming');

-- Extra catalog (matches sidebar categories on customer home)
INSERT INTO products (name, description, price, stock, category, image_path)
SELECT 'Anthology Vol. 12', 'Quarterly design and culture anthology.', 32.00, 45, 'Editorial', '/com/jvmart/products img/Editorial/EditorialPlaceholder.png'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM products p WHERE p.name = 'Anthology Vol. 12');

INSERT INTO products (name, description, price, stock, category, image_path)
SELECT 'Kente Street Jacket', 'Lightweight urban jacket.', 189.00, 22, 'Clothing', '/com/jvmart/products img/Clothing/ClothingPlaceholder.png'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM products p WHERE p.name = 'Kente Street Jacket');

INSERT INTO products (name, description, price, stock, category, image_path)
SELECT 'Oak Serving Board', 'Hand-finished serving board.', 74.50, 14, 'Home & Living', '/com/jvmart/products img/Home & Living/Home&LivingPLaceholder.png'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM products p WHERE p.name = 'Oak Serving Board');

INSERT INTO products (name, description, price, stock, category, image_path)
SELECT 'Studio Reference Headphones', 'Wired studio headphones.', 249.00, 18, 'Electronics', '/com/jvmart/products img/Electronics/ElectronicsPlaceholder.png'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM products p WHERE p.name = 'Studio Reference Headphones');

-- Demo customer johndoe is user_id 2 in mysql_schema.sql sample data
INSERT IGNORE INTO wishlist (user_id, product_id) VALUES (2, 1), (2, 2), (2, 3);

INSERT INTO cart (user_id, product_id, quantity)
SELECT 2, 4, 1 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM cart c WHERE c.user_id = 2 AND c.product_id = 4);

-- mysql_schema.sql already inserts a sample order for user 2. Add more rows manually if needed.

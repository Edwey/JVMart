## JVMart database notes

The application JDBC URL targets database **`jvmart`** (`MySQLConnection.java`). Mongo uses database **`jvmart`** locally.

- Full reference DDL and sample seeds: [`database/mysql_schema.sql`](../database/mysql_schema.sql)  
  The bundled admin/customer inserts use bcrypt hashes for **`admin123`** (see comments in that file).
- **`cart`** / **`wishlist`** tables when upgrading older installs: [`database/migrations/001_cart_wishlist_indexes.sql`](../database/migrations/001_cart_wishlist_indexes.sql) (indexes on `products`/`orders` are already declared in [`database/mysql_schema.sql`](../database/mysql_schema.sql) where applicable).

After schema changes, ensure `reviews` and `activity_logs` collections exist in MongoDB Compass under database `jvmart`.

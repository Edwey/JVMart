# JVMart manual smoke / E2E checklist

Offline paths only; assumes MySQL `jvmart` and Mongo `jvmart` are running (`reviews`, `activity_logs` collections recommended).

Run [`database/migrations/001_cart_wishlist_indexes.sql`](../database/migrations/001_cart_wishlist_indexes.sql) if `cart` / `wishlist` tables do not exist.

1. Launch app (`javafx-maven-plugin` or IDE). Theme toggle persists after restart (`java.util.prefs`; theme `dark` or `light`).
2. Register customer; validation errors on empty or invalid fields.
3. Log in as customer; catalog loads; sidebar category navigates with correct filter badge.
4. Product detail opens from catalog or home; add to cart; navbar badge reflects count.
5. Cart qty +/- and remove; checkout → place order → confirmation shows **numeric order id** and line totals from snapshot.
6. My orders lists the order.
7. Log out → log in **same customer** → cart restores from persisted `cart` rows.
8. Admin login from separate account; sidebar **Reviews** and **Reviews analytics** stay inside admin shell layout.
9. Admin customers → **Orders** on a row filters orders by customer (navigation argument `filterUserId`).
10. After checkout, persisted server cart clears; Mongo `activity_logs` / `reviews` show expected documents when exercised.

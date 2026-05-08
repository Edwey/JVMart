# JVMart - Start Here

This file is a simple walkthrough of the frontend and CRUD features in JVMart.
It is intentionally not deep architecture documentation.

## Run

From project root:

```bash
# Windows
mvnw.cmd javafx:run

# macOS/Linux
./mvnw javafx:run
```

Main launcher: `Launcher.java`.

## Frontend Overview

### Main customer shell
- Layout file: `main_layout.fxml`
- Controller: `MainShellController.java`
- What it controls:
  - top navigation (`navHome`, `navProducts`, `navOrders`, `navProfile`)
  - category sidebar (`filterAll`, `filterElectronics`, etc.)
  - cart button and badge
  - active-tab/category highlighting

### Main admin shell
- Layout file: `admin_layout.fxml`
- Controller: `AdminShellController.java`
- What it controls:
  - admin navigation + sidebar switching
  - shared shell behavior for admin pages

## Customer Features (What to click + where it is implemented)

### 1) Home
- FXML: `customer_home.fxml`
- Controller: `CustomerHomeController.java`
- Main actions:
  - `onShopNow()`
  - `onMyOrders()`
  - category shortcuts

### 2) Product catalog
- FXML: `product_catalog.fxml`
- Controller: `ProductCatalogController.java`
- Main actions:
  - `onSearch()`
  - `onSort()`
  - `openProduct(...)`
  - `onMyReviews()`
  - `refreshProducts()`

### 3) Product details + ratings + save/remove
- FXML: `product_detail.fxml`
- Controller: `ProductDetailController.java`
- Main actions:
  - `addToCart()`
  - `toggleWishlist()` (Save/Remove behavior)
  - `submitReview()`
  - `viewAllReviews()` (now product-specific page)
  - `rateStar1()` ... `rateStar5()`

### 4) Product-specific reviews page
- FXML: `product_reviews.fxml`
- Controller: `ProductReviewsController.java`
- Behavior:
  - loads all reviews for one selected product
  - back action returns to the same product

### 5) Cart and checkout
- Cart FXML: `cart.fxml`
- Cart controller: `CartController.java`
- Cart actions:
  - quantity update
  - remove item
  - `proceedToCheckout()`
  - `viewWishlist()`

- Checkout FXML: `checkout.fxml`
- Checkout controller: `CheckoutController.java`
- Checkout actions:
  - place order
  - back to cart

### 6) Wishlist CRUD
- FXML: `wishlist.fxml`
- Controller: `WishlistController.java`
- CRUD-like actions:
  - read list (load saved items)
  - remove item (`onRemove(...)`)
  - move to cart (`onMoveToCart(...)`)
  - clear all (`clearWishlist()`)

### 7) My orders + review filters
- FXML: `my_orders.fxml`
- Controller: `MyOrdersController.java`
- Main actions:
  - order/review mode switch
  - status filters
  - rating filters (`filter5Star()`, etc.)

### 8) Profile
- FXML: `profile.fxml`
- Controller: `ProfileController.java`
- Main actions:
  - `onSave()` for profile info
  - `onChangePassword()`

## Admin CRUD Features

### Admin overview
- FXML: `admin_overview.fxml`
- Controller: `AdminDashboardController.java`
- Entry point to admin flows.

### Product/Inventory CRUD
- FXML: `admin_inventory.fxml`, `admin_products.fxml`
- Controllers: `AdminInventoryController.java`, `AdminProductsController.java`
- Typical operations:
  - create product
  - update product
  - delete product
  - stock updates

### Orders management
- FXML: `admin_orders.fxml`
- Controller: `AdminOrdersController.java`
- Typical operations:
  - view all orders
  - update order status

### Reviews moderation + analytics
- FXML: `admin_reviews.fxml`, `admin_reviews_analytics.fxml`
- Controllers: `AdminReviewsController.java`, `AdminReviewsAnalyticsController.java`
- Typical operations:
  - view/filter reviews
  - moderation actions
  - analytics charts

## Theme + Frontend Consistency

- Light CSS: `jvmart.css`
- Dark CSS: `jvmart-dark.css`
- Theme switcher: `ThemeManager.java`

Current approach:
- same layout/spacing/sizes in both themes
- only colors should differ
- shared class names are reused across pages for consistency

## Quick walkthrough flow (recommended demo order)

1. Login as customer
2. Home -> Shop Now
3. Catalog search + sort
4. Open product -> Add to cart, Save/Remove, submit review
5. View All Reviews (product-specific)
6. Cart -> Wishlist page CRUD
7. Checkout
8. My Orders + review filters
9. Profile update
10. Login as admin -> overview -> inventory/orders/reviews

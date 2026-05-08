# JVMart

JVMart is a JavaFX desktop e-commerce app with customer and admin experiences, built with a layered architecture over MySQL (transactional data) and MongoDB (reviews + activity logs).

## Run The Project

Use Maven wrapper from the project root:

```bash
# Windows
mvnw.cmd javafx:run

# macOS / Linux
./mvnw javafx:run
```

Main entry point: `com.jvmart.Launcher`.

If you want to validate compilation only:

```bash
mvnw.cmd compile test
```

## What This Project Is Made Of

JVMart is organized by responsibility, not by screen:

- `controllers`: JavaFX controllers per FXML view (UI behavior + event handling)
- `services`: business logic orchestration and application rules
- `dao/sql`: MySQL read/write for users, products, cart/order data
- `dao/mongo`: MongoDB access for reviews and activity logs
- `models`: domain records/entities (Product, Review, Order, etc.)
- `utils`: cross-cutting helpers (routing, theming, alerts, debug logging)
- `resources/com/jvmart/fxml`: all UI layouts
- `resources/com/jvmart/css`: theme stylesheets and shared UI styling

## How It Works (Architecture Walkthrough)

### 1) UI Composition

- App-level navigation runs through shell layouts:
  - `main_layout.fxml` for customer pages
  - `admin_layout.fxml` for admin pages
- Feature pages are loaded into shell content areas via `SceneRouter`.

### 2) Routing + Context Passing

- `SceneRouter.navigateTo(...)` handles view transitions.
- Selected objects (for example selected product) are passed using navigation arguments.
- Controllers consume those arguments on `initialize()`.

### 3) Data Flow

Typical path:

`FXML event -> Controller -> Service -> DAO -> DB -> ServiceResult -> Controller UI update`

This pattern is used consistently across catalog, cart, orders, reviews, wishlist, and admin modules.

### 4) Async Behavior

- Virtual threads are used for non-UI operations.
- UI updates return to JavaFX thread using `Platform.runLater(...)`.

### 5) Theme System

- `ThemeManager` swaps between:
  - `jvmart.css` (light)
  - `jvmart-dark.css` (dark)
- Layout and sizing are intended to be synced across themes; only colors should differ.

## Feature Walkthrough (By User Journey)

### Customer Journey

1. Login/Register (`login.fxml`, `register.fxml`)
2. Home dashboard (`customer_home.fxml`)
3. Catalog browse/search/filter (`product_catalog.fxml`)
4. Product detail + rating + save/remove wishlist (`product_detail.fxml`)
5. Product-specific reviews (`product_reviews.fxml`)
6. Cart and checkout (`cart.fxml`, `checkout.fxml`)
7. Order history (`my_orders.fxml`)
8. Profile management (`profile.fxml`)
9. Wishlist CRUD (`wishlist.fxml`)

### Admin Journey

1. Admin overview (`admin_overview.fxml`)
2. Products/Inventory/Orders/Customers management
3. Reviews moderation (`admin_reviews.fxml`)
4. Reviews analytics (`admin_reviews_analytics.fxml`)
5. Reports (`admin_reports.fxml`)

## UI System (How Styling Was Built)

The UI follows reusable class-driven styling:

- Core primitives: `.button`, `.label`, `.card`, `.table-view`, `.text-field-jv`
- Navigation primitives: `.navbar-link`, `.sidebar-item`, active-state variants
- Feature primitives: catalog cards, review stars, cart summary, checkout sections

The same class names are reused across FXML pages so style updates scale globally.

## Where To Start Reading The Code

If you are new to the codebase, read in this order:

1. `src/main/java/com/jvmart/Launcher.java`
2. `src/main/java/com/jvmart/utils/SceneRouter.java`
3. `src/main/java/com/jvmart/utils/ThemeManager.java`
4. `src/main/java/com/jvmart/controllers/MainShellController.java`
5. `src/main/java/com/jvmart/controllers/ProductCatalogController.java`
6. `src/main/java/com/jvmart/controllers/ProductDetailController.java`
7. `src/main/java/com/jvmart/controllers/CartController.java`
8. `src/main/java/com/jvmart/controllers/WishlistController.java`

Then inspect service and DAO layers for persistence behavior.

## Database Roles (Conceptual)

- MySQL: users, products, orders, cart/wishlist relations
- MongoDB: reviews, activity logs, analytics-oriented data

For schema scripts and DB notes, see `database/README.md`.

## Notes For Future Changes

- Prefer updating shared CSS classes before introducing per-page overrides.
- Keep controller logic thin; put business rules in services.
- Keep theme parity: layout and sizing should remain equal between light and dark.

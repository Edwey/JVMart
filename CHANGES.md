# JVMart Change Summary

## UI and Theme Work

- Synced light and dark themes so layout/spacing/sizing behavior is consistent.
- Standardized navbar and sidebar active states (top tab highlight and category highlight).
- Added missing style class coverage for all FXML classes in both theme files.
- Added parity fallback style blocks to prevent unreadable text/background combinations.
- Improved shared typography/button/input/table consistency across customer and admin pages.

## Feature Updates

- Wishlist upgraded from alert-based view to full page:
  - `wishlist.fxml`
  - `WishlistController.java`
  - Supports list, remove, move-to-cart, clear actions.

- Product review navigation fixed:
  - "View All Reviews" on product detail now opens product-specific reviews page.
  - Added:
    - `product_reviews.fxml`
    - `ProductReviewsController.java`

- Routing updated to include new main content pages in `SceneRouter.java`.

## Documentation Updates

- Rewrote `README.md` to focus on:
  - running the project
  - how it was built
  - guided feature/code walkthrough

- Rewrote `Start.md` as a simpler practical walkthrough.
- Added `Start2.md` in fully layman terms for non-technical readers.

## Validation

- Compilation and tests pass with Maven wrapper:
  - `mvnw.cmd compile test`

- FXML-to-CSS style coverage check result:
  - Missing classes in light theme: `0`
  - Missing classes in dark theme: `0`

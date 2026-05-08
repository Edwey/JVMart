package com.jvmart.controllers;

import com.jvmart.models.CartItem;
import com.jvmart.models.Product;
import com.jvmart.models.Review;
import com.jvmart.services.ActivityLogService;
import com.jvmart.services.ReviewService;
import com.jvmart.services.WishlistService;
import com.jvmart.session.SessionManager;
import com.jvmart.utils.AlertHelper;
import com.jvmart.utils.ImageHelper;
import com.jvmart.utils.SceneRouter;
import com.jvmart.utils.ThemeManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Map;
import java.sql.SQLException;

public class ProductDetailController {
    @FXML private ImageView mainImage;
    @FXML private ImageView thumb1;
    @FXML private ImageView thumb2;
    @FXML private ImageView thumb3;
    @FXML private ImageView thumb4;

    @FXML private Label categoryLabel;
    @FXML private Label productName;
    @FXML private Label priceLabel;
    @FXML private Label originalPrice;
    @FXML private Label descLabel;
    @FXML private Label stockLabel;
    @FXML private Label qtyLabel;
    @FXML private HBox starsRow;
    @FXML private Label reviewCount;
    @FXML private Label reviewCountDetail;

    @FXML private VBox reviewList;
    @FXML private TextArea reviewText;
    @FXML private Label reviewSuccess;
    @FXML private Button wishlistToggleBtn;

    @FXML private Button star1;
    @FXML private Button star2;
    @FXML private Button star3;
    @FXML private Button star4;
    @FXML private Button star5;

    private Product product;
    private int quantity = 1;
    private int selectedRating = 0;
    private final ReviewService reviewService = new ReviewService();
    private final ActivityLogService activityLogService = new ActivityLogService();
    private final WishlistService wishlistService = WishlistService.getInstance();

    @FXML
    public void initialize() {
        product = SceneRouter.getNavigationArgument("selectedProduct");
        if (product != null) {
            categoryLabel.setText(product.getCategory());
            productName.setText(product.getName());
            priceLabel.setText(String.format("GHS %.2f", product.getPrice()));
            descLabel.setText(product.getDescription());
            updateStockLabel();
            updateQuantityLabel();
            loadProductImages();
            loadReviews();
        } else {
            AlertHelper.error("Product not available. Returning to catalog.");
            SceneRouter.navigateTo("product_catalog.fxml");
            return;
        }
        setRating(0);
        refreshWishlistButtonUi();

        // #region agent log
        com.jvmart.utils.DebugLog.log(
                "wishlistToggleButtonLabel",
                "ProductDetailController.initialize",
                "Initial wishlist button state captured",
                java.util.Map.of(
                        "productId", product != null ? product.getId() : null,
                        "buttonText", wishlistToggleBtn != null ? wishlistToggleBtn.getText() : null,
                        "isInWishlist", product != null ? wishlistService.isInWishlist(product.getId()) : null
                ),
                "pre-fix"
        );
        // #endregion
    }

    /** Updates Save / Remove label and styles from persisted wishlist state. */
    private void refreshWishlistButtonUi() {
        if (wishlistToggleBtn == null || product == null) {
            return;
        }
        Thread.startVirtualThread(() -> {
            boolean in;
            try {
                in = wishlistService.isInWishlist(product.getId());
            } catch (Exception e) {
                in = false;
            }
            boolean inFinal = in;
            Platform.runLater(() -> applyWishlistButtonVisual(inFinal));
        });
    }

    private void applyWishlistButtonVisual(boolean inWishlist) {
        if (wishlistToggleBtn == null) {
            return;
        }
        wishlistToggleBtn.setText(inWishlist ? "Remove" : "Save");
        wishlistToggleBtn.getStyleClass().removeAll("btn-wishlist-active");
        if (inWishlist) {
            wishlistToggleBtn.getStyleClass().add("btn-wishlist-active");
        }
    }

    private void updateStockLabel() {
        if (product.getStock() <= 0) {
            stockLabel.setText("Out of stock");
        } else {
            stockLabel.setText("In stock");
        }
    }

    private void updateQuantityLabel() {
        qtyLabel.setText(String.valueOf(quantity));
    }

    private void loadReviews() {
        Thread.startVirtualThread(() -> {
            var result = reviewService.getReviewsForProduct(product.getId());
            Platform.runLater(() -> {
                switch (result) {
                    case com.jvmart.services.ServiceResult.Success<?> success -> {
                        @SuppressWarnings("unchecked")
                        List<Review> reviews = (List<Review>) success.value();
                        if (reviewCount != null) {
                            reviewCount.setText(reviews.size() + " Reviews");
                        }
                        if (reviewCountDetail != null) {
                            reviewCountDetail.setText("(" + reviews.size() + ")");
                        }
                        if (reviewList != null) {
                            reviewList.getChildren().clear();
                            for (Review review : reviews) {
                                VBox card = new VBox(4);
                                card.getStyleClass().add("card-surface");
                                Label header = new Label(review.username() + " - " + review.rating() + " stars");
                                Label body = new Label(review.comment());
                                body.setWrapText(true);
                                card.getChildren().addAll(header, body);
                                reviewList.getChildren().add(card);
                            }
                        }

                        var avgResult = reviewService.getAverageRating(product.getId());
                        switch (avgResult) {
                            case com.jvmart.services.ServiceResult.Success<?> avgSuccess -> updateAverageStars((Double) avgSuccess.value());
                            case com.jvmart.services.ServiceResult.Failure<?> failure -> updateAverageStars(0.0);
                        }
                    }
                    case com.jvmart.services.ServiceResult.Failure<?> failure ->
                            AlertHelper.error("Failed to load reviews: " + failure.message());
                }
            });
        });
    }

    private void updateAverageStars(double avg) {
        starsRow.getChildren().clear();
        int fullStars = (int) Math.round(avg);
        for (int i = 1; i <= 5; i++) {
            boolean filled = i <= fullStars;
            Label star = new Label(filled ? "★" : "☆");
            star.getStyleClass().add(filled ? "review-star-filled" : "review-star-empty");
            starsRow.getChildren().add(star);
        }

        // #region agent log
        com.jvmart.utils.DebugLog.log(
                "productDetailAverageStars",
                "ProductDetailController.updateAverageStars",
                "Rendered average rating stars",
                java.util.Map.of("avg", avg, "fullStars", fullStars),
                "pre-fix"
        );
        // #endregion
    }

    private void setRating(int rating) {
        selectedRating = rating;
        Button[] buttons = new Button[]{star1, star2, star3, star4, star5};
        for (int i = 0; i < 5; i++) {
            boolean filled = (i + 1) <= rating;
            Button b = buttons[i];
            if (b == null) continue;
            b.getStyleClass().remove("star-btn-filled");
            if (filled) {
                b.setText("★");
                b.getStyleClass().add("star-btn-filled");
            } else {
                b.setText("☆");
            }
        }

        // #region agent log
        com.jvmart.utils.DebugLog.log(
                "productDetailSelectedRating",
                "ProductDetailController.setRating",
                "Updated selected rating stars",
                java.util.Map.of("rating", rating),
                "pre-fix"
        );
        // #endregion
    }

    @FXML
    private void decreaseQty() {
        if (quantity > 1) {
            quantity--;
            updateQuantityLabel();
        }
    }

    @FXML
    private void increaseQty() {
        if (product != null && quantity < product.getStock()) {
            quantity++;
            updateQuantityLabel();
        }
    }

    @FXML
    private void addToCart() {
        onAddToCart();
    }

    @FXML
    private void onAddToCart() {
        if (product == null) return;
        if (product.getStock() <= 0) {
            AlertHelper.error("This product is out of stock.");
            return;
        }
        SessionManager.getInstance().addToCart(new CartItem(product, quantity));
        activityLogService.logCurrentUser("ADD_TO_CART", "Added product #" + product.getId() + " to cart.");
        AlertHelper.success("Added to cart.");
    }

    @FXML
    private void submitReview() {
        onSubmitReview();
    }

    @FXML
    private void onSubmitReview() {
        if (product == null) return;
        if (selectedRating == 0) {
            AlertHelper.error("Please select a rating.");
            return;
        }
        String comment = reviewText.getText();
        if (comment == null || comment.trim().isEmpty()) {
            AlertHelper.error("Please enter a comment.");
            return;
        }
        var result = reviewService.submitReview(product.getId(), selectedRating, comment.trim());
        switch (result) {
            case com.jvmart.services.ServiceResult.Success<?> success -> {
                reviewSuccess.setVisible(true);
                reviewSuccess.setManaged(true);
                reviewText.clear();
                setRating(0);
                loadReviews();
            }
            case com.jvmart.services.ServiceResult.Failure<?> failure ->
                    AlertHelper.error("Failed to submit review: " + failure.message());
        }
    }

    @FXML private void rateStar1() { setRating(1); }
    @FXML private void rateStar2() { setRating(2); }
    @FXML private void rateStar3() { setRating(3); }
    @FXML private void rateStar4() { setRating(4); }
    @FXML private void rateStar5() { setRating(5); }

    @FXML
    private void loadMoreReviews() {
        if (product != null) {
            loadReviews();
        }
    }

    @FXML
    private void toggleWishlist() {
        if (product == null) {
            return;
        }

        boolean inWishlistBefore = wishlistService.isInWishlist(product.getId());
        String buttonTextBefore = wishlistToggleBtn != null ? wishlistToggleBtn.getText() : null;

        // #region agent log
        com.jvmart.utils.DebugLog.log(
                "wishlistToggleButtonLabel",
                "ProductDetailController.toggleWishlist",
                "Toggling wishlist (before state)",
                java.util.Map.of(
                        "productId", product.getId(),
                        "inWishlistBefore", inWishlistBefore,
                        "buttonTextBefore", buttonTextBefore
                ),
                "pre-fix"
        );
        // #endregion

        Thread.startVirtualThread(() -> {
            try {
                if (wishlistService.isInWishlist(product.getId())) {
                    wishlistService.removeFromWishlist(product.getId());
                    Platform.runLater(() -> AlertHelper.info("Wishlist", "Removed from your saved list."));
                } else if (wishlistService.addToWishlist(product)) {
                    Platform.runLater(() -> AlertHelper.success("Wishlist", "Saved to your wishlist."));
                } else {
                    Platform.runLater(() -> AlertHelper.info("Wishlist", "Already on your wishlist."));
                }
            } catch (SQLException e) {
                Platform.runLater(() -> AlertHelper.error("Wishlist", e.getMessage()));
            }
            boolean inWishlistAfter = wishlistService.isInWishlist(product.getId());

            Platform.runLater(() -> {
                applyWishlistButtonVisual(inWishlistAfter);
                String buttonTextAfter = wishlistToggleBtn != null ? wishlistToggleBtn.getText() : null;

                // #region agent log
                com.jvmart.utils.DebugLog.log(
                        "wishlistToggleButtonLabel",
                        "ProductDetailController.toggleWishlist",
                        "Toggling wishlist (after state)",
                        java.util.Map.of(
                                "productId", product.getId(),
                                "inWishlistAfter", inWishlistAfter,
                                "buttonTextAfter", buttonTextAfter
                        ),
                        "pre-fix"
                );
                // #endregion
            });
        });
    }

    @FXML private void goBack() { SceneRouter.navigateTo("product_catalog.fxml"); }
    
    @FXML
    private void viewAllReviews() {
        // #region agent log
        com.jvmart.utils.DebugLog.log(
                "viewAllReviewsNav",
                "ProductDetailController.viewAllReviews",
                "View All Reviews clicked; navigating",
                java.util.Map.of(
                        "productId", product != null ? product.getId() : null,
                        "targetFxml", "product_reviews.fxml"
                ),
                "pre-fix"
        );
        // #endregion
        SceneRouter.navigateTo("product_reviews.fxml", Map.of("selectedProduct", product));
    }

    @FXML private void selectThumb1() { setSelectedThumb(thumb1); }
    @FXML private void selectThumb2() { setSelectedThumb(thumb2); }
    @FXML private void selectThumb3() { setSelectedThumb(thumb3); }
    @FXML private void selectThumb4() { setSelectedThumb(thumb4); }

    private void setSelectedThumb(ImageView selected) {
        if (selected == null) return;
        Image selectedImage = selected.getImage();
        if (selectedImage != null) {
            mainImage.setImage(selectedImage);
        }
        for (ImageView thumb : List.of(thumb1, thumb2, thumb3, thumb4)) {
            if (thumb == null) continue;
            thumb.getStyleClass().removeAll("thumb-selected", "thumb-normal");
            thumb.getStyleClass().add(thumb == selected ? "thumb-selected" : "thumb-normal");
        }
    }

    private void loadProductImages() {
        Image image = ImageHelper.loadProductImage(product.getImagePath());
        if (image != null) {
            mainImage.setImage(image);
            for (ImageView thumb : List.of(thumb1, thumb2, thumb3, thumb4)) {
                thumb.setImage(image);
            }
            setSelectedThumb(thumb1);
        } else {
            mainImage.setImage(null);
            for (ImageView thumb : List.of(thumb1, thumb2, thumb3, thumb4)) {
                thumb.setImage(null);
            }
        }
    }
}

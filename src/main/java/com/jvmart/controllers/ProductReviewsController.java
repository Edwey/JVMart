package com.jvmart.controllers;

import com.jvmart.models.Product;
import com.jvmart.models.Review;
import com.jvmart.services.ReviewService;
import com.jvmart.utils.AlertHelper;
import com.jvmart.utils.SceneRouter;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class ProductReviewsController {
    @FXML private VBox list;
    @FXML private Label titleLabel;
    @FXML private Label subtitleLabel;

    private final ReviewService reviewService = new ReviewService();
    private Product product;

    @FXML
    public void initialize() {
        product = SceneRouter.getNavigationArgument("selectedProduct");
        if (product == null) {
            AlertHelper.error("Reviews", "No product selected.");
            SceneRouter.navigateTo("product_catalog.fxml");
            return;
        }

        if (titleLabel != null) {
            titleLabel.setText("Reviews");
        }
        if (subtitleLabel != null) {
            subtitleLabel.setText("All reviews for “" + product.getName() + "”.");
        }

        load();
    }

    @FXML
    private void back() {
        SceneRouter.navigateTo("product_detail.fxml", java.util.Map.of("selectedProduct", product));
    }

    private void load() {
        Thread.startVirtualThread(() -> {
            var res = reviewService.getReviewsForProduct(product.getId());
            Platform.runLater(() -> {
                switch (res) {
                    case com.jvmart.services.ServiceResult.Success<?> success -> {
                        @SuppressWarnings("unchecked")
                        List<Review> reviews = (List<Review>) success.value();
                        render(reviews);

                        // #region agent log
                        com.jvmart.utils.DebugLog.log(
                                "productAllReviews",
                                "ProductReviewsController.render",
                                "Rendered product reviews page",
                                java.util.Map.of("productId", product.getId(), "count", reviews.size()),
                                "pre-fix"
                        );
                        // #endregion
                    }
                    case com.jvmart.services.ServiceResult.Failure<?> failure ->
                            AlertHelper.error("Failed to load reviews: " + failure.message());
                    default -> AlertHelper.error("Failed to load reviews.");
                }
            });
        });
    }

    private void render(List<Review> reviews) {
        if (list == null) return;
        list.getChildren().clear();
        if (reviews == null || reviews.isEmpty()) {
            Label empty = new Label("No reviews yet for this product.");
            empty.getStyleClass().add("label-muted");
            list.getChildren().add(empty);
            return;
        }

        for (Review r : reviews) {
            list.getChildren().add(reviewCard(r));
        }
    }

    private VBox reviewCard(Review r) {
        VBox card = new VBox(10);
        card.getStyleClass().addAll("card-surface", "review-card");
        card.setPadding(new Insets(16));

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        Label user = new Label(r.username());
        user.getStyleClass().add("review-user");

        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        Label date = new Label(r.createdAt().format(DateTimeFormatter.ofPattern("dd MMM yyyy")));
        date.getStyleClass().add("label-muted");

        header.getChildren().addAll(user, spacer, date);

        HBox stars = new HBox(2);
        for (int i = 1; i <= 5; i++) {
            boolean filled = i <= r.rating();
            Label star = new Label(filled ? "★" : "☆");
            star.getStyleClass().add(filled ? "review-star-filled" : "review-star-empty");
            stars.getChildren().add(star);
        }

        Label body = new Label(r.comment());
        body.setWrapText(true);
        body.getStyleClass().add("review-body");

        card.getChildren().addAll(header, stars, body);
        return card;
    }
}


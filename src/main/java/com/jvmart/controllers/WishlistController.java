package com.jvmart.controllers;

import com.jvmart.services.WishlistService;
import com.jvmart.utils.AlertHelper;
import com.jvmart.utils.SceneRouter;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.sql.SQLException;

public class WishlistController {
    @FXML private VBox itemsBox;
    @FXML private VBox emptyState;
    @FXML private ScrollPane scroll;
    @FXML private Label countLabel;

    private final WishlistService wishlistService = WishlistService.getInstance();

    @FXML
    public void initialize() {
        refresh();
    }

    @FXML
    private void back() {
        SceneRouter.navigateTo("cart.fxml");
    }

    @FXML
    private void browseProducts() {
        SceneRouter.navigateTo("product_catalog.fxml");
    }

    @FXML
    private void clearWishlist() {
        Thread.startVirtualThread(() -> {
            try {
                wishlistService.clearWishlist();
                Platform.runLater(() -> {
                    AlertHelper.success("Wishlist", "Cleared.");
                    refresh();
                });
            } catch (SQLException e) {
                Platform.runLater(() -> AlertHelper.error("Wishlist", e.getMessage()));
            }
        });
    }

    private void refresh() {
        Thread.startVirtualThread(() -> {
            var items = wishlistService.getWishlistItems();
            Platform.runLater(() -> render(items));
        });
    }

    private void render(java.util.List<com.jvmart.models.WishlistItem> items) {
        int n = items == null ? 0 : items.size();
        if (countLabel != null) {
            countLabel.setText(n + (n == 1 ? " item saved" : " items saved"));
        }

        boolean isEmpty = n == 0;
        if (emptyState != null) {
            emptyState.setVisible(isEmpty);
            emptyState.setManaged(isEmpty);
        }
        if (scroll != null) {
            scroll.setVisible(!isEmpty);
            scroll.setManaged(!isEmpty);
        }
        if (itemsBox == null) return;
        itemsBox.getChildren().clear();

        if (isEmpty) return;

        for (var item : items) {
            itemsBox.getChildren().add(createRow(item));
        }

        // #region agent log
        com.jvmart.utils.DebugLog.log(
                "wishlistCrud",
                "WishlistController.render",
                "Rendered wishlist screen",
                java.util.Map.of("count", n),
                "pre-fix"
        );
        // #endregion
    }

    private VBox createRow(com.jvmart.models.WishlistItem item) {
        VBox card = new VBox(10);
        card.getStyleClass().addAll("card-surface", "wishlist-card");
        card.setPadding(new Insets(16));

        HBox top = new HBox(12);
        top.setAlignment(Pos.CENTER_LEFT);

        VBox meta = new VBox(2);
        Label name = new Label(item.productName());
        name.getStyleClass().add("wishlist-item-name");
        Label pid = new Label("Product #" + item.productId());
        pid.getStyleClass().add("label-muted");
        meta.getChildren().addAll(name, pid);
        HBox.setHgrow(meta, Priority.ALWAYS);

        Button view = new Button("View");
        view.getStyleClass().add("btn-ghost");
        view.setOnAction(e -> {
            // Navigate to product detail using productId; catalog already passes Product objects,
            // so here we simply route to catalog (user can open it) until we add a DAO fetch.
            SceneRouter.navigateTo("product_catalog.fxml");
            AlertHelper.info("Wishlist", "Open the product from the catalog to view details.");
        });

        top.getChildren().addAll(meta, view);

        HBox actions = new HBox(12);
        actions.setAlignment(Pos.CENTER_LEFT);

        Button moveToCart = new Button("Move to cart");
        moveToCart.getStyleClass().addAll("btn-primary", "btn-full");
        moveToCart.setOnAction(e -> onMoveToCart(item.productId()));

        Button remove = new Button("Remove");
        remove.getStyleClass().add("btn-outline");
        remove.setOnAction(e -> onRemove(item.productId()));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        actions.getChildren().addAll(moveToCart, spacer, remove);

        card.getChildren().addAll(top, actions);
        return card;
    }

    private void onRemove(int productId) {
        Thread.startVirtualThread(() -> {
            try {
                wishlistService.removeFromWishlist(productId);
                Platform.runLater(this::refresh);
            } catch (SQLException e) {
                Platform.runLater(() -> AlertHelper.error("Wishlist", e.getMessage()));
            }
        });
    }

    private void onMoveToCart(int productId) {
        Thread.startVirtualThread(() -> {
            try {
                boolean ok = wishlistService.moveToCart(productId);
                Platform.runLater(() -> {
                    if (ok) {
                        AlertHelper.success("Wishlist", "Moved to cart.");
                    } else {
                        AlertHelper.info("Wishlist", "Could not move item to cart.");
                    }
                    refresh();
                });
            } catch (SQLException e) {
                Platform.runLater(() -> AlertHelper.error("Wishlist", e.getMessage()));
            }
        });
    }
}


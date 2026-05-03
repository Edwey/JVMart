package com.jvmart.controllers;

import com.jvmart.models.CartItem;
import com.jvmart.models.Order;
import com.jvmart.models.Product;
import com.jvmart.services.ActivityLogService;
import com.jvmart.services.OrderService;
import com.jvmart.services.ProductService;
import com.jvmart.session.SessionManager;
import com.jvmart.utils.AlertHelper;
import com.jvmart.utils.ImageHelper;
import com.jvmart.utils.SceneRouter;
import com.jvmart.utils.ThemeManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;

import java.util.List;

public class CustomerHomeController {
    @FXML private Label cartCountLabel;
    @FXML private Label pendingOrdersLabel;
    @FXML private Label completedOrdersLabel;
    @FXML private VBox featuredProductsGrid;

    private final OrderService orderService = new OrderService();
    private final ProductService productService = new ProductService();
    private final ActivityLogService activityLogService = new ActivityLogService();

    @FXML
    public void initialize() {
        // Security check - ensure user is logged in and is a customer
        var user = SessionManager.getInstance().getCurrentUser();
        if (user == null) {
            SceneRouter.navigateTo("login.fxml");
            return;
        }
        
        // Redirect admins to their dashboard
        if ("admin".equals(user.getRole())) {
            SceneRouter.navigateTo("admin_overview.fxml");
            return;
        }

        updateCartCount();
        loadOrderStats();
        loadFeaturedProducts();
    }

    private void updateCartCount() {
        int count = SessionManager.getInstance().getCartCount();
        if (cartCountLabel != null) {
            cartCountLabel.setText(String.valueOf(count));
        }
    }

    private void loadOrderStats() {
        new Thread(() -> {
            int userId = SessionManager.getInstance().getCurrentUser().getId();
            var result = orderService.getOrdersForUser(userId);
            Platform.runLater(() -> {
                if (result instanceof com.jvmart.services.ServiceResult.Success<?> success) {
                    @SuppressWarnings("unchecked")
                    List<Order> orders = (List<Order>) success.value();
                    
                    long pending = orders.stream().filter(o -> "pending".equals(o.getStatus())).count();
                    long completed = orders.stream().filter(o -> "paid".equals(o.getStatus()) || "shipped".equals(o.getStatus())).count();
                    
                    if (pendingOrdersLabel != null) {
                        pendingOrdersLabel.setText(String.valueOf(pending));
                    }
                    if (completedOrdersLabel != null) {
                        completedOrdersLabel.setText(String.valueOf(completed));
                    }
                }
            });
        }).start();
    }

    private void loadFeaturedProducts() {
        Thread.startVirtualThread(() -> {
            var result = productService.getByCategory("all");
            Platform.runLater(() -> {
                if (result instanceof com.jvmart.services.ServiceResult.Success<?> success) {
                    @SuppressWarnings("unchecked")
                    List<Product> products = (List<Product>) success.value();
                    
                    // Show only the first 4 newest products
                    List<Product> featured = products.stream()
                            .sorted((a, b) -> Integer.compare(b.getId(), a.getId()))
                            .limit(4)
                            .toList();
                    
                    renderProductGrid(featured);
                }
            });
        });
    }

    private void renderProductGrid(List<Product> products) {
        if (featuredProductsGrid == null) {
            return;
        }
        featuredProductsGrid.getChildren().clear();
        
        HBox row = new HBox(16);
        for (Product product : products) {
            row.getChildren().add(createProductCard(product));
        }
        featuredProductsGrid.getChildren().add(row);
    }

    private VBox createProductCard(Product product) {
        VBox card = new VBox(10);
        card.setPrefWidth(260);
        card.getStyleClass().add("product-card");
        card.setOnMouseClicked(event -> openProduct(product));

        StackPane imageArea = new StackPane();
        imageArea.setPrefHeight(220);
        imageArea.getStyleClass().add("product-image-area");

        Rectangle clip = new Rectangle(260, 220);
        clip.setArcWidth(24);
        clip.setArcHeight(24);
        imageArea.setClip(clip);

        Image image = ImageHelper.loadProductImage(product.getImagePath());
        if (image != null) {
            ImageView imageView = new ImageView(image);
            imageView.setFitWidth(260);
            imageView.setFitHeight(220);
            imageView.setPreserveRatio(false);
            imageArea.getChildren().add(imageView);
        } else {
            Label placeholder = new Label(product.getCategory());
            placeholder.getStyleClass().add("product-image-placeholder");
            imageArea.getChildren().add(placeholder);
        }

        if (product.getStock() == 0) {
            Label soldOut = new Label("SOLD OUT");
            soldOut.getStyleClass().add("product-sold-out-badge");
            imageArea.getChildren().add(soldOut);
            imageArea.setOpacity(0.8);
        }

        VBox details = new VBox(12);
        details.setPadding(new Insets(16));
        details.getStyleClass().add("product-details");

        Label nameLabel = new Label(product.getName());
        nameLabel.setWrapText(true);
        nameLabel.getStyleClass().add("product-name");

        Label priceLabel = new Label(String.format("GHS %.2f", product.getPrice()));
        priceLabel.getStyleClass().add("product-price-label");

        Label stockLabel = new Label(product.getStock() > 0 ? "In stock: " + product.getStock() : "Out of stock");
        stockLabel.getStyleClass().add(product.getStock() > 0 ? "product-stock-in" : "product-stock-out");

        Button addBtn = new Button(product.getStock() > 0 ? "Add to Cart" : "Out of Stock");
        addBtn.setMaxWidth(Double.MAX_VALUE);
        addBtn.getStyleClass().add(product.getStock() > 0 ? "btn-add-cart" : "btn-out-of-stock");
        if (product.getStock() > 0) {
            addBtn.setOnAction(e -> {
                e.consume();
                onAddToCart(product);
            });
        } else {
            addBtn.setDisable(true);
        }

        details.getChildren().addAll(nameLabel, priceLabel, stockLabel, addBtn);
        card.getChildren().addAll(imageArea, details);
        return card;
    }

    private void onAddToCart(Product product) {
        SessionManager.getInstance().addToCart(new CartItem(product, 1));
        activityLogService.logCurrentUser("ADD_TO_CART", "Added product #" + product.getId() + " to cart.");
        updateCartCount();
        AlertHelper.success("Added to cart", product.getName() + " was added to your cart.");
    }

    private void openProduct(Product product) {
        activityLogService.logCurrentUser("VIEW_PRODUCT", "Viewed product #" + product.getId() + ": " + product.getName());
        SceneRouter.transferData.put("selectedProduct", product);
        SceneRouter.navigateTo("product_detail.fxml");
    }

    @FXML
    private void onShopNow() {
        SceneRouter.navigateTo("product_catalog.fxml");
    }

    @FXML
    private void onMyOrders() {
        SceneRouter.navigateTo("my_orders.fxml");
    }

    @FXML
    private void onViewAllProducts() {
        SceneRouter.navigateTo("product_catalog.fxml");
    }

    @FXML
    private void onElectronics() {
        SceneRouter.transferData.put("catalogCategory", "Electronics");
        SceneRouter.navigateTo("product_catalog.fxml");
    }

    @FXML
    private void onClothing() {
        SceneRouter.transferData.put("catalogCategory", "Clothing");
        SceneRouter.navigateTo("product_catalog.fxml");
    }

    @FXML
    private void onHomeLiving() {
        SceneRouter.transferData.put("catalogCategory", "Home & Living");
        SceneRouter.navigateTo("product_catalog.fxml");
    }

    @FXML
    private void onEditorial() {
        SceneRouter.transferData.put("catalogCategory", "Editorial");
        SceneRouter.navigateTo("product_catalog.fxml");
    }

    @FXML
    private void onToggleTheme() {
        if (featuredProductsGrid != null && featuredProductsGrid.getScene() != null) {
            ThemeManager.toggleTheme(featuredProductsGrid.getScene());
        }
    }
}

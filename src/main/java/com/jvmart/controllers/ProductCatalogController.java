package com.jvmart.controllers;

import com.jvmart.models.CartItem;
import com.jvmart.models.Product;
import com.jvmart.models.User;
import com.jvmart.services.ActivityLogService;
import com.jvmart.services.ProductService;
import com.jvmart.services.ReviewService;
import com.jvmart.session.SessionManager;
import com.jvmart.utils.AlertHelper;
import com.jvmart.utils.ImageHelper;
import com.jvmart.utils.SceneRouter;
import com.jvmart.utils.ThemeManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class ProductCatalogController {
    @FXML private TextField searchField;
    @FXML private ComboBox<String> sortCombo;
    @FXML private FlowPane productGrid;
    @FXML private Label cartCountLabel;
    @FXML private Label userNameLabel;
    @FXML private Label avatarLabel;
    @FXML private Label avatarInitials;
    @FXML private Button cartBtn;
    @FXML private VBox sidebar;

    private final ProductService productService = new ProductService();
    private final ActivityLogService activityLogService = new ActivityLogService();
    private final ReviewService reviewService = new ReviewService();
    private List<Product> allProducts;

    @FXML
    public void initialize() {
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
        
        String initial = user.getFullName().substring(0, 1).toUpperCase();
        if (userNameLabel != null) {
            userNameLabel.setText(user.getFullName());
        }
        if (avatarLabel != null) {
            avatarLabel.setText(initial);
        }
        if (avatarInitials != null) {
            avatarInitials.setText(initial);
        }

        if (sortCombo != null) {
            if (sortCombo.getItems().isEmpty()) {
                sortCombo.getItems().addAll("Newest", "Price: Low to High", "Price: High to Low", "Name: A-Z");
            }
            sortCombo.setValue("Newest");
        }

        String initialCategory = (String) SceneRouter.transferData.get("catalogCategory");
        loadProducts(initialCategory == null ? "all" : initialCategory);
        updateCartCount();

        if (searchField != null) {
            searchField.textProperty().addListener((obs, oldVal, newVal) -> onSearch());
        }

        if (productGrid != null) {
            productGrid.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (newScene != null) {
                    // Unbind previous binding to prevent memory leak
                    productGrid.prefWrapLengthProperty().unbind();
                    productGrid.prefWrapLengthProperty().bind(newScene.widthProperty().subtract(420));
                }
            });
        }
    }

    private void loadProducts(String category) {
        Thread.startVirtualThread(() -> {
            var result = productService.getByCategory(category);
            Platform.runLater(() -> {
                switch (result) {
                    case com.jvmart.services.ServiceResult.Success<?> success -> {
                        @SuppressWarnings("unchecked")
                        List<Product> products = (List<Product>) success.value();
                        allProducts = products;
                        applyCurrentView();
                    }
                    case com.jvmart.services.ServiceResult.Failure<?> failure ->
                            AlertHelper.error("Error loading products: " + failure.message());
                }
            });
        });
    }

    private void renderProductGrid(List<Product> products) {
        if (productGrid == null) {
            return;
        }
        productGrid.getChildren().clear();
        for (Product product : products) {
            productGrid.getChildren().add(createProductCard(product));
        }
    }

    private VBox createProductCard(Product product) {
        VBox card = new VBox(10);
        card.setPrefWidth(280);
        card.getStyleClass().add("product-card");
        card.setOnMouseClicked(event -> openProduct(product));

        StackPane imageArea = new StackPane();
        imageArea.setPrefHeight(260);
        imageArea.getStyleClass().add("product-image-area");

        Rectangle clip = new Rectangle(280, 260);
        clip.setArcWidth(24);
        clip.setArcHeight(24);
        imageArea.setClip(clip);

        Image image = ImageHelper.loadProductImage(product.getImagePath());
        if (image != null) {
            ImageView imageView = new ImageView(image);
            imageView.setFitWidth(280);
            imageView.setFitHeight(260);
            imageView.setPreserveRatio(false);
            imageArea.getChildren().add(imageView);
        } else {
            Label placeholder = new Label(product.getCategory());
            placeholder.getStyleClass().add("product-image-placeholder");
            imageArea.getChildren().add(placeholder);
        }

        if (product.getStock() == 0) {
            Label soldOut = new Label("S O L D   O U T");
            soldOut.getStyleClass().add("product-sold-out-badge");
            imageArea.getChildren().add(soldOut);
            imageArea.getStyleClass().add("product-image-dimmed");
        }

        VBox details = new VBox(16);
        details.setPadding(new Insets(24));
        details.getStyleClass().add("product-details");

        VBox titleBlock = new VBox(4);
        Label nameLabel = new Label(product.getName());
        nameLabel.setWrapText(true);
        nameLabel.getStyleClass().add("product-name");

        Label catLabel = new Label(product.getCategory());
        catLabel.getStyleClass().add("product-category-tag");
        titleBlock.getChildren().addAll(nameLabel, catLabel);

        VBox priceBlock = new VBox(2);
        Label investmentLabel = new Label("I N V E S T M E N T");
        investmentLabel.getStyleClass().add("product-investment-label");

        Label priceLabel = new Label(String.format("GHs %.2f", product.getPrice()));
        priceLabel.getStyleClass().add("product-price-label");
        priceBlock.getChildren().addAll(investmentLabel, priceLabel);

        Label stockLabel = new Label(product.getStock() > 0 ? "In stock: " + product.getStock() : "Out of stock");
        stockLabel.getStyleClass().add(product.getStock() > 0 ? "product-stock-in" : "product-stock-out");

        HBox metaRow = new HBox();
        Region metaSpacer = new Region();
        HBox.setHgrow(metaSpacer, Priority.ALWAYS);
        metaRow.getChildren().addAll(priceBlock, metaSpacer, stockLabel);

        Button addBtn = new Button(product.getStock() > 0 ? "A D D   T O   C A R T" : "O U T   O F   S T O C K");
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

        // Add average rating display
        var avgResult = reviewService.getAverageRating(product.getId());
        double avgRating = 0.0;
        if (avgResult instanceof com.jvmart.services.ServiceResult.Success<?> success) {
            avgRating = (Double) success.value();
        }
        
        VBox ratingBlock = new VBox(2);
        Label ratingLabel = new Label(String.format("⭐ %.1f", avgRating));
        ratingLabel.getStyleClass().add("product-rating-label");
        ratingBlock.getChildren().add(ratingLabel);
        
        details.getChildren().addAll(titleBlock, metaRow, ratingBlock, addBtn);
        card.getChildren().addAll(imageArea, details);
        return card;
    }

    private void onAddToCart(Product product) {
        SessionManager.getInstance().addToCart(new CartItem(product, 1));
        activityLogService.logCurrentUser("ADD_TO_CART", "Added product #" + product.getId() + " to cart.");
        updateCartCount();
        AlertHelper.success("Added to cart", product.getName() + " was added to your cart.");
    }

    private void updateCartCount() {
        int count = SessionManager.getInstance().getCartCount();
        if (cartCountLabel != null) {
            cartCountLabel.setText(String.valueOf(count));
        }
        if (cartBtn != null) {
            cartBtn.setText("Cart (" + count + ")");
        }
    }

    @FXML
    private void onSearch() {
        if (allProducts == null || searchField == null) {
            return;
        }
        applyCurrentView();
    }

    @FXML
    private void onSort() {
        if (allProducts == null || sortCombo == null) {
            return;
        }

        String sortType = sortCombo.getValue();
        Comparator<Product> comparator = switch (sortType) {
            case "Price: Low to High" -> Comparator.comparing(Product::getPrice);
            case "Price: High to Low" -> Comparator.comparing(Product::getPrice).reversed();
            case "Name: A-Z" -> Comparator.comparing(Product::getName);
            default -> Comparator.comparing(Product::getId).reversed();
        };

        List<Product> sorted = allProducts.stream().sorted(comparator).collect(Collectors.toList());
        renderProductGrid(applySearchFilter(sorted));
    }

    @FXML private void filterAll() { loadProducts("all"); }
    @FXML private void filterElectronics() { loadProducts("Electronics"); }
    @FXML private void filterClothing() { loadProducts("Clothing"); }
    @FXML private void filterHome() { loadProducts("Home & Living"); }
    @FXML private void filterEditorial() { loadProducts("Editorial"); }
    @FXML private void showFilterHelp() { AlertHelper.info("Filters", "Use the category list on the left to filter the archive."); }

    @FXML private void onCart() { SceneRouter.navigateTo("cart.fxml"); }
    @FXML private void onMyOrders() { SceneRouter.navigateTo("my_orders.fxml"); }
    @FXML private void onLogout() {
        activityLogService.logCurrentUser("LOGOUT", "User logged out.");
        SessionManager.getInstance().logout();
        SceneRouter.navigateTo("login.fxml");
    }

    @FXML
    private void onToggleTheme() {
        if (productGrid != null) {
            ThemeManager.toggleTheme(productGrid.getScene());
        }
    }

    @FXML private void navHome() { filterAll(); }
    @FXML private void navProducts() { SceneRouter.navigateTo("product_catalog.fxml"); }
    @FXML private void navOrders() { onMyOrders(); }
    @FXML private void openCart() { onCart(); }
    @FXML private void openNotifications() {
        AlertHelper.info("Notifications", "No new notifications yet.");
    }

    private void openProduct(Product product) {
        activityLogService.logCurrentUser("VIEW_PRODUCT", "Viewed product #" + product.getId() + ": " + product.getName());
        SceneRouter.transferData.put("selectedProduct", product);
        SceneRouter.navigateTo("product_detail.fxml");
    }

    private void applyCurrentView() {
        if (allProducts == null) {
            return;
        }
        List<Product> filtered = applySearchFilter(allProducts);
        if (sortCombo != null && sortCombo.getValue() != null) {
            Comparator<Product> comparator = switch (sortCombo.getValue()) {
                case "Price: Low to High" -> Comparator.comparing(Product::getPrice);
                case "Price: High to Low" -> Comparator.comparing(Product::getPrice).reversed();
                case "Name: A-Z" -> Comparator.comparing(Product::getName);
                default -> Comparator.comparing(Product::getId).reversed();
            };
            filtered = filtered.stream().sorted(comparator).collect(Collectors.toList());
        }
        renderProductGrid(filtered);
    }

    private List<Product> applySearchFilter(List<Product> source) {
        if (searchField == null || searchField.getText() == null || searchField.getText().isBlank()) {
            return source;
        }
        String query = searchField.getText().toLowerCase();
        return source.stream()
                .filter(p -> p.getName() != null && p.getName().toLowerCase().contains(query) || 
                           p.getCategory() != null && p.getCategory().toLowerCase().contains(query))
                .collect(Collectors.toList());
    }

    @FXML private void refreshProducts() {
        loadProducts("All Products");
        AlertHelper.info("Products Refreshed", "Product catalog has been refreshed successfully.");
    }

    @FXML private void clearFilters() {
        if (searchField != null) {
            searchField.clear();
        }
        loadProducts("All Products");
        AlertHelper.info("Filters Cleared", "All filters have been cleared.");
    }
}

package com.jvmart.controllers;

import com.jvmart.models.CartItem;
import com.jvmart.services.ActivityLogService;
import com.jvmart.utils.ImageHelper;
import com.jvmart.session.SessionManager;
import com.jvmart.utils.SceneRouter;
import com.jvmart.utils.ThemeManager;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.List;

public class CartController {
    @FXML private VBox cartItemsContainer;
    @FXML private VBox emptyState;
    @FXML private VBox filledState;
    @FXML private Label subtotalLabel;
    @FXML private Label totalLabel;
    @FXML private Label shippingLabel;
    @FXML private Button checkoutBtn;
    private final ActivityLogService activityLogService = new ActivityLogService();

    @FXML
    public void initialize() {
        refreshCart();
    }

    private void refreshCart() {
        List<CartItem> cart = SessionManager.getInstance().getCart();
        if (cart.isEmpty()) {
            emptyState.setVisible(true);
            emptyState.setManaged(true);
            filledState.setVisible(false);
            filledState.setManaged(false);
            checkoutBtn.setDisable(true);
        } else {
            emptyState.setVisible(false);
            emptyState.setManaged(false);
            filledState.setVisible(true);
            filledState.setManaged(true);
            checkoutBtn.setDisable(false);
            loadCartItems(cart);
        }
        updateSummary();
    }

    private void loadCartItems(List<CartItem> cart) {
        cartItemsContainer.getChildren().clear();
        for (CartItem item : cart) {
            cartItemsContainer.getChildren().add(createCartItemRow(item));
        }
    }

    private HBox createCartItemRow(CartItem item) {
        HBox row = new HBox(20);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(15));
        row.getStyleClass().add("cart-item-row");

        StackPane imgPlaceholder = new StackPane();
        imgPlaceholder.setPrefSize(60, 60);
        imgPlaceholder.getStyleClass().add("cart-item-image");

        Image image = ImageHelper.loadProductImage(item.getProduct().getImagePath());
        if (image != null) {
            ImageView imageView = new ImageView(image);
            imageView.setFitWidth(60);
            imageView.setFitHeight(60);
            imageView.setPreserveRatio(false);
            imgPlaceholder.getChildren().add(imageView);
        }

        VBox details = new VBox(4);
        Label nameLabel = new Label(item.getProduct().getName());
        nameLabel.getStyleClass().add("cart-item-name");
        Label catLabel = new Label(item.getProduct().getCategory());
        catLabel.getStyleClass().add("label-muted");
        details.getChildren().addAll(nameLabel, catLabel);
        HBox.setHgrow(details, Priority.ALWAYS);

        HBox qtyBox = new HBox(10);
        qtyBox.setAlignment(Pos.CENTER);
        Button minusBtn = new Button("-");
        minusBtn.getStyleClass().add("qty-btn");
        minusBtn.setOnAction(e -> {
            if (item.getQuantity() > 1) {
                SessionManager.getInstance().updateCartQuantity(item.getProduct().getId(), item.getQuantity() - 1);
            } else {
                SessionManager.getInstance().removeFromCart(item.getProduct().getId());
            }
            activityLogService.logCurrentUser("ADD_TO_CART", "Updated quantity for product #" + item.getProduct().getId() + ".");
            refreshCart();
        });

        Label qtyLabel = new Label(String.valueOf(item.getQuantity()));
        qtyLabel.getStyleClass().add("qty-label");

        Button plusBtn = new Button("+");
        plusBtn.getStyleClass().add("qty-btn");
        plusBtn.setOnAction(e -> {
            if (item.getQuantity() < item.getProduct().getStock()) {
                SessionManager.getInstance().updateCartQuantity(item.getProduct().getId(), item.getQuantity() + 1);
                activityLogService.logCurrentUser("ADD_TO_CART", "Updated quantity for product #" + item.getProduct().getId() + ".");
                refreshCart();
            }
        });
        qtyBox.getChildren().addAll(minusBtn, qtyLabel, plusBtn);

        Label lineTotalLabel = new Label(String.format("GHS %.2f", item.getLineTotal()));
        lineTotalLabel.getStyleClass().add("cart-item-line-total");
        lineTotalLabel.setPrefWidth(100);
        lineTotalLabel.setAlignment(Pos.CENTER_RIGHT);

        Button removeBtn = new Button("x");
        removeBtn.getStyleClass().add("remove-btn");
        removeBtn.setOnAction(e -> {
            SessionManager.getInstance().removeFromCart(item.getProduct().getId());
            activityLogService.logCurrentUser("REMOVE_FROM_CART", "Removed product #" + item.getProduct().getId() + " from cart.");
            refreshCart();
        });

        row.getChildren().addAll(imgPlaceholder, details, qtyBox, lineTotalLabel, removeBtn);
        return row;
    }

    private void updateSummary() {
        double total = SessionManager.getInstance().getCartTotal();
        if (subtotalLabel != null) {
            subtotalLabel.setText(String.format("GHS %.2f", total));
        }
        if (totalLabel != null) {
            totalLabel.setText(String.format("GHS %.2f", total));
        }
    }

    @FXML
    private void onCheckout() {
        SceneRouter.navigateTo("checkout.fxml");
    }

    @FXML
    private void onBack() {
        SceneRouter.navigateTo("customer_home.fxml");
    }

    @FXML
    private void onToggleTheme() {
        ThemeManager.toggleTheme(cartItemsContainer.getScene());
    }

    @FXML
    private void continueShopping() {
        onBack();
    }

    @FXML
    private void proceedToCheckout() {
        onCheckout();
    }
}

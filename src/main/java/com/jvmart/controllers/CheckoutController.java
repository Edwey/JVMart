package com.jvmart.controllers;

import com.jvmart.models.CartItem;
import com.jvmart.services.OrderService;
import com.jvmart.session.SessionManager;
import com.jvmart.utils.AlertHelper;
import com.jvmart.utils.ImageHelper;
import com.jvmart.utils.SceneRouter;
import com.jvmart.utils.ThemeManager;
import javafx.application.Platform;
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
import java.util.logging.Logger;

public class CheckoutController {
    @FXML private VBox checkoutItemsList;
    @FXML private Label checkoutSubtotal;
    @FXML private Label checkoutTotal;
    @FXML private Button placeOrderBtn;

    private static final Logger LOGGER = Logger.getLogger(CheckoutController.class.getName());
    private final OrderService orderService = new OrderService();

    @FXML
    public void initialize() {
        SessionManager session = SessionManager.getInstance();
        List<CartItem> cart = session.getCart();

        if (checkoutItemsList == null) {
            LOGGER.severe("checkoutItemsList is null - FXML injection failed");
            return;
        }

        checkoutItemsList.getChildren().clear();
        for (CartItem item : cart) {
            checkoutItemsList.getChildren().add(createCheckoutRow(item));
        }

        double total = session.getCartTotal();
        String totalStr = String.format("GHS %.2f", total);
        if (checkoutSubtotal != null) {
            checkoutSubtotal.setText(totalStr);
        }
        if (checkoutTotal != null) {
            checkoutTotal.setText(totalStr);
        }
    }

    private HBox createCheckoutRow(CartItem item) {
        HBox row = new HBox(15);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10));
        row.getStyleClass().add("checkout-item-row");

        StackPane imgPlaceholder = new StackPane();
        imgPlaceholder.setPrefSize(50, 50);
        imgPlaceholder.getStyleClass().add("cart-item-image");

        Image image = ImageHelper.loadProductImage(item.getProduct().getImagePath());
        if (image != null) {
            ImageView imageView = new ImageView(image);
            imageView.setFitWidth(50);
            imageView.setFitHeight(50);
            imageView.setPreserveRatio(false);
            imgPlaceholder.getChildren().add(imageView);
        }

        VBox details = new VBox(2);
        Label nameLabel = new Label(item.getProduct().getName());
        nameLabel.getStyleClass().add("cart-item-name");
        Label qtyPriceLabel = new Label(item.getQuantity() + " x GHS " + String.format("%.2f", item.getProduct().getPrice()));
        qtyPriceLabel.getStyleClass().add("label-muted");
        details.getChildren().addAll(nameLabel, qtyPriceLabel);
        HBox.setHgrow(details, Priority.ALWAYS);

        Label lineTotal = new Label(String.format("GHS %.2f", item.getLineTotal()));
        lineTotal.getStyleClass().add("cart-item-line-total");

        row.getChildren().addAll(imgPlaceholder, details, lineTotal);
        return row;
    }

    @FXML
    private void onPlaceOrder() {
        placeOrderBtn.setDisable(true);
        placeOrderBtn.setText("Placing order...");

        Thread.startVirtualThread(() -> {
            var result = orderService.placeOrder();
            Platform.runLater(() -> {
                switch (result) {
                    case com.jvmart.services.ServiceResult.Success<?> success -> {
                        Integer orderId = (Integer) success.value();
                        SceneRouter.transferData.put("lastOrderId", orderId);
                        SceneRouter.navigateTo("order_confirmation.fxml");
                    }
                    case com.jvmart.services.ServiceResult.Failure<?> failure -> {
                        AlertHelper.error("Failed to place order: " + failure.message());
                        placeOrderBtn.setDisable(false);
                        placeOrderBtn.setText("Place Order");
                    }
                }
            });
        });
    }

    @FXML
    private void onBack() {
        SceneRouter.navigateTo("customer_home.fxml");
    }

    @FXML
    private void onToggleTheme() {
        ThemeManager.toggleTheme(checkoutItemsList.getScene());
    }

    @FXML
    private void placeOrder() {
        onPlaceOrder();
    }

    @FXML
    private void returnToCart() {
        onBack();
    }
}

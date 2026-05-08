package com.jvmart.controllers;

import com.jvmart.models.User;
import com.jvmart.session.SessionManager;
import com.jvmart.utils.SceneRouter;
import com.jvmart.utils.ThemeManager;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class OrderConfirmationController {
    @FXML private Label orderIdLabel;
    @FXML private Label customerName;
    @FXML private Label deliveryNote;
    @FXML private Label confirmSubtotal;
    @FXML private Label confirmTotal;
    @FXML private VBox confirmedItemsList;

    @FXML
    public void initialize() {
        Object orderId = SceneRouter.getNavigationArgument("lastOrderId");
        if (orderId instanceof Number num) {
            orderIdLabel.setText("#JV-" + String.format("%04d", num.intValue()));
        }

        User user = SessionManager.getInstance().getCurrentUser();
        if (user != null) {
            customerName.setText(user.getFullName());
        }
        deliveryNote.setText("Cash on delivery");

        var snapshot = SessionManager.getInstance().getLastCartSnapshot();
        double total = snapshot.stream().mapToDouble(com.jvmart.models.CartItem::getLineTotal).sum();
        confirmSubtotal.setText(String.format("GHS %.2f", total));
        confirmTotal.setText(String.format("GHS %.2f", total));
        buildConfirmedItems(snapshot);
    }

    private void buildConfirmedItems(java.util.List<com.jvmart.models.CartItem> items) {
        if (confirmedItemsList == null) return;
        confirmedItemsList.getChildren().clear();
        for (var item : items) {
            HBox row = new HBox(8);
            Label name = new Label(item.getProduct().getName());
            Label qty = new Label("x" + item.getQuantity());
            Label price = new Label(String.format("GHS %.2f", item.getLineTotal()));
            HBox.setHgrow(name, Priority.ALWAYS);
            row.getChildren().addAll(name, qty, price);
            confirmedItemsList.getChildren().add(row);
        }
    }

    @FXML
    private void onViewOrders() {
        SceneRouter.navigateTo("my_orders.fxml");
    }

    @FXML
    private void onContinueShopping() {
        SceneRouter.navigateTo("product_catalog.fxml");
    }

    @FXML
    private void viewOrders() {
        onViewOrders();
    }

    @FXML
    private void continueShopping() {
        onContinueShopping();
    }

    @FXML
    private void onToggleTheme() {
        ThemeManager.toggleTheme(orderIdLabel.getScene());
    }
}

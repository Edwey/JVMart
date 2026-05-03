package com.jvmart.controllers;

import com.jvmart.session.SessionManager;
import com.jvmart.services.ActivityLogService;
import com.jvmart.utils.AlertHelper;
import com.jvmart.utils.SceneRouter;
import com.jvmart.utils.ThemeManager;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

public class MainShellController {
    @FXML private Button navHome;
    @FXML private Button navProducts;
    @FXML private Button navOrders;
    @FXML private Button navProfile;
    @FXML private Button profileBtn;
    @FXML private Button cartBtn;
    @FXML private Label cartBadge;
    @FXML private Label avatarInitials;
    @FXML private StackPane contentArea;
    @FXML private Button catAll;
    @FXML private Button catElec;
    @FXML private Button catClothing;
    @FXML private Button catHome;
    @FXML private Button catEdit;
    private final ActivityLogService activityLogService = new ActivityLogService();

    @FXML
    public void initialize() {
        var user = SessionManager.getInstance().getCurrentUser();
        if (user != null && avatarInitials != null) {
            avatarInitials.setText(user.getFullName().substring(0, 1).toUpperCase());
        }
        updateCartBadge();
        
        // Listen for cart changes and update badge in real-time
        SessionManager.getInstance().getCart().addListener((javafx.collections.ListChangeListener.Change<? extends com.jvmart.models.CartItem> c) -> {
            javafx.application.Platform.runLater(this::updateCartBadge);
        });
    }

    public void setContent(String contentFxml) throws java.io.IOException {
        Parent content = SceneRouter.loadFXML("/com/jvmart/fxml/" + contentFxml);
        contentArea.getChildren().setAll(content);
        updateActiveNav(contentFxml);
        updateCategoryStyle((String) SceneRouter.transferData.getOrDefault("catalogCategory", "all"));
    }

    private void updateActiveNav(String contentFxml) {
        setNavStyle(navHome, false);
        setNavStyle(navProducts, false);
        setNavStyle(navOrders, false);
        setNavStyle(navProfile, false);
        if (cartBtn != null) {
            cartBtn.getStyleClass().removeAll("btn-primary", "btn-secondary");
            cartBtn.getStyleClass().add("btn-secondary");
        }

        if ("customer_home.fxml".equals(contentFxml)) {
            setNavStyle(navHome, true);
        } else if ("product_catalog.fxml".equals(contentFxml)) {
            setNavStyle(navProducts, true);
        } else if ("my_orders.fxml".equals(contentFxml)) {
            setNavStyle(navOrders, true);
        } else if ("profile.fxml".equals(contentFxml)) {
            setNavStyle(navProfile, true);
        } else if ("cart.fxml".equals(contentFxml)) {
            setNavStyle(navProducts, true);
            if (cartBtn != null) {
                cartBtn.getStyleClass().remove("btn-secondary");
                if (!cartBtn.getStyleClass().contains("btn-primary")) {
                    cartBtn.getStyleClass().add("btn-primary");
                }
            }
        } else if ("checkout.fxml".equals(contentFxml) || "order_confirmation.fxml".equals(contentFxml)) {
            setNavStyle(navProducts, true);
        }
    }

    private void setNavStyle(Button button, boolean active) {
        if (button == null) {
            return;
        }
        button.getStyleClass().removeAll("navbar-link", "navbar-link-active");
        button.getStyleClass().add(active ? "navbar-link-active" : "navbar-link");
    }

    @FXML private void navHome() { SceneRouter.navigateTo("customer_home.fxml"); }
    @FXML private void navProducts() { SceneRouter.navigateTo("product_catalog.fxml"); }
    @FXML private void navOrders() { SceneRouter.navigateTo("my_orders.fxml"); }
    @FXML private void navProfile() { SceneRouter.navigateTo("profile.fxml"); }
    @FXML private void navCart() { SceneRouter.navigateTo("cart.fxml"); }
    @FXML private void openCart() { navCart(); }
    @FXML private void openNotifications() { AlertHelper.info("Notifications", "No new notifications yet."); }
    @FXML private void filterAll() { openCatalogCategory("all"); }
    @FXML private void filterElectronics() { openCatalogCategory("Electronics"); }
    @FXML private void filterClothing() { openCatalogCategory("Clothing"); }
    @FXML private void filterHome() { openCatalogCategory("Home & Living"); }
    @FXML private void filterEditorial() { openCatalogCategory("Editorial"); }
    @FXML private void onLogout() {
        activityLogService.logCurrentUser("LOGOUT", "User logged out.");
        SessionManager.getInstance().logout();
        SceneRouter.navigateTo("login.fxml");
    }
    @FXML private void onToggleTheme() {
        System.out.println("[MainShell] Theme button clicked");
        if (navHome != null && navHome.getScene() != null) {
            ThemeManager.toggleTheme(navHome.getScene());
        } else {
            System.err.println("[MainShell] Scene is null, cannot toggle theme");
        }
    }

    private void openCatalogCategory(String category) {
        SceneRouter.transferData.put("catalogCategory", category);
        updateCategoryStyle(category);
        SceneRouter.navigateTo("product_catalog.fxml");
    }

    private void updateCartBadge() {
        if (cartBadge != null) {
            cartBadge.setText(String.valueOf(SessionManager.getInstance().getCartCount()));
        }
    }

    private void updateCategoryStyle(String category) {
        setCategoryStyle(catAll, "all".equalsIgnoreCase(category));
        setCategoryStyle(catElec, "Electronics".equalsIgnoreCase(category));
        setCategoryStyle(catClothing, "Clothing".equalsIgnoreCase(category));
        setCategoryStyle(catHome, "Home & Living".equalsIgnoreCase(category));
        setCategoryStyle(catEdit, "Editorial".equalsIgnoreCase(category));
    }

    private void setCategoryStyle(Button button, boolean active) {
        if (button == null) {
            return;
        }
        button.getStyleClass().removeAll("sidebar-item", "sidebar-item-active");
        button.getStyleClass().add(active ? "sidebar-item-active" : "sidebar-item");
    }
}

package com.jvmart.controllers;

import com.jvmart.session.SessionManager;
import com.jvmart.services.ActivityLogService;
import com.jvmart.utils.AlertHelper;
import com.jvmart.utils.CsvExportUtil;
import com.jvmart.utils.SceneRouter;
import com.jvmart.utils.ThemeManager;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.Parent;
import java.util.List;

public class AdminShellController {
    @FXML private Button navHome;
    @FXML private Button navProducts;
    @FXML private Button navOrders;
    @FXML private Button navProfile;
    @FXML private Button navCustomers;
    @FXML private Button navInventory;
    @FXML private Button navReports;
    @FXML private Button searchBtn;
    @FXML private Button profileBtn;
    @FXML private Button sideOverview;
    @FXML private Button sideProducts;
    @FXML private Button sideOrders;
    @FXML private Button sideCustomers;
    @FXML private Button sideInventory;
    @FXML private Button sideReviews;
    @FXML private Button sideReviewsAnalytics;
    @FXML private Button sideReports;
    @FXML private Label adminInitials;
    @FXML private StackPane adminContent;
    private final ActivityLogService activityLogService = new ActivityLogService();

    @FXML
    public void initialize() {
        // Security check - ensure user is admin
        if (SessionManager.getInstance().getCurrentUser() == null || !"admin".equals(SessionManager.getInstance().getCurrentUser().getRole())) {
            SceneRouter.navigateTo("login.fxml");
            return;
        }

        if (adminInitials != null) {
            String username = SessionManager.getInstance().getCurrentUser().getUsername();
            adminInitials.setText(username == null || username.isBlank()
                    ? "AD"
                    : username.substring(0, Math.min(2, username.length())).toUpperCase());
        }
    }

    public void setContent(String contentFxml) throws java.io.IOException {
        Parent content = SceneRouter.loadFXML("/com/jvmart/fxml/" + contentFxml);
        adminContent.getChildren().setAll(content);
        updateActiveNav(contentFxml);
    }

    private void updateActiveNav(String contentFxml) {
        setNavStyle(navHome, false);
        setNavStyle(navProducts, false);
        setNavStyle(navOrders, false);
        setNavStyle(navCustomers, false);
        setNavStyle(navInventory, false);
        setNavStyle(navReports, false);
        setNavStyle(sideOverview, false);
        setNavStyle(sideProducts, false);
        setNavStyle(sideOrders, false);
        setNavStyle(sideCustomers, false);
        setNavStyle(sideInventory, false);
        setNavStyle(sideReviews, false);
        setNavStyle(sideReviewsAnalytics, false);
        setNavStyle(sideReports, false);

        switch (contentFxml) {
            case "admin_overview.fxml" -> {
                setNavStyle(navHome, true);
                setNavStyle(sideOverview, true);
            }
            case "admin_products.fxml" -> {
                setNavStyle(navProducts, true);
                setNavStyle(sideProducts, true);
            }
            case "admin_orders.fxml" -> {
                setNavStyle(navOrders, true);
                setNavStyle(sideOrders, true);
            }
            case "admin_customers.fxml" -> setNavStyle(sideCustomers, true);
            case "admin_inventory.fxml" -> setNavStyle(sideInventory, true);
            case "admin_reports.fxml" -> setNavStyle(sideReports, true);
            case "admin_reviews.fxml" -> setNavStyle(sideReviews, true);
            case "admin_reviews_analytics.fxml" -> setNavStyle(sideReviewsAnalytics, true);
            default -> { }
        }
    }

    private void setNavStyle(Button button, boolean active) {
        if (button == null) {
            return;
        }
        button.getStyleClass().removeAll("sidebar-item", "sidebar-item-active", "navbar-link", "navbar-link-active");
        if (button == navHome || button == navProducts || button == navOrders || button == navCustomers || button == navInventory || button == navReports) {
            button.getStyleClass().add(active ? "navbar-link-active" : "navbar-link");
        } else {
            button.getStyleClass().add(active ? "sidebar-item-active" : "sidebar-item");
        }
    }

    @FXML private void navHome() { SceneRouter.navigateTo("admin_overview.fxml"); }
    @FXML private void navProducts() { SceneRouter.navigateTo("admin_products.fxml"); }
    @FXML private void navOrders() { SceneRouter.navigateTo("admin_orders.fxml"); }
    @FXML private void navProfile() { SceneRouter.navigateTo("profile.fxml"); }
    @FXML private void navCustomers() { SceneRouter.navigateTo("admin_customers.fxml"); }
    @FXML private void navInventory() { SceneRouter.navigateTo("admin_inventory.fxml"); }
    @FXML private void navReports() { SceneRouter.navigateTo("admin_reports.fxml"); }
    @FXML private void onSearch() {
        AlertHelper.info("Quick Search", "Navigate to a specific page to use its search and filters:\n\n• Products - Search by name/category\n• Orders - Search by ID/customer\n• Customers - Search by name/email\n• Inventory - Filter by stock level");
    }
    @FXML private void gotoOverview() { navHome(); }
    @FXML private void gotoProducts() { navProducts(); }
    @FXML private void gotoOrders() { navOrders(); }
    @FXML private void gotoCustomers() { navCustomers(); }
    @FXML private void gotoInventory() { navInventory(); }
    @FXML private void gotoReports() { navReports(); }
    @FXML private void gotoReviews() { SceneRouter.navigateTo("admin_reviews.fxml"); }
    @FXML private void gotoReviewsAnalytics() { SceneRouter.navigateTo("admin_reviews_analytics.fxml"); }
    @FXML
    private void exportReport() {
        try {
            String username = SessionManager.getInstance().getCurrentUser() != null
                    ? SessionManager.getInstance().getCurrentUser().getUsername()
                    : "admin";
            var out = CsvExportUtil.exportCsv(
                    navHome != null ? navHome.getScene().getWindow() : null,
                    "Export Admin Snapshot",
                    "admin_snapshot.csv",
                    new String[]{"key", "value"},
                    List.of(
                            new String[]{"exported_at", java.time.LocalDateTime.now().toString()},
                            new String[]{"exported_by", username},
                            new String[]{"hint", "Use page-specific export for full datasets (Orders, Reviews, Analytics, Inventory, Customers)."}
                    )
            );
            if (out != null) {
                AlertHelper.success("Export complete", "Saved: " + out.toAbsolutePath());
            }
        } catch (Exception e) {
            AlertHelper.error("Export failed", e.getMessage());
        }
    }
    @FXML private void onLogout() { 
        activityLogService.logCurrentUser("LOGOUT", "User logged out.");
        SessionManager.getInstance().logout();
        SceneRouter.navigateTo("login.fxml"); 
    }
    @FXML private void onToggleTheme() {
        if (navHome != null && navHome.getScene() != null) {
            ThemeManager.toggleTheme(navHome.getScene());
        }
    }
}

package com.jvmart.controllers;

import com.jvmart.models.Order;
import com.jvmart.models.Review;
import com.jvmart.services.ActivityLogService;
import com.jvmart.services.OrderService;
import com.jvmart.services.ReviewService;
import com.jvmart.utils.GlobalRefresh;
import com.jvmart.session.SessionManager;
import com.jvmart.utils.AlertHelper;
import com.jvmart.utils.CsvExportUtil;
import com.jvmart.utils.SceneRouter;
import com.jvmart.utils.ThemeManager;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

public class AdminDashboardController implements GlobalRefresh.Refreshable {
    @FXML private Label totalSalesLabel;
    @FXML private Label ordersTodayLabel;
    @FXML private Label lowStockLabel;
    @FXML private Label customersLabel;
    @FXML private Label todayLabel;
    @FXML private TableView<Order> recentOrdersTable;
    @FXML private TableColumn<Order, String> colId;
    @FXML private TableColumn<Order, String> colCustomer;
    @FXML private TableColumn<Order, String> colItems;
    @FXML private TableColumn<Order, String> colTotal;
    @FXML private TableColumn<Order, String> colStatus;
    @FXML private TableColumn<Order, Void> colView;
    
    // Review management fields
    @FXML private TableView<Review> recentReviewsTable;
    @FXML private TableColumn<Review, String> colReviewId;
    @FXML private TableColumn<Review, String> colProduct;
    @FXML private TableColumn<Review, String> colReviewer;
    @FXML private TableColumn<Review, String> colRating;
    @FXML private TableColumn<Review, String> colComment;
    @FXML private TableColumn<Review, String> colReviewDate;
    @FXML private TableColumn<Review, Void> colReviewActions;

    private final OrderService orderService = new OrderService();
    private final ActivityLogService activityLogService = new ActivityLogService();
    private final ReviewService reviewService = new ReviewService();

    @FXML
    public void initialize() {
        if (SessionManager.getInstance().getCurrentUser() == null || !"admin".equals(SessionManager.getInstance().getCurrentUser().getRole())) {
            SceneRouter.navigateTo("login.fxml");
            return;
        }

        if (todayLabel != null) {
            todayLabel.setText(LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy")));
        }

        // Add null checks for all UI components to prevent NPE in shell FXML
        if (colId != null) {
            colId.setCellValueFactory(data -> new SimpleStringProperty("#JV-" + String.format("%04d", data.getValue().getId())));
        }
        if (colTotal != null) {
            colTotal.setCellValueFactory(data -> new SimpleStringProperty(String.format("GHS %.2f", data.getValue().getTotal())));
        }
        if (colStatus != null) {
            colStatus.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatus().toUpperCase()));
        }
        if (colItems != null) {
            colItems.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getItems().size())));
        }
        if (colCustomer != null) {
            colCustomer.setCellValueFactory(data -> new SimpleStringProperty("User #" + data.getValue().getUserId()));
        }
        if (colView != null) {
            colView.setCellFactory(column -> new javafx.scene.control.TableCell<>() {
                private final javafx.scene.control.Button viewBtn = new javafx.scene.control.Button("View");
                {
                    viewBtn.getStyleClass().add("btn-ghost");
                    viewBtn.setOnAction(e -> SceneRouter.navigateTo("admin_orders.fxml"));
                }

                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setGraphic(null);
                    } else {
                        setGraphic(viewBtn);
                    }
                }
            });
        }

        loadDashboardStats();
    }

    private void loadDashboardStats() {
        new Thread(() -> {
            var result = orderService.getDashboardStats();
            Platform.runLater(() -> {
                if (result instanceof com.jvmart.services.ServiceResult.Success<?> success) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> stats = (Map<String, Object>) success.value();
                    if (totalSalesLabel != null) {
                        totalSalesLabel.setText(String.format("GHS %.2f", (double) stats.get("totalRevenue")));
                    }
                    if (ordersTodayLabel != null) {
                        ordersTodayLabel.setText(String.valueOf(stats.get("ordersToday")));
                    }
                    if (lowStockLabel != null) {
                        lowStockLabel.setText(String.valueOf(stats.get("lowStockCount")));
                    }
                    if (customersLabel != null) {
                        customersLabel.setText(String.valueOf(stats.get("totalCustomers")));
                    }
                    if (recentOrdersTable != null) {
                        recentOrdersTable.setItems(FXCollections.observableArrayList((java.util.List<Order>) stats.get("recentOrders")));
                    }
                } else if (result instanceof com.jvmart.services.ServiceResult.Failure<?> failure) {
                    AlertHelper.error("Error loading dashboard stats: " + failure.message());
                }
            });
        }).start();
    }

    @FXML private void onNavProducts() { SceneRouter.navigateTo("admin_products.fxml"); }
    @FXML private void onNavOrders() { SceneRouter.navigateTo("admin_orders.fxml"); }
    @FXML private void onNavCustomers() { SceneRouter.navigateTo("admin_customers.fxml"); }
    @FXML private void onNavInventory() { SceneRouter.navigateTo("admin_inventory.fxml"); }
    @FXML private void onNavReports() { SceneRouter.navigateTo("admin_reports.fxml"); }
    @FXML private void onLogout() { 
        activityLogService.logCurrentUser("LOGOUT", "User logged out.");
        SessionManager.getInstance().logout();
        SceneRouter.navigateTo("login.fxml"); 
    }
    @FXML private void onToggleTheme() { ThemeManager.toggleTheme(totalSalesLabel.getScene()); }

    @FXML private void navHome() { SceneRouter.navigateTo("admin_overview.fxml"); }
    @FXML private void navProducts() { onNavProducts(); }
    @FXML private void navOrders() { onNavOrders(); }
    @FXML private void gotoOverview() { SceneRouter.navigateTo("admin_overview.fxml"); }
    @FXML private void gotoProducts() { onNavProducts(); }
    @FXML private void gotoOrders() { onNavOrders(); }
    @FXML private void gotoCustomers() { onNavCustomers(); }
    @FXML private void gotoInventory() { onNavInventory(); }
    @FXML private void gotoReports() { onNavReports(); }
    @FXML private void exportReport() {
        try {
            List<String[]> rows = new ArrayList<>();
            rows.add(new String[]{"metric", "value"});
            rows.add(new String[]{"date", todayLabel != null ? todayLabel.getText() : LocalDate.now().toString()});
            rows.add(new String[]{"total_sales", totalSalesLabel != null ? totalSalesLabel.getText() : "GHS 0.00"});
            rows.add(new String[]{"orders_today", ordersTodayLabel != null ? ordersTodayLabel.getText() : "0"});
            rows.add(new String[]{"low_stock", lowStockLabel != null ? lowStockLabel.getText() : "0"});
            rows.add(new String[]{"customers", customersLabel != null ? customersLabel.getText() : "0"});

            var out = CsvExportUtil.exportCsv(
                    totalSalesLabel != null ? totalSalesLabel.getScene().getWindow() : null,
                    "Export Dashboard Report",
                    "dashboard_report.csv",
                    null,
                    rows
            );
            if (out != null) {
                AlertHelper.success("Export complete", "Saved: " + out.toAbsolutePath());
            }
        } catch (Exception e) {
            AlertHelper.error("Export failed", e.getMessage());
        }
    }
    @FXML private void viewAllOrders() { SceneRouter.navigateTo("admin_orders.fxml"); }
    @FXML private void refreshDashboard() { 
        loadDashboardStats();
        AlertHelper.info("Dashboard Refreshed", "Dashboard data has been refreshed successfully.");
    }

    @FXML private void exportOrders() {
        try {
            List<String[]> rows = new ArrayList<>();
            if (recentOrdersTable != null) {
                for (Order o : recentOrdersTable.getItems()) {
                    rows.add(new String[]{
                            String.valueOf(o.getId()),
                            String.valueOf(o.getUserId()),
                            String.valueOf(o.getItems() != null ? o.getItems().size() : 0),
                            String.format("%.2f", o.getTotal()),
                            o.getStatus(),
                            o.getCreatedAt() != null ? o.getCreatedAt().toString() : ""
                    });
                }
            }
            var out = CsvExportUtil.exportCsv(
                    recentOrdersTable != null ? recentOrdersTable.getScene().getWindow() : null,
                    "Export Orders",
                    "orders_export.csv",
                    new String[]{"order_id", "user_id", "items", "total", "status", "created_at"},
                    rows
            );
            if (out != null) {
                AlertHelper.success("Export complete", "Saved: " + out.toAbsolutePath());
            }
        } catch (Exception e) {
            AlertHelper.error("Export failed", e.getMessage());
        }
    }

    @FXML private void viewAllReviews() {
        SceneRouter.navigateTo("admin_reviews.fxml");
    }

    @FXML private void viewReviewsAnalytics() {
        SceneRouter.navigateTo("admin_reviews_analytics.fxml");
    }

    @FXML private void exportReviews() {
        Thread.startVirtualThread(() -> {
            var result = reviewService.getAllReviews();
            Platform.runLater(() -> {
                try {
                    if (result instanceof com.jvmart.services.ServiceResult.Success<?> success) {
                        @SuppressWarnings("unchecked")
                        List<Review> reviews = (List<Review>) success.value();
                        List<String[]> rows = new ArrayList<>();
                        for (Review r : reviews) {
                            rows.add(new String[]{
                                    String.valueOf(r.productId()),
                                    r.username(),
                                    String.valueOf(r.rating()),
                                    r.comment(),
                                    r.createdAt() != null ? r.createdAt().toString() : ""
                            });
                        }
                        var out = CsvExportUtil.exportCsv(
                                recentReviewsTable != null ? recentReviewsTable.getScene().getWindow() : null,
                                "Export Reviews",
                                "all_reviews_export.csv",
                                new String[]{"product_id", "reviewer", "rating", "comment", "created_at"},
                                rows
                        );
                        if (out != null) {
                            AlertHelper.success("Export complete", "Saved: " + out.toAbsolutePath());
                        }
                    } else if (result instanceof com.jvmart.services.ServiceResult.Failure<?> failure) {
                        AlertHelper.error("Export failed", failure.message());
                    }
                } catch (Exception e) {
                    AlertHelper.error("Export failed", e.getMessage());
                }
            });
        });
    }

    @Override
    public void refresh() {
        loadDashboardStats();
        AlertHelper.info("Dashboard Refreshed", "Dashboard data has been refreshed successfully.");
    }

}

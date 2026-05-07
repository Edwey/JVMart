package com.jvmart.controllers;

import com.jvmart.models.Order;
import com.jvmart.services.ActivityLogService;
import com.jvmart.services.OrderService;
import com.jvmart.session.SessionManager;
import com.jvmart.utils.AlertHelper;
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

public class AdminDashboardController {
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

    private final OrderService orderService = new OrderService();
    private final ActivityLogService activityLogService = new ActivityLogService();

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
    @FXML private void exportReport() { AlertHelper.info("Export Report", "Export is not configured yet."); }
    @FXML private void viewAllOrders() { SceneRouter.navigateTo("admin_orders.fxml"); }
    @FXML private void refreshDashboard() { 
        loadDashboardStats();
        AlertHelper.info("Dashboard Refreshed", "Dashboard data has been refreshed successfully.");
    }

}

package com.jvmart.controllers;

import com.jvmart.services.OrderService;
import com.jvmart.session.SessionManager;
import com.jvmart.utils.AlertHelper;
import com.jvmart.utils.CsvExportUtil;
import com.jvmart.utils.SceneRouter;
import com.jvmart.utils.ThemeManager;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

public class AdminReportsController {
    @FXML private BarChart<String, Number> ordersChart;
    @FXML private Label totalRevLabel;
    @FXML private Label revGrowth;
    @FXML private Label monthRevLabel;
    @FXML private Label totalOrdersLabel;
    @FXML private Label newTodayLabel;
    @FXML private Label avgValueLabel;
    @FXML private TableView<String> customersTable;
    @FXML private TableColumn<String, String> colCustomer;
    @FXML private TableColumn<String, String> colStatus;
    @FXML private TableColumn<String, String> colLtv;
    @FXML private TableColumn<String, String> colLast;

    private final OrderService orderService = new OrderService();

    @FXML
    public void initialize() {
        // Security check - ensure user is admin
        if (SessionManager.getInstance().getCurrentUser() == null || !"admin".equals(SessionManager.getInstance().getCurrentUser().getRole())) {
            SceneRouter.navigateTo("login.fxml");
            return;
        }

        configureCustomersTable();

        loadReportData();
    }

    private void configureCustomersTable() {
        if (customersTable == null) {
            return;
        }
        if (colCustomer != null) {
            colCustomer.setCellValueFactory(data -> new SimpleStringProperty(data.getValue()));
        }
        if (colStatus != null) {
            colStatus.setCellValueFactory(data -> new SimpleStringProperty("Active"));
        }
        if (colLtv != null) {
            colLtv.setCellValueFactory(data -> new SimpleStringProperty("GHS 0.00"));
        }
        if (colLast != null) {
            colLast.setCellValueFactory(data -> new SimpleStringProperty(LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy"))));
        }
    }

    private void loadReportData() {
        new Thread(() -> {
            var result = orderService.getDashboardStats();
            Platform.runLater(() -> {
                if (result instanceof com.jvmart.services.ServiceResult.Success<?> success) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> stats = (Map<String, Object>) success.value();
                    double revenue = (double) stats.get("totalRevenue");
                    int ordersToday = ((Number) stats.get("ordersToday")).intValue();
                    int totalCustomers = ((Number) stats.get("totalCustomers")).intValue();

                    if (totalRevLabel != null) {
                        totalRevLabel.setText(String.format("GHS %.2f", revenue));
                    }
                    if (monthRevLabel != null) {
                        monthRevLabel.setText(String.format("GHS %.2f", revenue));
                    }
                    if (totalOrdersLabel != null) {
                        totalOrdersLabel.setText(String.valueOf(ordersToday));
                    }
                    if (newTodayLabel != null) {
                        newTodayLabel.setText(ordersToday + " new today");
                    }
                    if (avgValueLabel != null) {
                        avgValueLabel.setText(ordersToday == 0
                                ? "GHS 0.00"
                                : String.format("GHS %.2f", revenue / ordersToday));
                    }
                    if (revGrowth != null) {
                        revGrowth.setText(totalCustomers + " active customers");
                    }
                    populateCustomerTable(totalCustomers);
                    renderChart();
                } else if (result instanceof com.jvmart.services.ServiceResult.Failure<?> failure) {
                    AlertHelper.error("Error loading report: " + failure.message());
                }
            });
        }).start();
    }

    private void populateCustomerTable(int totalCustomers) {
        if (customersTable == null) {
            return;
        }
        List<String> rows = totalCustomers <= 0
                ? List.of("No active customers yet")
                : java.util.stream.IntStream.rangeClosed(1, Math.min(totalCustomers, 5))
                .mapToObj(i -> "Customer #" + i)
                .toList();
        customersTable.setItems(FXCollections.observableArrayList(rows));
    }

    private void renderChart() {
        if (ordersChart == null) {
            return;
        }
        ordersChart.getData().clear();
        ordersChart.setAnimated(false);
        ordersChart.setLegendVisible(false);
        
        // Configure axes explicitly
        javafx.scene.chart.CategoryAxis xAxis = (javafx.scene.chart.CategoryAxis) ordersChart.getXAxis();
        xAxis.setLabel("Date");
        xAxis.setStyle("-fx-font-size: 12px; -fx-font-weight: bold;");
        
        javafx.scene.chart.NumberAxis yAxis = (javafx.scene.chart.NumberAxis) ordersChart.getYAxis();
        yAxis.setLabel("Number of Orders");
        yAxis.setTickUnit(1);
        yAxis.setMinorTickCount(0);
        yAxis.setAutoRanging(true);
        yAxis.setForceZeroInRange(true);
        yAxis.setStyle("-fx-font-size: 12px; -fx-font-weight: bold;");

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Orders per Day");

        // Get real order data from database
        var result = orderService.getRecentOrders(10); // Get last 10 days of orders
        
        if (result instanceof com.jvmart.services.ServiceResult.Success<?> success) {
            @SuppressWarnings("unchecked")
            List<?> orders = (List<?>) success.value();

            // Group orders by date and count
            Map<String, Integer> dailyOrders = new java.util.HashMap<>();
            for (Object order : orders) {
                if (order instanceof com.jvmart.models.Order orderObj) {
                    String date = orderObj.getCreatedAt().toLocalDate().toString();
                    dailyOrders.merge(date, 1, Integer::sum);
                }
            }

            // Add data to chart (last 7 days)
            for (int i = 6; i >= 0; i--) {
                String date = LocalDate.now().minusDays(i).toString();
                int count = dailyOrders.getOrDefault(date, 0);
                XYChart.Data<String, Number> data = new XYChart.Data<>(date, count);
                series.getData().add(data);
                
                // Add value labels on top of bars
                if (count > 0) {
                    data.nodeProperty().addListener((obs, oldNode, newNode) -> {
                        if (newNode != null) {
                            Label valueLabel = new Label(String.valueOf(count));
                            valueLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #0a0708;");
                            valueLabel.setLayoutY(-20);
                            ((javafx.scene.Group) newNode).getChildren().add(valueLabel);
                        }
                    });
                }
            }
        }

        ordersChart.getData().add(series);
    }

    @FXML private void onNavDashboard() { SceneRouter.navigateTo("admin_overview.fxml"); }
    @FXML private void onNavProducts() { SceneRouter.navigateTo("admin_products.fxml"); }
    @FXML private void onNavOrders() { SceneRouter.navigateTo("admin_orders.fxml"); }
    @FXML private void onNavCustomers() { SceneRouter.navigateTo("admin_customers.fxml"); }
    @FXML private void onNavInventory() { SceneRouter.navigateTo("admin_inventory.fxml"); }
    @FXML private void onToggleTheme() {
        if (ordersChart != null) {
            ThemeManager.toggleTheme(ordersChart.getScene());
        }
    }

    @FXML
    private void onViewAllProducts() {
        SceneRouter.navigateTo("admin_products.fxml");
    }

    @FXML
    private void exportCustomers() {
        try {
            List<String[]> rows = new ArrayList<>();
            if (customersTable != null) {
                for (String c : customersTable.getItems()) {
                    rows.add(new String[]{
                            c,
                            "Active",
                            "GHS 0.00",
                            LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
                    });
                }
            }
            var out = CsvExportUtil.exportCsv(
                    customersTable != null ? customersTable.getScene().getWindow() : null,
                    "Export Customers",
                    "customers_report.csv",
                    new String[]{"customer", "status", "ltv", "last_activity"},
                    rows
            );
            if (out != null) {
                AlertHelper.success("Export complete", "Saved: " + out.toAbsolutePath());
            }
        } catch (Exception e) {
            AlertHelper.error("Export failed", e.getMessage());
        }
    }
}

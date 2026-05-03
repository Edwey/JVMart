package com.jvmart.controllers;

import com.jvmart.models.Order;
import com.jvmart.services.OrderService;
import com.jvmart.session.SessionManager;
import com.jvmart.utils.AlertHelper;
import com.jvmart.utils.SceneRouter;
import com.jvmart.utils.ThemeManager;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class MyOrdersController {
    @FXML private TableView<Order> ordersTable;
    @FXML private TableColumn<Order, String> colOrderId;
    @FXML private TableColumn<Order, String> colDate;
    @FXML private TableColumn<Order, String> colItems;
    @FXML private TableColumn<Order, String> colTotal;
    @FXML private TableColumn<Order, String> colStatus;
    @FXML private TableColumn<Order, Void> colAction;
    @FXML private Button page2Btn;
    @FXML private Button page3Btn;
    @FXML private Button filterAll;
    @FXML private Button filterPending;
    @FXML private Button filterPaid;
    @FXML private Button filterShipped;
    @FXML private Button filterCancelled;

    private static final int PAGE_SIZE = 10;
    private int currentPage = 1;
    private String statusFilter = "all";

    private final OrderService orderService = new OrderService();
    private FilteredList<Order> filteredOrders;
    private List<Order> allOrders = List.of();

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

        colOrderId.setCellValueFactory(data -> new SimpleStringProperty("#JV-" + String.format("%04d", data.getValue().getId())));
        colDate.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCreatedAt().format(DateTimeFormatter.ofPattern("dd MMM yyyy"))));
        colItems.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getItems().size())));
        colTotal.setCellValueFactory(data -> new SimpleStringProperty(String.format("GHS %.2f", data.getValue().getTotal())));
        colStatus.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatus().toUpperCase()));

        colStatus.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item);
                    getStyleClass().removeAll("status-pending", "status-paid", "status-shipped", "status-cancelled");
                    switch (item.toLowerCase()) {
                        case "pending" -> getStyleClass().add("status-pending");
                        case "paid" -> getStyleClass().add("status-paid");
                        case "shipped" -> getStyleClass().add("status-shipped");
                        case "cancelled" -> getStyleClass().add("status-cancelled");
                    }
                }
            }
        });

        colAction.setCellFactory(column -> new TableCell<>() {
            private final Button viewBtn = new Button("View");
            {
                viewBtn.getStyleClass().add("btn-ghost");
                viewBtn.setOnAction(e -> {
                    Order order = getTableView().getItems().get(getIndex());
                    if (order != null) {
                        showOrderDetails(order);
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : viewBtn);
            }
        });

        loadOrders();
    }

    private void loadOrders() {
        new Thread(() -> {
            int userId = SessionManager.getInstance().getCurrentUser().getId();
            var result = orderService.getOrdersForUser(userId);
            Platform.runLater(() -> {
                if (result instanceof com.jvmart.services.ServiceResult.Success<?> success) {
                    @SuppressWarnings("unchecked")
                    List<Order> orders = (List<Order>) success.value();
                    allOrders = orders;
                    filteredOrders = new FilteredList<>(FXCollections.observableArrayList(allOrders));
                    applyFiltersAndPage();
                    updateFilterTabs();
                } else if (result instanceof com.jvmart.services.ServiceResult.Failure<?> failure) {
                    AlertHelper.error("Error loading orders: " + failure.message());
                }
            });
        }).start();
    }

    @FXML private void onBack() { SceneRouter.navigateTo("product_catalog.fxml"); }
    @FXML private void onToggleTheme() { ThemeManager.toggleTheme(ordersTable.getScene()); }

    private void applyFiltersAndPage() {
        if (filteredOrders == null) return;
        filteredOrders.setPredicate(order -> {
            return switch (statusFilter) {
                case "pending", "paid", "shipped", "cancelled" -> order.getStatus().equalsIgnoreCase(statusFilter);
                default -> true;
            };
        });
        List<Order> filtered = filteredOrders.stream().toList();
        int total = filtered.size();
        int totalPages = Math.max(1, (int) Math.ceil(total / (double) PAGE_SIZE));
        currentPage = Math.min(currentPage, totalPages);
        int start = (currentPage - 1) * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, total);
        List<Order> pageItems = filtered.subList(start, end);
        ordersTable.setItems(FXCollections.observableArrayList(pageItems));
    }

    @FXML private void filterAll() { statusFilter = "all"; currentPage = 1; updateFilterTabs(); applyFiltersAndPage(); }
    @FXML private void filterPending() { statusFilter = "pending"; currentPage = 1; updateFilterTabs(); applyFiltersAndPage(); }
    @FXML private void filterPaid() { statusFilter = "paid"; currentPage = 1; updateFilterTabs(); applyFiltersAndPage(); }
    @FXML private void filterShipped() { statusFilter = "shipped"; currentPage = 1; updateFilterTabs(); applyFiltersAndPage(); }
    @FXML private void filterCancelled() { statusFilter = "cancelled"; currentPage = 1; updateFilterTabs(); applyFiltersAndPage(); }

    private void updateFilterTabs() {
        for (Button tab : new Button[]{filterAll, filterPending, filterPaid, filterShipped, filterCancelled}) {
            if (tab != null) {
                tab.getStyleClass().removeAll("filter-tab-active", "filter-tab");
                tab.getStyleClass().add("filter-tab");
            }
        }
        Button activeTab = switch (statusFilter) {
            case "pending" -> filterPending;
            case "paid" -> filterPaid;
            case "shipped" -> filterShipped;
            case "cancelled" -> filterCancelled;
            default -> filterAll;
        };
        if (activeTab != null) {
            activeTab.getStyleClass().removeAll("filter-tab-active", "filter-tab");
            activeTab.getStyleClass().add("filter-tab-active");
        }
    }

    @FXML
    private void prevPage() {
        if (currentPage > 1) {
            currentPage--;
            applyFiltersAndPage();
        }
    }

    @FXML
    private void nextPage() {
        int total = filteredOrders == null ? 0 : filteredOrders.size();
        int totalPages = Math.max(1, (int) Math.ceil(total / (double) PAGE_SIZE));
        if (currentPage < totalPages) {
            currentPage++;
            applyFiltersAndPage();
        }
    }

    @FXML
    private void goPage2() {
        currentPage = 2;
        applyFiltersAndPage();
    }

    @FXML
    private void goPage3() {
        currentPage = 3;
        applyFiltersAndPage();
    }

    @FXML
    private void contactSupport() {
        AlertHelper.info("Support", "Please contact support@jvmart.local");
    }

    @FXML
    private void onRowClick() {
        // reserved for future row expansion
    }

    private void showOrderDetails(Order order) {
        StringBuilder details = new StringBuilder();
        details.append("Order #JV-").append(String.format("%04d", order.getId())).append("\n");
        details.append("Date: ").append(order.getCreatedAt().format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm"))).append("\n");
        details.append("Status: ").append(order.getStatus().toUpperCase()).append("\n");
        details.append("Total: GHS ").append(String.format("%.2f", order.getTotal())).append("\n\n");
        details.append("ITEMS:\n");
        details.append("----------------------------------------\n");
        
        if (order.getItems() != null && !order.getItems().isEmpty()) {
            for (var item : order.getItems()) {
                details.append(item.productName()).append("\n");
                details.append("  Qty: ").append(item.quantity())
                       .append(" x GHS ").append(String.format("%.2f", item.unitPrice()))
                       .append(" = GHS ").append(String.format("%.2f", item.quantity() * item.unitPrice()))
                       .append("\n");
            }
        } else {
            details.append("No items in this order\n");
        }
        
        details.append("----------------------------------------\n");
        AlertHelper.info("Order Details", details.toString());
    }
}

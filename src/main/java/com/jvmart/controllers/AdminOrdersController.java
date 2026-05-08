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
import javafx.scene.layout.StackPane;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.List;

public class AdminOrdersController {
    @FXML private TableView<Order> ordersTable;
    @FXML private TableColumn<Order, String> colId;
    @FXML private TableColumn<Order, String> colCustomer;
    @FXML private TableColumn<Order, String> colDate;
    @FXML private TableColumn<Order, String> colTotal;
    @FXML private TableColumn<Order, String> colStatus;
    @FXML private TableColumn<Order, Void> colActions;
    @FXML private TextField searchField;
    @FXML private Label showingLabel;
    @FXML private Label currentPageLabel;
    @FXML private Label revenueLabel;
    @FXML private Label pendingLabel;
    @FXML private Label avgOrderLabel;
    @FXML private StackPane statusModalOverlay;
    @FXML private Label statusModalOrderId;
    @FXML private Button statusPending;
    @FXML private Button statusPaid;
    @FXML private Button statusShipped;
    @FXML private Button statusCancelled;
    @FXML private Button tabAll;
    @FXML private Button tabPending;
    @FXML private Button tabPaid;
    @FXML private Button tabShipped;
    @FXML private Button tabCancelled;

    private static final int PAGE_SIZE = 10;
    private int currentPage = 1;
    private String statusFilter = "all";
    private Order editingOrder;

    private final OrderService orderService = new OrderService();
    private FilteredList<Order> filteredOrders;
    private List<Order> allOrders = List.of();
    private Map<Integer, String> customerNames = Map.of();

    @FXML
    public void initialize() {
        // Security check - ensure user is admin
        if (SessionManager.getInstance().getCurrentUser() == null || !"admin".equals(SessionManager.getInstance().getCurrentUser().getRole())) {
            SceneRouter.navigateTo("login.fxml");
            return;
        }

        colId.setCellValueFactory(data -> new SimpleStringProperty("#JV-" + String.format("%04d", data.getValue().getId())));
        colDate.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCreatedAt().format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm"))));
        colCustomer.setCellValueFactory(data -> new SimpleStringProperty(
                customerNames.getOrDefault(data.getValue().getUserId(), "User #" + data.getValue().getUserId())));
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

        colActions.setCellFactory(column -> new TableCell<>() {
            private final Button editBtn = new Button("Update");
            {
                editBtn.getStyleClass().add("btn-ghost");
                editBtn.setOnAction(e -> {
                    Order order = getTableView().getItems().get(getIndex());
                    showStatusDialog(order);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : editBtn);
            }
        });

        loadOrders();
        loadStats();
        searchField.textProperty().addListener((obs, oldVal, newVal) -> onSearch());
    }

    private void loadOrders() {
        new Thread(() -> {
            var result = orderService.getAllOrders();
            var namesResult = orderService.getCustomerNames();
            Platform.runLater(() -> {
                switch (namesResult) {
                    case com.jvmart.services.ServiceResult.Success<?> success ->
                            customerNames = castToStringMap(success.value());
                    case com.jvmart.services.ServiceResult.Failure<?> ignored ->
                            customerNames = Map.of();
                }
                switch (result) {
                    case com.jvmart.services.ServiceResult.Success<?> success -> {
                        @SuppressWarnings("unchecked")
                        List<Order> orders = (List<Order>) success.value();
                        allOrders = orders;
                        filteredOrders = new FilteredList<>(FXCollections.observableArrayList(allOrders));
                        applyFiltersAndPage();
                        updateFilterTabs();
                    }
                    case com.jvmart.services.ServiceResult.Failure<?> failure ->
                        AlertHelper.error("Error loading orders: " + failure.message());
                }
            });
        }).start();
    }

    @FXML
    private void onSearch() {
        if (filteredOrders == null) return;
        String query = searchField.getText() == null ? "" : searchField.getText().toLowerCase();
        Integer filterUserId = SceneRouter.getNavigationArgument("filterUserId");
        filteredOrders.setPredicate(order -> {
            boolean matchesQuery = query.isEmpty() ||
                    String.valueOf(order.getId()).contains(query) ||
                    String.valueOf(order.getUserId()).contains(query) ||
                    customerNames.getOrDefault(order.getUserId(), "").toLowerCase().contains(query) ||
                    order.getStatus().toLowerCase().contains(query);
            boolean matchesStatus = switch (statusFilter) {
                case "pending", "paid", "shipped", "cancelled" -> order.getStatus().equalsIgnoreCase(statusFilter);
                default -> true;
            };
            boolean matchesUser = filterUserId == null || order.getUserId() == filterUserId;
            return matchesQuery && matchesStatus && matchesUser;
        });
        currentPage = 1;
        applyFiltersAndPage();
    }

    @FXML private void onNavDashboard() { SceneRouter.navigateTo("admin_overview.fxml"); }
    @FXML private void onNavProducts() { SceneRouter.navigateTo("admin_products.fxml"); }
    @FXML private void onNavCustomers() { SceneRouter.navigateTo("admin_customers.fxml"); }
    @FXML private void onNavInventory() { SceneRouter.navigateTo("admin_inventory.fxml"); }
    @FXML private void onNavReports() { SceneRouter.navigateTo("admin_reports.fxml"); }
    @FXML private void onToggleTheme() { ThemeManager.toggleTheme(ordersTable.getScene()); }

    @SuppressWarnings("unchecked")
    private Map<Integer, String> castToStringMap(Object value) {
        try {
            return (Map<Integer, String>) value;
        } catch (ClassCastException e) {
            return Map.of();
        }
    }

    private void applyFiltersAndPage() {
        List<Order> filtered = filteredOrders == null ? List.of() : filteredOrders.stream().toList();
        int total = filtered.size();
        int totalPages = Math.max(1, (int) Math.ceil(total / (double) PAGE_SIZE));
        currentPage = Math.min(currentPage, totalPages);
        int start = (currentPage - 1) * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, total);
        List<Order> pageItems = filtered.subList(start, end);
        ordersTable.setItems(FXCollections.observableArrayList(pageItems));
        showingLabel.setText("Showing " + (total == 0 ? 0 : start + 1) + "–" + end + " of " + total + " orders");
        
        // Update page indicator
        if (currentPageLabel != null) {
            currentPageLabel.setText(currentPage + " / " + totalPages);
        }
    }

    private void loadStats() {
        new Thread(() -> {
            var revenueResult = orderService.getTotalRevenue();
            var pendingResult = orderService.getPendingCount();
            var avgResult = orderService.getAverageOrderValue();
            Platform.runLater(() -> {
                if (revenueResult instanceof com.jvmart.services.ServiceResult.Success<?> success) {
                    revenueLabel.setText(String.format("GHS %.2f", success.value()));
                } else if (revenueResult instanceof com.jvmart.services.ServiceResult.Failure<?>) {
                    revenueLabel.setText("GHS 0");
                }
                if (pendingResult instanceof com.jvmart.services.ServiceResult.Success<?> success) {
                    pendingLabel.setText(String.valueOf(success.value()));
                } else if (pendingResult instanceof com.jvmart.services.ServiceResult.Failure<?>) {
                    pendingLabel.setText("0");
                }
                if (avgResult instanceof com.jvmart.services.ServiceResult.Success<?> success) {
                    avgOrderLabel.setText(String.format("GHS %.2f", success.value()));
                } else if (avgResult instanceof com.jvmart.services.ServiceResult.Failure<?>) {
                    avgOrderLabel.setText("GHS 0");
                }
            });
        }).start();
    }

    private void showStatusDialog(Order order) {
        editingOrder = order;
        statusModalOrderId.setText("Order #JV-" + String.format("%04d", order.getId()));
        statusModalOverlay.setVisible(true);
        statusModalOverlay.setManaged(true);
    }

    @FXML
    private void closeStatusModal() {
        statusModalOverlay.setVisible(false);
        statusModalOverlay.setManaged(false);
        editingOrder = null;
    }

    @FXML
    private void setStatusPending() { updateOrderStatus("pending"); }
    
    @FXML
    private void setStatusPaid() { updateOrderStatus("paid"); }
    
    @FXML
    private void setStatusShipped() { updateOrderStatus("shipped"); }
    
    @FXML
    private void setStatusCancelled() { updateOrderStatus("cancelled"); }
    
    private void updateOrderStatus(String status) {
        if (editingOrder == null) return;
        var result = orderService.updateStatus(editingOrder.getId(), status);
        if (result instanceof com.jvmart.services.ServiceResult.Success<?>) {
            closeStatusModal();
            loadOrders();
            AlertHelper.success("Status updated", "Order status changed to " + status.toUpperCase());
        } else if (result instanceof com.jvmart.services.ServiceResult.Failure<?> failure) {
            AlertHelper.error("Failed to update status: " + failure.message());
        }
    }

    @FXML private void filterAll() { statusFilter = "all"; updateFilterTabs(); onSearch(); }
    @FXML private void filterPending() { statusFilter = "pending"; updateFilterTabs(); onSearch(); }
    @FXML private void filterPaid() { statusFilter = "paid"; updateFilterTabs(); onSearch(); }
    @FXML private void filterShipped() { statusFilter = "shipped"; updateFilterTabs(); onSearch(); }
    @FXML private void filterCancelled() { statusFilter = "cancelled"; updateFilterTabs(); onSearch(); }

    private void updateFilterTabs() {
        // Reset all tabs to inactive
        for (Button tab : new Button[]{tabAll, tabPending, tabPaid, tabShipped, tabCancelled}) {
            if (tab != null) {
                tab.getStyleClass().removeAll("filter-tab-active", "filter-tab");
                tab.getStyleClass().add("filter-tab");
            }
        }
        // Set active tab
        Button activeTab = switch (statusFilter) {
            case "pending" -> tabPending;
            case "paid" -> tabPaid;
            case "shipped" -> tabShipped;
            case "cancelled" -> tabCancelled;
            default -> tabAll;
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
}

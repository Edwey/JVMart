package com.jvmart.controllers;

import com.jvmart.models.User;
import com.jvmart.services.UserService;
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
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import java.util.List;

public class AdminCustomersController {
    @FXML private TableView<User> customersTable;
    @FXML private TableColumn<User, String> colId;
    @FXML private TableColumn<User, String> colName;
    @FXML private TableColumn<User, String> colUsername;
    @FXML private TableColumn<User, String> colEmail;
    @FXML private TableColumn<User, String> colRegistered;
    @FXML private TableColumn<User, String> colOrders;
    @FXML private TableColumn<User, Void> colActions;
    @FXML private TextField searchField;
    @FXML private Label showingLabel;
    @FXML private Button page1;
    @FXML private Button page2;
    @FXML private Button page3;

    private static final int PAGE_SIZE = 10;
    private int currentPage = 1;

    private final UserService userService = new UserService();
    private final OrderService orderService = new OrderService();
    private FilteredList<User> filteredCustomers;
    private List<User> allCustomers = List.of();
    private java.util.Map<Integer, Integer> orderCounts = java.util.Map.of();

    @FXML
    public void initialize() {
        // Security check - ensure user is admin
        if (SessionManager.getInstance().getCurrentUser() == null || !"admin".equals(SessionManager.getInstance().getCurrentUser().getRole())) {
            SceneRouter.navigateTo("login.fxml");
            return;
        }

        colId.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getId())));
        colName.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getFullName()));
        colUsername.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getUsername()));
        colEmail.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getEmail()));
        colRegistered.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCreatedAt().toLocalDate().toString()));
        colOrders.setCellValueFactory(data -> {
            int countValue = orderCounts.getOrDefault(data.getValue().getId(), 0);
            return new SimpleStringProperty(countValue + " Orders");
        });
        colActions.setCellFactory(column -> new javafx.scene.control.TableCell<>() {
            private final javafx.scene.layout.HBox actions = new javafx.scene.layout.HBox(4);
            private final javafx.scene.control.Button viewOrdersBtn = new javafx.scene.control.Button("Orders");
            private final javafx.scene.control.Button emailBtn = new javafx.scene.control.Button("Email");
            
            {
                actions.setSpacing(4);
                actions.getChildren().addAll(viewOrdersBtn, emailBtn);
                
                viewOrdersBtn.getStyleClass().add("btn-ghost");
                emailBtn.getStyleClass().add("btn-ghost");
                
                viewOrdersBtn.setMinWidth(75);
                viewOrdersBtn.setMaxWidth(75);
                emailBtn.setMinWidth(65);
                emailBtn.setMaxWidth(65);
                
                viewOrdersBtn.setOnAction(e -> {
                    javafx.scene.control.TableRow<User> row = getTableRow();
                    if (row != null && row.getItem() != null) {
                        SceneRouter.navigateTo("admin_orders.fxml",
                                java.util.Map.of("filterUserId", row.getItem().getId()));
                    }
                });
                
                emailBtn.setOnAction(e -> {
                    javafx.scene.control.TableRow<User> row = getTableRow();
                    if (row != null && row.getItem() != null) {
                        AlertHelper.info("Contact Customer", "Email: " + row.getItem().getEmail());
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                int index = getIndex();
                if (empty || index < 0 || index >= getTableView().getItems().size()) {
                    setGraphic(null);
                } else {
                    User user = getTableView().getItems().get(index);
                    if (user != null) {
                        setGraphic(actions);
                    } else {
                        setGraphic(null);
                    }
                }
            }
        });

        loadCustomers();
        searchField.textProperty().addListener((obs, oldVal, newVal) -> onSearch());
    }

    private void loadCustomers() {
        new Thread(() -> {
            var result = userService.getAllCustomers();
            var orderResult = orderService.getOrderCounts();
            Platform.runLater(() -> {
                switch (orderResult) {
                    case com.jvmart.services.ServiceResult.Success<?> success ->
                        orderCounts = castToIntIntMap(success.value());
                    case com.jvmart.services.ServiceResult.Failure<?> ignored ->
                        orderCounts = java.util.Map.of();
                }

                switch (result) {
                    case com.jvmart.services.ServiceResult.Success<?> success -> {
                        @SuppressWarnings("unchecked")
                        List<User> customers = (List<User>) success.value();
                        allCustomers = customers;
                        filteredCustomers = new FilteredList<>(FXCollections.observableArrayList(allCustomers));
                        applyFilterAndPage();
                    }
                    case com.jvmart.services.ServiceResult.Failure<?> failure ->
                        AlertHelper.error("Error loading customers: " + failure.message());
                }
            });
        }).start();
    }

    @SuppressWarnings("unchecked")
    private java.util.Map<Integer, Integer> castToIntIntMap(Object value) {
        try {
            return (java.util.Map<Integer, Integer>) value;
        } catch (ClassCastException e) {
            return java.util.Map.of();
        }
    }

    @FXML
    private void onSearch() {
        if (filteredCustomers == null) return;
        String query = searchField.getText() == null ? "" : searchField.getText().toLowerCase();
        filteredCustomers.setPredicate(user -> {
            if (query.isEmpty()) return true;
            return user.getFullName().toLowerCase().contains(query) ||
                   user.getUsername().toLowerCase().contains(query) ||
                   user.getEmail().toLowerCase().contains(query);
        });
        currentPage = 1;
        applyFilterAndPage();
    }

    @FXML private void onNavDashboard() { SceneRouter.navigateTo("admin_overview.fxml"); }
    @FXML private void onNavProducts() { SceneRouter.navigateTo("admin_products.fxml"); }
    @FXML private void onNavOrders() { SceneRouter.navigateTo("admin_orders.fxml"); }
    @FXML private void onNavInventory() { SceneRouter.navigateTo("admin_inventory.fxml"); }
    @FXML private void onNavReports() { SceneRouter.navigateTo("admin_reports.fxml"); }
    @FXML private void onToggleTheme() { ThemeManager.toggleTheme(customersTable.getScene()); }

    private void applyFilterAndPage() {
        List<User> filtered = filteredCustomers == null ? List.of() : filteredCustomers.stream().toList();
        int total = filtered.size();
        int totalPages = Math.max(1, (int) Math.ceil(total / (double) PAGE_SIZE));
        currentPage = Math.min(currentPage, totalPages);
        int start = (currentPage - 1) * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, total);
        List<User> pageItems = filtered.subList(start, end);
        customersTable.setItems(FXCollections.observableArrayList(pageItems));
        showingLabel.setText("Showing " + (total == 0 ? 0 : start + 1) + "–" + end + " of " + total + " customers");
    }

    @FXML
    private void prevPage() {
        if (currentPage > 1) {
            currentPage--;
            applyFilterAndPage();
        }
    }

    @FXML
    private void nextPage() {
        int total = filteredCustomers == null ? 0 : filteredCustomers.size();
        int totalPages = Math.max(1, (int) Math.ceil(total / (double) PAGE_SIZE));
        if (currentPage < totalPages) {
            currentPage++;
            applyFilterAndPage();
        }
    }

    @FXML
    private void goPage2() {
        currentPage = 2;
        applyFilterAndPage();
    }

    @FXML
    private void goPage3() {
        currentPage = 3;
        applyFilterAndPage();
    }
}

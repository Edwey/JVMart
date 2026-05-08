package com.jvmart.controllers;

import com.jvmart.models.Product;
import com.jvmart.services.ProductService;
import com.jvmart.session.SessionManager;
import com.jvmart.utils.AlertHelper;
import com.jvmart.utils.SceneRouter;
import com.jvmart.utils.ThemeManager;
import com.jvmart.utils.GlobalRefresh;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.util.converter.IntegerStringConverter;

import java.util.List;

public class AdminInventoryController implements GlobalRefresh.Refreshable {
    @FXML private TableView<Product> inventoryTable;
    @FXML private TableColumn<Product, javafx.scene.image.ImageView> colImage;
    @FXML private TableColumn<Product, String> colProduct;
    @FXML private TableColumn<Product, String> colCategory;
    @FXML private TableColumn<Product, Integer> colStock;
    @FXML private TableColumn<Product, String> colStatus;
    @FXML private TableColumn<Product, Void> colSave;
    @FXML private Label stockValueLabel;
    @FXML private Label restockLabel;
    @FXML private Label showingLabel;

    private static final int PAGE_SIZE = 10;
    private int currentPage = 1;
    private boolean lowStockOnly = false;
    private final java.util.Set<Integer> dirtyStock = new java.util.HashSet<>();

    private final ProductService productService = new ProductService();
    private java.util.List<Product> allProducts = java.util.List.of();

    @FXML
    public void initialize() {
        // Security check - ensure user is admin
        if (SessionManager.getInstance().getCurrentUser() == null || !"admin".equals(SessionManager.getInstance().getCurrentUser().getRole())) {
            SceneRouter.navigateTo("login.fxml");
            return;
        }

        // Set up cell value factories
        colImage.setCellValueFactory(data -> {
            javafx.scene.image.ImageView imageView = new javafx.scene.image.ImageView();
            javafx.scene.image.Image image = com.jvmart.utils.ImageHelper.loadProductImage(data.getValue().getImagePath());
            if (image != null) {
                imageView.setImage(image);
                imageView.setFitWidth(60);
                imageView.setFitHeight(60);
                imageView.setPreserveRatio(true);
            }
            return new javafx.beans.property.SimpleObjectProperty<javafx.scene.image.ImageView>(imageView);
        });
        
        colProduct.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getName()));
        colCategory.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCategory()));
        
        inventoryTable.setEditable(true);
        colStock.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));
        colStock.setOnEditCommit(event -> {
            Product p = event.getRowValue();
            p.setStock(event.getNewValue());
            dirtyStock.add(p.getId());
            updateSummaryTiles(allProducts);
            inventoryTable.refresh();
        });
        colStock.setCellValueFactory(data -> new javafx.beans.property.SimpleIntegerProperty(data.getValue().getStock()).asObject());

        colStatus.setCellValueFactory(data -> {
            int stock = data.getValue().getStock();
            if (stock == 0) return new SimpleStringProperty("OUT OF STOCK");
            if (stock <= 5) return new SimpleStringProperty("LOW STOCK");
            return new SimpleStringProperty("IN STOCK");
        });

        colStatus.setCellFactory(column -> new TableCell<Product, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item);
                    getStyleClass().removeAll("stock-healthy", "stock-low", "stock-out");
                    switch (item) {
                        case "IN STOCK" -> getStyleClass().add("stock-healthy");
                        case "LOW STOCK" -> getStyleClass().add("stock-low");
                        case "OUT OF STOCK" -> getStyleClass().add("stock-out");
                    }
                }
            }
        });

        colSave.setCellFactory(column -> new TableCell<>() {
            private final javafx.scene.layout.HBox actions = new javafx.scene.layout.HBox(4);
            private final Button editBtn = new Button("Edit");
            private final Button restockBtn = new Button("Restock");
            private final Button saveBtn = new Button("Save");

            {
                actions.setSpacing(6);
                actions.getChildren().addAll(editBtn, restockBtn, saveBtn);

                editBtn.getStyleClass().add("btn-ghost");
                restockBtn.getStyleClass().add("btn-ghost");
                saveBtn.getStyleClass().add("btn-secondary");

                editBtn.setMinWidth(85);
                editBtn.setMaxWidth(85);
                restockBtn.setMinWidth(95);
                restockBtn.setMaxWidth(95);
                saveBtn.setMinWidth(85);
                saveBtn.setMaxWidth(85);

                editBtn.setOnAction(e -> {
                    Product p = getTableView().getItems().get(getIndex());
                    if (p != null) {
                        // TODO: Implement edit product dialog
                        AlertHelper.info("Edit Product", "Product editing will be implemented soon.");
                    }
                });

                restockBtn.setOnAction(e -> {
                    Product p = getTableView().getItems().get(getIndex());
                    if (p != null) {
                        TextInputDialog inputDialog = new TextInputDialog("10");
                        inputDialog.setTitle("Restock Product");
                        inputDialog.setHeaderText("Restock: " + p.getName());
                        inputDialog.setContentText("Enter quantity to add:");
                        
                        // Apply dark theme
                        inputDialog.getDialogPane().getStylesheets().add(
                            getClass().getResource("/com/jvmart/css/jvmart-dark.css").toExternalForm()
                        );
                        inputDialog.getDialogPane().setStyle("-fx-background-color: #231F20;");
                        
                        inputDialog.showAndWait().ifPresent(qtyStr -> {
                            try {
                                int qty = Integer.parseInt(qtyStr);
                                if (qty > 0) {
                                    p.setStock(p.getStock() + qty);
                                    dirtyStock.add(p.getId());
                                    updateSummaryTiles(allProducts);
                                    inventoryTable.refresh();
                                    AlertHelper.success("Stock Updated", "Added " + qty + " units to " + p.getName());
                                }
                            } catch (NumberFormatException ex) {
                                AlertHelper.error("Invalid Input", "Please enter a valid number.");
                            }
                        });
                    }
                });

                saveBtn.setOnAction(e -> {
                    Product p = getTableView().getItems().get(getIndex());
                    if (p != null) {
                        saveStockUpdate(p);
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
                    Product p = getTableView().getItems().get(index);
                    if (p != null) {
                        saveBtn.setDisable(!dirtyStock.contains(p.getId()));
                        setGraphic(actions);
                    } else {
                        setGraphic(null);
                    }
                }
            }
        });

        loadProducts();
    }

    private void loadProducts() {
        new Thread(() -> {
            var result = productService.getAllProducts();
            Platform.runLater(() -> {
                switch (result) {
                    case com.jvmart.services.ServiceResult.Success<?> success -> {
                        @SuppressWarnings("unchecked")
                        List<Product> products = (List<Product>) success.value();
                        allProducts = products;
                        applyFilterAndPage();
                        updateSummaryTiles(allProducts);
                    }
                    case com.jvmart.services.ServiceResult.Failure<?> failure ->
                        AlertHelper.error("Error loading inventory: " + failure.message());
                }
            });
        }).start();
    }

    

    @FXML private void onNavDashboard() { SceneRouter.navigateTo("admin_overview.fxml"); }
    @FXML private void onNavProducts() { SceneRouter.navigateTo("admin_products.fxml"); }
    @FXML private void onNavOrders() { SceneRouter.navigateTo("admin_orders.fxml"); }
    @FXML private void onNavCustomers() { SceneRouter.navigateTo("admin_customers.fxml"); }
    @FXML private void onNavReports() { SceneRouter.navigateTo("admin_reports.fxml"); }
    @FXML private void exportData() {
        AlertHelper.info("Export Data", "Data export functionality will be implemented soon.");
    }

    @Override
    public void refresh() {
        loadProducts();
        updateSummaryTiles(allProducts);
        AlertHelper.info("Inventory Refreshed", "Inventory data has been refreshed successfully.");
    }

    @FXML private void onToggleTheme() { ThemeManager.toggleTheme(inventoryTable.getScene()); }

    @FXML
    private void showFilterMenu() {
        List<String> filters = List.of("All Products", "Low Stock (≤5)", "Out of Stock", "In Stock (>5)");
        ChoiceDialog<String> dialog = new ChoiceDialog<>(lowStockOnly ? "Low Stock (≤5)" : "All Products", filters);
        dialog.setTitle("Filter Inventory");
        dialog.setHeaderText("Choose a filter");
        dialog.setContentText("Filter by:");
        
        // Apply dark theme styling
        dialog.getDialogPane().getStylesheets().add(
            getClass().getResource("/com/jvmart/css/jvmart-dark.css").toExternalForm()
        );
        dialog.getDialogPane().setStyle("-fx-background-color: #231F20;");
        
        // Style buttons
        for (javafx.scene.control.ButtonType buttonType : dialog.getDialogPane().getButtonTypes()) {
            javafx.scene.Node button = dialog.getDialogPane().lookupButton(buttonType);
            if (button != null) {
                button.setStyle(
                    "-fx-background-color: #BB4430;" +
                    "-fx-text-fill: #EFE6DD;" +
                    "-fx-font-weight: bold;" +
                    "-fx-background-radius: 8;" +
                    "-fx-padding: 8 16 8 16;"
                );
            }
        }
        
        dialog.showAndWait().ifPresent(selected -> {
            switch (selected) {
                case "Low Stock (≤5)" -> lowStockOnly = true;
                case "Out of Stock" -> {
                    // Special filter for out of stock
                    lowStockOnly = false;
                    showOutOfStockOnly();
                    return;
                }
                case "In Stock (>5)" -> {
                    lowStockOnly = false;
                    showInStockOnly();
                    return;
                }
                default -> lowStockOnly = false;
            }
            currentPage = 1;
            applyFilterAndPage();
        });
    }

    private void showOutOfStockOnly() {
        List<Product> filtered = allProducts.stream().filter(p -> p.getStock() == 0).toList();
        inventoryTable.setItems(FXCollections.observableArrayList(filtered));
        showingLabel.setText("Showing " + filtered.size() + " out of stock items");
    }

    private void showInStockOnly() {
        List<Product> filtered = allProducts.stream().filter(p -> p.getStock() > 5).toList();
        inventoryTable.setItems(FXCollections.observableArrayList(filtered));
        showingLabel.setText("Showing " + filtered.size() + " in stock items");
    }

    private void applyFilterAndPage() {
        List<Product> filtered = lowStockOnly
                ? allProducts.stream().filter(p -> p.getStock() <= 5).toList()
                : allProducts;
        int total = filtered.size();
        int totalPages = Math.max(1, (int) Math.ceil(total / (double) PAGE_SIZE));
        currentPage = Math.min(currentPage, totalPages);
        int start = (currentPage - 1) * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, total);
        List<Product> pageItems = filtered.subList(start, end);
        inventoryTable.setItems(FXCollections.observableArrayList(pageItems));
        showingLabel.setText("Showing " + (total == 0 ? 0 : start + 1) + "–" + end + " of " + total + " items");
    }

    private void updateSummaryTiles(List<Product> products) {
        double totalValue = products.stream().mapToDouble(p -> p.getPrice() * p.getStock()).sum();
        long lowStockCount = products.stream().filter(p -> p.getStock() <= 5).count();
        stockValueLabel.setText(String.format("GHS %.2f", totalValue));
        restockLabel.setText(lowStockCount + " SKUs");
    }

    private void saveStockUpdate(Product p) {
        var result = productService.saveProduct(p);
        switch (result) {
            case com.jvmart.services.ServiceResult.Success<?> ignored -> {
                dirtyStock.remove(p.getId());
                inventoryTable.refresh();
            }
            case com.jvmart.services.ServiceResult.Failure<?> failure ->
                AlertHelper.error("Error updating stock: " + failure.message());
        }
    }

    @FXML
    private void toggleLowStockFilter() {
        lowStockOnly = !lowStockOnly;
        currentPage = 1;
        applyFilterAndPage();
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
        int total = lowStockOnly ? (int) allProducts.stream().filter(p -> p.getStock() <= 5).count() : allProducts.size();
        int totalPages = Math.max(1, (int) Math.ceil(total / (double) PAGE_SIZE));
        if (currentPage < totalPages) {
            currentPage++;
            applyFilterAndPage();
        }
    }

    @FXML
    private void onRowClick() {
        Product selectedProduct = inventoryTable.getSelectionModel().getSelectedItem();
        if (selectedProduct != null) {
            // Handle row click - could show product details or edit dialog
            AlertHelper.info("Product Selected", "Selected: " + selectedProduct.getName());
        }
    }
}

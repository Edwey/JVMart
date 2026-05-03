package com.jvmart.controllers;

import com.jvmart.models.Product;
import com.jvmart.services.ProductService;
import com.jvmart.session.SessionManager;
import com.jvmart.utils.AlertHelper;
import com.jvmart.utils.SceneRouter;
import com.jvmart.utils.ThemeManager;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.List;

public class AdminProductsController {
    @FXML private TableView<Product> productsTable;
    @FXML private TableColumn<Product, String> colId;
    @FXML private TableColumn<Product, String> colName;
    @FXML private TableColumn<Product, String> colCategory;
    @FXML private TableColumn<Product, String> colPrice;
    @FXML private TableColumn<Product, String> colStock;
    @FXML private TableColumn<Product, Void> colActions;
    @FXML private Label showingLabel;
    @FXML private Label currentPageLabel;
    @FXML private ComboBox<String> sortCombo;
    @FXML private StackPane modalOverlay;
    @FXML private Label modalTitle;
    @FXML private TextField productNameField;
    @FXML private TextArea descField;
    @FXML private ComboBox<String> categoryCombo;
    @FXML private TextField priceField;
    @FXML private TextField stockField;
    @FXML private TextField imagePathField;
    @FXML private Label modalError;
    @FXML private Button saveProductBtn;
    @FXML private StackPane deleteOverlay;

    private static final int PAGE_SIZE = 10;
    private int currentPage = 1;
    private String categoryFilter = "all";
    private String sortOption = "Newest";

    private final ProductService productService = new ProductService();
    private List<Product> allProducts = List.of();
    private List<Product> filteredProducts = List.of();
    private Product editingProduct;
    private Product deleteTarget;

    @FXML
    private void prevPage() {
        if (currentPage > 1) {
            currentPage--;
            applyPage();
        }
    }

    @FXML
    private void nextPage() {
        int maxPage = (int) Math.ceil((double) filteredProducts.size() / PAGE_SIZE);
        if (currentPage < maxPage) {
            currentPage++;
            applyPage();
        }
    }

    private void applyPage() {
        int total = filteredProducts.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) total / PAGE_SIZE));
        currentPage = Math.min(currentPage, totalPages);
        int start = (currentPage - 1) * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, total);
        List<Product> pageItems = filteredProducts.subList(start, end);
        productsTable.setItems(FXCollections.observableArrayList(pageItems));

        // Update pagination info
        showingLabel.setText("Showing " + (total == 0 ? 0 : start + 1) + "–" + end + " of " + total + " products");
        
        // Update page number display
        if (currentPageLabel != null) {
            currentPageLabel.setText(currentPage + " / " + totalPages);
        }
    }

    @FXML
    public void initialize() {
        // Security check - ensure user is admin
        if (SessionManager.getInstance().getCurrentUser() == null || !"admin".equals(SessionManager.getInstance().getCurrentUser().getRole())) {
            SceneRouter.navigateTo("login.fxml");
            return;
        }

        // Setup sort combo
        if (sortCombo != null) {
            sortCombo.getItems().addAll("Newest", "Oldest", "Price: Low to High", "Price: High to Low", "Name: A-Z", "Name: Z-A");
            sortCombo.setValue("Newest");
            sortCombo.setOnAction(e -> applySortingAndFiltering());
        }

        colId.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getId())));
        colName.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getName()));
        colCategory.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCategory()));
        colPrice.setCellValueFactory(data -> new SimpleStringProperty(String.format("GHS %.2f", data.getValue().getPrice())));
        colStock.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getStock())));

        colStock.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item);
                    int stock = Integer.parseInt(item);
                    getStyleClass().removeAll("stock-healthy", "stock-low", "stock-out");
                    if (stock > 5) getStyleClass().add("stock-healthy");
                    else if (stock > 0) getStyleClass().add("stock-low");
                    else getStyleClass().add("stock-out");
                }
            }
        });

        colActions.setCellFactory(column -> new TableCell<>() {
            private final Button editBtn = new Button("Edit");
            private final Button deleteBtn = new Button("Delete");
            private final HBox actions = new HBox(6, editBtn, deleteBtn);
            {
                editBtn.getStyleClass().add("btn-ghost");
                deleteBtn.getStyleClass().add("btn-ghost");
                editBtn.setOnAction(e -> showModal(getTableView().getItems().get(getIndex())));
                deleteBtn.setOnAction(e -> showDelete(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : actions);
            }
        });

        categoryCombo.getItems().setAll("Electronics", "Clothing", "Home & Living", "Editorial");
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
                        applySortingAndFiltering();
                    }
                    case com.jvmart.services.ServiceResult.Failure<?> failure ->
                        AlertHelper.error("Error loading products: " + failure.message());
                }
            });
        }).start();
    }

    private void applySortingAndFiltering() {
        // Apply category filter
        filteredProducts = switch (categoryFilter) {
            case "all" -> allProducts;
            case "Electronics" -> allProducts.stream().filter(p -> "Electronics".equals(p.getCategory())).toList();
            case "Clothing" -> allProducts.stream().filter(p -> "Clothing".equals(p.getCategory())).toList();
            case "Home & Living" -> allProducts.stream().filter(p -> "Home & Living".equals(p.getCategory())).toList();
            case "Editorial" -> allProducts.stream().filter(p -> "Editorial".equals(p.getCategory())).toList();
            default -> allProducts;
        };

        // Apply sorting
        if (sortCombo != null && sortCombo.getValue() != null) {
            sortOption = sortCombo.getValue();
        }
        
        filteredProducts = switch (sortOption) {
            case "Oldest" -> filteredProducts.stream().sorted((a, b) -> Integer.compare(a.getId(), b.getId())).toList();
            case "Price: Low to High" -> filteredProducts.stream().sorted((a, b) -> Double.compare(a.getPrice(), b.getPrice())).toList();
            case "Price: High to Low" -> filteredProducts.stream().sorted((a, b) -> Double.compare(b.getPrice(), a.getPrice())).toList();
            case "Name: A-Z" -> filteredProducts.stream().sorted((a, b) -> a.getName().compareTo(b.getName())).toList();
            case "Name: Z-A" -> filteredProducts.stream().sorted((a, b) -> b.getName().compareTo(a.getName())).toList();
            default -> filteredProducts.stream().sorted((a, b) -> Integer.compare(b.getId(), a.getId())).toList(); // Newest
        };

        currentPage = 1;
        applyPage();
    }

    @FXML private void filterAll() { categoryFilter = "all"; applySortingAndFiltering(); }
    @FXML private void filterElectronics() { categoryFilter = "Electronics"; applySortingAndFiltering(); }
    @FXML private void filterClothing() { categoryFilter = "Clothing"; applySortingAndFiltering(); }
    @FXML private void filterHome() { categoryFilter = "Home & Living"; applySortingAndFiltering(); }
    @FXML private void filterEditorial() { categoryFilter = "Editorial"; applySortingAndFiltering(); }

    @FXML private void openAddProduct() {
        showModal(null);
    }

    @FXML private void onNavDashboard() { SceneRouter.navigateTo("admin_overview.fxml"); }
    @FXML private void onNavOrders() { SceneRouter.navigateTo("admin_orders.fxml"); }
    @FXML private void onNavCustomers() { SceneRouter.navigateTo("admin_customers.fxml"); }
    @FXML private void onNavInventory() { SceneRouter.navigateTo("admin_inventory.fxml"); }
    @FXML private void onNavReports() { SceneRouter.navigateTo("admin_reports.fxml"); }
    @FXML private void onToggleTheme() { ThemeManager.toggleTheme(productsTable.getScene()); }

    private void showModal(Product product) {
        editingProduct = product;
        modalTitle.setText(product == null ? "Add New Product" : "Edit Product");
        productNameField.setText(product == null ? "" : product.getName());
        descField.setText(product == null ? "" : product.getDescription());
        categoryCombo.setValue(product == null ? null : product.getCategory());
        priceField.setText(product == null ? "" : String.valueOf(product.getPrice()));
        stockField.setText(product == null ? "" : String.valueOf(product.getStock()));
        imagePathField.setText(product == null ? "" : product.getImagePath());
        modalError.setVisible(false);
        modalError.setManaged(false);
        modalOverlay.setVisible(true);
        modalOverlay.setManaged(true);
    }

    private void showDelete(Product product) {
        deleteTarget = product;
        deleteOverlay.setVisible(true);
        deleteOverlay.setManaged(true);
    }

    @FXML
    private void saveProduct() {
        String name = productNameField.getText();
        String category = categoryCombo.getValue();
        String desc = descField.getText();
        String priceText = priceField.getText();
        String stockText = stockField.getText();
        String imagePath = imagePathField.getText();

        double price;
        int stock;
        try {
            price = Double.parseDouble(priceText);
            stock = Integer.parseInt(stockText);
        } catch (NumberFormatException e) {
            modalError.setText("Price and stock must be valid numbers.");
            modalError.setVisible(true);
            modalError.setManaged(true);
            return;
        }

        if (name == null || name.isBlank()) {
            modalError.setText("Product name is required.");
            modalError.setVisible(true);
            modalError.setManaged(true);
            return;
        }
        if (category == null || category.isBlank()) {
            modalError.setText("Category is required.");
            modalError.setVisible(true);
            modalError.setManaged(true);
            return;
        }

        boolean isNewProduct = editingProduct == null;
        Product product = editingProduct == null
                ? new Product(0, name, desc, price, stock, category, sanitizeImagePath(imagePath))
                : new Product(editingProduct.getId(), name, desc, price, stock, category, sanitizeImagePath(imagePath));

        var result = productService.saveProduct(product);
        switch (result) {
            case com.jvmart.services.ServiceResult.Success<?> success -> {
                currentPage = 1;
                closeModal();
                loadProducts();
                AlertHelper.success("Product saved", isNewProduct ? "The new product was added." : "Product changes were saved.");
            }
            case com.jvmart.services.ServiceResult.Failure<?> failure -> {
                modalError.setText(failure.message());
                modalError.setVisible(true);
                modalError.setManaged(true);
            }
        }
    }

    @FXML
    private void closeModal() {
        modalOverlay.setVisible(false);
        modalOverlay.setManaged(false);
        editingProduct = null;
    }

    @FXML
    private void chooseImage() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose Product Image");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.webp"));

        String currentPath = imagePathField.getText();
        if (currentPath != null && !currentPath.isBlank()) {
            File currentFile = new File(currentPath);
            File parent = currentFile.getParentFile();
            if (parent != null && parent.exists()) {
                chooser.setInitialDirectory(parent);
            }
        }

        Stage stage = (Stage) modalOverlay.getScene().getWindow();
        File selected = chooser.showOpenDialog(stage);
        if (selected != null) {
            imagePathField.setText(selected.getAbsolutePath());
        }
    }

    @FXML
    private void confirmDelete() {
        if (deleteTarget == null) {
            closeDelete();
            return;
        }
        var result = productService.deleteProduct(deleteTarget.getId());
        switch (result) {
            case com.jvmart.services.ServiceResult.Success<?> success -> {
                closeDelete();
                loadProducts();
            }
            case com.jvmart.services.ServiceResult.Failure<?> failure -> {
                AlertHelper.error("Failed to delete: " + failure.message());
                closeDelete();
            }
        }
    }

    @FXML
    private void closeDelete() {
        deleteOverlay.setVisible(false);
        deleteOverlay.setManaged(false);
        deleteTarget = null;
    }

    private String sanitizeImagePath(String imagePath) {
        return imagePath == null ? "" : imagePath.trim();
    }
}

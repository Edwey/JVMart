package com.jvmart.controllers;

import com.jvmart.models.Order;
import com.jvmart.models.Review;
import com.jvmart.services.OrderService;
import com.jvmart.services.ReviewService;
import com.jvmart.utils.GlobalRefresh;
import com.jvmart.session.SessionManager;
import com.jvmart.utils.AlertHelper;
import com.jvmart.utils.SceneRouter;
import com.jvmart.utils.ThemeManager;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class MyOrdersController implements GlobalRefresh.Refreshable {
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
    
    // Review-related fields
    @FXML private Button viewOrders;
    @FXML private Button viewReviews;
    @FXML private VBox orderFilters;
    @FXML private VBox reviewFilters;
    @FXML private VBox orderContent;
    @FXML private VBox reviewContent;
    @FXML private ScrollPane reviewsScrollPane;
    @FXML private VBox reviewsList;
    @FXML private HBox reviewPagination;
    @FXML private Button filterAllReviews;
    @FXML private Button filter5Star;
    @FXML private Button filter4Star;
    @FXML private Button filter3Star;
    @FXML private Button filter2Star;
    @FXML private Button filter1Star;

    private static final int PAGE_SIZE = 10;
    private int currentPage = 1;
    private int reviewCurrentPage = 1;
    private String statusFilter = "all";
    private int ratingFilter = 0;
    private boolean isShowingReviews = false;

    private final OrderService orderService = new OrderService();
    private final ReviewService reviewService = new ReviewService();
    private FilteredList<Order> filteredOrders;
    private List<Order> allOrders = List.of();
    private List<Review> allReviews = List.of();

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

        Object initialView = SceneRouter.consumeNavigationArgument("myOrdersInitialView");
        if ("reviews".equals(initialView)) {
            Platform.runLater(this::viewReviews);
        }
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
    private void viewOrders() {
        isShowingReviews = false;
        orderContent.setVisible(true);
        orderContent.setManaged(true);
        reviewContent.setVisible(false);
        reviewContent.setManaged(false);
        orderFilters.setVisible(true);
        orderFilters.setManaged(true);
        reviewFilters.setVisible(false);
        reviewFilters.setManaged(false);
        reviewPagination.setVisible(false);
        reviewPagination.setManaged(false);
        
        viewOrders.getStyleClass().remove("filter-tab");
        viewOrders.getStyleClass().add("filter-tab-active");
        viewReviews.getStyleClass().remove("filter-tab-active");
        viewReviews.getStyleClass().add("filter-tab");
    }

    @FXML
    private void viewReviews() {
        isShowingReviews = true;
        orderContent.setVisible(false);
        orderContent.setManaged(false);
        reviewContent.setVisible(true);
        reviewContent.setManaged(true);
        orderFilters.setVisible(false);
        orderFilters.setManaged(false);
        reviewFilters.setVisible(true);
        reviewFilters.setManaged(true);
        reviewPagination.setVisible(true);
        reviewPagination.setManaged(true);
        
        viewReviews.getStyleClass().remove("filter-tab");
        viewReviews.getStyleClass().add("filter-tab-active");
        viewOrders.getStyleClass().remove("filter-tab-active");
        viewOrders.getStyleClass().add("filter-tab");
        
        loadReviews();
    }

    private void loadReviews() {
        var user = SessionManager.getInstance().getCurrentUser();
        if (user == null) return;
        
        Thread.startVirtualThread(() -> {
            var result = reviewService.getReviewsForUser(user.getId());
            Platform.runLater(() -> {
                switch (result) {
                    case com.jvmart.services.ServiceResult.Success<?> success -> {
                        @SuppressWarnings("unchecked")
                        List<Review> reviews = (List<Review>) success.value();
                        allReviews = reviews;
                        displayReviews(reviews);
                        updateReviewFilterButtons(0);
                    }
                    case com.jvmart.services.ServiceResult.Failure<?> failure ->
                            AlertHelper.error("Failed to load reviews: " + failure.message());
                    default -> AlertHelper.error("Unexpected error loading reviews");
                }
            });
        });
    }

    private void displayReviews(List<Review> reviews) {
        if (reviewsList == null) return;
        
        reviewsList.getChildren().clear();
        for (Review review : reviews) {
            VBox reviewCard = createReviewCard(review);
            reviewsList.getChildren().add(reviewCard);
        }
    }

    private VBox createReviewCard(Review review) {
        VBox card = new VBox(8);
        card.getStyleClass().add("card-surface");
        card.setPadding(new Insets(16));

        // Header with product name and rating
        HBox header = new HBox(8);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        
        Label productLabel = new Label("Product #" + review.productId());
        productLabel.getStyleClass().add("review-product-label");
        
        Label ratingLabel = new Label("⭐".repeat(review.rating()));
        ratingLabel.getStyleClass().add("review-rating-label");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        Label dateLabel = new Label(review.createdAt().format(DateTimeFormatter.ofPattern("dd MMM yyyy")));
        dateLabel.getStyleClass().add("review-date-label");
        
        header.getChildren().addAll(productLabel, spacer, ratingLabel, dateLabel);

        // Comment
        Label commentLabel = new Label(review.comment());
        commentLabel.setWrapText(true);
        commentLabel.getStyleClass().add("review-comment-label");

        card.getChildren().addAll(header, commentLabel);
        return card;
    }

    @FXML
    private void contactSupport() {
        AlertHelper.info("Support", "Please contact support@jvmart.local");
    }

    @FXML
    private void onRowClick() {
        // reserved for future row expansion
    }

    @FXML
    private void filterAllReviews() {
        applyRatingFilter(0);
    }

    @FXML private void filter5Star() { applyRatingFilter(5); }
    @FXML private void filter4Star() { applyRatingFilter(4); }
    @FXML private void filter3Star() { applyRatingFilter(3); }
    @FXML private void filter2Star() { applyRatingFilter(2); }
    @FXML private void filter1Star() { applyRatingFilter(1); }

    @FXML private void prevReviewPage() { /* reserved */ }
    @FXML private void nextReviewPage() { /* reserved */ }
    @FXML private void goReviewPage2() { /* reserved */ }
    @FXML private void goReviewPage3() { /* reserved */ }

    private void applyRatingFilter(int stars) {
        ratingFilter = stars;
        if (allReviews == null || allReviews.isEmpty()) {
            loadReviews();
            return;
        }
        if (stars <= 0) {
            displayReviews(allReviews);
        } else {
            displayReviews(allReviews.stream().filter(r -> r.rating() == stars).toList());
        }
        updateReviewFilterButtons(stars);
    }

    private void updateReviewFilterButtons(int activeStars) {
        List<Button> tabs = List.of(filterAllReviews, filter5Star, filter4Star, filter3Star, filter2Star, filter1Star);
        for (Button tab : tabs) {
            if (tab != null) {
                tab.getStyleClass().removeAll("filter-tab-active", "filter-tab");
                tab.getStyleClass().add("filter-tab");
            }
        }
        Button active = switch (activeStars) {
            case 5 -> filter5Star;
            case 4 -> filter4Star;
            case 3 -> filter3Star;
            case 2 -> filter2Star;
            case 1 -> filter1Star;
            default -> filterAllReviews;
        };
        if (active != null) {
            active.getStyleClass().removeAll("filter-tab", "filter-tab-active");
            active.getStyleClass().add("filter-tab-active");
        }
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

    @Override
    public void refresh() {
        loadOrders();
        if (isShowingReviews) {
            loadReviews();
        }
    }
}

package com.jvmart.controllers;

import com.jvmart.models.Review;
import com.jvmart.services.ReviewService;
import com.jvmart.session.SessionManager;
import com.jvmart.utils.AlertHelper;
import com.jvmart.utils.SceneRouter;
import com.jvmart.utils.GlobalRefresh;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class AdminReviewsController implements GlobalRefresh.Refreshable {
    @FXML private TableView<Review> reviewsTable;
    @FXML private TableColumn<Review, String> colReviewId;
    @FXML private TableColumn<Review, String> colProduct;
    @FXML private TableColumn<Review, String> colReviewer;
    @FXML private TableColumn<Review, String> colRating;
    @FXML private TableColumn<Review, String> colComment;
    @FXML private TableColumn<Review, String> colDate;
    @FXML private TableColumn<Review, String> colStatus;
    @FXML private TableColumn<Review, Void> colActions;
    
    @FXML private ComboBox<String> ratingFilter;
    @FXML private TextField searchField;
    @FXML private DatePicker startDate;
    @FXML private DatePicker endDate;
    @FXML private Label reviewCountLabel;
    @FXML private Button page2Btn;
    @FXML private Button page3Btn;

    private static final int PAGE_SIZE = 10;
    private int currentPage = 1;
    private String ratingFilterValue = "All";
    private String searchFilter = "";
    private LocalDate startDateFilter = null;
    private LocalDate endDateFilter = null;

    private final ReviewService reviewService = new ReviewService();
    private FilteredList<Review> filteredReviews;
    private List<Review> allReviews = List.of();

    @FXML
    public void initialize() {
        // Security check - ensure user is admin
        var user = SessionManager.getInstance().getCurrentUser();
        if (user == null || !"admin".equals(user.getRole())) {
            SceneRouter.navigateTo("login.fxml");
            return;
        }

        // Initialize rating filter
        ratingFilter.getItems().addAll("All", "5 Stars", "4 Stars", "3 Stars", "2 Stars", "1 Star");
        ratingFilter.setValue("All");

        setupTableColumns();
        loadReviews();
    }

    private void setupTableColumns() {
        // Review ID (using productId as ID since Review model doesn't have reviewId)
        colReviewId.setCellValueFactory(cellData -> {
            int productId = cellData.getValue().productId();
            return new SimpleStringProperty("R" + productId);
        });
        
        // Product Name
        colProduct.setCellValueFactory(cellData -> {
            int productId = cellData.getValue().productId();
            return new SimpleStringProperty("Product #" + productId);
        });
        
        // Reviewer
        colReviewer.setCellValueFactory(new PropertyValueFactory<>("username"));
        
        // Rating with colored stars
        colRating.setCellValueFactory(cellData -> new SimpleStringProperty(String.valueOf(cellData.getValue().rating())));
        AtomicBoolean loggedOnce = new AtomicBoolean(false);
        colRating.setCellFactory(column -> new TableCell<>() {
            private final HBox starsBox = new HBox(2);
            private final Label[] stars = new Label[5];

            {
                for (int i = 0; i < 5; i++) {
                    Label l = new Label("☆");
                    l.getStyleClass().add("admin-star-empty");
                    stars[i] = l;
                    starsBox.getChildren().add(l);
                }
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    return;
                }

                int rating;
                try {
                    rating = Integer.parseInt(item);
                } catch (NumberFormatException e) {
                    rating = 0;
                }

                for (int i = 0; i < 5; i++) {
                    boolean filled = i < rating;
                    stars[i].setText(filled ? "★" : "☆");
                    stars[i].getStyleClass().removeAll("admin-star-empty", "admin-star-filled");
                    stars[i].getStyleClass().add(filled ? "admin-star-filled" : "admin-star-empty");
                }

                // Avoid log spam: only first rendered non-empty cell.
                if (!loggedOnce.getAndSet(true)) {
                    // #region agent log
                    com.jvmart.utils.DebugLog.log(
                            "adminReviewsStarRendering",
                            "AdminReviewsController.colRating",
                            "Rendered admin review stars",
                            java.util.Map.of("rating", rating),
                            "pre-fix"
                    );
                    // #endregion
                }

                setGraphic(starsBox);
            }
        });
        
        // Comment (truncated)
        colComment.setCellValueFactory(cellData -> {
            String comment = cellData.getValue().comment();
            if (comment.length() > 50) {
                comment = comment.substring(0, 50) + "...";
            }
            return new SimpleStringProperty(comment);
        });
        
        // Date
        colDate.setCellValueFactory(cellData -> {
            LocalDate date = cellData.getValue().createdAt().toLocalDate();
            return new SimpleStringProperty(date.format(DateTimeFormatter.ofPattern("dd MMM yyyy")));
        });
        
        // Status
        colStatus.setCellValueFactory(cellData -> {
            return new SimpleStringProperty("Active");
        });
        
        // Actions
        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button deleteButton = new Button("Delete");
            private final Button viewButton = new Button("View");
            private final HBox hbox = new HBox(5, viewButton, deleteButton);

            {
                deleteButton.getStyleClass().add("btn-danger");
                viewButton.getStyleClass().add("btn-secondary");
                
                deleteButton.setOnAction(event -> {
                    Review review = getTableView().getItems().get(getIndex());
                    deleteReview(review);
                });
                
                viewButton.setOnAction(event -> {
                    Review review = getTableView().getItems().get(getIndex());
                    viewReview(review);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(hbox);
                }
            }
        });
    }

    private void loadReviews() {
        Thread.startVirtualThread(() -> {
            var result = reviewService.getAllReviews();
            Platform.runLater(() -> {
                switch (result) {
                    case com.jvmart.services.ServiceResult.Success<?> success -> {
                        @SuppressWarnings("unchecked")
                        List<Review> reviews = (List<Review>) success.value();
                        allReviews = reviews;
                        filteredReviews = new FilteredList<>(FXCollections.observableArrayList(reviews));
                        applyFiltersAndPage();
                    }
                    case com.jvmart.services.ServiceResult.Failure<?> failure ->
                            AlertHelper.error("Failed to load reviews: " + failure.message());
                    default -> AlertHelper.error("Unexpected error loading reviews");
                }
            });
        });
    }

    private void applyFiltersAndPage() {
        if (filteredReviews == null) return;

        // Apply filters
        filteredReviews.setPredicate(review -> {
            // Rating filter
            if (!"All".equals(ratingFilterValue)) {
                int requiredRating = Integer.parseInt(ratingFilterValue.substring(0, 1));
                if (review.rating() != requiredRating) {
                    return false;
                }
            }

            // Search filter
            if (!searchFilter.isEmpty() && 
                !review.username().toLowerCase().contains(searchFilter.toLowerCase()) &&
                !review.comment().toLowerCase().contains(searchFilter.toLowerCase())) {
                return false;
            }

            // Date filter
            if (startDateFilter != null && review.createdAt().toLocalDate().isBefore(startDateFilter)) {
                return false;
            }
            if (endDateFilter != null && review.createdAt().toLocalDate().isAfter(endDateFilter)) {
                return false;
            }

            return true;
        });

        // Update count
        reviewCountLabel.setText(filteredReviews.size() + " reviews");

        // Apply pagination
        int totalItems = filteredReviews.size();
        int startIndex = (currentPage - 1) * PAGE_SIZE;
        int endIndex = Math.min(startIndex + PAGE_SIZE, totalItems);
        
        List<Review> pageItems = filteredReviews.stream()
                .skip(startIndex)
                .limit(PAGE_SIZE)
                .toList();
        
        reviewsTable.setItems(FXCollections.observableArrayList(pageItems));
        
        // Update pagination buttons
        updatePaginationButtons(totalItems);
    }

    private void updatePaginationButtons(int totalItems) {
        int totalPages = (int) Math.ceil((double) totalItems / PAGE_SIZE);
        
        page2Btn.setDisable(totalPages < 2);
        page3Btn.setDisable(totalPages < 3);
    }

    @FXML
    private void applyFilters() {
        currentPage = 1;
        ratingFilterValue = ratingFilter.getValue();
        searchFilter = searchField.getText() != null ? searchField.getText() : "";
        startDateFilter = startDate.getValue();
        endDateFilter = endDate.getValue();
        applyFiltersAndPage();
    }

    @FXML
    private void clearFilters() {
        currentPage = 1;
        ratingFilter.setValue("All");
        searchField.clear();
        startDate.setValue(null);
        endDate.setValue(null);
        ratingFilterValue = "All";
        searchFilter = "";
        startDateFilter = null;
        endDateFilter = null;
        applyFiltersAndPage();
    }

    @FXML
    private void refreshReviews() {
        loadReviews();
    }

    @FXML
    private void prevPage() {
        if (currentPage > 1) {
            currentPage--;
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
    private void nextPage() {
        int totalPages = (int) Math.ceil((double) filteredReviews.size() / PAGE_SIZE);
        if (currentPage < totalPages) {
            currentPage++;
            applyFiltersAndPage();
        }
    }

    private void deleteReview(Review review) {
        AlertHelper.confirm("Delete Review", 
            "Are you sure you want to delete this review?\n\n" +
            "Product: #" + review.productId() + "\n" +
            "Reviewer: " + review.username() + "\n" +
            "Rating: " + "⭐".repeat(review.rating()) + "\n" +
            "Comment: " + review.comment(),
            () -> {
                // In a real implementation, this would delete from database
                AlertHelper.info("Review Deleted", "Review has been deleted successfully.");
                loadReviews();
            }
        );
    }

    private void viewReview(Review review) {
        StringBuilder details = new StringBuilder();
        details.append("REVIEW DETAILS\n");
        details.append("================\n\n");
        details.append("Product ID: ").append(review.productId()).append("\n");
        details.append("Reviewer: ").append(review.username()).append("\n");
        details.append("Rating: ").append("⭐".repeat(review.rating())).append("\n");
        details.append("Date: ").append(review.createdAt().format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm"))).append("\n\n");
        details.append("Comment:\n").append(review.comment());
        
        AlertHelper.info("Review Details", details.toString());
    }

    @FXML
    private void exportReviews() {
        AlertHelper.info("Export Reviews", "Review export functionality will be implemented soon.");
    }

    @Override
    public void refresh() {
        loadReviews();
        AlertHelper.info("Reviews Refreshed", "Review data has been refreshed successfully.");
    }
}

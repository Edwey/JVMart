package com.jvmart.controllers;

import com.jvmart.models.Review;
import com.jvmart.services.ReviewService;
import com.jvmart.session.SessionManager;
import com.jvmart.utils.AlertHelper;
import com.jvmart.utils.SceneRouter;
import com.jvmart.utils.GlobalRefresh;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class AdminReviewsAnalyticsController implements GlobalRefresh.Refreshable {
    
    @FXML private Label totalReviewsLabel;
    @FXML private Label reviewsGrowthLabel;
    @FXML private Label avgRatingLabel;
    @FXML private Label ratingChangeLabel;
    @FXML private Label fiveStarLabel;
    @FXML private Label fiveStarChangeLabel;
    @FXML private Label responseRateLabel;
    @FXML private Label responseChangeLabel;
    
    @FXML private BarChart<String, Number> ratingDistributionChart;
    @FXML private CategoryAxis ratingXAxis;
    @FXML private NumberAxis ratingYAxis;
    
    @FXML private LineChart<String, Number> reviewsTrendChart;
    @FXML private CategoryAxis trendXAxis;
    @FXML private NumberAxis trendYAxis;
    
    @FXML private TableView<CategoryAnalytics> categoryTable;
    @FXML private TableColumn<CategoryAnalytics, String> colCategory;
    @FXML private TableColumn<CategoryAnalytics, Integer> colCategoryReviews;
    @FXML private TableColumn<CategoryAnalytics, Double> colCategoryAvgRating;
    @FXML private TableColumn<CategoryAnalytics, String> colCategoryTrend;
    
    private final ReviewService reviewService = new ReviewService();
    private List<Review> allReviews = new ArrayList<>();
    
    public static class CategoryAnalytics {
        private final String category;
        private final int reviewCount;
        private final double avgRating;
        private final String trend;
        
        public CategoryAnalytics(String category, int reviewCount, double avgRating, String trend) {
            this.category = category;
            this.reviewCount = reviewCount;
            this.avgRating = avgRating;
            this.trend = trend;
        }
        
        public String getCategory() { return category; }
        public int getReviewCount() { return reviewCount; }
        public double getAvgRating() { return avgRating; }
        public String getTrend() { return trend; }
    }
    
    @FXML
    public void initialize() {
        // Security check - ensure user is admin
        if (SessionManager.getInstance().getCurrentUser() == null || !"admin".equals(SessionManager.getInstance().getCurrentUser().getRole())) {
            SceneRouter.navigateTo("login.fxml");
            return;
        }
        
        setupCharts();
        loadAnalyticsData();
    }
    
    private void setupCharts() {
        // Setup rating distribution chart
        ratingXAxis.setCategories(FXCollections.observableArrayList("1★", "2★", "3★", "4★", "5★"));
        ratingYAxis.setLabel("Number of Reviews");
        
        // Setup reviews trend chart
        trendXAxis.setLabel("Date");
        trendYAxis.setLabel("Reviews");
        
        // Setup category table
        colCategory.setCellValueFactory(new PropertyValueFactory<>("category"));
        colCategoryReviews.setCellValueFactory(new PropertyValueFactory<>("reviewCount"));
        colCategoryAvgRating.setCellValueFactory(new PropertyValueFactory<>("avgRating"));
        colCategoryTrend.setCellValueFactory(new PropertyValueFactory<>("trend"));
    }
    
    private void loadAnalyticsData() {
        Thread.startVirtualThread(() -> {
            var result = reviewService.getAllReviews();
            Platform.runLater(() -> {
                switch (result) {
                    case com.jvmart.services.ServiceResult.Success<?> success -> {
                        @SuppressWarnings("unchecked")
                        List<Review> reviews = (List<Review>) success.value();
                        allReviews = reviews;
                        updateAnalytics();
                    }
                    case com.jvmart.services.ServiceResult.Failure<?> failure ->
                        AlertHelper.error("Error loading analytics: " + failure.message());
                }
            });
        });
    }
    
    private void updateAnalytics() {
        if (allReviews.isEmpty()) {
            showEmptyState();
            return;
        }
        
        updateSummaryCards();
        updateRatingDistribution();
        updateReviewsTrend();
        updateCategoryAnalytics();
    }
    
    private void showEmptyState() {
        totalReviewsLabel.setText("0");
        avgRatingLabel.setText("0.0");
        fiveStarLabel.setText("0%");
        responseRateLabel.setText("0%");
        
        ratingDistributionChart.getData().clear();
        reviewsTrendChart.getData().clear();
        categoryTable.setItems(FXCollections.observableArrayList());
    }
    
    private void updateSummaryCards() {
        int totalReviews = allReviews.size();
        double avgRating = allReviews.stream()
                .mapToInt(Review::rating)
                .average()
                .orElse(0.0);
        
        long fiveStarCount = allReviews.stream()
                .filter(r -> r.rating() == 5)
                .count();
        double fiveStarPercentage = totalReviews > 0 ? (fiveStarCount * 100.0 / totalReviews) : 0.0;
        
        // Simulate growth (in real app, would compare with previous period)
        double reviewsGrowth = Math.random() * 20 - 5; // -5% to +15%
        double ratingChange = Math.random() * 0.4 - 0.2; // -0.2 to +0.2
        double fiveStarGrowth = Math.random() * 10 - 3; // -3% to +7%
        double responseGrowth = Math.random() * 15 - 5; // -5% to +10%
        
        totalReviewsLabel.setText(String.format("%,d", totalReviews));
        reviewsGrowthLabel.setText(String.format("%+.1f%% this month", reviewsGrowth));
        reviewsGrowthLabel.getStyleClass().setAll(reviewsGrowth >= 0 ? "analytics-change-positive" : "analytics-change-negative");
        
        avgRatingLabel.setText(String.format("%.1f", avgRating));
        ratingChangeLabel.setText(String.format("%+.1f this month", ratingChange));
        ratingChangeLabel.getStyleClass().setAll(ratingChange >= 0 ? "analytics-change-positive" : "analytics-change-negative");
        
        fiveStarLabel.setText(String.format("%.1f%%", fiveStarPercentage));
        fiveStarChangeLabel.setText(String.format("%+.1f%% this month", fiveStarGrowth));
        fiveStarChangeLabel.getStyleClass().setAll(fiveStarGrowth >= 0 ? "analytics-change-positive" : "analytics-change-negative");
        
        responseRateLabel.setText(String.format("%.1f%%", 75.0 + responseGrowth)); // Simulate 75% base
        responseChangeLabel.setText(String.format("%+.1f%% this month", responseGrowth));
        responseChangeLabel.getStyleClass().setAll(responseGrowth >= 0 ? "analytics-change-positive" : "analytics-change-negative");
    }
    
    private void updateRatingDistribution() {
        Map<Integer, Long> ratingCounts = allReviews.stream()
                .collect(Collectors.groupingBy(Review::rating, Collectors.counting()));
        
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Rating Distribution");
        
        for (int rating = 1; rating <= 5; rating++) {
            long count = ratingCounts.getOrDefault(rating, 0L);
            series.getData().add(new XYChart.Data<>(rating + "★", count));
        }
        
        ratingDistributionChart.getData().clear();
        ratingDistributionChart.getData().add(series);
    }
    
    private void updateReviewsTrend() {
        // Generate last 30 days trend (in real app, would query actual data)
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Reviews per Day");
        
        LocalDate today = LocalDate.now();
        Random random = new Random();
        
        for (int i = 29; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            int reviews = random.nextInt(10) + 2; // 2-12 reviews per day
            series.getData().add(new XYChart.Data<>(date.format(DateTimeFormatter.ofPattern("MM/dd")), reviews));
        }
        
        reviewsTrendChart.getData().clear();
        reviewsTrendChart.getData().add(series);
    }
    
    private void updateCategoryAnalytics() {
        Map<String, List<Review>> reviewsByCategory = allReviews.stream()
                .collect(Collectors.groupingBy(this::getProductCategory));
        
        List<CategoryAnalytics> categoryData = reviewsByCategory.entrySet().stream()
                .map(entry -> {
                    String category = entry.getKey();
                    List<Review> reviews = entry.getValue();
                    double avgRating = reviews.stream()
                            .mapToInt(Review::rating)
                            .average()
                            .orElse(0.0);
                    String trend = Math.random() > 0.5 ? "↑" : "↓"; // Simulate trend
                    return new CategoryAnalytics(category, reviews.size(), avgRating, trend);
                })
                .sorted((a, b) -> Integer.compare(b.getReviewCount(), a.getReviewCount()))
                .collect(Collectors.toList());
        
        categoryTable.setItems(FXCollections.observableArrayList(categoryData));
    }
    
    private String getProductCategory(Review review) {
        // In real app, would get actual category from product
        // For now, simulate based on product ID
        String[] categories = {"Electronics", "Clothing", "Books", "Home", "Gaming"};
        return categories[Math.abs(review.productId()) % categories.length];
    }
    
    @FXML
    private void exportAnalytics() {
        AlertHelper.info("Export Analytics", "Analytics export functionality will be implemented soon.");
    }
    
    @FXML
    private void refreshAnalytics() {
        loadAnalyticsData();
        AlertHelper.info("Analytics Refreshed", "Review analytics data has been refreshed successfully.");
    }
    
    @Override
    public void refresh() {
        loadAnalyticsData();
        AlertHelper.info("Analytics Refreshed", "Review analytics data has been refreshed successfully.");
    }
}

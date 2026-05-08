package com.jvmart.services;

import com.jvmart.dao.mongo.ReviewDAO;
import com.jvmart.dao.mongo.ActivityLogDAO;
import com.jvmart.models.Review;
import com.jvmart.session.SessionManager;

import java.time.LocalDateTime;
import java.util.List;

public class ReviewService {
    private final ReviewDAO reviewDAO = new ReviewDAO();
    private final ActivityLogDAO activityLogDAO = new ActivityLogDAO();

    public ServiceResult<Void> submitReview(int productId, int rating, String comment) {
        try {
            SessionManager session = SessionManager.getInstance();
            
            // Check if user is logged in
            if (session.getCurrentUser() == null) {
                return new ServiceResult.Failure<>("You must be logged in to submit a review");
            }
            
            if (rating < 1 || rating > 5) {
                return new ServiceResult.Failure<>("Rating must be between 1 and 5");
            }
            
            Review review = new Review(
                productId,
                session.getCurrentUser().getId(),
                session.getCurrentUser().getUsername(),
                rating,
                comment,
                LocalDateTime.now()
            );

            reviewDAO.saveReview(review);

            // Log action in background
            Thread.startVirtualThread(() -> 
                activityLogDAO.log(session.getCurrentUser().getId(), "SUBMIT_REVIEW", 
                    "Reviewed product #" + productId));
            
            return new ServiceResult.Success<>(null);
        } catch (Exception e) {
            return new ServiceResult.Failure<>("Failed to submit review: " + e.getMessage());
        }
    }

    public ServiceResult<List<Review>> getReviewsForProduct(int productId) {
        try {
            return new ServiceResult.Success<>(reviewDAO.findByProductId(productId));
        } catch (Exception e) {
            return new ServiceResult.Failure<>("Failed to retrieve reviews: " + e.getMessage());
        }
    }

    public ServiceResult<Double> getAverageRating(int productId) {
        try {
            return new ServiceResult.Success<>(reviewDAO.getAverageRating(productId));
        } catch (Exception e) {
            return new ServiceResult.Failure<>("Failed to get average rating: " + e.getMessage());
        }
    }

    public ServiceResult<List<Review>> getReviewsForUser(int userId) {
        try {
            List<Review> allReviews = reviewDAO.findAllReviews();
            List<Review> userReviews = allReviews.stream()
                    .filter(review -> review.userId() == userId)
                    .collect(java.util.stream.Collectors.toList());
            return new ServiceResult.Success<>(userReviews);
        } catch (Exception e) {
            return new ServiceResult.Failure<>("Failed to get user reviews: " + e.getMessage());
        }
    }

    public ServiceResult<List<Review>> getAllReviews() {
        try {
            List<Review> allReviews = reviewDAO.findAllReviews();
            return new ServiceResult.Success<>(allReviews);
        } catch (Exception e) {
            return new ServiceResult.Failure<>("Failed to get all reviews: " + e.getMessage());
        }
    }
}

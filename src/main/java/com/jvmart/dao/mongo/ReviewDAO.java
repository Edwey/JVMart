package com.jvmart.dao.mongo;

import com.jvmart.config.MongoConnection;
import com.jvmart.models.Review;

import java.util.ArrayList;
import java.util.List;

public class ReviewDAO {
    private static final String PRODUCT_ID = "productId";
    private static final String AVG_RATING = "avgRating";
    private static final String COLLECTION_NAME = "reviews";

    public void saveReview(Review review) {
        var collection = MongoConnection.getDatabase().getCollection(COLLECTION_NAME);
        var doc = new org.bson.Document()
                .append(PRODUCT_ID, review.productId())
                .append("userId", review.userId())
                .append("username", review.username())
                .append("rating", review.rating())
                .append("comment", review.comment())
                .append("createdAt", java.util.Date.from(review.createdAt().atZone(java.time.ZoneId.systemDefault()).toInstant()));
        collection.insertOne(doc);
    }

    public List<Review> findByProductId(int productId) {
        List<Review> reviews = new ArrayList<>();
        var collection = MongoConnection.getDatabase().getCollection(COLLECTION_NAME);
        for (var doc : collection.find(new org.bson.Document(PRODUCT_ID, productId))) {
            Review review = new Review(
                    doc.getInteger(PRODUCT_ID),
                    doc.getInteger("userId"),
                    doc.getString("username"),
                    doc.getInteger("rating"),
                    doc.getString("comment"),
                    doc.getDate("createdAt").toInstant()
                            .atZone(java.time.ZoneId.systemDefault())
                            .toLocalDateTime()
            );
            reviews.add(review);
        }
        return reviews;
    }

    public double getAverageRating(int productId) {
        var collection = MongoConnection.getDatabase().getCollection(COLLECTION_NAME);
        var pipeline = List.of(
                new org.bson.Document("$match", new org.bson.Document(PRODUCT_ID, productId)),
                new org.bson.Document("$group", new org.bson.Document("_id", null)
                        .append(AVG_RATING, new org.bson.Document("$avg", "$rating")))
        );
        var result = collection.aggregate(pipeline).first();
        
        return switch (result) {
            case org.bson.Document doc when doc.containsKey(AVG_RATING) -> doc.getDouble(AVG_RATING);
            case null, default -> 0.0;
        };
    }

    public List<Review> findAllReviews() {
        List<Review> reviews = new ArrayList<>();
        var collection = MongoConnection.getDatabase().getCollection(COLLECTION_NAME);
        for (var doc : collection.find()) {
            Review review = new Review(
                    doc.getInteger(PRODUCT_ID),
                    doc.getInteger("userId"),
                    doc.getString("username"),
                    doc.getInteger("rating"),
                    doc.getString("comment"),
                    doc.getDate("createdAt").toInstant()
                            .atZone(java.time.ZoneId.systemDefault())
                            .toLocalDateTime()
            );
            reviews.add(review);
        }
        return reviews;
    }
}

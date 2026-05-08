package com.jvmart.models;

import java.time.LocalDateTime;

/**
 * Wishlist Item Model
 * Represents a product that user has added to their wishlist
 */
public record WishlistItem(
    int id,
    int productId,
    String productName,
    String imagePath,
    double price,
    String category,
    LocalDateTime addedDate,
    boolean inStock,
    int stockLevel
) {
    public WishlistItem {
        // id and addedDate will be set by service when creating from database
    }
    
    /**
     * Create wishlist item from product
     */
    public static WishlistItem fromProduct(Product product) {
        return new WishlistItem(
            0, // ID will be set by service
            product.getId(),
            product.getName(),
            product.getImagePath(),
            product.getPrice(),
            product.getCategory(),
            LocalDateTime.now(),
            product.getStock() > 0,
            product.getStock()
        );
    }
    
    /**
     * Check if item is available (in stock)
     */
    public boolean isAvailable() {
        return inStock && stockLevel > 0;
    }
    
    /**
     * Get formatted price
     */
    public String getFormattedPrice() {
        return String.format("GHS %.2f", price);
    }
    
    /**
     * Get days since added
     */
    public long getDaysSinceAdded() {
        return java.time.temporal.ChronoUnit.DAYS.between(addedDate, LocalDateTime.now());
    }
}

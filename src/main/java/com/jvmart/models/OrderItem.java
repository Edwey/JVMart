package com.jvmart.models;

public record OrderItem(
    int id,
    int orderId,
    int productId,
    String productName,
    int quantity,
    double unitPrice
) {}

package com.jvmart.models;

import java.time.LocalDateTime;

public record Review(
    int productId,
    int userId,
    String username,
    int rating,
    String comment,
    LocalDateTime createdAt
) {}

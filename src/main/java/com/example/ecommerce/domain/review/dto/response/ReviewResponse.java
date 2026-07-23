package com.example.ecommerce.domain.review.dto.response;

import com.example.ecommerce.domain.review.entity.Review;
import java.time.LocalDateTime;

public record ReviewResponse(
        Long id,
        Long orderId,
        Long productId,
        String productName,
        int rating,
        String content,
        LocalDateTime createdAt
) {
    public static ReviewResponse from(Review review) {
        return new ReviewResponse(
                review.getId(),
                review.getOrder().getId(),
                review.getProduct().getId(),
                review.getProduct().getName(),
                review.getRating(),
                review.getContent(),
                review.getCreatedAt());
    }
}

package com.example.ecommerce.domain.review.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ReviewCreateRequest(
        @NotNull Long orderId,
        @NotNull Long productId,
        @Min(1) @Max(5) int rating,
        @NotBlank String content
) {
}

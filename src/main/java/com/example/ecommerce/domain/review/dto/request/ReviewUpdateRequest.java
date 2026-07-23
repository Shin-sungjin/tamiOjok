package com.example.ecommerce.domain.review.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record ReviewUpdateRequest(
        @Min(1) @Max(5) int rating,
        @NotBlank String content
) {
}

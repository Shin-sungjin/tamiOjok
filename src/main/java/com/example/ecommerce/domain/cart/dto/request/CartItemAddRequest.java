package com.example.ecommerce.domain.cart.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CartItemAddRequest(
        @NotNull Long productId,
        @Positive int quantity
) {
}

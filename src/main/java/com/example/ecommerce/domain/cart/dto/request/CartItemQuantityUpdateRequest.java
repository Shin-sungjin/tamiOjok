package com.example.ecommerce.domain.cart.dto.request;

import jakarta.validation.constraints.Positive;

public record CartItemQuantityUpdateRequest(
        @Positive int quantity
) {
}

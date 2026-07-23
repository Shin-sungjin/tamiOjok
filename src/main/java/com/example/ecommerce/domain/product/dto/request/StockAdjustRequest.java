package com.example.ecommerce.domain.product.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record StockAdjustRequest(
        @NotNull @Positive Integer quantity
) {
}

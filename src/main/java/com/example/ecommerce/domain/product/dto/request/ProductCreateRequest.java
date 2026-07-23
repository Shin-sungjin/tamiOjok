package com.example.ecommerce.domain.product.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record ProductCreateRequest(
        @NotBlank String name,
        @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal price,
        String description,
        @Min(0) int initialStock
) {
}

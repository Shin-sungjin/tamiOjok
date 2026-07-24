package com.example.ecommerce.domain.product.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

public record ProductUpdateRequest(
        @NotBlank String name,
        @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal price,
        String description,
        List<String> imageUrls
) {
}

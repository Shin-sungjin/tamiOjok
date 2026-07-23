package com.example.ecommerce.domain.product.dto.response;

import com.example.ecommerce.domain.product.entity.Product;
import com.example.ecommerce.domain.product.entity.ProductStock;
import com.example.ecommerce.domain.product.enums.ProductStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductResponse(
        Long id,
        String name,
        BigDecimal price,
        String description,
        ProductStatus status,
        int availableStock,
        LocalDateTime createdAt
) {
    public static ProductResponse of(Product product, ProductStock stock) {
        return new ProductResponse(
                product.getId(), product.getName(), product.getPrice(), product.getDescription(),
                product.getStatus(), stock.getAvailableQuantity(), product.getCreatedAt());
    }
}

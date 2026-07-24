package com.example.ecommerce.domain.product.dto.response;

import com.example.ecommerce.domain.product.entity.Product;
import com.example.ecommerce.domain.product.entity.ProductStock;
import com.example.ecommerce.domain.product.enums.ProductStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ProductResponse(
        Long id,
        String name,
        BigDecimal price,
        String description,
        ProductStatus status,
        int availableStock,
        List<String> imageUrls,
        LocalDateTime createdAt
) {
    public static ProductResponse of(Product product, ProductStock stock, List<String> imageUrls) {
        return new ProductResponse(
                product.getId(), product.getName(), product.getPrice(), product.getDescription(),
                product.getStatus(), stock.getAvailableQuantity(), imageUrls, product.getCreatedAt());
    }
}

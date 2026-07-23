package com.example.ecommerce.domain.cart.dto.response;

import com.example.ecommerce.domain.cart.entity.CartItem;
import java.math.BigDecimal;

public record CartItemResponse(
        Long id,
        Long productId,
        String productName,
        BigDecimal price,
        int quantity,
        BigDecimal lineTotal
) {
    public static CartItemResponse from(CartItem item) {
        return new CartItemResponse(
                item.getId(), item.getProduct().getId(), item.getProduct().getName(),
                item.getProduct().getPrice(), item.getQuantity(), item.getLineTotal());
    }
}

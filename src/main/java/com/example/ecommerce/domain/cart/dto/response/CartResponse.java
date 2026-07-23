package com.example.ecommerce.domain.cart.dto.response;

import com.example.ecommerce.domain.cart.entity.Cart;
import java.math.BigDecimal;
import java.util.List;

public record CartResponse(
        Long id,
        List<CartItemResponse> items,
        BigDecimal totalAmount
) {
    public static CartResponse from(Cart cart) {
        List<CartItemResponse> items = cart.getCartItems().stream()
                .map(CartItemResponse::from)
                .toList();
        return new CartResponse(cart.getId(), items, cart.getTotalAmount());
    }

    public static CartResponse empty() {
        return new CartResponse(null, List.of(), BigDecimal.ZERO);
    }
}

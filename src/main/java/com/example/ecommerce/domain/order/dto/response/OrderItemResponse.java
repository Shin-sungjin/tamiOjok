package com.example.ecommerce.domain.order.dto.response;

import com.example.ecommerce.domain.order.entity.OrderItem;
import java.math.BigDecimal;

public record OrderItemResponse(
        Long productId,
        String productName,
        BigDecimal orderPrice,
        int quantity,
        BigDecimal lineTotal
) {
    public static OrderItemResponse from(OrderItem item) {
        return new OrderItemResponse(
                item.getProduct().getId(), item.getProduct().getName(),
                item.getOrderPrice(), item.getQuantity(), item.getLineTotal());
    }
}

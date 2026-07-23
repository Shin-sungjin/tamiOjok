package com.example.ecommerce.domain.order.dto.response;

import com.example.ecommerce.domain.order.entity.Order;
import com.example.ecommerce.domain.order.enums.OrderStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        Long id,
        String orderNumber,
        OrderStatus status,
        BigDecimal totalAmount,
        BigDecimal discountAmount,
        BigDecimal paymentAmount,
        List<OrderItemResponse> items,
        LocalDateTime createdAt
) {
    public static OrderResponse from(Order order) {
        List<OrderItemResponse> items = order.getOrderItems().stream()
                .map(OrderItemResponse::from)
                .toList();
        return new OrderResponse(
                order.getId(), order.getOrderNumber(), order.getStatus(),
                order.getTotalAmount(), order.getDiscountAmount(), order.getPaymentAmount(),
                items, order.getCreatedAt());
    }
}

package com.example.ecommerce.domain.order.dto.response;

import com.example.ecommerce.domain.delivery.entity.Delivery;
import com.example.ecommerce.domain.delivery.enums.DeliveryStatus;
import com.example.ecommerce.domain.order.entity.Order;
import com.example.ecommerce.domain.order.enums.OrderStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record AdminOrderResponse(
        Long id,
        String orderNumber,
        String buyerEmail,
        String buyerName,
        OrderStatus status,
        BigDecimal paymentAmount,
        List<OrderItemResponse> items,
        DeliveryStatus deliveryStatus,
        String trackingNumber,
        LocalDateTime createdAt
) {
    public static AdminOrderResponse of(Order order, Delivery delivery) {
        return new AdminOrderResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getUser().getEmail(),
                order.getUser().getName(),
                order.getStatus(),
                order.getPaymentAmount(),
                order.getOrderItems().stream().map(OrderItemResponse::from).toList(),
                delivery != null ? delivery.getStatus() : null,
                delivery != null ? delivery.getTrackingNumber() : null,
                order.getCreatedAt()
        );
    }
}

package com.example.ecommerce.domain.order.dto.response;

import com.example.ecommerce.domain.delivery.entity.Delivery;
import com.example.ecommerce.domain.delivery.enums.DeliveryStatus;
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
        DeliveryStatus deliveryStatus,
        LocalDateTime createdAt
) {
    // 주문 생성 직후처럼 배송 레코드가 아직 없는 게 확실한 경우 전용 — 목록/상세
    // 조회에서는 아래 of(order, delivery)를 써서 실제 배송 상태를 반영해야 함
    // (order.status만으로는 배송완료/반품요청 여부를 알 수 없어서 목록 화면
    // 상태 배지가 상세 화면과 다르게 보이는 문제가 있었음).
    public static OrderResponse from(Order order) {
        return of(order, null);
    }

    public static OrderResponse of(Order order, Delivery delivery) {
        List<OrderItemResponse> items = order.getOrderItems().stream()
                .map(OrderItemResponse::from)
                .toList();
        return new OrderResponse(
                order.getId(), order.getOrderNumber(), order.getStatus(),
                order.getTotalAmount(), order.getDiscountAmount(), order.getPaymentAmount(),
                items, delivery != null ? delivery.getStatus() : null, order.getCreatedAt());
    }
}

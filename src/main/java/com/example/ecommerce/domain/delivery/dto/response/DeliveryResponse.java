package com.example.ecommerce.domain.delivery.dto.response;

import com.example.ecommerce.domain.delivery.entity.Delivery;
import com.example.ecommerce.domain.delivery.enums.DeliveryStatus;
import java.time.LocalDateTime;

public record DeliveryResponse(
        Long id,
        Long orderId,
        String courierCode,
        String trackingNumber,
        DeliveryStatus status,
        LocalDateTime shippedAt,
        LocalDateTime deliveredAt
) {
    public static DeliveryResponse from(Delivery delivery) {
        return new DeliveryResponse(
                delivery.getId(), delivery.getOrder().getId(), delivery.getCourierCode(),
                delivery.getTrackingNumber(), delivery.getStatus(), delivery.getShippedAt(),
                delivery.getDeliveredAt());
    }
}

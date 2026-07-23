package com.example.ecommerce.domain.delivery.entity;

import com.example.ecommerce.domain.delivery.enums.DeliveryStatus;
import com.example.ecommerce.domain.order.entity.Order;
import com.example.ecommerce.global.exception.CustomException;
import com.example.ecommerce.global.exception.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "deliveries")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Delivery {

    private static final Set<DeliveryStatus> POST_SHIPMENT_STATUSES =
            Set.of(DeliveryStatus.SHIPPED, DeliveryStatus.IN_TRANSIT, DeliveryStatus.DELIVERED);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "delivery_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;

    @Column(name = "courier_code", nullable = false)
    private String courierCode;

    @Column(name = "tracking_number", nullable = false)
    private String trackingNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeliveryStatus status;

    @Column(name = "shipped_at")
    private LocalDateTime shippedAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Builder
    private Delivery(Order order, String courierCode, String trackingNumber) {
        this.order = order;
        this.courierCode = courierCode;
        this.trackingNumber = trackingNumber;
        this.status = DeliveryStatus.IN_TRANSIT;
        this.shippedAt = LocalDateTime.now();
    }

    public boolean isPostShipment() {
        return POST_SHIPMENT_STATUSES.contains(this.status);
    }

    public void markDelivered() {
        if (this.status != DeliveryStatus.IN_TRANSIT) {
            throw new CustomException(ErrorCode.INVALID_DELIVERY_STATUS_TRANSITION);
        }
        this.status = DeliveryStatus.DELIVERED;
        this.deliveredAt = LocalDateTime.now();
    }

    public void requestReturn() {
        if (!isPostShipment()) {
            throw new CustomException(ErrorCode.RETURN_NOT_ALLOWED);
        }
        this.status = DeliveryStatus.RETURN_REQUESTED;
    }
}

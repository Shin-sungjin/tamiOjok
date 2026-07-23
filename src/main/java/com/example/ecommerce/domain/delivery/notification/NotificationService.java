package com.example.ecommerce.domain.delivery.notification;

public interface NotificationService {

    void notifyDeliveryCompleted(Long orderId);
}

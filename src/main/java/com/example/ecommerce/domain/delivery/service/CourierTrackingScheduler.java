package com.example.ecommerce.domain.delivery.service;

import com.example.ecommerce.domain.delivery.courier.CourierTrackingClient;
import com.example.ecommerce.domain.delivery.entity.Delivery;
import com.example.ecommerce.domain.delivery.enums.DeliveryStatus;
import com.example.ecommerce.domain.delivery.notification.NotificationService;
import com.example.ecommerce.domain.delivery.repository.DeliveryRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class CourierTrackingScheduler {

    private final DeliveryRepository deliveryRepository;
    private final CourierTrackingClient courierTrackingClient;
    private final NotificationService notificationService;

    @Scheduled(fixedRate = 60_000)
    @Transactional
    public void pollInTransitDeliveries() {
        List<Delivery> inTransitDeliveries = deliveryRepository.findByStatus(DeliveryStatus.IN_TRANSIT);

        for (Delivery delivery : inTransitDeliveries) {
            DeliveryStatus latestStatus = courierTrackingClient.fetchStatus(
                    delivery.getCourierCode(), delivery.getTrackingNumber());

            if (latestStatus == DeliveryStatus.DELIVERED) {
                delivery.markDelivered();
                notificationService.notifyDeliveryCompleted(delivery.getOrder().getId());
            }
        }
    }
}

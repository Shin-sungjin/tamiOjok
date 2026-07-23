package com.example.ecommerce.domain.delivery.courier;

import com.example.ecommerce.domain.delivery.enums.DeliveryStatus;

public interface CourierTrackingClient {

    DeliveryStatus fetchStatus(String courierCode, String trackingNumber);
}

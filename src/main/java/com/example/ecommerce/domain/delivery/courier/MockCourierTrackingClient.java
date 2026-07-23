package com.example.ecommerce.domain.delivery.courier;

import com.example.ecommerce.domain.delivery.enums.DeliveryStatus;
import org.springframework.stereotype.Component;

// TODO: 실제 택배사 배송조회 API(스마트택배 등) 연동 전까지의 개발용 placeholder.
@Component
public class MockCourierTrackingClient implements CourierTrackingClient {

    @Override
    public DeliveryStatus fetchStatus(String courierCode, String trackingNumber) {
        return DeliveryStatus.DELIVERED;
    }
}

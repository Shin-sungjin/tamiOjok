package com.example.ecommerce.domain.delivery.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

// TODO: 실제 알림 채널(푸시/문자/이메일) 연동 전까지의 개발용 placeholder.
@Slf4j
@Component
public class LogNotificationService implements NotificationService {

    @Override
    public void notifyDeliveryCompleted(Long orderId) {
        log.info("배송 완료 - 구매확정 및 리뷰 작성 알림 발송: orderId={}", orderId);
    }
}

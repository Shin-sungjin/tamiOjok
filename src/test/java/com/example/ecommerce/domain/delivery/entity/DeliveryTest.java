package com.example.ecommerce.domain.delivery.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.ecommerce.domain.delivery.enums.DeliveryStatus;
import com.example.ecommerce.domain.delivery.enums.ReturnReason;
import com.example.ecommerce.global.exception.CustomException;
import com.example.ecommerce.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;

class DeliveryTest {

    private Delivery delivery() {
        return Delivery.builder()
                .order(null)
                .courierCode("CJGLS")
                .trackingNumber("1234567890")
                .build(); // 생성 즉시 IN_TRANSIT으로 시작함
    }

    @Test
    void markDelivered_transitionsFromInTransit() {
        Delivery delivery = delivery();

        delivery.markDelivered();

        assertThat(delivery.getStatus()).isEqualTo(DeliveryStatus.DELIVERED);
        assertThat(delivery.getDeliveredAt()).isNotNull();
    }

    @Test
    void requestReturn_storesReasonAndDetail() {
        Delivery delivery = delivery();
        delivery.markDelivered();

        delivery.requestReturn(ReturnReason.DEFECTIVE_PRODUCT, "박스가 찌그러진 채로 도착했습니다.");

        assertThat(delivery.getStatus()).isEqualTo(DeliveryStatus.RETURN_REQUESTED);
        assertThat(delivery.getReturnReason()).isEqualTo(ReturnReason.DEFECTIVE_PRODUCT);
        assertThat(delivery.getReturnDetail()).isEqualTo("박스가 찌그러진 채로 도착했습니다.");
    }

    @Test
    void requestReturn_detailIsOptional() {
        Delivery delivery = delivery();

        delivery.requestReturn(ReturnReason.CHANGE_OF_MIND, null);

        assertThat(delivery.getReturnDetail()).isNull();
    }

    @Test
    void requestReturn_twice_throwsReturnNotAllowed() {
        // 이미 반품 요청된 배송은 RETURN_REQUESTED 상태라 isPostShipment()가
        // false를 반환함 — 중복 반품 요청을 막는 효과.
        Delivery delivery = delivery();
        delivery.requestReturn(ReturnReason.OTHER, "테스트");

        assertThatThrownBy(() -> delivery.requestReturn(ReturnReason.OTHER, "다시 요청"))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getErrorCode())
                .isEqualTo(ErrorCode.RETURN_NOT_ALLOWED);
    }
}

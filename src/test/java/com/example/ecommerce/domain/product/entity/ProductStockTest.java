package com.example.ecommerce.domain.product.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.ecommerce.global.exception.CustomException;
import com.example.ecommerce.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;

class ProductStockTest {

    private ProductStock stockOf(int stockQuantity) {
        return ProductStock.builder()
                .product(null)
                .stockQuantity(stockQuantity)
                .build();
    }

    @Test
    void reserve_reducesAvailableQuantity() {
        ProductStock stock = stockOf(10);

        stock.reserve(3);

        assertThat(stock.getReservedQuantity()).isEqualTo(3);
        assertThat(stock.getAvailableQuantity()).isEqualTo(7);
        assertThat(stock.getStockQuantity()).isEqualTo(10);
    }

    @Test
    void reserve_exactlyAvailableQuantity_succeeds() {
        ProductStock stock = stockOf(5);

        stock.reserve(5);

        assertThat(stock.getAvailableQuantity()).isZero();
    }

    @Test
    void reserve_moreThanAvailable_throwsInsufficientStock() {
        ProductStock stock = stockOf(5);
        stock.reserve(3); // 남은 가용 재고 2

        assertThatThrownBy(() -> stock.reserve(3))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INSUFFICIENT_STOCK);
        // 실패한 예약은 반영되지 않아야 함
        assertThat(stock.getReservedQuantity()).isEqualTo(3);
    }

    @Test
    void releaseReservation_returnsReservedQuantityToAvailable() {
        ProductStock stock = stockOf(10);
        stock.reserve(4);

        stock.releaseReservation(4);

        assertThat(stock.getReservedQuantity()).isZero();
        assertThat(stock.getAvailableQuantity()).isEqualTo(10);
    }

    @Test
    void releaseReservation_moreThanReserved_flooredAtZero() {
        // 결제 타임아웃 스케줄러 등에서 이미 부분 해제된 뒤 중복 호출되는 경우를
        // 대비한 방어 로직 — 마이너스로 내려가면 안 됨.
        ProductStock stock = stockOf(10);
        stock.reserve(2);

        stock.releaseReservation(5);

        assertThat(stock.getReservedQuantity()).isZero();
    }

    @Test
    void confirmDeduction_reducesBothStockAndReservedQuantity() {
        ProductStock stock = stockOf(10);
        stock.reserve(3);

        stock.confirmDeduction(3);

        assertThat(stock.getStockQuantity()).isEqualTo(7);
        assertThat(stock.getReservedQuantity()).isZero();
        assertThat(stock.getAvailableQuantity()).isEqualTo(7);
    }

    @Test
    void confirmDeduction_reservedQuantityFlooredAtZero() {
        ProductStock stock = stockOf(10);
        stock.reserve(2);

        stock.confirmDeduction(2);

        assertThat(stock.getReservedQuantity()).isZero();
    }

    @Test
    void restore_increasesStockQuantityOnly() {
        // 주문 취소 시 재고 복원 — 이미 확정 차감된(reservedQuantity에서 빠진)
        // 수량을 되돌리는 것이므로 reservedQuantity는 건드리지 않아야 함.
        ProductStock stock = stockOf(5);
        stock.reserve(2);
        stock.confirmDeduction(2);

        stock.restore(2);

        assertThat(stock.getStockQuantity()).isEqualTo(5);
        assertThat(stock.getReservedQuantity()).isZero();
    }
}

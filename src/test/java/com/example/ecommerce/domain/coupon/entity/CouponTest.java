package com.example.ecommerce.domain.coupon.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.ecommerce.domain.coupon.enums.DiscountType;
import com.example.ecommerce.global.exception.CustomException;
import com.example.ecommerce.global.exception.ErrorCode;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class CouponTest {

    private Coupon coupon(DiscountType type, long discountValue, long minOrderAmount, Long maxDiscountAmount) {
        return Coupon.builder()
                .code("TEST")
                .name("테스트 쿠폰")
                .discountType(type)
                .discountValue(BigDecimal.valueOf(discountValue))
                .minOrderAmount(BigDecimal.valueOf(minOrderAmount))
                .maxDiscountAmount(maxDiscountAmount == null ? null : BigDecimal.valueOf(maxDiscountAmount))
                .validFrom(LocalDateTime.now().minusDays(1))
                .validUntil(LocalDateTime.now().plusDays(1))
                .build();
    }

    @Test
    void percentageDiscount_flooredToWholeWon() {
        // 10% of 12,345 = 1234.5원 → 원 단위 버림(RoundingMode.DOWN)
        Coupon coupon = coupon(DiscountType.PERCENTAGE, 10, 0, null);

        BigDecimal discount = coupon.calculateDiscount(BigDecimal.valueOf(12345));

        assertThat(discount).isEqualByComparingTo("1234");
    }

    @Test
    void fixedAmountDiscount_returnsDiscountValueAsIs() {
        Coupon coupon = coupon(DiscountType.FIXED_AMOUNT, 5000, 0, null);

        BigDecimal discount = coupon.calculateDiscount(BigDecimal.valueOf(89000));

        assertThat(discount).isEqualByComparingTo("5000");
    }

    @Test
    void percentageDiscount_cappedByMaxDiscountAmount() {
        // 89,000원의 10% = 8,900원이지만 최대할인액 5,000원 캡에 걸려야 함
        // (Lighthouse/체크아웃 미리보기 검증 때 실제로 확인했던 WELCOME10 쿠폰 시나리오)
        Coupon coupon = coupon(DiscountType.PERCENTAGE, 10, 0, 5000L);

        BigDecimal discount = coupon.calculateDiscount(BigDecimal.valueOf(89000));

        assertThat(discount).isEqualByComparingTo("5000");
    }

    @Test
    void percentageDiscount_belowMaxDiscountAmount_notCapped() {
        Coupon coupon = coupon(DiscountType.PERCENTAGE, 10, 0, 5000L);

        BigDecimal discount = coupon.calculateDiscount(BigDecimal.valueOf(30000));

        assertThat(discount).isEqualByComparingTo("3000");
    }

    @Test
    void fixedAmountDiscount_cannotExceedOrderAmount() {
        // 정액 할인이 상품 금액보다 큰 경우(예: 소액 결제) 결제금액이 음수가
        // 되면 안 되므로 상품 금액으로 캡됨.
        Coupon coupon = coupon(DiscountType.FIXED_AMOUNT, 5000, 0, null);

        BigDecimal discount = coupon.calculateDiscount(BigDecimal.valueOf(3000));

        assertThat(discount).isEqualByComparingTo("3000");
    }

    @Test
    void orderAmountBelowMinimum_throwsMinOrderAmountNotMet() {
        Coupon coupon = coupon(DiscountType.FIXED_AMOUNT, 5000, 50000, null);

        assertThatThrownBy(() -> coupon.calculateDiscount(BigDecimal.valueOf(49999)))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getErrorCode())
                .isEqualTo(ErrorCode.COUPON_MIN_ORDER_AMOUNT_NOT_MET);
    }

    @Test
    void orderAmountExactlyAtMinimum_isAllowed() {
        Coupon coupon = coupon(DiscountType.FIXED_AMOUNT, 5000, 50000, null);

        BigDecimal discount = coupon.calculateDiscount(BigDecimal.valueOf(50000));

        assertThat(discount).isEqualByComparingTo("5000");
    }

    @Test
    void isValidNow_trueWithinValidPeriod() {
        Coupon coupon = coupon(DiscountType.FIXED_AMOUNT, 1000, 0, null);

        assertThat(coupon.isValidNow()).isTrue();
    }

    @Test
    void isValidNow_falseAfterExpiry() {
        Coupon expired = Coupon.builder()
                .code("EXPIRED")
                .name("종료된 쿠폰")
                .discountType(DiscountType.FIXED_AMOUNT)
                .discountValue(BigDecimal.valueOf(1000))
                .minOrderAmount(BigDecimal.ZERO)
                .validFrom(LocalDateTime.now().minusDays(90))
                .validUntil(LocalDateTime.now().minusDays(30))
                .build();

        assertThat(expired.isValidNow()).isFalse();
    }
}

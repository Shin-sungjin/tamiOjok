package com.example.ecommerce.domain.coupon.dto.request;

import com.example.ecommerce.domain.coupon.enums.DiscountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CouponCreateRequest(
        @NotBlank String code,
        @NotBlank String name,
        @NotNull DiscountType discountType,
        @NotNull @Positive BigDecimal discountValue,
        @NotNull @PositiveOrZero BigDecimal minOrderAmount,
        BigDecimal maxDiscountAmount,
        @NotNull LocalDateTime validFrom,
        @NotNull LocalDateTime validUntil
) {
}

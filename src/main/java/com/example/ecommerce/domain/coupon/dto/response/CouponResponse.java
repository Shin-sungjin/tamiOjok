package com.example.ecommerce.domain.coupon.dto.response;

import com.example.ecommerce.domain.coupon.entity.Coupon;
import com.example.ecommerce.domain.coupon.enums.DiscountType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CouponResponse(
        Long id,
        String code,
        String name,
        DiscountType discountType,
        BigDecimal discountValue,
        BigDecimal minOrderAmount,
        BigDecimal maxDiscountAmount,
        LocalDateTime validFrom,
        LocalDateTime validUntil
) {
    public static CouponResponse from(Coupon coupon) {
        return new CouponResponse(
                coupon.getId(), coupon.getCode(), coupon.getName(),
                coupon.getDiscountType(), coupon.getDiscountValue(), coupon.getMinOrderAmount(),
                coupon.getMaxDiscountAmount(), coupon.getValidFrom(), coupon.getValidUntil());
    }
}

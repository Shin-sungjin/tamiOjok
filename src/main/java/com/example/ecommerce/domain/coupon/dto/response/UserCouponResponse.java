package com.example.ecommerce.domain.coupon.dto.response;

import com.example.ecommerce.domain.coupon.entity.UserCoupon;
import com.example.ecommerce.domain.coupon.enums.DiscountType;
import com.example.ecommerce.domain.coupon.enums.UserCouponStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record UserCouponResponse(
        Long id,
        Long couponId,
        String code,
        String name,
        DiscountType discountType,
        BigDecimal discountValue,
        UserCouponStatus status,
        LocalDateTime issuedAt,
        LocalDateTime usedAt
) {
    public static UserCouponResponse from(UserCoupon userCoupon) {
        return new UserCouponResponse(
                userCoupon.getId(), userCoupon.getCoupon().getId(),
                userCoupon.getCoupon().getCode(), userCoupon.getCoupon().getName(),
                userCoupon.getCoupon().getDiscountType(), userCoupon.getCoupon().getDiscountValue(),
                userCoupon.getStatus(), userCoupon.getCreatedAt(), userCoupon.getUsedAt());
    }
}

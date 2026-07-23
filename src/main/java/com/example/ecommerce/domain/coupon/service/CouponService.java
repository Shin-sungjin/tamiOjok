package com.example.ecommerce.domain.coupon.service;

import com.example.ecommerce.domain.coupon.dto.request.CouponCreateRequest;
import com.example.ecommerce.domain.coupon.dto.response.CouponResponse;
import com.example.ecommerce.domain.coupon.entity.Coupon;
import com.example.ecommerce.domain.coupon.repository.CouponRepository;
import com.example.ecommerce.global.exception.CustomException;
import com.example.ecommerce.global.exception.ErrorCode;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CouponService {

    private final CouponRepository couponRepository;

    @Transactional
    public Long createCoupon(CouponCreateRequest request) {
        if (couponRepository.existsByCode(request.code())) {
            throw new CustomException(ErrorCode.DUPLICATE_COUPON_CODE);
        }

        Coupon coupon = Coupon.builder()
                .code(request.code())
                .name(request.name())
                .discountType(request.discountType())
                .discountValue(request.discountValue())
                .minOrderAmount(request.minOrderAmount())
                .maxDiscountAmount(request.maxDiscountAmount())
                .validFrom(request.validFrom())
                .validUntil(request.validUntil())
                .build();

        return couponRepository.save(coupon).getId();
    }

    public Page<CouponResponse> getCoupons(Pageable pageable) {
        return couponRepository.findAll(pageable).map(CouponResponse::from);
    }

    public Page<CouponResponse> getAvailableCoupons(Pageable pageable) {
        LocalDateTime now = LocalDateTime.now();
        return couponRepository.findByValidFromBeforeAndValidUntilAfter(now, now, pageable).map(CouponResponse::from);
    }
}

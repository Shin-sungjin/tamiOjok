package com.example.ecommerce.domain.coupon.controller;

import com.example.ecommerce.domain.coupon.dto.response.CouponResponse;
import com.example.ecommerce.domain.coupon.dto.response.UserCouponResponse;
import com.example.ecommerce.domain.coupon.service.CouponService;
import com.example.ecommerce.domain.coupon.service.UserCouponService;
import com.example.ecommerce.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;
    private final UserCouponService userCouponService;

    @GetMapping
    public ResponseEntity<Page<CouponResponse>> getAvailableCoupons(Pageable pageable) {
        return ResponseEntity.ok(couponService.getAvailableCoupons(pageable));
    }

    @PostMapping("/{couponId}/issue")
    public ResponseEntity<UserCouponResponse> issueCoupon(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                            @PathVariable Long couponId) {
        UserCouponResponse response = userCouponService.issueCoupon(userDetails.getUserId(), couponId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/my")
    public ResponseEntity<Page<UserCouponResponse>> getMyCoupons(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                                   Pageable pageable) {
        return ResponseEntity.ok(userCouponService.getMyCoupons(userDetails.getUserId(), pageable));
    }
}

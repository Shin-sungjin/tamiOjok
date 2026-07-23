package com.example.ecommerce.domain.coupon.controller;

import com.example.ecommerce.domain.coupon.dto.request.CouponCreateRequest;
import com.example.ecommerce.domain.coupon.dto.response.CouponResponse;
import com.example.ecommerce.domain.coupon.service.CouponService;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/coupons")
@RequiredArgsConstructor
public class AdminCouponController {

    private final CouponService couponService;

    @PostMapping
    public ResponseEntity<Void> createCoupon(@Valid @RequestBody CouponCreateRequest request) {
        Long couponId = couponService.createCoupon(request);
        return ResponseEntity.created(URI.create("/api/v1/coupons/" + couponId)).build();
    }

    @GetMapping
    public ResponseEntity<Page<CouponResponse>> getCoupons(Pageable pageable) {
        return ResponseEntity.ok(couponService.getCoupons(pageable));
    }
}

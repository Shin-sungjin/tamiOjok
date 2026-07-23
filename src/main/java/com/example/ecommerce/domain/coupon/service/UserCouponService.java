package com.example.ecommerce.domain.coupon.service;

import com.example.ecommerce.domain.coupon.dto.response.UserCouponResponse;
import com.example.ecommerce.domain.coupon.entity.Coupon;
import com.example.ecommerce.domain.coupon.entity.UserCoupon;
import com.example.ecommerce.domain.coupon.repository.CouponRepository;
import com.example.ecommerce.domain.coupon.repository.UserCouponRepository;
import com.example.ecommerce.domain.user.entity.User;
import com.example.ecommerce.domain.user.repository.UserRepository;
import com.example.ecommerce.global.exception.CustomException;
import com.example.ecommerce.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserCouponService {

    private final UserCouponRepository userCouponRepository;
    private final CouponRepository couponRepository;
    private final UserRepository userRepository;

    @Transactional
    public UserCouponResponse issueCoupon(Long userId, Long couponId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new CustomException(ErrorCode.COUPON_NOT_FOUND));

        if (!coupon.isValidNow()) {
            throw new CustomException(ErrorCode.COUPON_NOT_IN_VALID_PERIOD);
        }
        if (userCouponRepository.existsByUserAndCoupon(user, coupon)) {
            throw new CustomException(ErrorCode.DUPLICATE_COUPON_ISSUE);
        }

        UserCoupon userCoupon = UserCoupon.builder()
                .user(user)
                .coupon(coupon)
                .build();

        return UserCouponResponse.from(userCouponRepository.save(userCoupon));
    }

    public Page<UserCouponResponse> getMyCoupons(Long userId, Pageable pageable) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        return userCouponRepository.findByUser(user, pageable).map(UserCouponResponse::from);
    }
}

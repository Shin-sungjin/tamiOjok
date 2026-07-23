package com.example.ecommerce.domain.coupon.entity;

import com.example.ecommerce.domain.coupon.enums.UserCouponStatus;
import com.example.ecommerce.domain.order.entity.Order;
import com.example.ecommerce.domain.user.entity.User;
import com.example.ecommerce.global.entity.BaseTimeEntity;
import com.example.ecommerce.global.exception.CustomException;
import com.example.ecommerce.global.exception.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_coupons")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserCoupon extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_coupon_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_id", nullable = false)
    private Coupon coupon;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserCouponStatus status;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @Builder
    private UserCoupon(User user, Coupon coupon) {
        this.user = user;
        this.coupon = coupon;
        this.status = UserCouponStatus.AVAILABLE;
    }

    public boolean isOwnedBy(Long userId) {
        return this.user.getId().equals(userId);
    }

    public BigDecimal use(BigDecimal orderAmount) {
        if (this.status != UserCouponStatus.AVAILABLE) {
            throw new CustomException(ErrorCode.COUPON_ALREADY_USED);
        }
        if (!this.coupon.isValidNow()) {
            throw new CustomException(ErrorCode.COUPON_NOT_IN_VALID_PERIOD);
        }

        BigDecimal discount = this.coupon.calculateDiscount(orderAmount);
        this.status = UserCouponStatus.USED;
        this.usedAt = LocalDateTime.now();
        return discount;
    }

    public void assignOrder(Order order) {
        this.order = order;
    }

    public void restore() {
        this.status = UserCouponStatus.AVAILABLE;
        this.usedAt = null;
        this.order = null;
    }
}

package com.example.ecommerce.domain.coupon.repository;

import com.example.ecommerce.domain.coupon.entity.Coupon;
import com.example.ecommerce.domain.coupon.entity.UserCoupon;
import com.example.ecommerce.domain.order.entity.Order;
import com.example.ecommerce.domain.user.entity.User;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserCouponRepository extends JpaRepository<UserCoupon, Long> {

    Page<UserCoupon> findByUser(User user, Pageable pageable);

    boolean existsByUserAndCoupon(User user, Coupon coupon);

    Optional<UserCoupon> findByOrder(Order order);
}

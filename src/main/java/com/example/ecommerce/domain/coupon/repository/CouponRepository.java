package com.example.ecommerce.domain.coupon.repository;

import com.example.ecommerce.domain.coupon.entity.Coupon;
import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CouponRepository extends JpaRepository<Coupon, Long> {

    boolean existsByCode(String code);

    Page<Coupon> findByValidFromBeforeAndValidUntilAfter(LocalDateTime from, LocalDateTime until, Pageable pageable);
}

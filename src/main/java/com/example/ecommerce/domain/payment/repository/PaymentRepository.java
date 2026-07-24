package com.example.ecommerce.domain.payment.repository;

import com.example.ecommerce.domain.order.entity.Order;
import com.example.ecommerce.domain.payment.entity.Payment;
import java.math.BigDecimal;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByOrder(Order order);

    @Query("select coalesce(sum(p.paidAmount), 0) from Payment p where p.status = 'PAID'")
    BigDecimal sumPaidAmount();
}

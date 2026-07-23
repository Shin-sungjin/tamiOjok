package com.example.ecommerce.domain.payment.repository;

import com.example.ecommerce.domain.order.entity.Order;
import com.example.ecommerce.domain.payment.entity.Payment;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByOrder(Order order);
}

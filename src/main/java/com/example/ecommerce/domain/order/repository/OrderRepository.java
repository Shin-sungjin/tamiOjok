package com.example.ecommerce.domain.order.repository;

import com.example.ecommerce.domain.order.entity.Order;
import com.example.ecommerce.domain.order.enums.OrderStatus;
import com.example.ecommerce.domain.user.entity.User;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {

    Page<Order> findByUser(User user, Pageable pageable);

    List<Order> findByStatusAndCreatedAtBefore(OrderStatus status, LocalDateTime threshold);
}

package com.example.ecommerce.domain.order.repository;

import com.example.ecommerce.domain.order.entity.Order;
import com.example.ecommerce.domain.order.enums.OrderStatus;
import com.example.ecommerce.domain.user.entity.User;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface OrderRepository extends JpaRepository<Order, Long> {

    Page<Order> findByUser(User user, Pageable pageable);

    Page<Order> findByStatus(OrderStatus status, Pageable pageable);

    List<Order> findByStatusAndCreatedAtBefore(OrderStatus status, LocalDateTime threshold);

    long countByStatus(OrderStatus status);

    @Query("select count(o) from Order o where o.status = 'PREPARING' "
            + "and not exists (select 1 from Delivery d where d.order = o)")
    long countPendingShipment();
}

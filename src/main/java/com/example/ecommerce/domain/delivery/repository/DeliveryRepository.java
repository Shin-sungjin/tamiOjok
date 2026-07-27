package com.example.ecommerce.domain.delivery.repository;

import com.example.ecommerce.domain.delivery.entity.Delivery;
import com.example.ecommerce.domain.delivery.enums.DeliveryStatus;
import com.example.ecommerce.domain.delivery.enums.ReturnReason;
import com.example.ecommerce.domain.order.entity.Order;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface DeliveryRepository extends JpaRepository<Delivery, Long> {

    Optional<Delivery> findByOrder(Order order);

    List<Delivery> findByOrderIn(List<Order> orders);

    List<Delivery> findByStatus(DeliveryStatus status);

    long countByStatus(DeliveryStatus status);

    List<Delivery> findByReturnReason(ReturnReason returnReason);

    @Query("""
            SELECT d.returnReason AS reason, COUNT(d) AS count
            FROM Delivery d
            WHERE d.returnReason IS NOT NULL
            GROUP BY d.returnReason
            """)
    List<ReturnReasonCount> countGroupedByReturnReason();

    interface ReturnReasonCount {
        ReturnReason getReason();
        Long getCount();
    }
}

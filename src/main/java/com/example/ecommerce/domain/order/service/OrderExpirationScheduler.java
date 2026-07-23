package com.example.ecommerce.domain.order.service;

import com.example.ecommerce.domain.order.entity.Order;
import com.example.ecommerce.domain.order.entity.OrderItem;
import com.example.ecommerce.domain.order.enums.OrderStatus;
import com.example.ecommerce.domain.order.repository.OrderRepository;
import com.example.ecommerce.domain.product.service.StockService;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class OrderExpirationScheduler {

    private static final int PAYMENT_TIMEOUT_MINUTES = 10;

    private final OrderRepository orderRepository;
    private final StockService stockService;

    @Scheduled(fixedRate = 60_000)
    @Transactional
    public void expireUnpaidOrders() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(PAYMENT_TIMEOUT_MINUTES);
        List<Order> expiredOrders = orderRepository.findByStatusAndCreatedAtBefore(
                OrderStatus.PENDING_PAYMENT, threshold);

        for (Order order : expiredOrders) {
            for (OrderItem item : order.getOrderItems()) {
                stockService.releaseReservation(item.getProduct().getId(), item.getQuantity());
            }
            order.expire();
        }
    }
}

package com.example.ecommerce.domain.order.service;

import com.example.ecommerce.domain.delivery.entity.Delivery;
import com.example.ecommerce.domain.delivery.repository.DeliveryRepository;
import com.example.ecommerce.domain.order.dto.request.OrderCreateRequest;
import com.example.ecommerce.domain.order.dto.request.OrderCreateRequest.OrderItemRequest;
import com.example.ecommerce.domain.order.dto.response.OrderResponse;
import com.example.ecommerce.domain.order.entity.Order;
import com.example.ecommerce.domain.order.entity.OrderItem;
import com.example.ecommerce.domain.order.repository.OrderRepository;
import com.example.ecommerce.domain.product.entity.Product;
import com.example.ecommerce.domain.product.repository.ProductRepository;
import com.example.ecommerce.domain.product.service.StockService;
import com.example.ecommerce.domain.user.entity.User;
import com.example.ecommerce.domain.user.repository.UserRepository;
import com.example.ecommerce.global.exception.CustomException;
import com.example.ecommerce.global.exception.ErrorCode;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final StockService stockService;
    private final DeliveryRepository deliveryRepository;

    @Transactional
    public OrderResponse createOrder(Long userId, OrderCreateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        List<OrderItem> orderItems = request.items().stream()
                .map(this::toOrderItem)
                .toList();

        for (OrderItemRequest itemRequest : request.items()) {
            stockService.reserve(itemRequest.productId(), itemRequest.quantity());
        }

        BigDecimal totalAmount = orderItems.stream()
                .map(OrderItem::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Order order = Order.builder()
                .orderNumber(generateOrderNumber())
                .user(user)
                .totalAmount(totalAmount)
                .discountAmount(BigDecimal.ZERO)
                .orderItems(orderItems)
                .build();

        return OrderResponse.from(orderRepository.save(order));
    }

    public OrderResponse getMyOrder(Long userId, Long orderId) {
        Order order = getOrderOrThrow(orderId);
        validateOwnership(order, userId);
        return OrderResponse.from(order);
    }

    public Page<OrderResponse> getMyOrders(Long userId, Pageable pageable) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        return orderRepository.findByUser(user, pageable).map(OrderResponse::from);
    }

    @Transactional
    public void cancelOrder(Long userId, Long orderId) {
        Order order = getOrderOrThrow(orderId);
        validateOwnership(order, userId);

        boolean isShipped = deliveryRepository.findByOrder(order)
                .map(Delivery::isPostShipment)
                .orElse(false);
        if (isShipped) {
            throw new CustomException(ErrorCode.ORDER_ALREADY_SHIPPED);
        }

        cancelAndRelease(order);
    }

    @Transactional
    public void cancelBySystem(Long orderId) {
        cancelAndRelease(getOrderOrThrow(orderId));
    }

    private void cancelAndRelease(Order order) {
        boolean shouldRestoreStock = order.isCancellableWithStockReturn();
        order.cancel();

        for (OrderItem item : order.getOrderItems()) {
            if (shouldRestoreStock) {
                stockService.restoreStock(item.getProduct().getId(), item.getQuantity());
            } else {
                stockService.releaseReservation(item.getProduct().getId(), item.getQuantity());
            }
        }
    }

    public Order getOrderEntityOrThrow(Long orderId) {
        return getOrderOrThrow(orderId);
    }

    @Transactional
    public void completePayment(Long orderId) {
        Order order = getOrderOrThrow(orderId);
        order.completePayment();
        for (OrderItem item : order.getOrderItems()) {
            stockService.confirmDeduction(item.getProduct().getId(), item.getQuantity());
        }
    }

    @Transactional
    public void startPreparing(Long orderId) {
        Order order = getOrderOrThrow(orderId);
        order.startPreparing();
    }

    private OrderItem toOrderItem(OrderItemRequest itemRequest) {
        Product product = productRepository.findById(itemRequest.productId())
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));
        return OrderItem.builder()
                .product(product)
                .orderPrice(product.getPrice())
                .quantity(itemRequest.quantity())
                .build();
    }

    private Order getOrderOrThrow(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));
    }

    private void validateOwnership(Order order, Long userId) {
        if (!order.isOwnedBy(userId)) {
            throw new CustomException(ErrorCode.ORDER_ACCESS_DENIED);
        }
    }

    private String generateOrderNumber() {
        return "ORD" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }
}

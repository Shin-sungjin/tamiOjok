package com.example.ecommerce.domain.order.service;

import com.example.ecommerce.domain.cart.entity.Cart;
import com.example.ecommerce.domain.cart.entity.CartItem;
import com.example.ecommerce.domain.cart.repository.CartRepository;
import com.example.ecommerce.domain.coupon.entity.UserCoupon;
import com.example.ecommerce.domain.coupon.repository.UserCouponRepository;
import com.example.ecommerce.domain.delivery.entity.Delivery;
import com.example.ecommerce.domain.delivery.repository.DeliveryRepository;
import com.example.ecommerce.domain.order.dto.request.OrderCreateRequest;
import com.example.ecommerce.domain.order.dto.request.OrderCreateRequest.OrderItemRequest;
import com.example.ecommerce.domain.order.dto.response.AdminOrderResponse;
import com.example.ecommerce.domain.order.dto.response.OrderResponse;
import com.example.ecommerce.domain.order.entity.Order;
import com.example.ecommerce.domain.order.entity.OrderItem;
import com.example.ecommerce.domain.order.enums.OrderStatus;
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
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
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
    private final CartRepository cartRepository;
    private final UserCouponRepository userCouponRepository;

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

        return OrderResponse.from(createAndSaveOrder(user, orderItems, userId, request.userCouponId()));
    }

    @Transactional
    public OrderResponse createOrderFromCart(Long userId, Long userCouponId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        Cart cart = cartRepository.findByUser_Id(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.EMPTY_CART));
        if (cart.getCartItems().isEmpty()) {
            throw new CustomException(ErrorCode.EMPTY_CART);
        }

        List<OrderItem> orderItems = cart.getCartItems().stream()
                .map(this::toOrderItem)
                .toList();

        for (CartItem cartItem : cart.getCartItems()) {
            stockService.reserve(cartItem.getProduct().getId(), cartItem.getQuantity());
        }

        OrderResponse response = OrderResponse.from(createAndSaveOrder(user, orderItems, userId, userCouponId));
        cart.clear();
        return response;
    }

    private Order createAndSaveOrder(User user, List<OrderItem> orderItems, Long userId, Long userCouponId) {
        BigDecimal totalAmount = orderItems.stream()
                .map(OrderItem::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        UserCoupon userCoupon = null;
        BigDecimal discountAmount = BigDecimal.ZERO;
        if (userCouponId != null) {
            userCoupon = getUserCouponOrThrow(userCouponId);
            validateCouponOwnership(userCoupon, userId);
            discountAmount = userCoupon.use(totalAmount);
        }

        Order order = Order.builder()
                .orderNumber(generateOrderNumber())
                .user(user)
                .totalAmount(totalAmount)
                .discountAmount(discountAmount)
                .orderItems(orderItems)
                .build();
        order = orderRepository.save(order);

        if (userCoupon != null) {
            userCoupon.assignOrder(order);
        }

        return order;
    }

    public OrderResponse getMyOrder(Long userId, Long orderId) {
        Order order = getOrderOrThrow(orderId);
        validateOwnership(order, userId);
        return OrderResponse.of(order, deliveryRepository.findByOrder(order).orElse(null));
    }

    public Page<OrderResponse> getMyOrders(Long userId, Pageable pageable) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        Page<Order> orders = orderRepository.findByUser(user, pageable);
        Map<Long, Delivery> deliveriesByOrderId = deliveriesByOrderId(orders.getContent());
        return orders.map(order -> OrderResponse.of(order, deliveriesByOrderId.get(order.getId())));
    }

    @Transactional
    public void cancelOrder(Long userId, Long orderId) {
        Order order = getOrderOrThrow(orderId);
        validateOwnership(order, userId);

        // 배송 레코드는 등록되는 즉시 IN_TRANSIT으로 시작해 SHIPPED 단계가 없고
        // (Delivery 생성자 참고), RETURN_REQUESTED도 배송이 시작된 이후에만 될 수
        // 있으므로, 배송 레코드가 하나라도 존재하면 이미 배송이 시작된 것으로 본다.
        // (Delivery.isPostShipment()는 SHIPPED/IN_TRANSIT/DELIVERED만 포함하고
        // RETURN_REQUESTED는 빠져 있어서, 반품 요청 후에도 취소가 가능해지는
        // 버그가 있었음 — 배송 레코드 존재 여부로 판단하도록 수정)
        boolean hasDelivery = deliveryRepository.findByOrder(order).isPresent();
        if (hasDelivery) {
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

        userCouponRepository.findByOrder(order).ifPresent(UserCoupon::restore);
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

    public Page<AdminOrderResponse> getOrdersForAdmin(OrderStatus status, Pageable pageable) {
        Page<Order> orders = status != null
                ? orderRepository.findByStatus(status, pageable)
                : orderRepository.findAll(pageable);
        Map<Long, Delivery> deliveriesByOrderId = deliveriesByOrderId(orders.getContent());
        return orders.map(order -> AdminOrderResponse.of(order, deliveriesByOrderId.get(order.getId())));
    }

    public AdminOrderResponse getOrderForAdmin(Long orderId) {
        Order order = getOrderOrThrow(orderId);
        return AdminOrderResponse.of(order, deliveryRepository.findByOrder(order).orElse(null));
    }

    private Map<Long, Delivery> deliveriesByOrderId(List<Order> orders) {
        if (orders.isEmpty()) {
            return Map.of();
        }
        return deliveryRepository.findByOrderIn(orders).stream()
                .collect(Collectors.toMap(delivery -> delivery.getOrder().getId(), delivery -> delivery));
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

    private OrderItem toOrderItem(CartItem cartItem) {
        Product product = cartItem.getProduct();
        return OrderItem.builder()
                .product(product)
                .orderPrice(product.getPrice())
                .quantity(cartItem.getQuantity())
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

    private UserCoupon getUserCouponOrThrow(Long userCouponId) {
        return userCouponRepository.findById(userCouponId)
                .orElseThrow(() -> new CustomException(ErrorCode.COUPON_NOT_FOUND));
    }

    private void validateCouponOwnership(UserCoupon userCoupon, Long userId) {
        if (!userCoupon.isOwnedBy(userId)) {
            throw new CustomException(ErrorCode.COUPON_ACCESS_DENIED);
        }
    }

    private String generateOrderNumber() {
        return "ORD" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }
}

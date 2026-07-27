package com.example.ecommerce.domain.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.ecommerce.domain.cart.entity.Cart;
import com.example.ecommerce.domain.cart.repository.CartRepository;
import com.example.ecommerce.domain.coupon.entity.UserCoupon;
import com.example.ecommerce.domain.coupon.repository.UserCouponRepository;
import com.example.ecommerce.domain.delivery.entity.Delivery;
import com.example.ecommerce.domain.delivery.repository.DeliveryRepository;
import com.example.ecommerce.domain.order.dto.request.OrderCreateRequest;
import com.example.ecommerce.domain.order.dto.response.OrderResponse;
import com.example.ecommerce.domain.order.entity.Order;
import com.example.ecommerce.domain.order.entity.OrderItem;
import com.example.ecommerce.domain.order.enums.OrderStatus;
import com.example.ecommerce.domain.order.repository.OrderRepository;
import com.example.ecommerce.domain.product.entity.Product;
import com.example.ecommerce.domain.product.enums.ProductStatus;
import com.example.ecommerce.domain.product.repository.ProductRepository;
import com.example.ecommerce.domain.product.service.StockService;
import com.example.ecommerce.domain.user.entity.User;
import com.example.ecommerce.domain.user.enums.Provider;
import com.example.ecommerce.domain.user.enums.Role;
import com.example.ecommerce.domain.user.enums.UserStatus;
import com.example.ecommerce.domain.user.repository.UserRepository;
import com.example.ecommerce.global.exception.CustomException;
import com.example.ecommerce.global.exception.ErrorCode;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private StockService stockService;
    @Mock
    private DeliveryRepository deliveryRepository;
    @Mock
    private CartRepository cartRepository;
    @Mock
    private UserCouponRepository userCouponRepository;

    @InjectMocks
    private OrderService orderService;

    private Order preparingOrder(Long productId) {
        User user = User.builder()
                .email("buyer@test.com")
                .name("구매자")
                .provider(Provider.LOCAL)
                .role(Role.USER)
                .status(UserStatus.ACTIVE)
                .build();
        ReflectionTestUtils.setField(user, "id", 1L);

        Product product = Product.builder()
                .name("탐미오족")
                .price(BigDecimal.valueOf(89000))
                .description("")
                .status(ProductStatus.ON_SALE)
                .build();
        ReflectionTestUtils.setField(product, "id", productId);

        OrderItem item = OrderItem.builder()
                .product(product)
                .orderPrice(BigDecimal.valueOf(89000))
                .quantity(1)
                .build();

        Order order = Order.builder()
                .orderNumber("ORD-TEST-1")
                .user(user)
                .totalAmount(BigDecimal.valueOf(89000))
                .discountAmount(BigDecimal.ZERO)
                .orderItems(new ArrayList<>(List.of(item)))
                .build();
        ReflectionTestUtils.setField(order, "id", 1L);
        order.completePayment();
        order.startPreparing();
        return order;
    }

    private User buildUser(Long id) {
        User user = User.builder()
                .email("buyer@test.com")
                .name("구매자")
                .provider(Provider.LOCAL)
                .role(Role.USER)
                .status(UserStatus.ACTIVE)
                .build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private Product buildProduct(Long id, BigDecimal price) {
        Product product = Product.builder()
                .name("탐미오족")
                .price(price)
                .description("")
                .status(ProductStatus.ON_SALE)
                .build();
        ReflectionTestUtils.setField(product, "id", id);
        return product;
    }

    // 배송이 시작되면(반품요청 포함) 배송 레코드가 항상 존재하므로, 배송
    // 레코드 존재 여부만으로 취소 가능 여부를 판단해야 함. Delivery.isPostShipment()
    // 기준으로만 판단하던 예전 로직은 RETURN_REQUESTED가 그 목록에 없어서
    // 반품 요청 후에도 취소가 통과되는 버그가 있었음 — 이 테스트는 그 버그가
    // 재발하지 않는지 확인한다 (Delivery는 실제 엔티티 대신 mock으로 만들어
    // 상태와 무관하게 "레코드가 존재한다"는 사실만으로 검증).
    @Test
    void cancelOrder_throwsWhenDeliveryRecordExists() {
        Order order = preparingOrder(10L);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(deliveryRepository.findByOrder(order)).thenReturn(Optional.of(mock(Delivery.class)));

        assertThatThrownBy(() -> orderService.cancelOrder(1L, 1L))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ORDER_ALREADY_SHIPPED);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PREPARING);
        verify(stockService, never()).restoreStock(any(), anyInt());
        verify(stockService, never()).releaseReservation(any(), anyInt());
    }

    @Test
    void cancelOrder_succeedsWhenNoDeliveryRegisteredYet() {
        Order order = preparingOrder(10L);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(deliveryRepository.findByOrder(order)).thenReturn(Optional.empty());
        when(userCouponRepository.findByOrder(order)).thenReturn(Optional.empty());

        orderService.cancelOrder(1L, 1L);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(stockService).restoreStock(10L, 1);
    }

    @Test
    void createOrder_reservesStockAndCreatesOrder_withoutCoupon() {
        User user = buildUser(1L);
        Product product = buildProduct(10L, BigDecimal.valueOf(10000));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        OrderCreateRequest request = new OrderCreateRequest(
                List.of(new OrderCreateRequest.OrderItemRequest(10L, 2)), null);

        OrderResponse response = orderService.createOrder(1L, request);

        verify(stockService).reserve(10L, 2);
        assertThat(response.totalAmount()).isEqualByComparingTo(BigDecimal.valueOf(20000));
        assertThat(response.discountAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.paymentAmount()).isEqualByComparingTo(BigDecimal.valueOf(20000));
        assertThat(response.items()).hasSize(1);
    }

    @Test
    void createOrder_appliesCouponDiscount_whenUserCouponProvided() {
        User user = buildUser(1L);
        Product product = buildProduct(10L, BigDecimal.valueOf(10000));
        UserCoupon userCoupon = mock(UserCoupon.class);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(userCouponRepository.findById(5L)).thenReturn(Optional.of(userCoupon));
        when(userCoupon.isOwnedBy(1L)).thenReturn(true);
        when(userCoupon.use(BigDecimal.valueOf(10000))).thenReturn(BigDecimal.valueOf(3000));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        OrderCreateRequest request = new OrderCreateRequest(
                List.of(new OrderCreateRequest.OrderItemRequest(10L, 1)), 5L);

        OrderResponse response = orderService.createOrder(1L, request);

        assertThat(response.discountAmount()).isEqualByComparingTo(BigDecimal.valueOf(3000));
        assertThat(response.paymentAmount()).isEqualByComparingTo(BigDecimal.valueOf(7000));
        verify(userCoupon).assignOrder(any(Order.class));
    }

    @Test
    void createOrder_throwsCouponAccessDenied_whenCouponNotOwnedByUser() {
        User user = buildUser(1L);
        Product product = buildProduct(10L, BigDecimal.valueOf(10000));
        UserCoupon userCoupon = mock(UserCoupon.class);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(userCouponRepository.findById(5L)).thenReturn(Optional.of(userCoupon));
        when(userCoupon.isOwnedBy(1L)).thenReturn(false);

        OrderCreateRequest request = new OrderCreateRequest(
                List.of(new OrderCreateRequest.OrderItemRequest(10L, 1)), 5L);

        assertThatThrownBy(() -> orderService.createOrder(1L, request))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getErrorCode())
                .isEqualTo(ErrorCode.COUPON_ACCESS_DENIED);

        verify(orderRepository, never()).save(any());
    }

    @Test
    void createOrderFromCart_throwsEmptyCart_whenCartHasNoItems() {
        User user = buildUser(1L);
        Cart cart = Cart.builder().user(user).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(cartRepository.findByUser_Id(1L)).thenReturn(Optional.of(cart));

        assertThatThrownBy(() -> orderService.createOrderFromCart(1L, null))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getErrorCode())
                .isEqualTo(ErrorCode.EMPTY_CART);
    }

    @Test
    void createOrderFromCart_reservesStockAndClearsCart_onSuccess() {
        User user = buildUser(1L);
        Product product = buildProduct(10L, BigDecimal.valueOf(15000));
        Cart cart = Cart.builder().user(user).build();
        cart.addItem(product, 3);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(cartRepository.findByUser_Id(1L)).thenReturn(Optional.of(cart));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        OrderResponse response = orderService.createOrderFromCart(1L, null);

        verify(stockService).reserve(10L, 3);
        assertThat(response.totalAmount()).isEqualByComparingTo(BigDecimal.valueOf(45000));
        assertThat(cart.getCartItems()).isEmpty();
    }
}

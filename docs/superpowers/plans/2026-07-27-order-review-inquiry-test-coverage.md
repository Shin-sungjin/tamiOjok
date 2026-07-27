# 주문생성/리뷰/문의 테스트 커버리지 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** OrderService.createOrder/createOrderFromCart, ReviewService, InquiryService의 서비스 계층 테스트를 채우고, 이 프로젝트 최초의 컨트롤러(MockMvc) 테스트를 도입해 세 도메인의 API 라우팅/인증/에러코드 매핑까지 검증한다.

**Architecture:** 서비스 계층은 기존 `OrderServiceTest`/`CartServiceTest`/`DeliveryTest`와 동일한 Mockito 단위 테스트 패턴을 재사용. 컨트롤러 계층은 `@SpringBootTest(webEnvironment = MOCK) + @AutoConfigureMockMvc` + `@MockitoBean`으로 전체 스프링 컨텍스트(실제 `SecurityConfig`/JWT 필터 포함)를 그대로 띄우고 대상 서비스만 목으로 교체. `@AuthenticationPrincipal CustomUserDetails`를 흉내내기 위한 공유 테스트 헬퍼(`TestAuthentication`)를 도입한다.

**Tech Stack:** Spring Boot 4.1.0 / Spring Framework 7.0.8 / Spring Security 7.1.0, JUnit 5, Mockito, AssertJ, H2(test), Jackson.

## Global Constraints

- 서비스 테스트: `@ExtendWith(MockitoExtension.class)` + `@Mock`/`@InjectMocks`, 엔티티 ID는 `org.springframework.test.util.ReflectionTestUtils.setField(entity, "id", value)`로 세팅 (기존 `OrderServiceTest.preparingOrder` 패턴과 동일).
- 컨트롤러 테스트: `@WebMvcTest` 사용 금지 (이 프로젝트의 `SecurityConfig`가 여러 커스텀 빈에 의존해 슬라이스 스캔이 불안정함) — 반드시 `@SpringBootTest` + `@AutoConfigureMockMvc` + `@MockitoBean`(대상 서비스만 교체) 사용.
- `@AutoConfigureMockMvc`의 정확한 import 경로는 `org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc` (Spring Boot 4.1의 `spring-boot-webmvc-test` 모듈로 분리됨 — 구버전 문서에 흔한 `org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc`가 아님, jar 내부 확인 완료).
- `@MockitoBean`은 `org.springframework.test.context.bean.override.mockito.MockitoBean` (구버전 `@MockBean` 아님).
- CSRF는 `SecurityConfig`에서 앱 전역 비활성화되어 있으므로 MockMvc 요청에 `.with(csrf())` 불필요.
- 컨트롤러 인증 주입은 `CustomUserDetails`를 감싼 `UsernamePasswordAuthenticationToken`을 `SecurityMockMvcRequestPostProcessors.authentication(...)`으로 주입 (`@WithMockUser`는 principal 타입이 달라 사용 불가).
- 모든 새 테스트 파일 상단에 패키지 선언은 위치한 디렉터리와 일치해야 함 (예: `src/test/java/com/example/ecommerce/domain/order/service/`는 `package com.example.ecommerce.domain.order.service;`).
- 각 태스크 종료 시 `./gradlew test`(대상 클래스만이 아니라 전체 스위트) 통과를 확인하고 커밋한다.
- 전체 태스크(1~6)를 마친 뒤에만 push + PR 생성 + 머지 (Task 7). 도메인별로 PR을 쪼개지 않는다.
- 브랜치명: `test/order-review-inquiry-coverage` (Task 1에서 생성).

---

### Task 1: 주문생성 서비스 테스트 (OrderServiceTest 확장)

**Files:**
- Modify: `src/test/java/com/example/ecommerce/domain/order/service/OrderServiceTest.java`

**Interfaces:**
- Consumes: `OrderService.createOrder(Long userId, OrderCreateRequest request)`, `OrderService.createOrderFromCart(Long userId, Long userCouponId)` (이미 구현되어 있음, `src/main/java/com/example/ecommerce/domain/order/service/OrderService.java` 참고)
- Produces: 없음 (다른 태스크가 이 파일에 의존하지 않음)

- [ ] **Step 1: 새 import 추가**

`src/test/java/com/example/ecommerce/domain/order/service/OrderServiceTest.java` 상단 import 블록(현재 12~40번째 줄)에 아래 3개를 추가한다 (기존 import는 그대로 둠):

```java
import com.example.ecommerce.domain.cart.entity.Cart;
import com.example.ecommerce.domain.coupon.entity.UserCoupon;
import com.example.ecommerce.domain.order.dto.request.OrderCreateRequest;
```

(`any`, `never`, `mock`, `verify`, `List`, `Optional` 등은 이미 import되어 있으므로 그대로 사용)

- [ ] **Step 2: fixture 헬퍼 2개 추가**

기존 `preparingOrder(Long productId)` 메서드 바로 아래에 추가:

```java
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
```

- [ ] **Step 3: createOrder 테스트 3개 작성 (실패 확인 전 우선 작성)**

클래스 마지막 `}` 바로 앞(기존 `cancelOrder_succeedsWhenNoDeliveryRegisteredYet` 다음)에 추가:

```java
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
```

- [ ] **Step 4: createOrderFromCart 테스트 2개 작성**

바로 이어서 추가:

```java
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
```

- [ ] **Step 5: 브랜치 생성 + 전체 테스트 실행**

```bash
git checkout -b test/order-review-inquiry-coverage
./gradlew test
```

Expected: `BUILD SUCCESSFUL`, 새로 추가된 5개 테스트 포함 전체 통과.

- [ ] **Step 6: 커밋**

```bash
git add src/test/java/com/example/ecommerce/domain/order/service/OrderServiceTest.java
git commit -m "test: OrderService.createOrder/createOrderFromCart 테스트 추가"
```

---

### Task 2: 공유 인증 테스트 헬퍼 + 주문생성 컨트롤러 테스트

**Files:**
- Create: `src/test/java/com/example/ecommerce/support/TestAuthentication.java`
- Create: `src/test/java/com/example/ecommerce/domain/order/controller/OrderControllerTest.java`

**Interfaces:**
- Consumes: `OrderController`(`src/main/java/com/example/ecommerce/domain/order/controller/OrderController.java`)의 `POST /api/v1/orders`, `OrderService.createOrder(Long, OrderCreateRequest)`, `CustomUserDetails(User user)` 생성자
- Produces: `TestAuthentication.asUser(User user): RequestPostProcessor` — Task 4, Task 6이 그대로 재사용

- [ ] **Step 1: 공유 인증 헬퍼 작성**

`src/test/java/com/example/ecommerce/support/TestAuthentication.java` 신규 생성:

```java
package com.example.ecommerce.support;

import com.example.ecommerce.domain.user.entity.User;
import com.example.ecommerce.global.security.CustomUserDetails;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

public final class TestAuthentication {

    private TestAuthentication() {
    }

    public static RequestPostProcessor asUser(User user) {
        CustomUserDetails principal = new CustomUserDetails(user);
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        return SecurityMockMvcRequestPostProcessors.authentication(authentication);
    }
}
```

- [ ] **Step 2: OrderControllerTest 작성**

`src/test/java/com/example/ecommerce/domain/order/controller/OrderControllerTest.java` 신규 생성:

```java
package com.example.ecommerce.domain.order.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.ecommerce.domain.order.dto.request.OrderCreateRequest;
import com.example.ecommerce.domain.order.dto.response.OrderResponse;
import com.example.ecommerce.domain.order.entity.Order;
import com.example.ecommerce.domain.order.entity.OrderItem;
import com.example.ecommerce.domain.order.service.OrderService;
import com.example.ecommerce.domain.product.entity.Product;
import com.example.ecommerce.domain.product.enums.ProductStatus;
import com.example.ecommerce.domain.user.entity.User;
import com.example.ecommerce.domain.user.enums.Provider;
import com.example.ecommerce.domain.user.enums.Role;
import com.example.ecommerce.domain.user.enums.UserStatus;
import com.example.ecommerce.global.exception.CustomException;
import com.example.ecommerce.global.exception.ErrorCode;
import com.example.ecommerce.support.TestAuthentication;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OrderService orderService;

    private User testUser(Long id) {
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

    private Order sampleOrder(Long id) {
        Product product = Product.builder()
                .name("탐미오족")
                .price(BigDecimal.valueOf(10000))
                .description("")
                .status(ProductStatus.ON_SALE)
                .build();
        ReflectionTestUtils.setField(product, "id", 10L);

        OrderItem item = OrderItem.builder()
                .product(product)
                .orderPrice(BigDecimal.valueOf(10000))
                .quantity(2)
                .build();

        Order order = Order.builder()
                .orderNumber("ORD-TEST-1")
                .user(testUser(1L))
                .totalAmount(BigDecimal.valueOf(20000))
                .discountAmount(BigDecimal.ZERO)
                .orderItems(new ArrayList<>(List.of(item)))
                .build();
        ReflectionTestUtils.setField(order, "id", id);
        return order;
    }

    @Test
    void createOrder_returns201_whenAuthenticated() throws Exception {
        when(orderService.createOrder(eq(1L), any())).thenReturn(OrderResponse.from(sampleOrder(1L)));

        OrderCreateRequest request = new OrderCreateRequest(
                List.of(new OrderCreateRequest.OrderItemRequest(10L, 2)), null);

        mockMvc.perform(post("/api/v1/orders")
                        .with(TestAuthentication.asUser(testUser(1L)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalAmount").value(20000))
                .andExpect(jsonPath("$.items.length()").value(1));
    }

    @Test
    void createOrder_returns401_whenNotAuthenticated() throws Exception {
        OrderCreateRequest request = new OrderCreateRequest(
                List.of(new OrderCreateRequest.OrderItemRequest(10L, 2)), null);

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createOrder_returns403_whenCouponNotOwnedByUser() throws Exception {
        when(orderService.createOrder(eq(1L), any()))
                .thenThrow(new CustomException(ErrorCode.COUPON_ACCESS_DENIED));

        OrderCreateRequest request = new OrderCreateRequest(
                List.of(new OrderCreateRequest.OrderItemRequest(10L, 2)), 99L);

        mockMvc.perform(post("/api/v1/orders")
                        .with(TestAuthentication.asUser(testUser(1L)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("COUPON_ACCESS_DENIED"));
    }
}
```

- [ ] **Step 3: 전체 테스트 실행**

```bash
./gradlew test
```

Expected: `BUILD SUCCESSFUL`. (이 프로젝트 최초의 `@SpringBootTest`+`MockMvc` 컨텍스트 로딩이라 첫 실행이 서비스 단위 테스트보다 느릴 수 있음 — 정상)

만약 `AutoConfigureMockMvc` 관련 `ClassNotFoundException`/`NoSuchMethodError`가 나면 import 경로가
`org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc`인지 다시 확인할 것 (Spring Boot 4.1에서 이동된 패키지).

- [ ] **Step 4: 커밋**

```bash
git add src/test/java/com/example/ecommerce/support/TestAuthentication.java \
        src/test/java/com/example/ecommerce/domain/order/controller/OrderControllerTest.java
git commit -m "test: 공유 인증 테스트 헬퍼 + 주문생성 컨트롤러 테스트 추가"
```

---

### Task 3: 리뷰 서비스 테스트 (ReviewServiceTest 신규)

**Files:**
- Create: `src/test/java/com/example/ecommerce/domain/review/service/ReviewServiceTest.java`

**Interfaces:**
- Consumes: `ReviewService`(`src/main/java/com/example/ecommerce/domain/review/service/ReviewService.java`)의 `createReview`/`updateReview`/`deleteReview`
- Produces: 없음

- [ ] **Step 1: 파일 작성**

```java
package com.example.ecommerce.domain.review.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.ecommerce.domain.delivery.entity.Delivery;
import com.example.ecommerce.domain.delivery.repository.DeliveryRepository;
import com.example.ecommerce.domain.order.entity.Order;
import com.example.ecommerce.domain.order.entity.OrderItem;
import com.example.ecommerce.domain.order.repository.OrderRepository;
import com.example.ecommerce.domain.product.entity.Product;
import com.example.ecommerce.domain.product.enums.ProductStatus;
import com.example.ecommerce.domain.product.repository.ProductRepository;
import com.example.ecommerce.domain.review.dto.request.ReviewCreateRequest;
import com.example.ecommerce.domain.review.dto.request.ReviewUpdateRequest;
import com.example.ecommerce.domain.review.entity.Review;
import com.example.ecommerce.domain.review.repository.ReviewRepository;
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
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private DeliveryRepository deliveryRepository;

    @InjectMocks
    private ReviewService reviewService;

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

    private Product buildProduct(Long id) {
        Product product = Product.builder()
                .name("탐미오족")
                .price(BigDecimal.valueOf(89000))
                .description("")
                .status(ProductStatus.ON_SALE)
                .build();
        ReflectionTestUtils.setField(product, "id", id);
        return product;
    }

    private Order buildOrder(Long id, User user, Product product) {
        OrderItem item = OrderItem.builder()
                .product(product)
                .orderPrice(product.getPrice())
                .quantity(1)
                .build();
        Order order = Order.builder()
                .orderNumber("ORD-TEST-1")
                .user(user)
                .totalAmount(product.getPrice())
                .discountAmount(BigDecimal.ZERO)
                .orderItems(new ArrayList<>(List.of(item)))
                .build();
        ReflectionTestUtils.setField(order, "id", id);
        return order;
    }

    private Delivery deliveredDelivery(Order order) {
        Delivery delivery = Delivery.builder()
                .order(order)
                .courierCode("CJGLS")
                .trackingNumber("1234567890")
                .build();
        delivery.markDelivered();
        return delivery;
    }

    @Test
    void createReview_succeeds_whenProductOrderedAndDelivered() {
        User user = buildUser(1L);
        Product product = buildProduct(10L);
        Order order = buildOrder(1L, user, product);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(deliveryRepository.findByOrder(order)).thenReturn(Optional.of(deliveredDelivery(order)));
        when(reviewRepository.existsByOrderAndProduct(order, product)).thenReturn(false);
        when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> inv.getArgument(0));

        ReviewCreateRequest request = new ReviewCreateRequest(1L, 10L, 5, "맛있어요");

        var response = reviewService.createReview(1L, request);

        assertThat(response.rating()).isEqualTo(5);
        assertThat(response.content()).isEqualTo("맛있어요");
    }

    @Test
    void createReview_throwsReviewNotAllowed_whenProductNotInOrder() {
        User user = buildUser(1L);
        Product orderedProduct = buildProduct(10L);
        Product otherProduct = buildProduct(20L);
        Order order = buildOrder(1L, user, orderedProduct);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(productRepository.findById(20L)).thenReturn(Optional.of(otherProduct));
        when(deliveryRepository.findByOrder(order)).thenReturn(Optional.of(deliveredDelivery(order)));

        ReviewCreateRequest request = new ReviewCreateRequest(1L, 20L, 5, "맛있어요");

        assertThatThrownBy(() -> reviewService.createReview(1L, request))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getErrorCode())
                .isEqualTo(ErrorCode.REVIEW_NOT_ALLOWED);
    }

    @Test
    void createReview_throwsReviewNotAllowed_whenNotDelivered() {
        User user = buildUser(1L);
        Product product = buildProduct(10L);
        Order order = buildOrder(1L, user, product);
        Delivery inTransitDelivery = Delivery.builder()
                .order(order).courierCode("CJGLS").trackingNumber("1234567890").build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(deliveryRepository.findByOrder(order)).thenReturn(Optional.of(inTransitDelivery));

        ReviewCreateRequest request = new ReviewCreateRequest(1L, 10L, 5, "맛있어요");

        assertThatThrownBy(() -> reviewService.createReview(1L, request))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getErrorCode())
                .isEqualTo(ErrorCode.REVIEW_NOT_ALLOWED);
    }

    @Test
    void createReview_throwsDuplicateReview_whenAlreadyReviewed() {
        User user = buildUser(1L);
        Product product = buildProduct(10L);
        Order order = buildOrder(1L, user, product);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(deliveryRepository.findByOrder(order)).thenReturn(Optional.of(deliveredDelivery(order)));
        when(reviewRepository.existsByOrderAndProduct(order, product)).thenReturn(true);

        ReviewCreateRequest request = new ReviewCreateRequest(1L, 10L, 5, "맛있어요");

        assertThatThrownBy(() -> reviewService.createReview(1L, request))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getErrorCode())
                .isEqualTo(ErrorCode.DUPLICATE_REVIEW);
    }

    @Test
    void updateReview_throwsAccessDenied_whenNotOwner() {
        User owner = buildUser(1L);
        Product product = buildProduct(10L);
        Order order = buildOrder(1L, owner, product);
        Review review = Review.builder().user(owner).order(order).product(product).rating(4).content("좋아요").build();
        ReflectionTestUtils.setField(review, "id", 1L);
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));

        ReviewUpdateRequest request = new ReviewUpdateRequest(1, "별로예요");

        assertThatThrownBy(() -> reviewService.updateReview(2L, 1L, request))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getErrorCode())
                .isEqualTo(ErrorCode.REVIEW_ACCESS_DENIED);
    }

    @Test
    void deleteReview_throwsAccessDenied_whenNotOwner() {
        User owner = buildUser(1L);
        Product product = buildProduct(10L);
        Order order = buildOrder(1L, owner, product);
        Review review = Review.builder().user(owner).order(order).product(product).rating(4).content("좋아요").build();
        ReflectionTestUtils.setField(review, "id", 1L);
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));

        assertThatThrownBy(() -> reviewService.deleteReview(2L, 1L))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getErrorCode())
                .isEqualTo(ErrorCode.REVIEW_ACCESS_DENIED);

        verify(reviewRepository, never()).delete(any());
    }
}
```

- [ ] **Step 2: 전체 테스트 실행**

```bash
./gradlew test
```

Expected: `BUILD SUCCESSFUL`, 신규 6개 테스트 포함 통과.

- [ ] **Step 3: 커밋**

```bash
git add src/test/java/com/example/ecommerce/domain/review/service/ReviewServiceTest.java
git commit -m "test: ReviewService 테스트 추가"
```

---

### Task 4: 리뷰 컨트롤러 테스트 (ReviewControllerTest 신규)

**Files:**
- Create: `src/test/java/com/example/ecommerce/domain/review/controller/ReviewControllerTest.java`

**Interfaces:**
- Consumes: `TestAuthentication.asUser(User)`(Task 2에서 생성), `ReviewController`의 `POST/PUT/DELETE /api/v1/reviews`
- Produces: 없음

- [ ] **Step 1: 파일 작성**

```java
package com.example.ecommerce.domain.review.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.ecommerce.domain.review.dto.request.ReviewCreateRequest;
import com.example.ecommerce.domain.review.dto.request.ReviewUpdateRequest;
import com.example.ecommerce.domain.review.dto.response.ReviewResponse;
import com.example.ecommerce.domain.review.service.ReviewService;
import com.example.ecommerce.domain.user.entity.User;
import com.example.ecommerce.domain.user.enums.Provider;
import com.example.ecommerce.domain.user.enums.Role;
import com.example.ecommerce.domain.user.enums.UserStatus;
import com.example.ecommerce.global.exception.CustomException;
import com.example.ecommerce.global.exception.ErrorCode;
import com.example.ecommerce.support.TestAuthentication;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ReviewService reviewService;

    private User testUser(Long id) {
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

    @Test
    void createReview_returns201_whenAuthenticated() throws Exception {
        ReviewResponse response = new ReviewResponse(1L, 1L, 10L, "탐미오족", 5, "맛있어요", LocalDateTime.now());
        when(reviewService.createReview(eq(1L), any())).thenReturn(response);

        ReviewCreateRequest request = new ReviewCreateRequest(1L, 10L, 5, "맛있어요");

        mockMvc.perform(post("/api/v1/reviews")
                        .with(TestAuthentication.asUser(testUser(1L)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rating").value(5));
    }

    @Test
    void updateReview_returns403_whenNotOwner() throws Exception {
        when(reviewService.updateReview(eq(2L), eq(1L), any()))
                .thenThrow(new CustomException(ErrorCode.REVIEW_ACCESS_DENIED));

        ReviewUpdateRequest request = new ReviewUpdateRequest(1, "별로예요");

        mockMvc.perform(put("/api/v1/reviews/1")
                        .with(TestAuthentication.asUser(testUser(2L)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("REVIEW_ACCESS_DENIED"));
    }

    @Test
    void deleteReview_returns403_whenNotOwner() throws Exception {
        doThrow(new CustomException(ErrorCode.REVIEW_ACCESS_DENIED))
                .when(reviewService).deleteReview(2L, 1L);

        mockMvc.perform(delete("/api/v1/reviews/1")
                        .with(TestAuthentication.asUser(testUser(2L))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("REVIEW_ACCESS_DENIED"));
    }
}
```

- [ ] **Step 2: 전체 테스트 실행**

```bash
./gradlew test
```

Expected: `BUILD SUCCESSFUL`, 신규 3개 테스트 포함 통과.

- [ ] **Step 3: 커밋**

```bash
git add src/test/java/com/example/ecommerce/domain/review/controller/ReviewControllerTest.java
git commit -m "test: 리뷰 컨트롤러 테스트 추가"
```

---

### Task 5: 문의 서비스 테스트 (InquiryServiceTest 신규)

**Files:**
- Create: `src/test/java/com/example/ecommerce/domain/inquiry/service/InquiryServiceTest.java`

**Interfaces:**
- Consumes: `InquiryService`(`src/main/java/com/example/ecommerce/domain/inquiry/service/InquiryService.java`)의 `createInquiry`/`getMyInquiry`, `OrderService.getOrderEntityOrThrow(Long)`
- Produces: 없음

- [ ] **Step 1: 파일 작성**

```java
package com.example.ecommerce.domain.inquiry.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.example.ecommerce.domain.inquiry.dto.request.InquiryCreateRequest;
import com.example.ecommerce.domain.inquiry.entity.Inquiry;
import com.example.ecommerce.domain.inquiry.repository.InquiryRepository;
import com.example.ecommerce.domain.order.entity.Order;
import com.example.ecommerce.domain.order.entity.OrderItem;
import com.example.ecommerce.domain.order.service.OrderService;
import com.example.ecommerce.domain.product.entity.Product;
import com.example.ecommerce.domain.product.enums.ProductStatus;
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
class InquiryServiceTest {

    @Mock
    private InquiryRepository inquiryRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private OrderService orderService;

    @InjectMocks
    private InquiryService inquiryService;

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

    private Order buildOrder(Long id, User owner) {
        Product product = Product.builder()
                .name("탐미오족")
                .price(BigDecimal.valueOf(89000))
                .description("")
                .status(ProductStatus.ON_SALE)
                .build();
        ReflectionTestUtils.setField(product, "id", 10L);
        OrderItem item = OrderItem.builder()
                .product(product).orderPrice(product.getPrice()).quantity(1).build();
        Order order = Order.builder()
                .orderNumber("ORD-TEST-1")
                .user(owner)
                .totalAmount(product.getPrice())
                .discountAmount(BigDecimal.ZERO)
                .orderItems(new ArrayList<>(List.of(item)))
                .build();
        ReflectionTestUtils.setField(order, "id", id);
        return order;
    }

    @Test
    void createInquiry_succeeds_withoutOrder() {
        User user = buildUser(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(inquiryRepository.save(any(Inquiry.class))).thenAnswer(inv -> inv.getArgument(0));

        InquiryCreateRequest request = new InquiryCreateRequest("배송", "배송 문의", "언제 오나요?", null);

        var response = inquiryService.createInquiry(1L, request);

        assertThat(response.title()).isEqualTo("배송 문의");
        assertThat(response.orderId()).isNull();
    }

    @Test
    void createInquiry_succeeds_whenOrderOwnedByUser() {
        User user = buildUser(1L);
        Order order = buildOrder(1L, user);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(orderService.getOrderEntityOrThrow(1L)).thenReturn(order);
        when(inquiryRepository.save(any(Inquiry.class))).thenAnswer(inv -> inv.getArgument(0));

        InquiryCreateRequest request = new InquiryCreateRequest("주문", "주문 문의", "취소하고 싶어요", 1L);

        var response = inquiryService.createInquiry(1L, request);

        assertThat(response.orderId()).isEqualTo(1L);
    }

    @Test
    void createInquiry_throwsAccessDenied_whenOrderNotOwnedByUser() {
        User owner = buildUser(1L);
        Order order = buildOrder(1L, owner);
        when(userRepository.findById(2L)).thenReturn(Optional.of(buildUser(2L)));
        when(orderService.getOrderEntityOrThrow(1L)).thenReturn(order);

        InquiryCreateRequest request = new InquiryCreateRequest("주문", "주문 문의", "취소하고 싶어요", 1L);

        assertThatThrownBy(() -> inquiryService.createInquiry(2L, request))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ORDER_ACCESS_DENIED);
    }

    @Test
    void getMyInquiry_throwsAccessDenied_whenNotOwner() {
        User owner = buildUser(1L);
        Inquiry inquiry = Inquiry.builder()
                .user(owner).order(null).category("배송").title("배송 문의").content("언제 오나요?").build();
        ReflectionTestUtils.setField(inquiry, "id", 1L);
        when(inquiryRepository.findById(1L)).thenReturn(Optional.of(inquiry));

        assertThatThrownBy(() -> inquiryService.getMyInquiry(2L, 1L))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INQUIRY_ACCESS_DENIED);
    }
}
```

- [ ] **Step 2: 전체 테스트 실행**

```bash
./gradlew test
```

Expected: `BUILD SUCCESSFUL`, 신규 4개 테스트 포함 통과.

- [ ] **Step 3: 커밋**

```bash
git add src/test/java/com/example/ecommerce/domain/inquiry/service/InquiryServiceTest.java
git commit -m "test: InquiryService 테스트 추가"
```

---

### Task 6: 문의 컨트롤러 테스트 (InquiryControllerTest 신규)

**Files:**
- Create: `src/test/java/com/example/ecommerce/domain/inquiry/controller/InquiryControllerTest.java`

**Interfaces:**
- Consumes: `TestAuthentication.asUser(User)`(Task 2에서 생성), `InquiryController`의 `POST/GET /api/v1/inquiries`
- Produces: 없음

- [ ] **Step 1: 파일 작성**

```java
package com.example.ecommerce.domain.inquiry.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.ecommerce.domain.inquiry.dto.request.InquiryCreateRequest;
import com.example.ecommerce.domain.inquiry.dto.response.InquiryResponse;
import com.example.ecommerce.domain.inquiry.enums.InquiryStatus;
import com.example.ecommerce.domain.inquiry.service.InquiryService;
import com.example.ecommerce.domain.user.entity.User;
import com.example.ecommerce.domain.user.enums.Provider;
import com.example.ecommerce.domain.user.enums.Role;
import com.example.ecommerce.domain.user.enums.UserStatus;
import com.example.ecommerce.global.exception.CustomException;
import com.example.ecommerce.global.exception.ErrorCode;
import com.example.ecommerce.support.TestAuthentication;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class InquiryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private InquiryService inquiryService;

    private User testUser(Long id) {
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

    @Test
    void createInquiry_returns201_whenAuthenticated() throws Exception {
        InquiryResponse response = new InquiryResponse(
                1L, null, "배송", "배송 문의", "언제 오나요?", null, InquiryStatus.WAITING, LocalDateTime.now(), null);
        when(inquiryService.createInquiry(eq(1L), any())).thenReturn(response);

        InquiryCreateRequest request = new InquiryCreateRequest("배송", "배송 문의", "언제 오나요?", null);

        mockMvc.perform(post("/api/v1/inquiries")
                        .with(TestAuthentication.asUser(testUser(1L)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("배송 문의"));
    }

    @Test
    void getMyInquiry_returns403_whenNotOwner() throws Exception {
        when(inquiryService.getMyInquiry(eq(2L), eq(1L)))
                .thenThrow(new CustomException(ErrorCode.INQUIRY_ACCESS_DENIED));

        mockMvc.perform(get("/api/v1/inquiries/1")
                        .with(TestAuthentication.asUser(testUser(2L))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("INQUIRY_ACCESS_DENIED"));
    }

    @Test
    void createInquiry_returns401_whenNotAuthenticated() throws Exception {
        InquiryCreateRequest request = new InquiryCreateRequest("배송", "배송 문의", "언제 오나요?", null);

        mockMvc.perform(post("/api/v1/inquiries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}
```

- [ ] **Step 2: 전체 테스트 실행**

```bash
./gradlew test
```

Expected: `BUILD SUCCESSFUL`, 신규 3개 테스트 포함 통과.

- [ ] **Step 3: 커밋**

```bash
git add src/test/java/com/example/ecommerce/domain/inquiry/controller/InquiryControllerTest.java
git commit -m "test: 문의 컨트롤러 테스트 추가"
```

---

### Task 7: 전체 검증 + 문서화 + PR

**Files:**
- Modify: `quest.log`
- Modify: `hist.log`

**Interfaces:**
- Consumes: Task 1~6의 모든 결과물
- Produces: 없음 (최종 태스크)

- [ ] **Step 1: 전체 스위트 최종 확인**

```bash
./gradlew test
```

Expected: `BUILD SUCCESSFUL`. 새로 추가된 테스트 총 24개(서비스 15개: 주문생성 5 + 리뷰 6 + 문의 4, 컨트롤러 9개: 주문 3 + 리뷰 3 + 문의 3) 확인.

- [ ] **Step 2: quest.log 갱신**

"바이브 코딩 졸업 로드맵" 3번 항목의 `[ ] 나머지 도메인(주문 생성/장바구니/리뷰/문의 등)은 아직 미착수` 줄을 찾아 아래처럼 교체 (장바구니는 이미 이전 세션에 완료됨을 유지하고, 이번에 끝낸 3개 도메인 + 컨트롤러 테스트 도입을 기록):

```
       - [x] 주문 생성/리뷰/문의: OrderServiceTest에 createOrder/
         createOrderFromCart 5개 추가, ReviewServiceTest 6개, InquiryServiceTest
         4개 신규 작성 (2026-07-27). 이 프로젝트 최초로 컨트롤러(MockMvc) 테스트도
         도입 — OrderControllerTest/ReviewControllerTest/InquiryControllerTest
         각 2~3개씩, 라우팅/인증(401)/GlobalExceptionHandler의 에러코드→HTTP상태
         매핑 검증. 공유 인증 헬퍼(support/TestAuthentication)로 CustomUserDetails
         기반 인증을 MockMvc 요청에 주입.
```

3번 항목 상단의 총 개수 요약 줄(`src/test 에 컨텍스트 로딩 테스트 1개 + ...`)도 최신 합계로 갱신.

- [ ] **Step 3: hist.log 갱신**

`마지막 갱신:` 줄과 타임라인 요약 리스트에 새 항목 추가, 상세 항목을 기존 스타일(배경/내용/검증)로 최상단(가장 최근 항목 자리)에 추가. 이번 작업의 배경("남은 3개 도메인 테스트 커버리지 + 이 프로젝트 최초 컨트롤러 테스트 도입, superpowers:brainstorming/writing-plans 스킬로 설계 문서와 계획을 먼저 작성한 뒤 진행")과 Spring Boot 4.1의 `AutoConfigureMockMvc` 패키지 이동(`org.springframework.boot.webmvc.test.autoconfigure`) 같은 향후 참고할 만한 발견을 기록.

- [ ] **Step 4: 문서 커밋**

```bash
git add quest.log hist.log
git commit -m "docs: 주문생성/리뷰/문의 테스트 커버리지 작업 기록"
```

- [ ] **Step 5: 푸시 + PR 생성 + 머지**

```bash
git push -u origin test/order-review-inquiry-coverage
gh pr create --title "test: 주문생성/리뷰/문의 테스트 커버리지 + 컨트롤러 테스트 도입" --body "$(cat <<'EOF'
## Summary
- OrderService.createOrder/createOrderFromCart, ReviewService, InquiryService
  서비스 계층 테스트 15개 추가
- 이 프로젝트 최초의 컨트롤러(MockMvc) 테스트 도입: OrderControllerTest/
  ReviewControllerTest/InquiryControllerTest 9개 (라우팅/인증/에러코드 매핑 검증)
- 공유 테스트 인증 헬퍼(support/TestAuthentication) 신규

설계 문서: docs/superpowers/specs/2026-07-27-order-review-inquiry-test-coverage-design.md
구현 계획: docs/superpowers/plans/2026-07-27-order-review-inquiry-test-coverage.md

## Test plan
- [x] ./gradlew test 전체 통과 (신규 24개 테스트 포함)

🤖 Generated with [Claude Code](https://claude.com/claude-code)
EOF
)"
gh pr merge --merge --delete-branch
```

Expected: PR 머지 완료, 로컬 `main` 브랜치로 전환 후 fast-forward 동기화 확인.

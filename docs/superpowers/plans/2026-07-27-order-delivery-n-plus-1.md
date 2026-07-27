# 주문 목록 N+1 조회 해소 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** `OrderService.getMyOrders`/`getOrdersForAdmin`의 N+1 배송 조회를 배치 조회로 전환한다.

**Architecture:** `DeliveryRepository`에 `findByOrderIn(List<Order>)` 배치 조회 메서드를 추가하고, `OrderService`에 이를 `Map<Long, Delivery>`로 그룹핑하는 공유 private 헬퍼를 추가해 두 목록 조회 메서드가 재사용한다. 이 프로젝트에 이미 존재하는 `ProductService.mapWithImages` 배치조회 패턴(`findByProductIn` + `Collectors.toMap`)을 그대로 따른다.

**Tech Stack:** Spring Boot 4.1 / Spring Data JPA, JUnit 5, Mockito, AssertJ.

## Global Constraints

- 서비스 테스트: 기존 `OrderServiceTest`의 `@ExtendWith(MockitoExtension.class)` + `@Mock`/`@InjectMocks` 패턴, 엔티티 ID는 `ReflectionTestUtils.setField`로 세팅.
- 빈 리스트에 대한 `IN ()` 쿼리 방지 가드는 `ProductService.ratingStatsByProductId`와 동일하게 넣는다 (`if (orders.isEmpty()) return Map.of();`).
- `getMyOrder`(단건 조회), `deliveryRepository.findByOrder(Order)` 단건 메서드는 변경하지 않는다 (다른 곳에서 계속 사용).
- 태스크 종료 시 `./gradlew test`(전체 스위트) 통과 확인 후 커밋.
- 작업 완료 후 하나의 PR로 묶는다.

---

### Task 1: DeliveryRepository 배치 조회 + OrderService 적용 + 테스트

**Files:**
- Modify: `src/main/java/com/example/ecommerce/domain/delivery/repository/DeliveryRepository.java`
- Modify: `src/main/java/com/example/ecommerce/domain/order/service/OrderService.java`
- Modify: `src/test/java/com/example/ecommerce/domain/order/service/OrderServiceTest.java`

**Interfaces:**
- Consumes: 없음 (기존 `Order`/`Delivery` 엔티티, `OrderResponse.of`/`AdminOrderResponse.of` 정적 팩토리는 이미 존재)
- Produces: 없음 (다른 태스크가 의존하지 않는 단일 태스크)

- [ ] **Step 1: DeliveryRepository에 배치 조회 메서드 추가**

`src/main/java/com/example/ecommerce/domain/delivery/repository/DeliveryRepository.java`
현재 내용:

```java
package com.example.ecommerce.domain.delivery.repository;

import com.example.ecommerce.domain.delivery.entity.Delivery;
import com.example.ecommerce.domain.delivery.enums.DeliveryStatus;
import com.example.ecommerce.domain.order.entity.Order;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeliveryRepository extends JpaRepository<Delivery, Long> {

    Optional<Delivery> findByOrder(Order order);

    List<Delivery> findByStatus(DeliveryStatus status);

    long countByStatus(DeliveryStatus status);
}
```

`findByOrder(Order order)` 바로 아래에 한 줄 추가:

```java
    Optional<Delivery> findByOrder(Order order);

    List<Delivery> findByOrderIn(List<Order> orders);

    List<Delivery> findByStatus(DeliveryStatus status);
```

- [ ] **Step 2: OrderService에 배치조회 헬퍼 추가 + getMyOrders/getOrdersForAdmin 수정**

`src/main/java/com/example/ecommerce/domain/order/service/OrderService.java` 상단 import 블록(24번째 줄 `import java.math.BigDecimal;` 부근)에 추가:

```java
import java.util.Map;
import java.util.stream.Collectors;
```

(`java.util.List`는 이미 import되어 있음)

`getMyOrders` 메서드(현재 121~126번째 줄)를 아래로 교체:

```java
    public Page<OrderResponse> getMyOrders(Long userId, Pageable pageable) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        Page<Order> orders = orderRepository.findByUser(user, pageable);
        Map<Long, Delivery> deliveriesByOrderId = deliveriesByOrderId(orders.getContent());
        return orders.map(order -> OrderResponse.of(order, deliveriesByOrderId.get(order.getId())));
    }
```

`getOrdersForAdmin` 메서드(현재 186~191번째 줄)를 아래로 교체:

```java
    public Page<AdminOrderResponse> getOrdersForAdmin(OrderStatus status, Pageable pageable) {
        Page<Order> orders = status != null
                ? orderRepository.findByStatus(status, pageable)
                : orderRepository.findAll(pageable);
        Map<Long, Delivery> deliveriesByOrderId = deliveriesByOrderId(orders.getContent());
        return orders.map(order -> AdminOrderResponse.of(order, deliveriesByOrderId.get(order.getId())));
    }
```

두 메서드가 공유하는 private 헬퍼를 `getOrderForAdmin` 메서드(현재 193~196번째 줄) 바로 아래에 추가:

```java
    private Map<Long, Delivery> deliveriesByOrderId(List<Order> orders) {
        if (orders.isEmpty()) {
            return Map.of();
        }
        return deliveryRepository.findByOrderIn(orders).stream()
                .collect(Collectors.toMap(delivery -> delivery.getOrder().getId(), delivery -> delivery));
    }
```

`Delivery` 엔티티 타입을 참조하므로 상단 import 블록에 추가 필요:

```java
import com.example.ecommerce.domain.delivery.entity.Delivery;
```

(`com.example.ecommerce.domain.delivery.repository.DeliveryRepository`는 이미 import되어 있음, 8번째 줄)

- [ ] **Step 3: getMyOrders 테스트 2개 추가**

`src/test/java/com/example/ecommerce/domain/order/service/OrderServiceTest.java` 상단 import 블록에 추가:

```java
import com.example.ecommerce.domain.delivery.enums.DeliveryStatus;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
```

클래스 마지막 `}` 바로 앞(기존 `createOrderFromCart_reservesStockAndClearsCart_onSuccess` 다음)에 추가:

```java
    @Test
    void getMyOrders_batchFetchesDeliveries_notOnePerOrder() {
        User user = buildUser(1L);
        Product product = buildProduct(10L, BigDecimal.valueOf(10000));
        Order orderWithDelivery = buildOrderWithId(2L, user, product);
        Order orderWithoutDelivery = buildOrderWithId(3L, user, product);
        Delivery delivery = mock(Delivery.class);
        when(delivery.getOrder()).thenReturn(orderWithDelivery);
        when(delivery.getStatus()).thenReturn(DeliveryStatus.IN_TRANSIT);
        PageRequest pageable = PageRequest.of(0, 10);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(orderRepository.findByUser(user, pageable))
                .thenReturn(new PageImpl<>(List.of(orderWithDelivery, orderWithoutDelivery), pageable, 2));
        when(deliveryRepository.findByOrderIn(List.of(orderWithDelivery, orderWithoutDelivery)))
                .thenReturn(List.of(delivery));

        Page<OrderResponse> result = orderService.getMyOrders(1L, pageable);

        Map<Long, OrderResponse> byOrderId = result.getContent().stream()
                .collect(Collectors.toMap(OrderResponse::id, r -> r));
        assertThat(byOrderId).hasSize(2);
        assertThat(byOrderId.get(2L).deliveryStatus()).isEqualTo(DeliveryStatus.IN_TRANSIT);
        assertThat(byOrderId.get(3L).deliveryStatus()).isNull();
        verify(deliveryRepository).findByOrderIn(any());
        verify(deliveryRepository, never()).findByOrder(any());
    }

    @Test
    void getMyOrders_returnsEmptyPage_withoutQueryingDeliveries() {
        User user = buildUser(1L);
        PageRequest pageable = PageRequest.of(0, 10);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(orderRepository.findByUser(user, pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        Page<OrderResponse> result = orderService.getMyOrders(1L, pageable);

        assertThat(result.getContent()).isEmpty();
        verify(deliveryRepository, never()).findByOrderIn(any());
    }
```

두 테스트가 공용으로 쓰는 새 fixture 헬퍼를 `buildProduct` 메서드(현재 116~125번째 줄) 바로 아래에 추가:

```java
    private Order buildOrderWithId(Long id, User user, Product product) {
        OrderItem item = OrderItem.builder()
                .product(product)
                .orderPrice(product.getPrice())
                .quantity(1)
                .build();
        Order order = Order.builder()
                .orderNumber("ORD-TEST-" + id)
                .user(user)
                .totalAmount(product.getPrice())
                .discountAmount(BigDecimal.ZERO)
                .orderItems(new ArrayList<>(List.of(item)))
                .build();
        ReflectionTestUtils.setField(order, "id", id);
        return order;
    }
```

- [ ] **Step 4: 전체 테스트 실행**

```bash
./gradlew test
```

Expected: `BUILD SUCCESSFUL`, 신규 2개 테스트 포함 통과.

- [ ] **Step 5: 브랜치 생성 + 커밋**

```bash
git checkout -b fix/order-delivery-n-plus-1
git add src/main/java/com/example/ecommerce/domain/delivery/repository/DeliveryRepository.java \
        src/main/java/com/example/ecommerce/domain/order/service/OrderService.java \
        src/test/java/com/example/ecommerce/domain/order/service/OrderServiceTest.java
git commit -m "fix: 주문 목록 조회 N+1 배송 조회를 배치 조회로 전환"
```

---

### Task 2: 문서화 + PR

**Files:**
- Modify: `quest.log`
- Modify: `hist.log`

**Interfaces:**
- Consumes: Task 1의 결과물
- Produces: 없음 (최종 태스크)

- [ ] **Step 1: quest.log 갱신**

"사용자 실사용 중 발견한 버그 6건 수정" 섹션 아래 "📋 다음에 고도화할 부분 (신규)"의
`AdminOrderResponse.of(order, delivery) 패턴을 getMyOrders에도...` 항목을 찾아
해결 완료로 갱신 (예: 항목 앞에 `[x]` 표시하고 배치조회로 전환했다는 내용과 검증
방식을 짧게 추가).

- [ ] **Step 2: hist.log 갱신**

`마지막 갱신:` 헤더와 타임라인 요약 리스트에 새 항목 추가, 상세 항목을 기존
스타일(배경/내용/검증)로 최상단에 추가. 배경은 "이전 세션에서 발견해 quest.log에
기록해둔 N+1 이슈를 브레인스토밍으로 재설계 후 해소"로, 내용은 Task 1의 변경사항
요약, 검증은 신규 테스트 2개(`getMyOrders_batchFetchesDeliveries_notOnePerOrder`가
`findByOrderIn` 1회만 호출되고 `findByOrder`는 호출되지 않음을 확인)로 기록.

- [ ] **Step 3: 커밋**

```bash
git add quest.log hist.log
git commit -m "docs: 주문 목록 N+1 해소 작업 기록"
```

- [ ] **Step 4: 푸시 + PR 생성 + 머지**

```bash
git push -u origin fix/order-delivery-n-plus-1
gh pr create --title "fix: 주문 목록 조회 N+1 배송 조회를 배치 조회로 전환" --body "$(cat <<'EOF'
## Summary
- OrderService.getMyOrders/getOrdersForAdmin이 주문 개수만큼 배송을 개별
  조회하던 N+1 패턴을 DeliveryRepository.findByOrderIn 배치 조회로 전환
- ProductService.mapWithImages의 기존 배치조회 패턴(findByProductIn +
  Collectors.toMap)을 그대로 재사용

설계 문서: docs/superpowers/specs/2026-07-27-order-delivery-n-plus-1-design.md
구현 계획: docs/superpowers/plans/2026-07-27-order-delivery-n-plus-1.md

## Test plan
- [x] ./gradlew test 전체 통과 (신규 2개 포함), findByOrderIn 1회만 호출되고
  findByOrder는 호출되지 않는 것 확인

🤖 Generated with [Claude Code](https://claude.com/claude-code)
EOF
)"
gh pr merge --merge --delete-branch
```

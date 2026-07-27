# 주문 목록 조회 N+1 해소 설계

날짜: 2026-07-27
관련: quest.log "사용자 실사용 리포트 버그 6건 수정" 항목 D의 "다음에 고도화할 부분"에서
발견/기록된 이슈 (2026-07-27)

## 배경

주문 목록 상태 표시 통합 작업(D) 중, `OrderResponse.of(order, delivery)` 패턴을
고객용/관리자용 응답 DTO가 공유하게 되면서 두 목록 조회 메서드 모두 페이지당
주문 개수만큼 배송 조회 쿼리를 추가로 날리는 N+1 패턴이 남아있음이 확인됨:

```java
// OrderService.getMyOrders
return orderRepository.findByUser(user, pageable)
        .map(order -> OrderResponse.of(order, deliveryRepository.findByOrder(order).orElse(null)));

// OrderService.getOrdersForAdmin
return orders.map(order -> AdminOrderResponse.of(order, deliveryRepository.findByOrder(order).orElse(null)));
```

두 메서드 모두 페이지 크기가 N이면 `주문 조회 1회 + 배송 조회 N회` = `N+1` 쿼리가 발생함.
단건 조회인 `getMyOrder`는 이 문제와 무관 (변경 없음).

이 프로젝트에는 이미 정확히 동일한 문제를 해결한 전례가 있음
(`ProductService.mapWithImages` — 상품 목록 N개에 대한 이미지/평점을
`findByProductIn(List<Product>)` 배치 조회 + `Map`으로 그룹핑해 O(1) 조회로
전환한 패턴). 이번 설계는 그 패턴을 배송 조회에 그대로 적용한다.

## 설계

### 1. `DeliveryRepository`에 배치 조회 메서드 추가

```java
List<Delivery> findByOrderIn(List<Order> orders);
```

Spring Data JPA derived query — `ProductImageRepository.findByProductInOrderByProductIdAscSortOrderAsc`와
동일한 방식(신규 `@Query` 불필요).

### 2. `OrderService`에 공유 배치조회 헬퍼 추가

```java
private Map<Long, Delivery> deliveriesByOrderId(List<Order> orders) {
    if (orders.isEmpty()) {
        return Map.of();
    }
    return deliveryRepository.findByOrderIn(orders).stream()
            .collect(Collectors.toMap(delivery -> delivery.getOrder().getId(), delivery -> delivery));
}
```

빈 리스트 가드는 `ProductService.ratingStatsByProductId`와 동일한 이유(빈 `IN ()`
쿼리 방지)로 넣음.

### 3. `getMyOrders`/`getOrdersForAdmin`가 헬퍼 공유

```java
public Page<OrderResponse> getMyOrders(Long userId, Pageable pageable) {
    User user = userRepository.findById(userId)
            .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    Page<Order> orders = orderRepository.findByUser(user, pageable);
    Map<Long, Delivery> deliveriesByOrderId = deliveriesByOrderId(orders.getContent());
    return orders.map(order -> OrderResponse.of(order, deliveriesByOrderId.get(order.getId())));
}

public Page<AdminOrderResponse> getOrdersForAdmin(OrderStatus status, Pageable pageable) {
    Page<Order> orders = status != null
            ? orderRepository.findByStatus(status, pageable)
            : orderRepository.findAll(pageable);
    Map<Long, Delivery> deliveriesByOrderId = deliveriesByOrderId(orders.getContent());
    return orders.map(order -> AdminOrderResponse.of(order, deliveriesByOrderId.get(order.getId())));
}
```

응답 타입(`OrderResponse` vs `AdminOrderResponse`)이 서로 달라 매핑 클로저는
각자 두되, 배치조회+맵 구성 로직(`deliveriesByOrderId`)은 하나로 공유해 DRY를
지킴.

### 효과

페이지당 쿼리 수가 `1(주문) + N(배송)`에서 `1(주문) + 1(배송 배치)`로 감소.
`getMyOrder`(단건 조회)는 원래도 N+1이 아니므로 변경하지 않음.

## 테스트

`OrderServiceTest`에 두 메서드용 테스트를 추가해, 배송이 있는 주문/없는 주문이
섞인 목록에서 각 주문에 올바른 `deliveryStatus`가 매핑되는지 확인하고,
`verify(deliveryRepository).findByOrderIn(anyList())`로 배치 조회 1회만
호출되는지(개별 `findByOrder` 호출이 없는지) 검증한다.

## 스코프 밖

- `getMyOrder`(단건 조회)는 이번 스코프 아님 — N+1과 무관.
- `deliveryRepository.findByOrder(Order)` 단건 메서드는 다른 곳(반품 요청,
  배송 등록 등 단건 처리 로직)에서 계속 쓰이므로 유지, 삭제하지 않음.
- 프론트엔드 변경 없음 — 순수 백엔드 쿼리 최적화, API 응답 형태는 동일.

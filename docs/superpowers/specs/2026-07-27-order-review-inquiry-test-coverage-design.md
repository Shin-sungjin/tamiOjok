# 주문생성/리뷰/문의 테스트 커버리지 설계

날짜: 2026-07-27
관련: quest.log "바이브 코딩 졸업 로드맵" 3번(테스트 코드 작성)

## 배경

재고 예약/차감, 쿠폰 할인 계산, 결제 금액 위변조 검증, 장바구니 재고 검증,
배송 반품 플로우까지는 이미 테스트가 작성되어 있음. 남은 도메인은
**주문 생성(OrderService.createOrder/createOrderFromCart), 리뷰
(ReviewService), 문의(InquiryService)** 세 곳이며, 이번 작업은 이 세
도메인의 테스트 커버리지를 채우는 것이 목표.

추가로, 이 프로젝트에는 아직 컨트롤러(API) 레벨 테스트가 하나도 없음
(`src/test`에 서비스/엔티티 단위 테스트만 존재). 이번 기회에 컨트롤러
테스트 인프라를 처음 도입한다.

## 테스트 계층 구성

### 서비스 계층 (기존 패턴 재사용)

기존 `OrderServiceTest`/`CartServiceTest`/`DeliveryTest`와 동일한 스타일:
`@ExtendWith(MockitoExtension.class)`, `@Mock`/`@InjectMocks`, 엔티티는
`ReflectionTestUtils.setField`로 ID 세팅한 fixture 헬퍼 메서드 사용.
비즈니스 규칙 분기(성공/예외 경계)를 검증하는 데 집중.

### 컨트롤러 계층 (신규 도입)

- **구성**: `@SpringBootTest(webEnvironment = WebEnvironment.MOCK)` +
  `@AutoConfigureMockMvc`, 대상 서비스 빈은 `@MockitoBean`으로 교체.
  `@WebMvcTest` 슬라이스 테스트는 이 프로젝트의 `SecurityConfig`가 JWT
  필터/OAuth2 핸들러 등 여러 빈에 의존하고 있어 슬라이스 스캔 범위 밖에
  놓이기 쉬우므로 배제하고, 기존 `EcommerceApplicationTests`가 이미
  검증해둔 테스트용 H2 + 더미 OAuth2 설정을 그대로 재사용하는 전체
  컨텍스트 방식을 쓴다.
- **인증**: 컨트롤러가 `@AuthenticationPrincipal CustomUserDetails`로
  현재 사용자를 받으므로 `@WithMockUser`(기본 `UserDetails` 기반)는
  맞지 않음. `CustomUserDetails`를 감싼
  `UsernamePasswordAuthenticationToken`을
  `SecurityMockMvcRequestPostProcessors.authentication(...)`으로 요청에
  주입하는 재사용 가능한 테스트 헬퍼를 하나 만들어 세 컨트롤러 테스트가
  공유한다 (`src/test/java/com/example/ecommerce/support/` 또는
  유사한 공유 테스트 유틸 위치).
- **CSRF**: 앱 전역에서 비활성화되어 있으므로 `.with(csrf())` 불필요.
- **범위**: 라우팅, 인증 여부(401), `GlobalExceptionHandler`의
  `CustomException → HTTP 상태` 매핑, 응답 바디 형태 검증에 집중.
  비즈니스 규칙 자체(예: 반품 가능 조건, 쿠폰 계산)는 서비스 테스트가
  이미 커버하므로 컨트롤러 테스트에서 중복 검증하지 않는다.

## 도메인별 핵심 검증 포인트

| 도메인 | 서비스 테스트 | 컨트롤러 테스트 |
|---|---|---|
| 주문생성 | `createOrder`(아이템별 재고 예약 후 주문 생성, 쿠폰 지정 시 소유권 검증), `createOrderFromCart`(빈 장바구니 시 `EMPTY_CART`, 성공 시 장바구니 클리어) | POST `/api/v1/orders` 성공 시 201, 미인증 401, 타인 쿠폰 사용 시도 시 서비스가 던진 `COUPON_ACCESS_DENIED`가 올바른 HTTP 상태로 매핑되는지 |
| 리뷰 | `createReview`(주문한 상품인지 + 배송완료 여부 검증 → `REVIEW_NOT_ALLOWED`, 중복 리뷰 → `DUPLICATE_REVIEW`), 수정/삭제 시 소유권 검증(`REVIEW_ACCESS_DENIED`) | POST `/api/v1/reviews` 성공 시 201, 타인 리뷰 수정/삭제 시도 시 403 매핑 |
| 문의 | `createInquiry`(주문 연결 시 소유권 검증 `ORDER_ACCESS_DENIED`), 조회 소유권 검증(`INQUIRY_ACCESS_DENIED`) | POST `/api/v1/inquiries` 성공 시 201, 타인 문의 조회 시 403 매핑 |

## 진행 순서

주문생성(서비스 → 컨트롤러) → 리뷰(서비스 → 컨트롤러) → 문의(서비스 →
컨트롤러). 도메인 하나가 끝날 때마다 `./gradlew test`로 전체 스위트
통과를 확인한다.

## 커밋/PR 전략

이 설계에 포함된 작업 전체(6개 테스트 클래스: 서비스 3개 + 컨트롤러
3개, 공유 인증 테스트 헬퍼 1개)를 마친 뒤 하나의 PR로 묶는다. 테스트
전용 작업이라 도메인별로 PR을 쪼갤 실익이 적다고 판단.

## 스코프 밖

- 컨트롤러 테스트는 이번에 도입하는 6개 엔드포인트(주문생성 2개, 리뷰
  생성/수정/삭제, 문의 생성/조회)에 한정하고, 기존에 이미 구현된 다른
  도메인(상품/유저/쿠폰/배송/관리자)의 컨트롤러 테스트는 이번 스코프에
  포함하지 않는다 (필요하면 별도 후속 작업으로).
- 통합 테스트(`@SpringBootTest` + 실제 Postgres, 전체 플로우 E2E)는
  대상이 아님 — 이미 claude-in-chrome 기반 실사용 검증으로 갈음하고
  있음.

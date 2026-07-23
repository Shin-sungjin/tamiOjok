# tamiOjok 사용자 매뉴얼

이 문서는 tamiOjok REST API를 처음 사용하는 사람을 위한 안내서입니다. 인증 방법, 일반 사용자 시나리오, 관리자 시나리오, 전체 API 목록, 에러 코드를 순서대로 설명합니다.

모든 예시는 로컬 실행(`http://localhost:8080`) 기준이며, `db/seed.sql`을 실행했다는 가정 하에 테스트 계정 이메일을 사용합니다. (계정 정보는 [`README.md`](../README.md) 참고, 비밀번호는 공통으로 `Password1!`)

## 목차

1. [인증 방식](#1-인증-방식)
2. [공통 응답 형식](#2-공통-응답-형식)
3. [일반 사용자 시나리오](#3-일반-사용자-시나리오)
4. [관리자 시나리오](#4-관리자-시나리오)
5. [백그라운드 스케줄러](#5-백그라운드-스케줄러)
6. [API 레퍼런스](#6-api-레퍼런스)
7. [에러 코드 목록](#7-에러-코드-목록)

---

## 1. 인증 방식

- **Access Token**: 로그인/토큰재발급 응답의 JSON 바디로 내려주는 JWT입니다. 이후 요청 시 `Authorization: Bearer {accessToken}` 헤더로 전달합니다. (기본 유효기간 30분)
- **Refresh Token**: 로그인/토큰재발급 시 `HttpOnly` 쿠키(`refreshToken`)로 내려갑니다. Access Token이 만료되면 `POST /api/v1/auth/refresh`를 쿠키와 함께 호출해 새 Access Token을 재발급받습니다. (기본 유효기간 14일)
- **소셜 로그인**: `/oauth2/authorization/{kakao|naver|google}`로 브라우저를 리다이렉트하면 OAuth 로그인 후 `app.oauth2.redirect-uri`로 토큰과 함께 리다이렉트됩니다. 최초 소셜 로그인 시 휴대폰번호/배송지 등 추가정보가 없으면 사용자 상태가 `NEED_INFO`가 되고 `app.oauth2.need-info-uri`로 리다이렉트되며, `POST /api/v1/users/me/additional-info` 호출 전까지는 주문 등 일부 기능 이용이 제한됩니다.
- **관리자 API**(`/api/v1/admin/**`)는 `role=ADMIN`인 사용자만 호출할 수 있습니다.
- `GET /api/v1/products/**`, `GET /api/v1/products/{id}/reviews`, 회원가입/로그인/토큰재발급, OAuth 관련 경로는 인증 없이 호출 가능합니다. 그 외 모든 API는 Access Token이 필요합니다.

## 2. 공통 응답 형식

성공 응답은 각 API별 DTO를 그대로 반환합니다. 목록 조회는 Spring Data의 `Page` 형식(`content`, `totalElements`, `totalPages`, `number`, `size` 등)으로 내려갑니다. 페이지네이션은 쿼리 파라미터 `page`, `size`, `sort`를 사용합니다. (예: `?page=0&size=20&sort=createdAt,desc`)

에러 응답은 다음 형식으로 통일되어 있습니다.

```json
{
  "code": "PRODUCT_NOT_FOUND",
  "message": "상품을 찾을 수 없습니다.",
  "errors": []
}
```

`@Valid` 검증 실패(`400 Bad Request`, `code: INVALID_REQUEST`)일 때는 `errors` 배열에 필드별 검증 메시지가 채워집니다. 전체 에러 코드는 [7. 에러 코드 목록](#7-에러-코드-목록)을 참고하세요.

---

## 3. 일반 사용자 시나리오

아래는 회원가입부터 리뷰 작성까지 실제 서비스 사용 흐름을 curl 예시로 따라가 봅니다.

### 3.1 회원가입 / 로그인

```bash
# 회원가입
curl -X POST http://localhost:8080/api/v1/auth/signup \
  -H "Content-Type: application/json" \
  -d '{"email":"newuser@example.com","password":"Password1!","name":"신규유저","phoneNumber":"010-9999-8888"}'

# 로그인 (Access Token은 바디로, Refresh Token은 Set-Cookie로 내려옵니다)
curl -i -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"newuser@example.com","password":"Password1!"}'
```

응답 예시:

```json
{ "accessToken": "eyJhbGciOi...", "tokenType": "Bearer" }
```

이후 요청에서는 `Authorization: Bearer eyJhbGciOi...` 헤더를 사용합니다. 아래 예시들은 `$TOKEN` 변수에 Access Token이 담겨 있다고 가정합니다.

```bash
# Access Token 만료 시 재발급 (Refresh Token 쿠키 필요)
curl -i -X POST http://localhost:8080/api/v1/auth/refresh --cookie "refreshToken=..."

# 내 정보 조회
curl http://localhost:8080/api/v1/users/me -H "Authorization: Bearer $TOKEN"

# 로그아웃 (서버에 저장된 refresh token 폐기)
curl -X POST http://localhost:8080/api/v1/auth/logout -H "Authorization: Bearer $TOKEN"
```

### 3.2 상품 둘러보기

```bash
# 판매중인 상품 목록 (인증 불필요)
curl "http://localhost:8080/api/v1/products?page=0&size=10"

# 상품 상세
curl http://localhost:8080/api/v1/products/1

# 상품별 리뷰 목록 (인증 불필요)
curl http://localhost:8080/api/v1/products/1/reviews
```

### 3.3 장바구니 담기

```bash
# 장바구니에 상품 담기 (같은 상품을 다시 담으면 수량이 합산됩니다)
curl -X POST http://localhost:8080/api/v1/cart/items \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"productId":1,"quantity":2}'

# 내 장바구니 조회
curl http://localhost:8080/api/v1/cart -H "Authorization: Bearer $TOKEN"

# 특정 항목 수량 변경 / 삭제
curl -X PUT http://localhost:8080/api/v1/cart/items/{cartItemId} \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -d '{"quantity":3}'
curl -X DELETE http://localhost:8080/api/v1/cart/items/{cartItemId} -H "Authorization: Bearer $TOKEN"
```

### 3.4 쿠폰 발급 (선택)

```bash
# 현재 발급 가능한(유효기간 내) 쿠폰 목록
curl http://localhost:8080/api/v1/coupons -H "Authorization: Bearer $TOKEN"

# 쿠폰 발급받기
curl -X POST http://localhost:8080/api/v1/coupons/1/issue -H "Authorization: Bearer $TOKEN"

# 내가 보유한 쿠폰 목록
curl http://localhost:8080/api/v1/coupons/my -H "Authorization: Bearer $TOKEN"
```

### 3.5 주문 생성

두 가지 방식으로 주문을 생성할 수 있습니다. 두 방식 모두 `userCouponId`를 지정하면 보유 쿠폰(발급받았고 아직 미사용인 쿠폰)이 자동으로 적용되어 `discountAmount`가 계산됩니다.

```bash
# (A) 상품을 직접 지정해서 주문
curl -X POST http://localhost:8080/api/v1/orders \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"items":[{"productId":1,"quantity":1}],"userCouponId":null}'

# (B) 장바구니에 담긴 항목 그대로 주문 (주문 성공 시 장바구니는 자동으로 비워집니다)
curl -X POST "http://localhost:8080/api/v1/orders/from-cart?userCouponId=1" \
  -H "Authorization: Bearer $TOKEN"
```

주문 생성 시 재고는 즉시 차감되지 않고 `reserved_quantity`로 **예약**됩니다. `PENDING_PAYMENT` 상태로 10분 안에 결제가 확인되지 않으면 스케줄러가 자동으로 주문을 취소하고 예약을 해제합니다. (자세한 내용은 [5. 백그라운드 스케줄러](#5-백그라운드-스케줄러) 참고)

```bash
# 내 주문 목록 / 상세 조회
curl "http://localhost:8080/api/v1/orders?page=0&size=10" -H "Authorization: Bearer $TOKEN"
curl http://localhost:8080/api/v1/orders/{orderId} -H "Authorization: Bearer $TOKEN"

# 주문 취소 (배송 시작 전에만 가능 — 배송 시작 후에는 반품 요청을 이용해야 합니다)
curl -X DELETE http://localhost:8080/api/v1/orders/{orderId} -H "Authorization: Bearer $TOKEN"
```

### 3.6 결제

이 프로젝트는 실제 PG사 대신 `MockPgClient`를 사용합니다. 실제 서비스에서는 프론트엔드가 PG 결제창을 띄운 뒤 결제 성공 콜백에서 아래 API를 호출해 서버가 PG 서버에 직접 실제 결제금액을 조회/대조하는 구조입니다(위변조 방지).

```bash
curl -X POST http://localhost:8080/api/v1/payments/confirm \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"orderId":1,"pgProvider":"MOCK_PG","pgTransactionId":"MOCKPG-TEST-0001","paidAmount":89000}'

# 결제 내역 조회
curl http://localhost:8080/api/v1/payments/orders/{orderId} -H "Authorization: Bearer $TOKEN"
```

주문 금액(`payment_amount`)과 PG에서 확인된 `paidAmount`가 다르면(위변조 의심) PG 결제가 즉시 취소되고, 결제는 `FAILED` 처리되며 주문도 함께 `CANCELLED` 상태로 자동 취소됩니다. 금액이 일치하면 결제가 `PAID` 처리되고 주문 상태가 `PAYMENT_COMPLETED`로 바뀌며 예약된 재고가 실제로 차감됩니다.

### 3.7 배송 조회 / 반품 요청

```bash
curl http://localhost:8080/api/v1/orders/{orderId}/delivery -H "Authorization: Bearer $TOKEN"

# 배송이 시작된 이후에만 반품 요청 가능
curl -X POST http://localhost:8080/api/v1/orders/{orderId}/delivery/return-request \
  -H "Authorization: Bearer $TOKEN"
```

### 3.8 리뷰 작성

배송 상태가 `DELIVERED`이고, 실제로 해당 주문에 포함된 상품에 대해서만 리뷰를 작성할 수 있습니다. 하나의 주문-상품 조합에는 리뷰를 1개만 작성할 수 있습니다.

```bash
curl -X POST http://localhost:8080/api/v1/reviews \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"orderId":1,"productId":1,"rating":5,"content":"만족스러워요!"}'

curl -X PUT http://localhost:8080/api/v1/reviews/{reviewId} \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"rating":4,"content":"다시 생각해보니 4점이 맞는 것 같아요."}'

curl -X DELETE http://localhost:8080/api/v1/reviews/{reviewId} -H "Authorization: Bearer $TOKEN"
curl "http://localhost:8080/api/v1/reviews?page=0&size=10" -H "Authorization: Bearer $TOKEN"  # 내 리뷰 목록
```

### 3.9 1:1 문의

```bash
curl -X POST http://localhost:8080/api/v1/inquiries \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"category":"배송","title":"배송이 늦어요","content":"주문한지 5일이 지났습니다.","orderId":1}'

curl "http://localhost:8080/api/v1/inquiries?page=0&size=10" -H "Authorization: Bearer $TOKEN"
curl http://localhost:8080/api/v1/inquiries/{inquiryId} -H "Authorization: Bearer $TOKEN"
```

---

## 4. 관리자 시나리오

관리자 계정(`admin@tamiojok.com` / `Password1!`)으로 로그인해 얻은 토큰을 사용합니다.

```bash
# 1) 상품 등록
curl -X POST http://localhost:8080/api/v1/admin/products \
  -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" \
  -d '{"name":"신상품","price":10000,"description":"설명","initialStock":100}'

# 2) 상품 정보/판매상태 수정, 재입고
curl -X PUT http://localhost:8080/api/v1/admin/products/{productId} \
  -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" \
  -d '{"name":"신상품","price":12000,"description":"설명 수정"}'
curl -X PUT http://localhost:8080/api/v1/admin/products/{productId}/status/ON_SALE -H "Authorization: Bearer $ADMIN_TOKEN"
curl -X POST http://localhost:8080/api/v1/admin/products/{productId}/stock/restock \
  -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" -d '{"quantity":50}'

# 3) 결제 완료된 주문을 배송 준비 상태로 전환
curl -X POST http://localhost:8080/api/v1/admin/orders/{orderId}/prepare -H "Authorization: Bearer $ADMIN_TOKEN"

# 4) 배송 등록 (송장 등록 → 이후 스케줄러가 배송 상태를 자동 추적합니다)
curl -X POST http://localhost:8080/api/v1/admin/orders/{orderId}/delivery \
  -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" \
  -d '{"courierCode":"CJ","trackingNumber":"1234567890"}'

# 5) 쿠폰 생성
curl -X POST http://localhost:8080/api/v1/admin/coupons \
  -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" \
  -d '{"code":"AUGUST15","name":"8월 특가 15% 할인","discountType":"PERCENTAGE","discountValue":15,"minOrderAmount":30000,"maxDiscountAmount":10000,"validFrom":"2026-08-01T00:00:00","validUntil":"2026-08-31T23:59:59"}'

# 6) 미답변 문의 목록 조회 후 답변 등록
curl "http://localhost:8080/api/v1/admin/inquiries?status=WAITING" -H "Authorization: Bearer $ADMIN_TOKEN"
curl -X POST http://localhost:8080/api/v1/admin/inquiries/{inquiryId}/answer \
  -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" \
  -d '{"answer":"확인 후 처리해드리겠습니다."}'
```

---

## 5. 백그라운드 스케줄러

| 스케줄러 | 주기 | 동작 |
| --- | --- | --- |
| `OrderExpirationScheduler` | 1분 | `PENDING_PAYMENT` 상태로 10분이 지난 주문을 찾아 예약 재고를 해제하고 주문을 자동 취소합니다. |
| `CourierTrackingScheduler` | 1분 | `IN_TRANSIT` 상태인 배송 건에 대해 택배사 추적 API(`CourierTrackingClient`, 로컬은 Mock 구현체)를 조회하고, `DELIVERED`로 바뀌면 상태를 갱신하고 구매확정/리뷰 작성 알림을 발송합니다. |

---

## 6. API 레퍼런스

### 인증 / 회원 (`user` 도메인)

| Method | URL | 인증 | 설명 |
| --- | --- | --- | --- |
| POST | `/api/v1/auth/signup` | - | 회원가입 |
| POST | `/api/v1/auth/login` | - | 로그인 (Access Token 응답 + Refresh Token 쿠키) |
| POST | `/api/v1/auth/refresh` | Refresh 쿠키 | Access Token 재발급 |
| POST | `/api/v1/auth/logout` | 필요 | 로그아웃 |
| GET | `/api/v1/users/me` | 필요 | 내 정보 조회 |
| POST | `/api/v1/users/me/additional-info` | 필요 | 소셜 로그인 후 추가정보(휴대폰/배송지) 입력 |
| GET | `/oauth2/authorization/{kakao\|naver\|google}` | - | 소셜 로그인 시작 (브라우저 리다이렉트) |

### 상품 (`product` 도메인)

| Method | URL | 인증 | 설명 |
| --- | --- | --- | --- |
| GET | `/api/v1/products` | - | 판매중 상품 목록(페이지) |
| GET | `/api/v1/products/{productId}` | - | 상품 상세 |
| POST | `/api/v1/admin/products` | ADMIN | 상품 등록 |
| PUT | `/api/v1/admin/products/{productId}` | ADMIN | 상품 정보 수정 |
| PUT | `/api/v1/admin/products/{productId}/status/{status}` | ADMIN | 판매 상태 변경 (`ON_SALE`/`OUT_OF_STOCK`/`HIDDEN`) |
| POST | `/api/v1/admin/products/{productId}/stock/restock` | ADMIN | 재고 추가 |

### 장바구니 (`cart` 도메인)

| Method | URL | 인증 | 설명 |
| --- | --- | --- | --- |
| GET | `/api/v1/cart` | 필요 | 내 장바구니 조회 |
| POST | `/api/v1/cart/items` | 필요 | 장바구니에 상품 담기(동일 상품은 수량 합산) |
| PUT | `/api/v1/cart/items/{cartItemId}` | 필요 | 장바구니 항목 수량 변경 |
| DELETE | `/api/v1/cart/items/{cartItemId}` | 필요 | 장바구니 항목 삭제 |
| DELETE | `/api/v1/cart` | 필요 | 장바구니 전체 비우기 |

### 주문 (`order` 도메인)

| Method | URL | 인증 | 설명 |
| --- | --- | --- | --- |
| POST | `/api/v1/orders` | 필요 | 상품 직접 지정 주문 생성 (`userCouponId`로 쿠폰 적용 가능) |
| POST | `/api/v1/orders/from-cart` | 필요 | 장바구니 기반 주문 생성 (쿼리파라미터 `userCouponId`) |
| GET | `/api/v1/orders` | 필요 | 내 주문 목록(페이지) |
| GET | `/api/v1/orders/{orderId}` | 필요 | 내 주문 상세 |
| DELETE | `/api/v1/orders/{orderId}` | 필요 | 주문 취소 (배송 시작 전만 가능) |
| POST | `/api/v1/admin/orders/{orderId}/prepare` | ADMIN | 주문을 배송준비(`PREPARING`) 상태로 전환 |

### 결제 (`payment` 도메인)

| Method | URL | 인증 | 설명 |
| --- | --- | --- | --- |
| POST | `/api/v1/payments/confirm` | 필요 | PG 결제 승인 처리(금액 위변조 검증 포함) |
| GET | `/api/v1/payments/orders/{orderId}` | 필요 | 주문별 결제 내역 조회 |

### 배송 (`delivery` 도메인)

| Method | URL | 인증 | 설명 |
| --- | --- | --- | --- |
| GET | `/api/v1/orders/{orderId}/delivery` | 필요 | 배송 조회 |
| POST | `/api/v1/orders/{orderId}/delivery/return-request` | 필요 | 반품 요청 |
| POST | `/api/v1/admin/orders/{orderId}/delivery` | ADMIN | 배송(송장) 등록 |

### 리뷰 (`review` 도메인)

| Method | URL | 인증 | 설명 |
| --- | --- | --- | --- |
| POST | `/api/v1/reviews` | 필요 | 리뷰 작성 (배송완료 주문상품만 가능) |
| GET | `/api/v1/reviews` | 필요 | 내 리뷰 목록 |
| PUT | `/api/v1/reviews/{reviewId}` | 필요 | 내 리뷰 수정 |
| DELETE | `/api/v1/reviews/{reviewId}` | 필요 | 내 리뷰 삭제 |
| GET | `/api/v1/products/{productId}/reviews` | - | 상품별 리뷰 목록(공개) |

### 1:1 문의 (`inquiry` 도메인)

| Method | URL | 인증 | 설명 |
| --- | --- | --- | --- |
| POST | `/api/v1/inquiries` | 필요 | 문의 등록 |
| GET | `/api/v1/inquiries` | 필요 | 내 문의 목록 |
| GET | `/api/v1/inquiries/{inquiryId}` | 필요 | 내 문의 상세 |
| GET | `/api/v1/admin/inquiries?status=` | ADMIN | 전체 문의 목록(상태 필터 가능) |
| POST | `/api/v1/admin/inquiries/{inquiryId}/answer` | ADMIN | 문의 답변 등록 |

### 쿠폰 (`coupon` 도메인)

| Method | URL | 인증 | 설명 |
| --- | --- | --- | --- |
| GET | `/api/v1/coupons` | 필요 | 현재 발급 가능한(유효기간 내) 쿠폰 목록 |
| POST | `/api/v1/coupons/{couponId}/issue` | 필요 | 쿠폰 발급받기 (중복 발급 불가) |
| GET | `/api/v1/coupons/my` | 필요 | 내가 보유한 쿠폰 목록 |
| POST | `/api/v1/admin/coupons` | ADMIN | 쿠폰 생성 |
| GET | `/api/v1/admin/coupons` | ADMIN | 전체 쿠폰 목록 |

---

## 7. 에러 코드 목록

| code | HTTP Status | message |
| --- | --- | --- |
| `DUPLICATE_EMAIL` | 409 | 이미 가입된 이메일입니다. |
| `INVALID_CREDENTIALS` | 401 | 이메일 또는 비밀번호가 올바르지 않습니다. |
| `USER_NOT_FOUND` | 404 | 사용자를 찾을 수 없습니다. |
| `INVALID_TOKEN` | 401 | 유효하지 않은 토큰입니다. |
| `EXPIRED_REFRESH_TOKEN` | 401 | 리프레시 토큰이 만료되었습니다. |
| `NEED_ADDITIONAL_INFO` | 403 | 추가 정보 입력이 필요합니다. |
| `INVALID_REQUEST` | 400 | 잘못된 요청입니다. (`errors`에 필드별 검증 메시지 포함) |
| `PRODUCT_NOT_FOUND` | 404 | 상품을 찾을 수 없습니다. |
| `INSUFFICIENT_STOCK` | 409 | 재고가 부족합니다. |
| `ORDER_NOT_FOUND` | 404 | 주문을 찾을 수 없습니다. |
| `ORDER_ACCESS_DENIED` | 403 | 본인의 주문만 조회/취소할 수 있습니다. |
| `EMPTY_ORDER_ITEMS` | 400 | 주문 항목이 비어 있습니다. |
| `EMPTY_CART` | 400 | 장바구니가 비어 있습니다. |
| `INVALID_ORDER_STATUS_TRANSITION` | 409 | 현재 주문 상태에서는 처리할 수 없습니다. |
| `ORDER_ALREADY_SHIPPED` | 409 | 이미 배송이 시작되어 취소할 수 없습니다. 반품/교환을 이용해주세요. |
| `PAYMENT_NOT_ALLOWED` | 409 | 결제 대기 상태의 주문이 아닙니다. |
| `PAYMENT_ALREADY_PROCESSED` | 409 | 이미 처리된 결제입니다. |
| `PAYMENT_NOT_FOUND` | 404 | 결제 내역을 찾을 수 없습니다. |
| `DELIVERY_NOT_FOUND` | 404 | 배송 정보를 찾을 수 없습니다. |
| `DELIVERY_ALREADY_EXISTS` | 409 | 이미 배송이 등록된 주문입니다. |
| `INVALID_DELIVERY_STATUS_TRANSITION` | 409 | 현재 배송 상태에서는 처리할 수 없습니다. |
| `RETURN_NOT_ALLOWED` | 409 | 배송 시작 전이거나 이미 반품 요청된 주문입니다. |
| `INQUIRY_NOT_FOUND` | 404 | 문의를 찾을 수 없습니다. |
| `INQUIRY_ACCESS_DENIED` | 403 | 본인의 문의만 조회할 수 있습니다. |
| `INQUIRY_ALREADY_ANSWERED` | 409 | 이미 답변이 완료된 문의입니다. |
| `REVIEW_NOT_FOUND` | 404 | 리뷰를 찾을 수 없습니다. |
| `REVIEW_ACCESS_DENIED` | 403 | 본인의 리뷰만 수정/삭제할 수 있습니다. |
| `REVIEW_NOT_ALLOWED` | 409 | 배송 완료된 주문 상품만 리뷰를 작성할 수 있습니다. |
| `DUPLICATE_REVIEW` | 409 | 이미 해당 주문 상품에 대한 리뷰를 작성했습니다. |
| `CART_ITEM_NOT_FOUND` | 404 | 장바구니에서 해당 상품을 찾을 수 없습니다. |
| `DUPLICATE_COUPON_CODE` | 409 | 이미 존재하는 쿠폰 코드입니다. |
| `COUPON_NOT_FOUND` | 404 | 쿠폰을 찾을 수 없습니다. |
| `COUPON_ACCESS_DENIED` | 403 | 본인의 쿠폰만 사용할 수 있습니다. |
| `COUPON_ALREADY_USED` | 409 | 이미 사용되었거나 사용할 수 없는 쿠폰입니다. |
| `COUPON_NOT_IN_VALID_PERIOD` | 409 | 쿠폰 사용 가능 기간이 아닙니다. |
| `COUPON_MIN_ORDER_AMOUNT_NOT_MET` | 409 | 쿠폰 최소 주문 금액을 충족하지 않습니다. |
| `DUPLICATE_COUPON_ISSUE` | 409 | 이미 발급받은 쿠폰입니다. |

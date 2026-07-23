-- ============================================================
-- tamiOjok B2C E-Commerce 더미(테스트) 데이터
--
-- schema.sql 실행 후 이 스크립트를 실행하세요.
-- 모든 로컬 계정의 비밀번호는 "Password1!" 입니다.
-- (BCrypt로 미리 해시된 값을 그대로 넣었습니다.)
--
-- 실행 예)
--   psql -h localhost -U postgres -d ecommerce -f db/schema.sql
--   psql -h localhost -U postgres -d ecommerce -f db/seed.sql
-- ============================================================

BEGIN;

-- ============================================================
-- 1. Users & Addresses
-- ============================================================

INSERT INTO users (user_id, email, password, name, phone_number, provider, provider_id, role, status, created_at) VALUES
    (1, 'admin@tamiojok.com', '$2b$10$DS4b3o3H4x9s8Ket4xljIOn.5L/Xo0lfuUC6Loh8LjWm..SWJUDke', '관리자',   '010-0000-0000', 'LOCAL', NULL,               'ADMIN', 'ACTIVE',    now() - interval '90 days'),
    (2, 'hong@example.com',   '$2b$10$DS4b3o3H4x9s8Ket4xljIOn.5L/Xo0lfuUC6Loh8LjWm..SWJUDke', '홍길동',   '010-1111-2222', 'LOCAL', NULL,               'USER',  'ACTIVE',    now() - interval '60 days'),
    (3, 'kim@example.com',    '$2b$10$DS4b3o3H4x9s8Ket4xljIOn.5L/Xo0lfuUC6Loh8LjWm..SWJUDke', '김민준',   '010-2222-3333', 'LOCAL', NULL,               'USER',  'ACTIVE',    now() - interval '45 days'),
    (4, 'lee@example.com',    '$2b$10$DS4b3o3H4x9s8Ket4xljIOn.5L/Xo0lfuUC6Loh8LjWm..SWJUDke', '이서연',   '010-3333-4444', 'LOCAL', NULL,               'USER',  'ACTIVE',    now() - interval '30 days'),
    (5, 'park@example.com',   NULL,                                                            '박지훈',   NULL,            'KAKAO', 'kakao_1029384',   'USER',  'NEED_INFO', now() - interval '1 days');
-- park@example.com 은 카카오 OAuth로 첫 로그인만 한 상태(추가정보 미입력)를 가정합니다.

SELECT setval('users_user_id_seq', (SELECT MAX(user_id) FROM users));

INSERT INTO user_addresses (address_id, user_id, recipient_name, recipient_phone, zipcode, address_main, address_detail, is_default) VALUES
    (1, 2, '홍길동', '010-1111-2222', '06236', '서울특별시 강남구 테헤란로 123', '101동 1001호', TRUE),
    (2, 2, '홍길동', '010-1111-2222', '13529', '경기도 성남시 분당구 판교역로 456', '회사 3층', FALSE),
    (3, 3, '김민준', '010-2222-3333', '48058', '부산광역시 해운대구 센텀중앙로 45', '202동 305호', TRUE),
    (4, 4, '이서연', '010-3333-4444', '34141', '대전광역시 유성구 대학로 99', NULL, TRUE);

SELECT setval('user_addresses_address_id_seq', (SELECT MAX(address_id) FROM user_addresses));

-- ============================================================
-- 2. Products & Stock
-- ============================================================

INSERT INTO products (product_id, name, price, description, status, created_at) VALUES
    (1, '무선 블루투스 이어폰',      89000,  '노이즈 캔슬링을 지원하는 무선 이어폰입니다.',            'ON_SALE',      now() - interval '80 days'),
    (2, '기계식 키보드 (적축)',      129000, '타건감이 우수한 87키 기계식 키보드입니다.',              'ON_SALE',      now() - interval '80 days'),
    (3, '스마트워치 밴드',           45000,  '다양한 색상으로 교체 가능한 스마트워치 밴드입니다.',      'ON_SALE',      now() - interval '75 days'),
    (4, '스테인리스 텀블러 500ml',   19800,  '보온·보냉이 모두 가능한 스테인리스 텀블러입니다.',        'ON_SALE',      now() - interval '70 days'),
    (5, '노트북 파우치 15인치',      32000,  '15인치 노트북까지 수납 가능한 파우치입니다.',            'ON_SALE',      now() - interval '65 days'),
    (6, '데스크 매트 XL',            27000,  '책상 전체를 덮는 초대형 데스크 매트입니다.',              'OUT_OF_STOCK', now() - interval '60 days'),
    (7, '블루투스 스피커',           59000,  '휴대용 방수 블루투스 스피커입니다.',                      'ON_SALE',      now() - interval '55 days'),
    (8, '캠핑 접이식 의자',          68000,  '단종 예정 상품으로 더 이상 신규 노출되지 않습니다.',      'HIDDEN',       now() - interval '50 days');

SELECT setval('products_product_id_seq', (SELECT MAX(product_id) FROM products));

-- reserved_quantity: 진행 중인 PENDING_PAYMENT 주문(order_id=2, product 3, 수량 2)만큼만 예약되어 있습니다.
INSERT INTO product_stocks (product_id, stock_quantity, reserved_quantity, updated_at) VALUES
    (1, 149, 0, now() - interval '7 days'),
    (2, 79,  0, now() - interval '5 days'),
    (3, 199, 2, now() - interval '1 days'),
    (4, 298, 0, now() - interval '7 days'),
    (5, 119, 0, now() - interval '5 days'),
    (6, 0,   0, now() - interval '20 days'),
    (7, 59,  0, now() - interval '12 days'),
    (8, 15,  0, now() - interval '50 days');

-- ============================================================
-- 3. Coupons
-- ============================================================

INSERT INTO coupons (coupon_id, code, name, discount_type, discount_value, min_order_amount, max_discount_amount, valid_from, valid_until, created_at) VALUES
    (1, 'WELCOME10',   '신규가입 10% 할인 쿠폰',   'PERCENTAGE',   10,   30000,  5000,  now() - interval '60 days', now() + interval '30 days', now() - interval '60 days'),
    (2, 'FLAT5000',    '5,000원 즉시 할인 쿠폰',   'FIXED_AMOUNT', 5000, 50000,  NULL,  now() - interval '60 days', now() + interval '5 days',  now() - interval '60 days'),
    (3, 'SUMMER20',    '여름 특가 20% 할인 쿠폰',  'PERCENTAGE',   20,   100000, 20000, now() - interval '20 days', now() + interval '20 days', now() - interval '20 days'),
    (4, 'EXPIRED2024', '종료된 프로모션 쿠폰',     'FIXED_AMOUNT', 3000, 10000,  NULL,  now() - interval '90 days', now() - interval '30 days', now() - interval '90 days');
-- EXPIRED2024 는 유효기간이 지나 더 이상 발급받을 수 없는 쿠폰의 예시입니다 (COUPON_NOT_IN_VALID_PERIOD).

SELECT setval('coupons_coupon_id_seq', (SELECT MAX(coupon_id) FROM coupons));

-- ============================================================
-- 4. Orders / Items / Payments / Deliveries
-- ============================================================

-- Order 1: 홍길동, 결제완료 + 배송완료 + 쿠폰(FLAT5000) 사용, 리뷰 작성 완료
INSERT INTO orders (order_id, order_number, user_id, total_amount, discount_amount, payment_amount, status, created_at) VALUES
    (1, 'ORD2024000000000001', 2, 128600, 5000, 123600, 'PREPARING', now() - interval '7 days');

INSERT INTO order_items (order_item_id, order_id, product_id, order_price, quantity) VALUES
    (1, 1, 1, 89000, 1),
    (2, 1, 4, 19800, 2);

INSERT INTO payments (payment_id, order_id, pg_provider, pg_transaction_id, requested_amount, paid_amount, status, paid_at) VALUES
    (1, 1, 'MOCK_PG', 'MOCKPG-0001', 123600, 123600, 'PAID', now() - interval '6 days 20 hours');

INSERT INTO deliveries (delivery_id, order_id, courier_code, tracking_number, status, shipped_at, delivered_at) VALUES
    (1, 1, 'CJ', '1234567890', 'DELIVERED', now() - interval '5 days', now() - interval '3 days');

-- Order 2: 김민준, 결제 대기 중 (재고는 예약(reserved)만 된 상태)
INSERT INTO orders (order_id, order_number, user_id, total_amount, discount_amount, payment_amount, status, created_at) VALUES
    (2, 'ORD2024000000000002', 3, 90000, 0, 90000, 'PENDING_PAYMENT', now() - interval '1 days');

INSERT INTO order_items (order_item_id, order_id, product_id, order_price, quantity) VALUES
    (3, 2, 3, 45000, 2);

-- Order 3: 홍길동, 결제 전 주문 취소
INSERT INTO orders (order_id, order_number, user_id, total_amount, discount_amount, payment_amount, status, created_at) VALUES
    (3, 'ORD2024000000000003', 2, 59000, 0, 59000, 'CANCELLED', now() - interval '10 days');

INSERT INTO order_items (order_item_id, order_id, product_id, order_price, quantity) VALUES
    (4, 3, 7, 59000, 1);

-- Order 4: 이서연, 결제완료 + 배송중 + 쿠폰(SUMMER20) 사용
INSERT INTO orders (order_id, order_number, user_id, total_amount, discount_amount, payment_amount, status, created_at) VALUES
    (4, 'ORD2024000000000004', 4, 161000, 20000, 141000, 'PREPARING', now() - interval '5 days');

INSERT INTO order_items (order_item_id, order_id, product_id, order_price, quantity) VALUES
    (5, 4, 2, 129000, 1),
    (6, 4, 5, 32000,  1);

INSERT INTO payments (payment_id, order_id, pg_provider, pg_transaction_id, requested_amount, paid_amount, status, paid_at) VALUES
    (2, 4, 'MOCK_PG', 'MOCKPG-0002', 141000, 141000, 'PAID', now() - interval '4 days');

INSERT INTO deliveries (delivery_id, order_id, courier_code, tracking_number, status, shipped_at, delivered_at) VALUES
    (2, 4, 'LOTTE', '9988776655', 'IN_TRANSIT', now() - interval '2 days', NULL);

-- Order 5: 김민준, 배송완료 후 반품 요청됨
INSERT INTO orders (order_id, order_number, user_id, total_amount, discount_amount, payment_amount, status, created_at) VALUES
    (5, 'ORD2024000000000005', 3, 59000, 0, 59000, 'PREPARING', now() - interval '12 days');

INSERT INTO order_items (order_item_id, order_id, product_id, order_price, quantity) VALUES
    (7, 5, 7, 59000, 1);

INSERT INTO payments (payment_id, order_id, pg_provider, pg_transaction_id, requested_amount, paid_amount, status, paid_at) VALUES
    (3, 5, 'MOCK_PG', 'MOCKPG-0003', 59000, 59000, 'PAID', now() - interval '11 days');

INSERT INTO deliveries (delivery_id, order_id, courier_code, tracking_number, status, shipped_at, delivered_at) VALUES
    (3, 5, 'HANJIN', '5566778899', 'RETURN_REQUESTED', now() - interval '10 days', now() - interval '8 days');

-- Order 6: 홍길동, 결제만 완료되고 아직 배송 준비 전
INSERT INTO orders (order_id, order_number, user_id, total_amount, discount_amount, payment_amount, status, created_at) VALUES
    (6, 'ORD2024000000000006', 2, 45000, 0, 45000, 'PAYMENT_COMPLETED', now() - interval '2 days');

INSERT INTO order_items (order_item_id, order_id, product_id, order_price, quantity) VALUES
    (8, 6, 3, 45000, 1);

INSERT INTO payments (payment_id, order_id, pg_provider, pg_transaction_id, requested_amount, paid_amount, status, paid_at) VALUES
    (4, 6, 'MOCK_PG', 'MOCKPG-0004', 45000, 45000, 'PAID', now() - interval '1 days');

SELECT setval('orders_order_id_seq', (SELECT MAX(order_id) FROM orders));
SELECT setval('order_items_order_item_id_seq', (SELECT MAX(order_item_id) FROM order_items));
SELECT setval('payments_payment_id_seq', (SELECT MAX(payment_id) FROM payments));
SELECT setval('deliveries_delivery_id_seq', (SELECT MAX(delivery_id) FROM deliveries));

-- ============================================================
-- 5. User Coupons (발급/사용 내역)
-- ============================================================

INSERT INTO user_coupons (user_coupon_id, user_id, coupon_id, order_id, status, used_at, created_at) VALUES
    (1, 2, 1, NULL, 'AVAILABLE', NULL,                        now() - interval '15 days'),
    (2, 2, 2, 1,    'USED',      now() - interval '7 days',   now() - interval '20 days'),
    (3, 3, 1, NULL, 'AVAILABLE', NULL,                        now() - interval '10 days'),
    (4, 4, 3, 4,    'USED',      now() - interval '5 days',   now() - interval '10 days');

SELECT setval('user_coupons_user_coupon_id_seq', (SELECT MAX(user_coupon_id) FROM user_coupons));

-- ============================================================
-- 6. Reviews (배송완료된 Order 1에 대한 리뷰)
-- ============================================================

INSERT INTO reviews (review_id, user_id, order_id, product_id, rating, content, created_at) VALUES
    (1, 2, 1, 1, 5, '음질도 좋고 배터리도 오래가서 만족스럽습니다!', now() - interval '2 days'),
    (2, 2, 1, 4, 4, '가볍고 튼튼해서 매일 들고 다니기 좋아요.',        now() - interval '2 days');

SELECT setval('reviews_review_id_seq', (SELECT MAX(review_id) FROM reviews));

-- ============================================================
-- 7. Inquiries (1:1 문의)
-- ============================================================

INSERT INTO inquiries (inquiry_id, user_id, order_id, category, title, content, answer, status, created_at, answered_at) VALUES
    (1, 2, 1,    '배송',   '배송이 너무 늦어요',   '주문한지 며칠이 지났는데 아직도 배송준비중입니다.', '불편을 드려 죄송합니다. 확인 결과 오늘 발송 처리되었습니다.', 'ANSWERED', now() - interval '6 days', now() - interval '5 days'),
    (2, 3, 2,    '결제',   '결제가 안돼요',        '카드 결제를 시도했는데 계속 실패합니다.',           NULL,                                                            'WAITING',  now() - interval '1 days', NULL),
    (3, 4, NULL, '상품문의', '재입고 문의',          '데스크 매트 XL 재입고는 언제쯤 될까요?',            '다음 주 중 재입고 예정입니다.',                                'ANSWERED', now() - interval '4 days', now() - interval '3 days');

SELECT setval('inquiries_inquiry_id_seq', (SELECT MAX(inquiry_id) FROM inquiries));

-- ============================================================
-- 8. Cart (아직 주문하지 않은 장바구니)
-- ============================================================

INSERT INTO carts (cart_id, user_id) VALUES
    (1, 3);

SELECT setval('carts_cart_id_seq', (SELECT MAX(cart_id) FROM carts));

INSERT INTO cart_items (cart_item_id, cart_id, product_id, quantity) VALUES
    (1, 1, 1, 1),
    (2, 1, 7, 2);

SELECT setval('cart_items_cart_item_id_seq', (SELECT MAX(cart_item_id) FROM cart_items));

COMMIT;

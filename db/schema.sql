-- ============================================================
-- tamiOjok B2C E-Commerce Database Schema (PostgreSQL)
--
-- 이 스크립트는 JPA 엔티티(src/main/java/.../domain/**/entity)를
-- 기준으로 작성된 참고용 DDL입니다.
-- 애플리케이션은 spring.jpa.hibernate.ddl-auto=update 로 스키마를
-- 자동 생성/갱신하므로 운영 환경에서는 이 파일을 직접 실행할 필요가
-- 없지만, 로컬에 DB만 먼저 세팅하거나 스키마를 검토할 때 사용합니다.
--
-- 실행 순서: schema.sql -> seed.sql
-- ============================================================

-- 기존 테이블을 정리하고 새로 만들고 싶을 때 사용 (주의: 전체 삭제)
-- DROP TABLE IF EXISTS user_coupons, coupons, cart_items, carts, reviews,
--     inquiries, deliveries, payments, order_items, orders,
--     product_stocks, products, user_addresses, users CASCADE;

-- ============================================================
-- 1. Auth & Users
-- ============================================================

CREATE TABLE users (
    user_id         BIGSERIAL PRIMARY KEY,
    email           VARCHAR(255) NOT NULL UNIQUE,
    password        VARCHAR(255),                          -- OAuth 가입자는 NULL
    name            VARCHAR(100) NOT NULL,
    phone_number    VARCHAR(30),
    provider        VARCHAR(20)  NOT NULL,                  -- LOCAL, KAKAO, NAVER, GOOGLE
    provider_id     VARCHAR(255),
    role            VARCHAR(20)  NOT NULL,                  -- USER, ADMIN
    status          VARCHAR(20)  NOT NULL,                  -- ACTIVE, NEED_INFO, SUSPENDED
    refresh_token   VARCHAR(500),
    created_at      TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE TABLE user_addresses (
    address_id      BIGSERIAL PRIMARY KEY,
    user_id         BIGINT       NOT NULL REFERENCES users (user_id),
    recipient_name  VARCHAR(100) NOT NULL,
    recipient_phone VARCHAR(30)  NOT NULL,
    zipcode         VARCHAR(20)  NOT NULL,
    address_main    VARCHAR(255) NOT NULL,
    address_detail  VARCHAR(255),
    is_default      BOOLEAN      NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_user_addresses_user_id ON user_addresses (user_id);

-- ============================================================
-- 2. Products & Stock
-- ============================================================

CREATE TABLE products (
    product_id      BIGSERIAL PRIMARY KEY,
    name            VARCHAR(255)   NOT NULL,
    price           NUMERIC(12, 2) NOT NULL,
    description     TEXT,
    status          VARCHAR(20)    NOT NULL,                -- ON_SALE, OUT_OF_STOCK, HIDDEN
    created_at      TIMESTAMP      NOT NULL DEFAULT now()
);

CREATE INDEX idx_products_status ON products (status);

-- product_stocks 는 products 와 1:1 (PK 공유, @MapsId)
CREATE TABLE product_stocks (
    product_id        BIGINT  PRIMARY KEY REFERENCES products (product_id),
    stock_quantity    INTEGER NOT NULL,
    reserved_quantity INTEGER NOT NULL DEFAULT 0,
    updated_at        TIMESTAMP
);

-- ============================================================
-- 3. Orders & Items
-- ============================================================

CREATE TABLE orders (
    order_id        BIGSERIAL PRIMARY KEY,
    order_number    VARCHAR(50)    NOT NULL UNIQUE,
    user_id         BIGINT         NOT NULL REFERENCES users (user_id),
    total_amount    NUMERIC(12, 2) NOT NULL,
    discount_amount NUMERIC(12, 2) NOT NULL DEFAULT 0,
    payment_amount  NUMERIC(12, 2) NOT NULL,
    status          VARCHAR(30)    NOT NULL,                -- PENDING_PAYMENT, PAYMENT_COMPLETED, PREPARING, CANCELLED
    created_at      TIMESTAMP      NOT NULL DEFAULT now()
);

CREATE INDEX idx_orders_user_id ON orders (user_id);
CREATE INDEX idx_orders_status_created_at ON orders (status, created_at);

CREATE TABLE order_items (
    order_item_id BIGSERIAL PRIMARY KEY,
    order_id      BIGINT         NOT NULL REFERENCES orders (order_id),
    product_id    BIGINT         NOT NULL REFERENCES products (product_id),
    order_price   NUMERIC(12, 2) NOT NULL,
    quantity      INTEGER        NOT NULL
);

CREATE INDEX idx_order_items_order_id ON order_items (order_id);

-- ============================================================
-- 4. Payments & Deliveries
-- ============================================================

CREATE TABLE payments (
    payment_id         BIGSERIAL PRIMARY KEY,
    order_id           BIGINT         NOT NULL UNIQUE REFERENCES orders (order_id),
    pg_provider        VARCHAR(50)    NOT NULL,
    pg_transaction_id  VARCHAR(255)   NOT NULL,
    requested_amount   NUMERIC(12, 2) NOT NULL,
    paid_amount        NUMERIC(12, 2),
    status             VARCHAR(20)    NOT NULL,             -- READY, PAID, FAILED, CANCELLED
    paid_at            TIMESTAMP
);

CREATE TABLE deliveries (
    delivery_id      BIGSERIAL PRIMARY KEY,
    order_id         BIGINT      NOT NULL UNIQUE REFERENCES orders (order_id),
    courier_code     VARCHAR(50) NOT NULL,
    tracking_number  VARCHAR(100) NOT NULL,
    status           VARCHAR(30) NOT NULL,                  -- READY, SHIPPED, IN_TRANSIT, DELIVERED, RETURN_REQUESTED
    shipped_at       TIMESTAMP,
    delivered_at     TIMESTAMP
);

-- ============================================================
-- 5. CS & Inquiries
-- ============================================================

CREATE TABLE inquiries (
    inquiry_id   BIGSERIAL PRIMARY KEY,
    user_id      BIGINT      NOT NULL REFERENCES users (user_id),
    order_id     BIGINT      REFERENCES orders (order_id),
    category     VARCHAR(50) NOT NULL,
    title        VARCHAR(255) NOT NULL,
    content      TEXT        NOT NULL,
    answer       TEXT,
    status       VARCHAR(20) NOT NULL,                      -- WAITING, ANSWERED
    created_at   TIMESTAMP   NOT NULL DEFAULT now(),
    answered_at  TIMESTAMP
);

CREATE INDEX idx_inquiries_user_id ON inquiries (user_id);

-- ============================================================
-- 6. Reviews
-- ============================================================

CREATE TABLE reviews (
    review_id   BIGSERIAL PRIMARY KEY,
    user_id     BIGINT    NOT NULL REFERENCES users (user_id),
    order_id    BIGINT    NOT NULL REFERENCES orders (order_id),
    product_id  BIGINT    NOT NULL REFERENCES products (product_id),
    rating      INTEGER   NOT NULL CHECK (rating BETWEEN 1 AND 5),
    content     TEXT      NOT NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uq_reviews_order_product ON reviews (order_id, product_id);
CREATE INDEX idx_reviews_product_id ON reviews (product_id);

-- ============================================================
-- 7. Cart
-- ============================================================

CREATE TABLE carts (
    cart_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE REFERENCES users (user_id)
);

CREATE TABLE cart_items (
    cart_item_id BIGSERIAL PRIMARY KEY,
    cart_id      BIGINT  NOT NULL REFERENCES carts (cart_id),
    product_id   BIGINT  NOT NULL REFERENCES products (product_id),
    quantity     INTEGER NOT NULL
);

CREATE INDEX idx_cart_items_cart_id ON cart_items (cart_id);

-- ============================================================
-- 8. Coupons
-- ============================================================

CREATE TABLE coupons (
    coupon_id           BIGSERIAL PRIMARY KEY,
    code                VARCHAR(50)    NOT NULL UNIQUE,
    name                VARCHAR(255)   NOT NULL,
    discount_type       VARCHAR(20)    NOT NULL,            -- FIXED_AMOUNT, PERCENTAGE
    discount_value      NUMERIC(12, 2) NOT NULL,
    min_order_amount    NUMERIC(12, 2) NOT NULL DEFAULT 0,
    max_discount_amount NUMERIC(12, 2),
    valid_from          TIMESTAMP      NOT NULL,
    valid_until         TIMESTAMP      NOT NULL,
    created_at          TIMESTAMP      NOT NULL DEFAULT now()
);

CREATE TABLE user_coupons (
    user_coupon_id BIGSERIAL PRIMARY KEY,
    user_id        BIGINT      NOT NULL REFERENCES users (user_id),
    coupon_id      BIGINT      NOT NULL REFERENCES coupons (coupon_id),
    order_id       BIGINT      REFERENCES orders (order_id),
    status         VARCHAR(20) NOT NULL,                    -- AVAILABLE, USED
    used_at        TIMESTAMP,
    created_at     TIMESTAMP   NOT NULL DEFAULT now()
);

CREATE INDEX idx_user_coupons_user_id ON user_coupons (user_id);
CREATE UNIQUE INDEX uq_user_coupons_user_coupon ON user_coupons (user_id, coupon_id);

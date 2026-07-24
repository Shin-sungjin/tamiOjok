-- ============================================================
-- 상품 이미지 (목록/상세 갤러리용)
-- ============================================================

CREATE TABLE product_images (
    product_image_id BIGSERIAL PRIMARY KEY,
    product_id        BIGINT NOT NULL REFERENCES products (product_id),
    image_url         VARCHAR(1000) NOT NULL,
    sort_order         INT NOT NULL DEFAULT 0
);

CREATE INDEX idx_product_images_product_id ON product_images (product_id, sort_order);

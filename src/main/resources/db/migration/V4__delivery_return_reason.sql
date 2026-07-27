-- ============================================================
-- 반품 요청 사유 (선택 사유 + 상세 설명)
-- ============================================================

ALTER TABLE deliveries
    ADD COLUMN return_reason VARCHAR(30),
    ADD COLUMN return_detail TEXT;

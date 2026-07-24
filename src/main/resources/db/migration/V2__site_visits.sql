-- ============================================================
-- 관리자 대시보드용 방문자 추적 테이블
-- ============================================================

CREATE TABLE site_visits (
    site_visit_id   BIGSERIAL PRIMARY KEY,
    created_at      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_site_visits_created_at ON site_visits (created_at);

# 디자인 & 성능 고도화 로드맵

마지막 갱신: 2026-07-27

## 0. 진행 방식

- 디자인 방향: 기존 톤(다크 헤더 + 골드 포인트, `frontend/src/index.css`의 디자인 토큰) 유지, 새 스타일로 갈아엎지 않고 **완성도**(일관성/반응형/빈 상태/접근성)를 올리는 데 집중.
- 성능은 "측정 없이 감으로 최적화"하지 않기 위해, 항목마다 근거(코드에서 확인한 사실)를 남김.
- 이 문서는 백로그다. 우선순위(P0/P1/P2)는 참고용이고, 실제 착수 순서는 세션마다 골라서 진행하면 됨 — `hist.log`에 진행 내역을 계속 추가.

## 1. 현재 상태 진단 (2026-07-27 기준 코드 확인)

**디자인**
- `index.css`에 CSS 커스텀 프로퍼티 기반 디자인 토큰(색상/반경/그림자/여백)이 이미 있고, `clamp()`로 반응형 타이포/여백 처리가 대체로 잘 되어 있음.
- 로딩 상태가 전부 `<p>불러오는 중...</p>` 텍스트 한 줄 — 스켈레톤/스피너 없음.
- 리스트 페이지들의 빈 상태(검색결과 없음 등)는 텍스트 한 줄 수준으로 최소한만 구현됨.
- `:focus-visible` 등 키보드 포커스 스타일이 별도로 정의되어 있지 않음 (브라우저 기본값 의존).
- 파비콘이 Vite 기본 아이콘(`/vite.svg`)으로, 다크+골드 브랜딩과 맞지 않음.
- `body`의 `font-family`가 `'Pretendard'`를 1순위로 지정하지만 실제로 로드하는 `@font-face`/웹폰트 링크가 없어 항상 시스템 폰트로 폴백됨 (버그는 아니지만 의도한 폰트가 아님).

**성능**
- 라우트별 코드 스플리팅이 전혀 없어 관리자 페이지 코드까지 전부 한 번에 로드되고 있었음 → **오늘 조치함** (아래 2번 참고).
- 백엔드 응답 gzip 압축 미설정 → **오늘 조치함**.
- 상품 목록/찜목록 이미지는 `loading="lazy"` 적용되어 있었으나 상세페이지 썸네일·관리자 화면 이미지는 누락 → **오늘 조치함**.
- 상품 목록 API는 페이지당 12개로 이미 페이지네이션됨 (전체 로드 아님, 문제 없음).
- 평균 별점/리뷰수는 배치 집계 쿼리(`findRatingStatsByProductIds`)로 N+1 방지 처리가 이미 되어 있음 (PR #18에서 처리).
- 정적 자산 캐시(`nginx.conf`)는 `/assets/`만 immutable, 나머지는 no-cache로 이미 분리되어 있음 (PR #17).
- Lighthouse 등으로 실측한 적은 아직 없음 — 아래 로드맵의 실측 항목이 우선순위 판단의 다음 단계.

## 2. 오늘 완료한 항목

- [x] 프론트 라우트 코드 스플리팅: `App.tsx`의 페이지 컴포넌트를 `React.lazy` + `Suspense`로 전환 (첫 진입 페이지인 `ProductListPage`만 즉시 로드, 나머지는 라우트 진입 시 청크 로드). 관리자 페이지 6개가 일반 사용자 번들에서 완전히 분리됨.
  - 검증: Docker `node:20-slim`에서 `tsc -b && vite build` 통과. 메인 번들 228KB(gzip 76.5KB) + 페이지별 0.3~6KB 청크로 분리 확인.
- [x] 백엔드 gzip 압축 활성화 (`application.properties`: `server.compression.enabled=true`, JSON/HTML/CSS/JS 대상, 1KB 이상 응답).
- [x] 누락된 `loading="lazy"` 보강: 상품 상세 썸네일, 관리자 상품 등록/수정 썸네일, 관리자 상품 목록 테이블 썸네일. (메인 이미지는 LCP 후보라 의도적으로 eager 유지)
- [x] `index.html`에 `meta description`, `theme-color` 추가.
- 검증: `gradlew compileJava compileTestJava` 통과, 프론트 `tsc -b && vite build` 통과.

## 3. Lighthouse 베이스라인 실측 (2026-07-27)

Docker로 프로덕션 빌드(`docker compose up -d --build db backend frontend`)를 띄우고, `alpine/socat`으로 frontend 컨테이너를 호스트에 임시 릴레이(`localhost:18090`)한 뒤 `justinribeiro/lighthouse` 컨테이너로 측정. 체크아웃 페이지는 로그인 필요(액세스 토큰이 메모리에만 있고 저장소에 없어 스크립트로 로그인시키지 않으면 로그인 화면으로 리다이렉트됨) → 이번엔 제외, 대신 로그인 페이지로 대체.

| 페이지 | Performance | Accessibility | Best Practices | SEO | LCP | TBT | CLS |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 상품 목록 (`/`) | 95 | 98 | 93 | 89 | 2.5s | 10ms | 0 |
| 상품 상세 (`/products/1`) | 92 | 97 | 93 | 90 | 3.0s | 0ms | 0 |
| 로그인 (`/login`) | 96 | 97 | 93 | 90 | 2.3s | 0ms | 0 |

전반적으로 점수 자체는 이미 준수한 편(오늘 오전 코드 스플리팅/gzip 조치 이후 기준). 다만 감사 도중 아래 4-1 항목(특히 CORS 이슈)을 발견함 — 우선순위 낮은 점수 항목 튜닝보다 먼저 처리할 가치가 있음.

## 4. 다음 단계 백로그

### P0 — 다음 세션에 바로 착수 가능 (실측/재현으로 확인된 것 우선)

| 항목 | 내용 | 비고 |
| --- | --- | --- |
| nginx 정적 자산(JS/CSS) gzip 미적용 | 4-1 참고, 152KB(JS)+16KB(CSS) 절감 가능 | 오늘 백엔드 API는 압축했지만 정적 파일은 nginx가 서빙 — nginx.conf에 `gzip on` 필요 |
| 로딩 스켈레톤 | 상품 카드/상세/주문목록 등 주요 리스트에 텍스트 대신 스켈레톤 UI | 체감 성능(perceived performance) 개선, 디자인 완성도 항목이기도 함 |
| `:focus-visible` 스타일 정의 | 버튼/링크/인풋에 브랜드 컬러(`--color-accent`) 기반 포커스 링 추가 | 접근성, 구현 난이도 낮음 |
| 찜하기 버튼 tap-target 겹침 | `.wishlist-btn`(32x32)이 카드 링크(`<a>`)와 겹쳐 Lighthouse가 "부적절한 탭 타겟"으로 지적 | 모바일 터치 정확도 문제, 실제 UX에도 영향 |
| 상품 카드 텍스트 명도 대비 부족 | `.product-card__image-placeholder`(3.77:1), `.rating-stars__count`(4.15:1) — WCAG AA 기준(4.5:1) 미달 | `--color-text-muted` 톤 조정 또는 해당 요소만 진하게 |

### P1 — 조사/의사결정 후 진행

| 항목 | 내용 | 비고 |
| --- | --- | --- |
| 이미지 최적화 파이프라인 | 상품 이미지 WebP 변환 + 리사이즈 (현재 원본 URL 그대로 사용) | 이미지 저장 방식(로컬/S3 등) 먼저 확인 필요 |
| 브랜드 파비콘/아이콘 세트 | 🐷 이모지 로고 톤에 맞는 실제 아이콘 에셋 필요 | 디자인 에셋 제작 필요 — 방향 확정 후 진행 |
| 빈 상태(empty state) 보강 | "검색 결과가 없습니다" 등에 아이콘/CTA 추가 | 페이지별로 텍스트만 있는 곳 목록화 필요 |
| API 응답 캐싱(React Query 등) | 같은 데이터 반복 요청 여부 확인 후 도입 검토 | 현재 각 페이지가 개별 `useEffect` fetch — 중복 호출 여부 실측 먼저 |

### P2 — 우선순위 낮음 / 근거 보강 필요

| 항목 | 내용 | 비고 |
| --- | --- | --- |
| 관리자 테이블 가상화 | 상품/주문 목록이 커질 경우 `react-window` 등 검토 | 현재 데이터 규모에선 불필요, 실제 운영 데이터 늘어난 뒤 재평가 |
| 웹폰트(Pretendard) 실제 로드 | 서브셋 + `font-display: swap`으로 자체 호스팅 | 브랜드 폰트 도입은 성능 비용(다운로드)이 있어 우선순위 낮게 책정 — 디자인 쪽에서 필요성 재확인 필요 |
| DB 인덱스 점검 | 상품 검색(`nameContainingIgnoreCase`)이 데이터 늘어나면 풀스캔 가능성 | 현재 시드 데이터 규모에선 문제 없음, 운영 데이터 기준 재점검 |

## 4-1. 발견된 이슈 상세

### ✅ (해결됨) CORS 화이트리스트 불일치 시 모든 POST 요청 403 (배포 안정성)

Lighthouse 감사 중 `/api/v1/track/visit`(공개 엔드포인트, `permitAll`)이 403을 반환하는 걸
발견 — 최초엔 Lighthouse 컨테이너의 특이 동작인 줄 알았으나, `claude-in-chrome`으로 실제
브라우저에서 재현하고 curl로 원인을 좁혀서 확인함.

**원인**: 브라우저는 same-origin POST 요청에도 `Origin` 헤더를 항상 붙이는데, Spring
Security의 CORS 필터는 `Origin` 헤더가 존재하면 same-origin 여부와 무관하게
`app.cors.allowed-origins`(`CORS_ALLOWED_ORIGINS`) 화이트리스트와 대조해서, 없으면
403으로 막아버림. `curl`은 `Origin` 헤더를 안 보내므로 이 문제를 재현하지 못했음
(그래서 처음엔 정상으로 보였음).

```
Origin 헤더 없음(curl)                    → 204 (통과)
Origin: http://localhost:18090 (화이트리스트에 없음) → 403
Origin: http://localhost:5173  (화이트리스트에 있음) → 204 (통과)
```

**왜 지금 안 걸리는가 / 언제 터지는가**: 현재 `.env`의 `CORS_ALLOWED_ORIGINS`에는
`http://localhost:5173`(vite dev)과 당시 Cloudflare Quick Tunnel URL이 등록돼 있어서
그 두 경로로 접속하면 문제없이 동작함. 하지만 `infra_setup.md`/`hist.log`에 이미 기록된
대로 Quick Tunnel URL은 컨테이너 재기동마다 바뀜 — URL이 바뀐 뒤 `.env`를 안 고치고
그대로 쓰면, nginx가 프론트/API를 같은 origin으로 프록시해주는 구조임에도 불구하고
**로그인, 주문 생성, 장바구니 담기 등 모든 POST/PUT/DELETE 요청이 조용히 403으로
막힘** (에러 메시지가 사용자에게 명확히 안 보일 수 있어 "그냥 안 되는 사이트"로
보일 위험).

**적용한 해결책 (2026-07-27, 사용자 확인 후 진행)**: "nginx가 프론트+API를 같은
origin으로 묶어주는 구조이므로 same-origin 요청은 화이트리스트 없이도 항상 허용하고,
정말 다른 origin(vite dev 서버, 악성 사이트 등)만 화이트리스트로 판단한다"는 방향
(문서에 적어뒀던 두 방향 중 "방향 B: 동적 Origin 검증")으로 근본 해결함.

- `OriginAwareCorsConfigurationSource`(신규, `global/security/`) — 정적 화이트리스트
  비교 대신, 요청 자체의 Host(`request.getServerName()`/`getServerPort()`)와 `Origin`
  헤더가 일치하면 same-origin으로 간주해 허용. 일치하지 않으면 기존처럼
  `app.cors.allowed-origins` 화이트리스트로 판단.
- `SecurityConfig`의 `corsConfigurationSource()`가 이 클래스를 사용하도록 교체.
- **중요한 구현 함정**: `CorsConfigurationSource`가 `null`을 반환하면 Spring은 preflight가
  아닌 실제 요청(GET/POST 등)에 대해 CORS 검증 자체를 건너뛰고 통과시켜 버림(클라이언트
  측 방어로만 취급) — 처음엔 이 사실을 모르고 "허용 안 함 = null 반환"으로 구현했다가,
  실제 브라우저로 크로스사이트 공격을 흉내내는 검증(Origin: `https://evil.example.com`)을
  해보고 나서야 뚫려 있는 걸 발견함. **항상 non-null `CorsConfiguration`을 반환하되,
  허용 안 할 땐 `allowedOrigins`를 비워서 Spring이 명시적으로 403 거부하도록** 수정.
- **또 다른 함정**: nginx가 `proxy_set_header Host $host;`로 Host를 전달했는데, nginx의
  `$host` 변수는 포트를 생략함 — 비표준 포트(예: 로컬 테스트용 임시 포트)로 접속하면
  백엔드가 원래 포트를 몰라서 same-origin 판정이 어긋남. 게다가
  `server.forward-headers-strategy=framework`가 활성화된 상태에서 `X-Forwarded-Proto`만
  있고 `X-Forwarded-Host`가 없으면, Spring의 `ForwardedHeaderFilter`가 포트를 그 스킴의
  기본 포트로 재해석해버려서 `Host` 헤더의 포트 정보가 무시되는 것도 확인함. →
  `frontend/nginx.conf`에서 `Host`/`X-Forwarded-Host`를 전부 `$http_host`(포트 포함)로
  전달하도록 수정.
- **검증**: 단위 테스트 4개(`OriginAwareCorsConfigurationSourceTest`) 작성 —
  Host와 Origin이 일치하는 경우 허용, 화이트리스트에 있는 경우 허용, 어느 쪽에도 안
  걸리는 경우(악성 사이트 시뮬레이션) 거부, Origin 헤더 자체가 없는 경우(서버-서버 호출)
  영향 없음. `gradlew test` 통과. 추가로 Docker 컨테이너를 재기동해 실제 환경에서
  curl(정상 same-origin 통과 / 악성 Origin 위장 시 403 실제로 확인 / 화이트리스트 경로
  통과)과 `claude-in-chrome`(실제 브라우저 전체 흐름 — `track/visit`, `auth/refresh`,
  이후 인증 API까지 전부 정상)으로 재확인 완료.

### 🟡 nginx가 정적 자산(JS/CSS)에 gzip을 안 걸어줌

오늘 백엔드(`server.compression.enabled=true`)는 압축을 켰지만, 프론트 정적 파일은
nginx가 직접 서빙하고 있어 별개로 설정해야 함. Lighthouse가 메인 JS 228KB 중 152KB,
CSS 20KB 중 16KB를 압축으로 줄일 수 있다고 지적. `frontend/nginx.conf`의 `http`/`server`
블록에 `gzip on; gzip_types text/css application/javascript ...;` 추가하면 해결됨.

### 🟡 접근성: 명도 대비 부족 + 찜하기 버튼 탭 타겟 겹침

- `--color-text-muted`(#857b6f)를 밝은 배경 위 작은 텍스트(품절 placeholder, 리뷰 개수)에
  쓸 때 WCAG AA 기준(4.5:1)에 못 미침 (측정값 3.77~4.15:1).
- `.wishlist-btn`(32×32)이 상품 카드 전체를 감싸는 `<a>` 링크와 영역이 겹쳐서, 모바일에서
  찜하기를 누르려다 상품 상세로 이동해버릴 수 있음.

### 🟢 `robots.txt` 없음

파일 자체가 없어 요청 시 `index.html`로 폴백됨 (SPA 라우팅 규칙 때문). SEO 감사에서
"문법을 이해할 수 없음" 대량 오류로 잡힘 — `frontend/public/robots.txt` 파일 하나
추가하면 해결되는 사소한 항목.

## 5. 참고

- `quest.log`의 "바이브 코딩 졸업 로드맵"과 별개 트랙 — 저긴 테스트/CI/PG연동 등 엔지니어링 기반 작업, 이 문서는 사용자 대면 디자인/성능 작업.
- 진행 내역은 계속 `hist.log`에 기록.

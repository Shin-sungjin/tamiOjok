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

P0 백로그(Lighthouse 베이스라인 실측으로 발견된 항목들)는 전부 처리 완료 — 상세는
4-1 참고. 다음은 P1부터.

### P1

| 항목 | 상태 | 내용 |
| --- | --- | --- |
| 브랜드 파비콘 | ✅ 완료 | `frontend/public/favicon.svg` 신설 — 헤더와 동일한 다크(#1c140f)+골드(#e2a610) 톤에 🐷 이모지. 기존 `/vite.svg` 참조는 사실 404였음(파일 자체가 없었음) — 파비콘이 아예 안 뜨고 있던 걸 겸사겸사 발견/수정. |
| 빈 상태(empty state) 보강 | ✅ 완료 | `components/EmptyState.tsx` 신설(아이콘+메시지+선택적 CTA). 고객 대면 8개 페이지(상품목록/찜목록/장바구니/주문서/주문목록/내쿠폰함/내리뷰/문의목록/쿠폰목록) 적용, CTA는 맥락에 맞는 곳으로(예: 내쿠폰함 비어있으면 → 쿠폰 발급 페이지). 관리자 목록 페이지는 내부 도구라 이번 범위에서 제외. |
| API 응답 캐싱 | ✅ 완료 | 실측 결과 `useMyCoupons` 훅(상품목록/찜목록/상품상세에서 사용)이 페이지 이동마다 동일 요청을 반복하고 있었음 — `useWishlist`와 같은 모듈스코프 패턴으로 60초 TTL 캐시 + 동시요청 중복제거 추가. React Query 등 새 라이브러리 도입 없이 기존 코드베이스 패턴 재사용. claude-in-chrome으로 상품목록→상세→목록→찜목록 4번 이동 시 `/coupons/my` 요청이 1번만 발생하는 것 실측 확인 (체크아웃 등 정확도가 중요한 화면은 이 훅을 안 쓰고 항상 새로 조회하므로 캐시 지연이 결제 흐름엔 영향 없음). |
| 이미지 최적화 파이프라인 | ⏸ 보류 — 방향 결정 필요 | 코드 확인 결과 애초에 이미지 업로드/저장 기능이 없음 — 관리자가 상품 등록 시 이미지 URL을 직접 타이핑해서 넣는 구조(`ProductImage.imageUrl`은 그냥 문자열 컬럼, 서버가 원본 파일을 전혀 통제하지 않음). "최적화"보다 범위가 큼 — (a) 실제 업로드+저장(로컬/S3 등) 기능부터 새로 만들거나, (b) 외부 이미지 프록시 서비스(예: images.weserv.nl류)로 기존 URL을 리사이즈/WebP 변환해 우회하는 방법 중 선택 필요. (b)는 제3자 서비스에 이미지 URL을 넘기는 것이라 의존성/프라이버시 트레이드오프가 있어 임의로 진행하지 않음 — 다음에 방향부터 논의.

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

### ✅ (해결됨) nginx가 정적 자산(JS/CSS)에 gzip을 안 걸어줌

백엔드(`server.compression.enabled=true`)는 압축을 켰지만, 프론트 정적 파일은 nginx가
직접 서빙하고 있어 별개로 설정이 필요했음. Lighthouse가 메인 JS 228KB 중 152KB, CSS
20KB 중 16KB를 압축으로 줄일 수 있다고 지적한 항목.

**적용한 해결책 (2026-07-27)**: `frontend/nginx.conf`의 `server` 블록에
`gzip on; gzip_vary on; gzip_min_length 1024; gzip_comp_level 6;
gzip_types text/css application/javascript application/json image/svg+xml;` 추가
(`text/html`은 nginx가 기본으로 압축 대상에 포함하므로 별도 지정 불필요).

**검증**: Docker로 재빌드 후 `Accept-Encoding: gzip`으로 실제 응답을 받아
`Content-Encoding: gzip` 헤더와 실제 바이트 감소를 확인 — 메인 JS 228,767바이트 →
76,534바이트(약 66% 감소, Lighthouse가 예측한 절감폭과 거의 일치). 헤더 없이 요청하면
228,767바이트 그대로 나가는 것도 대조 확인. 백엔드 `gradlew test`도 통과.

### ✅ (해결됨) 로딩 스켈레톤 부재

로딩 상태가 전부 `<p>불러오는 중...</p>` 텍스트 한 줄이었음 (17곳). 체감 성능과
디자인 완성도 둘 다에 걸리는 항목.

**적용한 해결책 (2026-07-27)**: `index.css`에 shimmer 애니메이션(`@keyframes
skeleton-shimmer`, `.skeleton`/`.skeleton-line`, `prefers-reduced-motion` 대응) 추가.
`components/Skeleton.tsx`에 재사용 가능한 조각(`SkeletonLine`, `ProductCardSkeleton`
/`ProductGridSkeleton`, `CardListSkeleton`, `TableSkeleton`, `StatGridSkeleton`) 구성.
기존 구조 클래스(`.product-card`, `.card`, `.table`, `.stat-tile` 등)를 그대로 재사용해서
실제 레이아웃과 스켈레톤 모양이 일치하도록 함. 전체 17곳 페이지(상품목록/찜목록/상품상세
/장바구니/주문서/주문목록/주문상세/문의목록·상세/쿠폰목록/내쿠폰함/내리뷰/관리자
대시보드·상품·주문·문의 목록)에 페이지 구조에 맞는 조합으로 적용.

**검증**: Docker `tsc -b && vite build` 통과. XMLHttpRequest를 브라우저 콘솔에서
일시적으로 지연시켜(소스 수정 없이 `javascript_tool`로 런타임에만 패치) 실제 스켈레톤
렌더링을 claude-in-chrome으로 스크린샷 확인 — 상품상세(2단 레이아웃)/주문목록(테이블)
/쿠폰목록(카드리스트)/관리자 대시보드(통계 타일+차트 패널) 4개 유형 전부 실제 레이아웃과
일치, 지연 해제 후 정상적으로 실제 데이터로 전환되는 것도 확인.

### ✅ (해결됨) 접근성: 명도 대비 부족 + 찜하기 버튼 탭 타겟 + focus-visible 부재

- `--color-text-muted`(#857b6f)를 밝은 배경 위 작은 텍스트(품절 placeholder, 리뷰 개수)에
  쓸 때 WCAG AA 기준(4.5:1)에 못 미침 (측정값 3.77~4.15:1).
- `.wishlist-btn`(32×32)이 상품 카드 전체를 감싸는 `<a>` 링크와 영역이 겹쳐서, 모바일에서
  찜하기를 누르려다 상품 상세로 이동해버릴 수 있음.
- `:focus-visible` 스타일이 전혀 정의돼 있지 않아 브라우저 기본값에 의존, 일부 입력
  필드는 `:focus { outline: none; }`로 아예 지워버려서 키보드 사용자가 포커스 위치를
  알기 어려웠음.

**적용한 해결책 (2026-07-27)**:
- `--color-text-muted`를 `#857b6f` → `#756b60`으로 어둡게 조정. 흰 배경 대비
  4.15:1→5.21:1, `--color-neutral-soft` 배경 대비 3.77:1→4.74:1로 계산상 WCAG AA(4.5:1)
  통과 확인 (상대휘도 공식으로 직접 계산 후 적용 — Lighthouse가 원래 리포트한 4.15:1
  수치와 계산값이 일치하는 것으로 공식 적용이 맞았음을 교차 검증). 전역 토큰이라
  카드/폼라벨/통계타일 등 muted 텍스트를 쓰는 모든 곳에 일괄 적용됨.
- `.wishlist-btn`을 32px → 44px로 확대(Google/WCAG 권장 최소 터치 타겟), 위치를
  카드 모서리에서 살짝 안쪽으로(10px→8px) 조정하고 그림자 추가로 이미지 위에서
  더 잘 구분되도록 함.
- `:focus-visible` 전역 규칙 추가(`outline: 2px solid var(--color-accent)`).
  `outline: none`으로 지우던 3개 입력 필드 그룹은 `:focus-visible` 전용 규칙을
  별도로 추가해 키보드 포커스 시에만 outline이 되살아나도록 함 (마우스 클릭 시엔
  기존처럼 border-color 변화만 유지).
- **구현 중 발견한 부수 버그**: `.product-card`/`.card` 내부에서 카드 전체를 감싸는
  `<a>`가 block 자식(div/h2/p)을 포함하는데도 `display` 기본값(inline)인 채로
  있어서, 브라우저가 이를 익명 블록 박스로 쪼개 렌더링 — 그 결과 keyboard focus
  outline이 카드 테두리를 온전히 감싸지 못하고 있었음 (focus-visible 작업 중
  실제 브라우저로 확인하다가 발견). `.product-card > a`, `.card > a`에
  `display: block` 추가로 수정.

**검증**: Docker `tsc -b && vite build` 통과. claude-in-chrome으로 실제 키보드 Tab
포커스 이동을 재현해 `document.activeElement`의 computed style로 outline이
`#e2a610` 2px solid로 정확히 적용되는 것을 확인하고, 스크린샷으로 카드 테두리를
따라 골드 링이 온전한 사각형으로 보이는 것까지 시각 확인. 찜하기 버튼 확대도
스크린샷으로 확인.

### 🟢 `robots.txt` 없음

파일 자체가 없어 요청 시 `index.html`로 폴백됨 (SPA 라우팅 규칙 때문). SEO 감사에서
"문법을 이해할 수 없음" 대량 오류로 잡힘 — `frontend/public/robots.txt` 파일 하나
추가하면 해결되는 사소한 항목.

## 5. 참고

- `quest.log`의 "바이브 코딩 졸업 로드맵"과 별개 트랙 — 저긴 테스트/CI/PG연동 등 엔지니어링 기반 작업, 이 문서는 사용자 대면 디자인/성능 작업.
- 진행 내역은 계속 `hist.log`에 기록.

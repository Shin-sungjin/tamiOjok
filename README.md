# tamiOjok

Spring Boot 기반 B2C 이커머스 백엔드 프로젝트입니다. 회원가입/로그인부터 상품, 장바구니, 주문, 결제, 배송, 리뷰, 1:1 문의, 쿠폰까지 이커머스에 필요한 핵심 도메인을 REST API로 제공합니다.

- 상세 기획/요구사항: [`PRD.md`](./PRD.md)
- 사용 방법(회원가입부터 주문/결제까지 전체 흐름과 API 예시): [`docs/USER_MANUAL.md`](./docs/USER_MANUAL.md)
- DB 스키마 및 더미 데이터: [`db/schema.sql`](./db/schema.sql), [`db/seed.sql`](./db/seed.sql)

## 기술 스택

| 영역 | 사용 기술 |
| --- | --- |
| Language / Runtime | Java 21 |
| Framework | Spring Boot 4.1 (Web MVC, Data JPA, Security, Validation, OAuth2 Client, Actuator) |
| Database | PostgreSQL (테스트: H2) |
| 인증 | JWT (Access/Refresh), OAuth2 소셜 로그인 (Kakao / Naver / Google) |
| 빌드 도구 | Gradle (Wrapper 포함) |
| 기타 | Lombok, jjwt |

## 도메인 구성

| 도메인 | 설명 |
| --- | --- |
| `user` | 회원가입/로그인/JWT 토큰 재발급/로그아웃, 소셜 로그인, 배송지 관리 |
| `product` | 상품 조회/등록/수정, 재고 관리 |
| `cart` | 장바구니 담기/수량변경/삭제/비우기 |
| `order` | 주문 생성(직접 주문 / 장바구니 기반), 조회, 취소, 관리자 주문 준비 처리 |
| `payment` | PG 결제 승인 및 위변조(금액 검증) 처리 |
| `delivery` | 배송 등록/조회, 반품 요청, 택배사 배송상태 자동 추적(스케줄러) |
| `review` | 배송완료 주문상품에 대한 리뷰 작성/수정/삭제, 상품별 리뷰 조회 |
| `inquiry` | 1:1 문의 등록/조회, 관리자 답변 |
| `coupon` | 쿠폰(정액/정률) 정의, 발급, 주문 시 할인 적용, 주문취소 시 쿠폰 복원 |

각 도메인은 `controller → service → repository → entity` 계층 구조를 따르며, API 응답은 Entity가 아닌 별도 DTO(`dto/response`)로 매핑됩니다.

## 시작하기

### 1. 사전 준비물

- JDK 21
- PostgreSQL 14+ (로컬 실행 시)

### 2. 데이터베이스 준비

```bash
# PostgreSQL에 ecommerce 데이터베이스 생성
createdb ecommerce

# 스키마 생성 + 더미 데이터 삽입 (선택 사항, 개발/테스트용)
psql -h localhost -U postgres -d ecommerce -f db/schema.sql
psql -h localhost -U postgres -d ecommerce -f db/seed.sql
```

> 운영 코드는 `spring.jpa.hibernate.ddl-auto=update` 설정으로 애플리케이션 기동 시 스키마를 자동 생성/갱신합니다. `db/schema.sql`을 직접 실행하지 않고 빈 데이터베이스에 바로 애플리케이션을 띄워도 됩니다. `db/seed.sql`은 테스트 계정과 샘플 데이터가 필요할 때만 실행하세요.

### 3. 환경변수

`src/main/resources/application.properties`에 정의된 값들은 아래 환경변수로 덮어쓸 수 있습니다. (지정하지 않으면 로컬 개발용 기본값이 사용됩니다.)

| 환경변수 | 설명 | 기본값 |
| --- | --- | --- |
| `DB_URL` | PostgreSQL 접속 URL | `jdbc:postgresql://localhost:5432/ecommerce` |
| `DB_USERNAME` / `DB_PASSWORD` | DB 계정 | `postgres` / `postgres` |
| `JWT_SECRET` | JWT 서명 키 (운영에서는 반드시 변경) | 샘플 문자열 |
| `JWT_ACCESS_VALIDITY_MS` / `JWT_REFRESH_VALIDITY_MS` | Access/Refresh 토큰 유효기간(ms) | 30분 / 14일 |
| `KAKAO_CLIENT_ID` / `KAKAO_CLIENT_SECRET` | 카카오 OAuth 클라이언트 정보 | 샘플 값 |
| `NAVER_CLIENT_ID` / `NAVER_CLIENT_SECRET` | 네이버 OAuth 클라이언트 정보 | 샘플 값 |
| `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET` | 구글 OAuth 클라이언트 정보 | 샘플 값 |
| `OAUTH2_REDIRECT_URI` | 소셜 로그인 성공 후 프론트 리다이렉트 URL | `http://localhost:3000/oauth2/redirect` |
| `OAUTH2_NEED_INFO_URI` | 추가정보 입력이 필요할 때 리다이렉트 URL | `http://localhost:3000/oauth2/additional-info` |

### 4. 애플리케이션 실행

```bash
# Windows
gradlew.bat bootRun

# macOS / Linux
./gradlew bootRun
```

기본적으로 `http://localhost:8080`에서 API가 제공됩니다.

### 5. 빌드 / 테스트

```bash
./gradlew build      # 컴파일 + 테스트 + jar 빌드
./gradlew test       # 테스트만 실행
./gradlew compileJava # 컴파일만 확인
```

## 테스트 계정 (db/seed.sql 기준)

더미 데이터를 삽입했다면 아래 계정으로 바로 로그인해볼 수 있습니다. 모든 로컬 계정의 비밀번호는 `Password1!` 입니다.

| 이메일 | 역할 | 비고 |
| --- | --- | --- |
| `admin@tamiojok.com` | ADMIN | 관리자 API(`/api/v1/admin/**`) 테스트용 |
| `hong@example.com` | USER | 배송완료 주문/리뷰/결제완료 주문 등 다양한 상태 보유 |
| `kim@example.com` | USER | 결제대기 주문, 반품요청 배송, 장바구니 보유 |
| `lee@example.com` | USER | 배송중 주문, 쿠폰 사용 이력 보유 |
| `park@example.com` | USER (`NEED_INFO`) | 소셜 로그인 직후 추가정보 미입력 상태 예시 |

자세한 API 사용법과 전체 엔드포인트 목록은 [`docs/USER_MANUAL.md`](./docs/USER_MANUAL.md)를 참고하세요.

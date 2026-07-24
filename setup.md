# 로컬 실행 가이드 (setup.md)

이 프로젝트(tamiOjok)를 내 PC에서 직접 띄우기 위한 단계별 정리입니다.
현재 이 환경 기준 설치 상태를 확인해보면:

- ✅ JDK 21 설치됨 (`Eclipse Adoptium Temurin 21.0.11`)
- ❌ Node.js 미설치 (프론트엔드 실행 시 필요)
- ❌ PostgreSQL 미설치 (백엔드 실행 시 필요)

아래 순서대로 진행하면 됩니다.

---

## 1. 사전 준비물 설치

### 1-1. PostgreSQL (백엔드 필수)

- [postgresql.org](https://www.postgresql.org/download/windows/) 에서 14 이상 버전 설치 프로그램 다운로드 후 설치
- 설치 중 superuser(`postgres`) 비밀번호를 `postgres`로 설정하면 아래 기본값 그대로 사용 가능 (다르게 설정했다면 3단계 환경변수에서 `DB_PASSWORD`로 덮어쓰기)
- 설치 후 확인:

  ```powershell
  psql --version
  ```

  `psql`이 PATH에 안 잡히면 설치 폴더(예: `C:\Program Files\PostgreSQL\16\bin`)를 환경변수 PATH에 추가

### 1-2. Node.js (프론트엔드 실행 시에만 필요)

- [nodejs.org](https://nodejs.org) 에서 LTS(20.x 이상) 설치, 또는 [nvm-windows](https://github.com/coreybutler/nvm-windows)로 버전 관리
- 설치 후 확인:

  ```powershell
  node -v   # v20.x 이상
  npm -v
  ```

- 백엔드만 먼저 확인하고 싶다면 이 단계는 건너뛰고 4단계로 이동해도 됨

---

## 2. 데이터베이스 준비

```powershell
# PostgreSQL에 ecommerce 데이터베이스 생성
createdb -U postgres ecommerce
```

- 테이블 스키마는 애플리케이션이 직접 만들지 않고, 기동 시점에 **Flyway**(`src/main/resources/db/migration/V1__init_schema.sql`)가 자동 적용함
- 더미 데이터(`db/seed.sql`)는 선택 사항이며, **반드시 애플리케이션을 한 번 띄워서 Flyway가 테이블을 만든 뒤에** 실행해야 함 (순서 중요 — 빈 DB에 바로 실행하면 실패)

---

## 3. 환경변수 설정 (선택)

`src/main/resources/application.properties`는 아래 값들을 환경변수로 덮어쓸 수 있고, 지정하지 않으면 로컬 개발용 기본값이 사용됩니다. **로컬에서 그냥 띄워보는 정도면 이 단계는 건너뛰어도 됩니다.**

| 환경변수 | 설명 | 기본값 |
| --- | --- | --- |
| `DB_URL` | PostgreSQL 접속 URL | `jdbc:postgresql://localhost:5432/ecommerce` |
| `DB_USERNAME` / `DB_PASSWORD` | DB 계정 | `postgres` / `postgres` |
| `JWT_SECRET` | JWT 서명 키 | 샘플 문자열 (로컬 실행은 그대로 둬도 무방) |
| `KAKAO_CLIENT_ID` 등 OAuth 관련 값 | 소셜 로그인 클라이언트 정보 | 샘플 값 (소셜 로그인 테스트 안 하면 무시해도 됨) |

PostgreSQL 설치 시 비밀번호를 `postgres` 외 다른 값으로 설정했다면, PowerShell에서 이 세션에만 적용되도록 지정 후 실행:

```powershell
$env:DB_PASSWORD = "실제비밀번호"
.\gradlew.bat bootRun
```

---

## 4. 백엔드 실행

프로젝트 루트(`C:\dev\vibe`)에서:

```powershell
.\gradlew.bat bootRun
```

- 첫 실행은 의존성 다운로드 때문에 시간이 걸릴 수 있음
- 정상 기동되면 `http://localhost:8080`에서 API 제공
- 기동 로그에 Flyway 마이그레이션 적용 메시지가 보이면 스키마 생성 성공

### 4-1. (선택) 더미 데이터 삽입

백엔드가 한 번 정상 기동되어 테이블이 생성된 뒤:

```powershell
psql -h localhost -U postgres -d ecommerce -f db/seed.sql
```

테스트 계정 (비밀번호 전부 `Password1!`):

| 이메일 | 역할 | 비고 |
| --- | --- | --- |
| `admin@tamiojok.com` | ADMIN | 관리자 API 테스트용 |
| `hong@example.com` | USER | 배송완료 주문/리뷰/결제완료 주문 보유 |
| `kim@example.com` | USER | 결제대기 주문, 반품요청 배송, 장바구니 보유 |
| `lee@example.com` | USER | 배송중 주문, 쿠폰 사용 이력 보유 |
| `park@example.com` | USER (NEED_INFO) | 소셜 로그인 직후 추가정보 미입력 상태 예시 |

### 4-2. 동작 확인

```powershell
curl http://localhost:8080/actuator/health
```

`{"status":"UP"}` 이 나오면 정상.

---

## 5. 프론트엔드 실행 (선택)

백엔드가 떠 있는 상태에서, 새 터미널에서:

```powershell
cd frontend
npm install
copy .env.example .env.local
npm run dev
```

- `http://localhost:5173`에서 열림
- 로그인/회원가입/상품목록/상품상세만 구현된 최소 스캐폴딩 (`frontend/README.md` 참고)
- 백엔드 CORS 설정(`app.cors.allowed-origins`)이 이미 `http://localhost:5173`을 허용하도록 되어 있어 포트를 바꾸지 않는 한 별도 설정 불필요

---

## 6. 빌드 / 테스트

```powershell
.\gradlew.bat build       # 컴파일 + 테스트 + jar 빌드
.\gradlew.bat test        # 테스트만 실행 (H2 인메모리 DB 사용, PostgreSQL 불필요)
.\gradlew.bat compileJava # 컴파일만 확인
```

`test`는 H2를 쓰므로 PostgreSQL이 없어도 실행 가능 — 백엔드 컴파일/테스트만 먼저 확인하고 싶다면 1-1(PostgreSQL 설치) 단계를 건너뛰고 바로 이걸 실행해도 됨.

---

## 트러블슈팅

- **`gradlew.bat`이 안 먹힘**: PowerShell 실행 정책 때문일 수 있음. `.\gradlew.bat` 처럼 `.\`을 붙여서 실행
- **`FlywayException` 또는 `ddl-auto=validate` 에러로 기동 실패**: DB에 기존 테이블이 남아있는 상태에서 스키마가 바뀐 경우. 로컬 테스트 DB라면 `dropdb -U postgres ecommerce && createdb -U postgres ecommerce`로 초기화 후 재기동
- **`psql: command not found`**: PostgreSQL `bin` 폴더가 PATH에 없음. 설치 경로의 `bin` 디렉터리를 환경변수 PATH에 추가
- **포트 충돌 (`8080` 또는 `5173` 이미 사용 중)**: 다른 프로세스가 점유 중인지 확인 (`netstat -ano | findstr :8080`) 후 종료하거나, 백엔드는 `server.port` 환경변수, 프론트는 `npm run dev -- --port 5174`로 포트 변경

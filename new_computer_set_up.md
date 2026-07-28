# 새 컴퓨터에서 프로젝트 세팅하기 (new_computer_set_up.md)

이 프로젝트(tamiOjok)는 GitHub(`https://github.com/Shin-sungjin/tamiOjok.git`)에 커밋되어 있습니다. 다른 컴퓨터에서 클론해서 띄울 때 해야 할 일을 정리한 문서입니다.

더 자세한 배경 설명이 필요하면:
- 로컬 직접 실행(gradlew) 상세 가이드: [`setup.md`](./setup.md)
- Docker Compose 실행 및 인프라 배경: [`infra_setup.md`](./infra_setup.md)의 "다른 컴퓨터에서 동일 환경 세팅하기" 섹션

---

## 0. 먼저 알아둘 것: git에 없는 것들

`.gitignore`에 의해 아래 항목들은 저장소에 포함되어 있지 않습니다. 새 컴퓨터에서는 이 항목들을 새로 만들어야 합니다.

| 항목 | 비고 |
| --- | --- |
| `.env` | 시크릿 값 포함. `.env.example`을 복사해서 직접 채워야 함 |
| `frontend/.env.local` | 프론트를 로컬로 직접 실행할 때만 필요 |
| `build/`, `.gradle/` | Gradle 빌드/캐시 산출물. 최초 빌드 시 자동 생성 |
| `frontend/node_modules/` | `npm install`로 생성 |
| `.idea/`, `.vscode/` 등 IDE 설정 | 각자 IDE에서 새로 열면 됨 |

## 1. 저장소 클론

```bash
git clone https://github.com/Shin-sungjin/tamiOjok.git
cd tamiOjok
```

## 2. 실행 방식 선택

두 가지 방법이 있습니다. 목적에 맞게 선택하세요.

- **방법 A — Docker Compose (권장)**: PostgreSQL/백엔드/프론트/Cloudflare Tunnel을 컨테이너 4개로 한 번에 띄움. 지금 실제 운영 중인 방식과 동일. Docker만 설치하면 되고 Java/Node/PostgreSQL을 개별 설치할 필요 없음.
- **방법 B — 로컬 직접 실행**: 코드를 수정하며 개발할 때 편함 (hot reload 등). JDK 21, PostgreSQL, (프론트 만지면) Node.js를 직접 설치해야 함.

---

## 방법 A. Docker Compose로 한 번에 띄우기

### A-1. Docker 설치

- **Windows**: WSL2 + VirtualMachinePlatform 활성화 → 재부팅 → `wsl --update` → Docker Desktop 설치 (WSL2 backend)
- **Mac**: Docker Desktop for Mac 설치
- **Linux**: Docker Engine + Docker Compose plugin 설치 (`apt`/`dnf` 등)

### A-2. `.env` 파일 준비

```bash
cp .env.example .env
```

`.env`를 열어서 최소한 아래 두 값은 반드시 채우기 (비워두면 `docker compose up` 시 에러):

- `DB_PASSWORD` — 임의의 강력한 비밀번호
- `JWT_SECRET` — 임의의 긴 랜덤 문자열 (예: `openssl rand -base64 48`)

소셜 로그인(Kakao/Naver/Google)을 테스트할 게 아니면 나머지 OAuth 값은 비워둬도 됨 (샘플 값으로 자동 대체).

### A-3. 빌드 + 기동

```bash
docker compose up -d --build
docker compose ps          # 4개 컨테이너 모두 Up/healthy 인지 확인
```

### A-4. (최초 1회, 선택) 테스트 데이터 삽입

backend가 Flyway로 테이블을 만든 뒤에 실행해야 함:

```bash
docker exec -i -e PGPASSWORD=$(grep '^DB_PASSWORD=' .env | cut -d= -f2) \
  $(docker compose ps -q db) psql -U postgres -d ecommerce < db/seed.sql
```

### A-5. 접속 주소 확인

```bash
docker compose logs cloudflared | grep trycloudflare.com
```

출력된 `https://....trycloudflare.com` 주소로 접속 (컨테이너 포트를 호스트에 열지 않으므로 `localhost:8080`/`localhost:5173`으로는 접속 불가). Quick Tunnel URL은 기동할 때마다 바뀜.

---

## 방법 B. 로컬에서 직접 실행 (개발용)

### B-1. 사전 준비물 설치

- **JDK 21** — [Eclipse Temurin](https://adoptium.net/) 등
- **PostgreSQL 14+** — [postgresql.org](https://www.postgresql.org/download/) 설치, superuser(`postgres`) 비밀번호 확인해두기
  ```bash
  psql --version
  ```
- **Node.js 20+ (LTS)** — 프론트엔드를 만질 때만 필요
  ```bash
  node -v
  npm -v
  ```

### B-2. 데이터베이스 생성

```bash
createdb -U postgres ecommerce
```

테이블 스키마는 Flyway(`src/main/resources/db/migration`)가 애플리케이션 최초 기동 시 자동 적용함.

### B-3. 환경변수 설정 (선택)

기본값 그대로 써도 되면 건너뛰어도 됨. PostgreSQL 비밀번호를 `postgres`가 아닌 다른 값으로 설정했다면:

```bash
# Windows PowerShell
$env:DB_PASSWORD = "실제비밀번호"

# macOS / Linux
export DB_PASSWORD=실제비밀번호
```

전체 환경변수 목록은 [`setup.md`](./setup.md#3-환경변수-설정-선택) 참고.

### B-4. 백엔드 실행

```bash
# Windows
.\gradlew.bat bootRun

# macOS / Linux
./gradlew bootRun
```

`http://localhost:8080/actuator/health`가 `{"status":"UP"}`을 반환하면 정상.

### B-5. (선택) 테스트 데이터 삽입

백엔드가 한 번 정상 기동된 뒤(Flyway가 테이블을 만든 뒤):

```bash
psql -h localhost -U postgres -d ecommerce -f db/seed.sql
```

### B-6. 프론트엔드 실행 (선택)

백엔드가 떠 있는 상태에서 새 터미널:

```bash
cd frontend
npm install
cp .env.example .env.local
npm run dev
```

`http://localhost:5173`에서 열림.

### B-7. 빌드 / 테스트

```bash
# Windows
.\gradlew.bat build       # 컴파일 + 테스트 + jar 빌드
.\gradlew.bat test        # 테스트만 실행 (H2 인메모리 DB 사용, PostgreSQL 불필요)

# macOS / Linux
./gradlew build
./gradlew test
```

---

## 공통: 테스트 계정 (seed.sql 삽입 시)

비밀번호는 전부 `Password1!`

| 이메일 | 역할 | 비고 |
| --- | --- | --- |
| `admin@tamiojok.com` | ADMIN | 관리자 API 테스트용 |
| `hong@example.com` | USER | 배송완료 주문/리뷰/결제완료 주문 보유 |
| `kim@example.com` | USER | 결제대기 주문, 반품요청 배송, 장바구니 보유 |
| `lee@example.com` | USER | 배송중 주문, 쿠폰 사용 이력 보유 |
| `park@example.com` | USER (NEED_INFO) | 소셜 로그인 직후 추가정보 미입력 상태 예시 |

---

## 트러블슈팅

- **`gradlew.bat`이 안 먹힘**: `.\gradlew.bat` 처럼 `.\`을 붙여서 실행 (PowerShell 실행 정책 문제)
- **`FlywayException` / `ddl-auto=validate` 에러로 기동 실패**: 기존 테이블이 남은 상태에서 스키마가 바뀐 경우. 로컬 테스트 DB면 `dropdb -U postgres ecommerce && createdb -U postgres ecommerce`로 초기화 후 재기동
- **`psql: command not found`**: PostgreSQL `bin` 폴더가 PATH에 없음. 설치 경로의 `bin` 디렉터리를 PATH에 추가
- **포트 충돌 (`8080`/`5173`)**: 점유 프로세스 확인 (`netstat -ano | findstr :8080`) 후 종료하거나 포트 변경
- **Docker Compose 기동 실패**: `.env`의 `DB_PASSWORD`/`JWT_SECRET`이 비어있지 않은지 확인
- **`wsl --status` 오류 / WSL 비활성**: Windows 기능(WSL, VirtualMachinePlatform) 활성화 후 재부팅 필요

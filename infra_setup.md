# 인프라 구축 가이드 (infra_setup.md)

tamiOjok을 Docker로 컨테이너화해서 띄우고, Cloudflare(무료 플랜)로 외부에 노출하기 위한 단계별 계획입니다.

## 아키텍처 개요

- **컨테이너 오케스트레이션**: Docker Compose
- **컨테이너 베이스 이미지**: 전부 **Rocky Linux 9** (`rockylinux:9`) 기반으로 통일
  - backend (Spring Boot) — Rocky + OpenJDK 21 (AppStream)
  - db (PostgreSQL) — Rocky + PGDG(PostgreSQL 공식) yum 레포
  - frontend (React 빌드 결과물 서빙) — Rocky + nginx (dnf)
  - cloudflared (Cloudflare Tunnel 클라이언트) — Rocky + Cloudflare 공식 RPM 레포
- **외부 노출**: Cloudflare Tunnel — 공인 IP/포트포워딩/방화벽 설정 없이 무료로 외부 접속 제공
- **Docker**: Docker Desktop (Windows, WSL2 backend) — 개인 사용 무료(Personal license)

> 참고: PostgreSQL 공식 이미지나 cloudflared 공식 이미지는 보통 Debian 계열입니다. 전부 Rocky Linux로 통일하는 건 유지보수 부담이 조금 더 있지만(패키지명, 레포 설정을 직접 관리), 명시적으로 요청하신 방향이라 이 문서는 그 방식을 기준으로 작성했습니다.

---

## 컨테이너 구조 & 요청 흐름

Docker GUI(Docker Desktop)에서 보이는 4개 컨테이너(`db`, `backend`, `frontend`, `cloudflared`)는 각각 별도 프로세스로 뜨는 4개의 독립된 서버입니다. 하나의 "앱"이 아니라, 역할이 분리된 4개 서비스가 `vibe_internal`이라는 하나의 내부 bridge 네트워크로 묶여서 서로 컨테이너 이름(DNS처럼 동작)으로 통신합니다.

```
[사용자 브라우저]
      │  https://<임의문자열>.trycloudflare.com
      ▼
[cloudflared 컨테이너] ── Cloudflare 엣지로 나가는 아웃바운드 연결만 맺음
      │  (인바운드 포트를 전혀 열지 않음 — 방화벽/포트포워딩 불필요)
      │  내부적으로 http://frontend:80 으로 전달
      ▼
[frontend 컨테이너] (nginx)
      ├─ 정적 파일(React 빌드 결과물) 서빙 — SPA 라우팅은 try_files로 index.html 폴백
      └─ /api/ 로 시작하는 요청만 내부적으로 backend:8080 으로 리버스 프록시
      ▼        (같은 origin에서 오는 것처럼 보이므로 CORS 설정이 필요 없음)
[backend 컨테이너] (Spring Boot)
      ├─ REST API 처리, JWT 인증
      ├─ 기동 시 Flyway가 db에 마이그레이션 자동 적용
      └─ JPA/Hibernate로 db와 통신
      ▼
[db 컨테이너] (PostgreSQL)
      └─ named volume(db-data)에 데이터 영속화 — 컨테이너를 지워도 데이터는 유지됨
```

핵심 포인트:
- **backend/db/frontend는 호스트에 포트를 노출하지 않습니다.** 그래서 이 PC에서도 `localhost:8080`으로는 접속이 안 되고, 반드시 cloudflared가 발급한 Tunnel URL로만 접속해야 합니다.
- **frontend(nginx)가 사실상 API 게이트웨이 역할도 겸합니다.** 프론트엔드 JS는 `/api/v1/...` 같은 상대경로로만 호출하고, nginx가 그걸 backend로 중계합니다. 덕분에 외부에는 frontend 하나만 노출해도 되고, CORS 설정 없이 동작합니다.
- **cloudflared는 서버가 아니라 "밖에서 안으로 들어오는 문"을 뚫어주는 클라이언트**입니다. 공인 IP나 라우터 포트포워딩 없이도 외부 접속을 가능하게 해줍니다.

---

## 0단계. 호스트 환경 준비 (Windows, Docker Desktop 구동용)

1. Windows 기능 활성화: WSL, Virtual Machine Platform
2. **재부팅**
3. 재부팅 후 WSL2 리눅스 커널 최신화

   ```powershell
   wsl --update
   ```

4. Docker Desktop 설치 ([docker.com](https://www.docker.com/products/docker-desktop/)) — 설치 시 "Use WSL2 based engine" 옵션 선택
5. 설치 확인

   ```powershell
   docker --version
   docker compose version
   ```

---

## 1단계. Dockerfile 작성 (Rocky Linux 9 베이스)

레포 구조에 아래 파일들을 추가할 예정:

```
Dockerfile                  # backend (Spring Boot)
frontend/Dockerfile         # frontend (React 빌드 + nginx)
docker/db/Dockerfile        # PostgreSQL
docker/cloudflared/Dockerfile
docker-compose.yml
.env.example
```

- **backend**: multi-stage 빌드
  - build stage: `rockylinux:9` + `dnf install java-21-openjdk-devel` + `./gradlew build`
  - run stage: `rockylinux:9-minimal` + `java-21-openjdk-headless` + 빌드된 jar만 복사
- **db**: `rockylinux:9` + PGDG(PostgreSQL 공식) yum 레포 등록 → `postgresql16-server` 설치 → `initdb` → 데이터 볼륨 마운트
- **frontend**: multi-stage
  - build stage: `rockylinux:9` + NodeSource/AppStream nodejs 20 → `npm install && npm run build`
    - `VITE_API_BASE_URL` 빌드 인자를 비워두면(기본값) axios가 상대경로(`/api/v1/...`)로 요청 → nginx가 중계
  - run stage: `rockylinux:9` + `dnf install nginx` → 빌드 결과물을 nginx 문서 루트로 복사, `nginx.conf`로 기본 설정 교체
    - `location /api/` → `backend:8080`으로 리버스 프록시 (X-Forwarded-* 헤더 전달)
    - `location /` → `try_files $uri /index.html` (React Router 새로고침/직접 진입 대응)
- **cloudflared**: `rockylinux:9` + Cloudflare 공식 RPM 레포 등록 → `dnf install cloudflared`
- backend `application.properties`에 `server.forward-headers-strategy=framework` 추가
  → nginx가 넘겨주는 `X-Forwarded-Proto/Host`를 신뢰해서 실제 공개 주소 기준으로 동작(향후 OAuth2 redirect-uri 등에 영향)
- 루트/`frontend/`에 `.dockerignore` 추가 (`.git`, `build/`, `node_modules`, `dist` 등 제외 → 빌드 컨텍스트 크기 축소, 호스트에 남은 `node_modules`가 컨테이너 빌드에 섞여 들어가는 것 방지)

---

## 2단계. docker-compose.yml 구성

- 서비스: `backend`, `db`, `frontend`, `cloudflared`
- `db`는 named volume(`db-data`)으로 데이터 영속화
- `backend`는 `depends_on: db`, 환경변수로 `DB_URL=jdbc:postgresql://db:5432/ecommerce` 등 내부 서비스명 사용
- `frontend`는 `depends_on: backend`
- `cloudflared`는 `depends_on: [backend, frontend]`, 터널 토큰으로 인증
- 모든 서비스는 하나의 내부 bridge 네트워크로 묶고, 외부에는 `cloudflared`를 통해서만 노출 (backend/db/frontend는 호스트 포트를 굳이 열지 않아도 됨)

---

## 3단계. 환경변수 / 시크릿 관리

- `.env` 파일 생성 (`.gitignore`에 포함, 절대 커밋 금지)
  - `DB_PASSWORD`, `JWT_SECRET`, `KAKAO_CLIENT_SECRET` 등 기존 `application.properties`가 참조하는 값들
  - `CLOUDFLARE_TUNNEL_TOKEN` (4단계에서 발급)
- `.env.example`은 키 이름만 남기고 값은 비워서 커밋

---

## 4단계. Cloudflare Tunnel 설정

현재 Cloudflare 계정은 방금 생성했고 연결된 도메인이 없는 상태 → **우선 Quick Tunnel로 테스트**하고, 도메인이 준비되면 영구 주소로 전환합니다.

### 4-1. 지금 (도메인 없음): Quick Tunnel

```bash
cloudflared tunnel --url http://frontend:80
```

- 실행할 때마다 `https://무작위문자열.trycloudflare.com` 형태의 임시 URL이 발급됨 (완전 무료, 계정 로그인도 불필요)
- 컨테이너 재시작하면 URL이 바뀌므로 테스트/데모용으로만 사용

### 4-2. 나중에 (도메인 확보 후): Named Tunnel — 영구 주소

1. 도메인을 Cloudflare에 연결 (직접 구매한 도메인의 네임서버를 Cloudflare로 변경, 또는 Cloudflare Registrar에서 구매)
2. `cloudflared tunnel login` — 브라우저 인증
3. `cloudflared tunnel create tamiojok` — 터널 생성, 자격증명 파일 발급
4. `cloudflared tunnel route dns tamiojok api.내도메인.com` — DNS 레코드 자동 등록
5. `config.yml`에 ingress 규칙 작성 (예: `api.내도메인.com` → `backend:8080`, `내도메인.com` → `frontend:80`)
6. docker-compose의 `cloudflared` 서비스를 토큰 기반(`cloudflared tunnel run --token $CLOUDFLARE_TUNNEL_TOKEN`) 또는 config 파일 마운트 방식으로 전환

---

## 5단계. 전체 기동 및 검증

```bash
docker compose up -d --build
docker compose ps
docker compose logs -f backend
```

- backend healthcheck: `curl http://localhost:8080/actuator/health` (컨테이너 내부 또는 포트 노출 시)
- Cloudflare Tunnel URL로 외부에서 접속되는지 확인

---

## 다른 컴퓨터에서 동일 환경 세팅하기

이 레포를 새 컴퓨터(또는 다른 팀원 PC)에 클론해서 지금과 똑같이 띄우는 절차입니다.

1. 레포 클론
   ```bash
   git clone https://github.com/Shin-sungjin/tamiOjok.git
   cd tamiOjok
   ```
2. Docker 설치
   - **Windows**: 0단계 참고 — WSL/VirtualMachinePlatform 활성화 → 재부팅 → `wsl --update` → Docker Desktop 설치(WSL2 backend)
   - **Mac**: Docker Desktop for Mac 설치만 하면 됨 (WSL 관련 단계 불필요)
   - **Linux**: Docker Engine + Docker Compose plugin 설치 (`apt`/`dnf` 등 배포판 패키지 매니저 사용)
3. `.env` 파일 준비
   ```bash
   cp .env.example .env
   ```
   그 다음 `.env`를 열어 최소한 아래 두 값은 반드시 채우기 (비워두면 `docker compose up` 시 에러):
   - `DB_PASSWORD` — 임의의 강력한 비밀번호
   - `JWT_SECRET` — 임의의 긴 랜덤 문자열 (예: `openssl rand -base64 48`)

   소셜 로그인(Kakao/Naver/Google)을 테스트할 게 아니라면 나머지 OAuth 값들은 비워둬도 됨(샘플 값으로 자동 대체됨).
4. 빌드 + 기동
   ```bash
   docker compose up -d --build
   docker compose ps          # 4개 컨테이너 모두 Up/healthy 인지 확인
   ```
5. (최초 1회, 선택) 테스트 데이터 넣기 — backend가 Flyway로 테이블을 만든 뒤에 실행해야 함
   ```bash
   docker exec -i -e PGPASSWORD=$(grep '^DB_PASSWORD=' .env | cut -d= -f2) \
     $(docker compose ps -q db) psql -U postgres -d ecommerce < db/seed.sql
   ```
6. 접속 주소 확인 (Cloudflare Quick Tunnel URL은 기동할 때마다 바뀜)
   ```bash
   docker compose logs cloudflared | grep trycloudflare.com
   ```
   여기 출력된 `https://....trycloudflare.com` 주소로 접속. (`localhost:8080`이나 `localhost:5173`이 아님 — 컨테이너 포트를 호스트에 열지 않음)

> 여러 컴퓨터에서 **동시에** 띄우면 Cloudflare Tunnel URL이 컴퓨터마다 다르게 발급됩니다 (임시 URL이라 매번 새로 생성). 항상 같은 주소로 접속하고 싶다면 아래 4-2단계(Named Tunnel + 도메인)로 전환해야 합니다.

---

## 배포 방법 & 신경 써야 할 점

지금 구성은 **"이 PC를 서버처럼 계속 켜두고, Cloudflare Tunnel로 밖에 노출"**하는 방식입니다. 정식으로 "배포"한다고 하면 크게 세 갈래 방향이 있습니다.

### 옵션 A. 지금 방식 그대로, PC를 상시 서버로 사용
- 장점: 추가 비용 없음, 지금 만든 그대로 씀
- 단점: PC가 꺼지거나 절전모드, 인터넷 끊김, Docker Desktop 재시작 등에 그대로 서비스 다운됨. 개인 PC라 안정성이 낮음 (데모/개인 프로젝트용으로만 적합)
- 이 방향이면: 4-2단계(도메인 확보 + Named Tunnel)로 전환해서 URL을 고정시키는 게 사실상 필수

### 옵션 B. 클라우드 VM에 지금 docker-compose를 그대로 올리기
- 예: AWS EC2 / GCP Compute Engine / NCP 서버 / Vultr 등에 Docker만 설치하고 이 레포를 클론 → `docker compose up -d --build`
- 지금 구조가 거의 그대로 이식 가능 (Rocky Linux 베이스, docker-compose 그대로 사용)
- Cloudflare Tunnel을 계속 쓸 수도 있고(포트 개방 불필요), 원한다면 VM의 공인 IP로 직접 노출도 가능
- 장점: PC를 안 켜놔도 됨, 안정성 확보. 단점: 서버 비용 발생

### 옵션 C. 관리형 플랫폼 (Railway, Fly.io, Render 등) + 관리형 DB
- backend/frontend는 각 플랫폼에 컨테이너로 배포, DB는 직접 운영하는 대신 관리형 Postgres(RDS, Supabase, Neon 등) 사용
- 장점: 서버 운영 부담(백업, 패치, 모니터링)을 플랫폼에 위임. 단점: 지금 만든 `docker/db` 커스텀 이미지는 관리형 DB로 대체되므로 그대로는 못 씀, 비용도 옵션 B보다 대체로 비쌈

### 어떤 방향이든 공통으로 신경 써야 할 것

- **시크릿 관리**: `.env`는 절대 커밋 금지(지금도 `.gitignore` 처리됨). 클라우드 배포 시엔 `.env` 파일 대신 플랫폼의 Secret 관리 기능(AWS Secrets Manager, GitHub Actions Secrets 등) 사용을 권장
- **DB 백업**: 지금은 named volume에만 데이터가 있고 백업 스크립트가 없음. `pg_dump`를 주기적으로 돌리거나, 클라우드 이전 시 관리형 DB의 자동 백업 기능을 쓰는 걸 권장 — 지금 상태로 컨테이너/볼륨을 실수로 지우면 데이터가 완전히 사라짐
- **Cloudflare Quick Tunnel은 데모용**: URL이 매번 바뀌므로 소셜 로그인(카카오/네이버/구글) redirect-uri 등록이 계속 깨짐. 실사용하려면 도메인을 확보해서 4-2단계(Named Tunnel)로 전환 필요
- **이미지 용량**: Rocky Linux 풀 이미지 기반이라 slim/alpine 계열보다 이미지가 큼(특히 backend build stage). 클라우드에 올릴 때 빌드 시간·디스크 여유를 고려. (필요하면 나중에 `rockylinux:9-minimal` 비중을 늘려 최적화 가능하지만, 지금은 요청하신 "Rocky Linux로 통일" 방침을 우선함)
- **CI/CD 없음**: 지금은 사람이 직접 `docker compose build/up`을 실행하는 수동 배포. 배포를 자주 하게 되면 GitHub Actions 등으로 "push하면 자동 빌드→배포" 파이프라인을 구성하는 걸 고려할 만함 (지금 범위 밖이라 별도 요청 시 진행)
- **DB 마이그레이션 운영**: Flyway가 기동 시 자동으로 최신 버전까지 적용됨. 운영 DB에 적용할 마이그레이션은 컬럼/테이블 삭제 같은 파괴적 변경을 특히 주의해서 작성해야 함 (되돌리기 어려움)

---

## 트러블슈팅

- **`wsl --status` 오류 / WSL 기능 비활성 상태**: Windows 기능(WSL, VirtualMachinePlatform)이 꺼져 있는 경우. 활성화 후 반드시 재부팅 필요
- **Docker Desktop이 시작되지 않음**: BIOS에서 가상화(Intel VT-x/AMD-V)가 켜져 있는지 확인
- **Rocky Linux 이미지에서 dnf 패키지 설치가 느림/실패**: `--nogpgcheck` 남용하지 말고, 레포 GPG 키 등록이 제대로 됐는지 우선 확인
- **PostgreSQL 데이터가 컨테이너 재생성 시 날아감**: named volume(`db-data`)이 compose 파일에 제대로 선언·마운트됐는지 확인
- **Cloudflare Quick Tunnel URL이 계속 바뀜**: 정상 동작. 영구 주소가 필요하면 4-2단계(Named Tunnel + 도메인)로 전환

---

## 진행 상황 로그

### 2026-07-24

- [x] Docker Desktop 구동 전제조건 확인 → 이 PC에 WSL2/Docker Desktop 모두 미설치 상태였음
- [x] 관리자 권한으로 Windows 기능 활성화 완료
  - `Microsoft-Windows-Subsystem-Linux`
  - `VirtualMachinePlatform`
  - (`Enable-WindowsOptionalFeature -Online ... -NoRestart` 로 실행, 재부팅은 보류)
- [x] 재부팅 완료 (사용자가 직접 진행)
- [x] 재부팅 후 `wsl --update`로 WSL2 커널 최신화
- [x] Docker Desktop 확인 → 이미 설치되어 있었음(`29.6.2`), 앱만 기동되어 있지 않아 실행함
- [x] `Dockerfile`(backend), `frontend/Dockerfile`, `docker/db/Dockerfile`(+`docker-entrypoint.sh`), `docker/cloudflared/Dockerfile` 작성
- [x] `docker-compose.yml`, `.env.example` 작성, `.gitignore`에 `.env` 추가
- [x] `docker compose build` 전체 통과, `docker compose up -d`로 4개 컨테이너(db/backend/frontend/cloudflared) 전부 기동 확인
  - backend `actuator/health` → `UP`, frontend → HTTP 200
  - Cloudflare Quick Tunnel 정상 연결 확인 (URL은 매 실행마다 바뀜)
- [x] Cloudflare 상태: 계정 방금 신규 가입, **연결된 도메인 없음** → 현재는 Quick Tunnel로 검증 완료. 도메인 확보되면 4-2단계(Named Tunnel)로 전환 필요
- [x] 기동 검증 중 발견한 기존 버그 2건도 같이 수정함 (Docker 도입과 무관하게 이미 있던 문제)
  - `frontend/tsconfig.node.json`: project reference에 `composite` 누락 + `noEmit`/`allowImportingTsExtensions` 충돌로 `npm run build`가 원래도 실패하는 상태였음 → `composite: true` + `emitDeclarationOnly: true`로 수정
  - `build.gradle`: Spring Boot 4.1부터 자동설정이 모듈별로 쪼개지면서 Flyway autoconfigure(`org.springframework.boot:spring-boot-flyway`)가 클래스패스에 없어 Flyway가 아예 동작하지 않고 있었음 → 의존성 추가로 해결. (PR #14에서 Flyway 도입 시엔 실제 Postgres로 검증 못 했었음 — 이번에 처음으로 진짜 기동 검증됨)

- [x] `.env`에 실제 랜덤 시크릿 값 채움 (`JWT_SECRET`, `DB_PASSWORD` — 직접 생성. OAuth 값은 실제 콘솔 등록이 필요해 비워둠, compose 기본값으로 대체됨)
- [x] Cloudflare Tunnel이 frontend만 노출하는 구조라 브라우저에서 API 호출이 안 되던 문제 발견 → nginx에 `/api/` 리버스 프록시 + SPA `try_files` 추가, `VITE_API_BASE_URL` 기본값을 상대경로로 변경 → `docker exec`로 컨테이너 내부에서 `/api/v1/products` 프록시 동작 확인 완료
- [x] `server.forward-headers-strategy=framework` 추가 (nginx 뒤에서 X-Forwarded 헤더 신뢰)
- [x] 루트 + `frontend/`에 `.dockerignore` 추가
- [x] `db/seed.sql`로 테스트 데이터 삽입 완료 (상품/회원/주문 등), API로 조회 확인
- [x] "다른 컴퓨터에서 동일 환경 세팅하기", "배포 방법 & 신경 써야 할 점", "컨테이너 구조 & 요청 흐름" 섹션 문서화

**다음 세션 진입 포인트**: 인프라 파일 일체 작성 + 로컬 Docker 기동 검증 + API 프록시 + seed 데이터까지 완료. 다음은 (1) 도메인 확보되면 4-2단계(Named Tunnel)로 전환, 또는 (2) 다른 기능 개발 이어가기.

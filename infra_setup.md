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
  - build stage: `rockylinux:9` + NodeSource/AppStream nodejs 20 → `npm ci && npm run build`
  - run stage: `rockylinux:9` + `dnf install nginx` → 빌드 결과물을 nginx 문서 루트로 복사
- **cloudflared**: `rockylinux:9` + Cloudflare 공식 RPM 레포 등록 → `dnf install cloudflared`

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

**다음 세션 진입 포인트**: 인프라 파일 일체 작성 + 로컬 Docker 기동 검증까지 완료. 다음은 (1) 도메인 확보되면 4-2단계(Named Tunnel)로 전환, 또는 (2) 다른 기능 개발 이어가기.

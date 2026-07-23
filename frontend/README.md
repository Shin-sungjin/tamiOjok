# tamiOjok Frontend

tamiOjok 백엔드 API를 사용하는 React + TypeScript + Vite 프론트엔드입니다. React 학습을 위한 **최소 스캐폴딩** 단계로, 로그인/회원가입/상품목록/상품상세 화면만 구현되어 있습니다. 나머지(장바구니, 주문, 결제, 마이페이지 등)는 이 구조를 참고해서 직접 확장해보세요.

## 1. Node.js 설치

이 저장소를 만든 환경에는 Node.js가 설치되어 있지 않아 `npm install`을 대신 실행해드리지 못했습니다. 아래 방법으로 먼저 설치하세요.

- [nodejs.org](https://nodejs.org)에서 LTS 버전(20.x 이상) 설치, 또는
- Windows라면 [nvm-windows](https://github.com/coreybutler/nvm-windows)로 버전 관리하며 설치

설치 후 확인:

```bash
node -v   # v20.x 이상
npm -v
```

## 2. 프로젝트 설정

```bash
cd frontend
npm install
cp .env.example .env.local   # 필요하면 VITE_API_BASE_URL 값 수정
```

## 3. 개발 서버 실행

```bash
npm run dev
```

`http://localhost:5173`에서 열립니다. 백엔드(`http://localhost:8080`)가 함께 떠 있어야 API 호출이 됩니다. (백엔드 `application.properties`의 `app.cors.allowed-origins`가 `http://localhost:5173`을 허용하도록 이미 설정되어 있습니다. 포트를 바꾸면 그 값도 같이 바꿔주세요.)

## 4. 폴더 구조

```
src/
├── api/            axios 클라이언트, 도메인별 API 함수, 백엔드 DTO와 맞춘 타입
│   ├── client.ts       Access Token 자동 첨부 + 401 시 자동 refresh
│   ├── tokenStore.ts   Access Token 메모리 저장소 (새로고침 시 초기화됨)
│   ├── auth.ts         회원가입/로그인/로그아웃/재발급/내 정보
│   ├── products.ts     상품 목록/상세
│   ├── errors.ts       에러 응답을 사람이 읽을 메시지로 변환
│   └── types.ts        백엔드 DTO 타입
├── context/
│   └── AuthContext.tsx 로그인 상태를 앱 전역에서 공유 (useAuth 훅)
├── components/
│   ├── Header.tsx          로그인 상태에 따른 상단 네비게이션
│   └── ProtectedRoute.tsx  로그인 필요한 페이지를 감싸는 래퍼 (아직 미사용, 예시용)
├── pages/
│   ├── LoginPage.tsx
│   ├── SignupPage.tsx
│   ├── ProductListPage.tsx
│   └── ProductDetailPage.tsx
├── App.tsx         라우트 정의
└── main.tsx         앱 진입점 (BrowserRouter + AuthProvider)
```

## 5. 인증 흐름 이해하기

1. 로그인 성공 시 백엔드가 Access Token은 응답 JSON으로, Refresh Token은 `HttpOnly` 쿠키로 내려줍니다.
2. Access Token은 `tokenStore.ts`(그냥 메모리 변수)에 저장하고, `client.ts`의 요청 인터셉터가 매 요청에 `Authorization` 헤더로 붙입니다.
3. 새로고침하면 메모리가 초기화되므로, `AuthContext`가 앱 시작 시 `/api/v1/auth/refresh`를 한 번 호출해서(쿠키는 브라우저가 자동으로 보냄) Access Token을 재발급받고 로그인 상태를 복원합니다.
4. API 응답이 401이면 `client.ts`가 자동으로 한 번 refresh를 시도한 뒤 원래 요청을 재시도합니다. 그래도 실패하면 로그아웃 처리됩니다.

## 6. 다음에 직접 확장해볼 것들 (제안)

- `POST /api/v1/cart/items` 연동 → `CartPage` 만들고 `ProductDetailPage`에 "장바구니 담기" 버튼 추가
- `useAuth()`의 `user.status === 'NEED_INFO'`일 때 추가정보 입력 페이지로 유도
- `ProtectedRoute`로 장바구니/주문/마이페이지 감싸기
- 주문 생성 → 결제 승인 → 배송 조회까지 이어지는 체크아웃 플로우
- 전체 API 목록은 [`../docs/USER_MANUAL.md`](../docs/USER_MANUAL.md) 참고

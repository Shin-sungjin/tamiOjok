// Access Token은 메모리에만 보관합니다 (새로고침하면 사라지고, 대신
// 아래 client.ts가 앱 시작 시 /auth/refresh를 한 번 호출해 재발급받습니다).
// Refresh Token은 서버가 내려주는 HttpOnly 쿠키에만 저장되므로 여기서 다루지 않습니다.

let accessToken: string | null = null

export function getAccessToken(): string | null {
  return accessToken
}

export function setAccessToken(token: string | null): void {
  accessToken = token
}

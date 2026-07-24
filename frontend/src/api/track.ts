import { apiClient } from './client'

const VISIT_FLAG_KEY = 'tamiojok:visit-tracked'

// 세션당 한 번만 방문을 기록합니다 (관리자 대시보드의 "오늘 방문자수" 집계용).
export function trackVisitOnce(): void {
  if (sessionStorage.getItem(VISIT_FLAG_KEY)) {
    return
  }
  sessionStorage.setItem(VISIT_FLAG_KEY, '1')
  apiClient.post('/api/v1/track/visit').catch(() => undefined)
}

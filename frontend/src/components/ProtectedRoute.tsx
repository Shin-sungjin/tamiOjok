import type { ReactNode } from 'react'
import { Navigate, useLocation } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

export function ProtectedRoute({ children }: { children: ReactNode }) {
  const { user, isLoading } = useAuth()
  const location = useLocation()

  if (isLoading) {
    return <p className="page">로딩 중...</p>
  }

  if (!user) {
    return <Navigate to="/login" replace />
  }

  // OAuth 최초 로그인 등으로 필수 정보(연락처/배송지)가 없으면 결제 전에 채워넣도록 강제합니다.
  if (user.status === 'NEED_INFO' && location.pathname !== '/additional-info') {
    return <Navigate to="/additional-info" replace />
  }

  return <>{children}</>
}

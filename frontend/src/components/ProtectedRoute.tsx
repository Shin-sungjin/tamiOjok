import type { ReactNode } from 'react'
import { Navigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

// 로그인이 필요한 화면(장바구니, 주문 등)을 앞으로 추가할 때
// <ProtectedRoute><CartPage /></ProtectedRoute> 형태로 감싸서 사용하세요.
export function ProtectedRoute({ children }: { children: ReactNode }) {
  const { user, isLoading } = useAuth()

  if (isLoading) {
    return <p>로딩 중...</p>
  }

  if (!user) {
    return <Navigate to="/login" replace />
  }

  return <>{children}</>
}

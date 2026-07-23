import { createContext, useCallback, useContext, useEffect, useState, type ReactNode } from 'react'
import * as authApi from '../api/auth'
import { setAccessToken } from '../api/tokenStore'
import type { LoginRequest, SignupRequest, UserResponse } from '../api/types'

interface AuthContextValue {
  user: UserResponse | null
  isLoading: boolean
  login: (request: LoginRequest) => Promise<void>
  signup: (request: SignupRequest) => Promise<void>
  logout: () => Promise<void>
  refreshUser: () => Promise<void>
}

const AuthContext = createContext<AuthContextValue | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<UserResponse | null>(null)
  const [isLoading, setIsLoading] = useState(true)

  // 앱을 새로고침해도 refreshToken 쿠키가 살아있다면 자동으로 재로그인합니다.
  useEffect(() => {
    authApi
      .refresh()
      .then((tokens) => {
        setAccessToken(tokens.accessToken)
        return authApi.getMe()
      })
      .then(setUser)
      .catch(() => {
        setAccessToken(null)
        setUser(null)
      })
      .finally(() => setIsLoading(false))
  }, [])

  const login = useCallback(async (request: LoginRequest) => {
    const tokens = await authApi.login(request)
    setAccessToken(tokens.accessToken)
    setUser(await authApi.getMe())
  }, [])

  const signup = useCallback(async (request: SignupRequest) => {
    await authApi.signup(request)
  }, [])

  const logout = useCallback(async () => {
    await authApi.logout().catch(() => undefined)
    setAccessToken(null)
    setUser(null)
  }, [])

  const refreshUser = useCallback(async () => {
    setUser(await authApi.getMe())
  }, [])

  return (
    <AuthContext.Provider value={{ user, isLoading, login, signup, logout, refreshUser }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth는 AuthProvider 내부에서만 사용할 수 있습니다.')
  }
  return context
}

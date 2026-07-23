import { apiClient } from './client'
import type { LoginRequest, SignupRequest, TokenResponse, UserResponse } from './types'

export async function signup(request: SignupRequest): Promise<void> {
  await apiClient.post('/api/v1/auth/signup', request)
}

export async function login(request: LoginRequest): Promise<TokenResponse> {
  const response = await apiClient.post<TokenResponse>('/api/v1/auth/login', request)
  return response.data
}

export async function refresh(): Promise<TokenResponse> {
  const response = await apiClient.post<TokenResponse>('/api/v1/auth/refresh')
  return response.data
}

export async function logout(): Promise<void> {
  await apiClient.post('/api/v1/auth/logout')
}

export async function getMe(): Promise<UserResponse> {
  const response = await apiClient.get<UserResponse>('/api/v1/users/me')
  return response.data
}

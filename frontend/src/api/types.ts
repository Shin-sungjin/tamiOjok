// 백엔드 DTO(record)와 1:1로 대응하는 타입 정의.
// src/main/java/.../domain/**/dto/response 를 참고해 필드명을 맞췄습니다.

export type Role = 'USER' | 'ADMIN'
export type UserStatus = 'ACTIVE' | 'NEED_INFO' | 'SUSPENDED'
export type Provider = 'LOCAL' | 'KAKAO' | 'NAVER' | 'GOOGLE'
export type ProductStatus = 'ON_SALE' | 'OUT_OF_STOCK' | 'HIDDEN'

export interface TokenResponse {
  accessToken: string
  needAdditionalInfo: boolean
}

export interface UserResponse {
  id: number
  email: string
  name: string
  phoneNumber: string | null
  provider: Provider
  role: Role
  status: UserStatus
}

export interface ProductResponse {
  id: number
  name: string
  price: number
  description: string | null
  status: ProductStatus
  availableStock: number
  createdAt: string
}

export interface PageResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
  size: number
  first: boolean
  last: boolean
}

export interface ErrorResponse {
  code: string
  message: string
  errors: string[]
}

export interface SignupRequest {
  email: string
  password: string
  name: string
  phoneNumber?: string
}

export interface LoginRequest {
  email: string
  password: string
}

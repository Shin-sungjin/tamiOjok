// 백엔드 DTO(record)와 1:1로 대응하는 타입 정의.
// src/main/java/.../domain/**/dto/response 를 참고해 필드명을 맞췄습니다.

export type Role = 'USER' | 'ADMIN'
export type UserStatus = 'ACTIVE' | 'NEED_INFO' | 'SUSPENDED'
export type Provider = 'LOCAL' | 'KAKAO' | 'NAVER' | 'GOOGLE'
export type ProductStatus = 'ON_SALE' | 'OUT_OF_STOCK' | 'HIDDEN'
export type OrderStatus = 'PENDING_PAYMENT' | 'PAYMENT_COMPLETED' | 'PREPARING' | 'CANCELLED'
export type PaymentStatus = 'READY' | 'PAID' | 'FAILED' | 'CANCELLED'
export type DeliveryStatus = 'READY' | 'SHIPPED' | 'IN_TRANSIT' | 'DELIVERED' | 'RETURN_REQUESTED'
export type InquiryStatus = 'WAITING' | 'ANSWERED'
export type DiscountType = 'FIXED_AMOUNT' | 'PERCENTAGE'
export type UserCouponStatus = 'AVAILABLE' | 'USED'

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

export interface AdditionalInfoRequest {
  phoneNumber: string
  recipientName: string
  recipientPhone: string
  zipcode: string
  addressMain: string
  addressDetail?: string
}

export interface CartItemResponse {
  id: number
  productId: number
  productName: string
  price: number
  quantity: number
  lineTotal: number
}

export interface CartResponse {
  id: number | null
  items: CartItemResponse[]
  totalAmount: number
}

export interface CartItemAddRequest {
  productId: number
  quantity: number
}

export interface OrderItemResponse {
  productId: number
  productName: string
  orderPrice: number
  quantity: number
  lineTotal: number
}

export interface OrderResponse {
  id: number
  orderNumber: string
  status: OrderStatus
  totalAmount: number
  discountAmount: number
  paymentAmount: number
  items: OrderItemResponse[]
  createdAt: string
}

export interface OrderCreateRequest {
  items: { productId: number; quantity: number }[]
  userCouponId?: number
}

export interface PaymentConfirmRequest {
  orderId: number
  pgProvider: string
  pgTransactionId: string
  paidAmount: number
}

export interface PaymentResponse {
  id: number
  orderId: number
  pgProvider: string
  status: PaymentStatus
  requestedAmount: number
  paidAmount: number
  paidAt: string | null
}

export interface DeliveryResponse {
  id: number
  orderId: number
  courierCode: string
  trackingNumber: string
  status: DeliveryStatus
  shippedAt: string | null
  deliveredAt: string | null
}

export interface ReviewCreateRequest {
  orderId: number
  productId: number
  rating: number
  content: string
}

export interface ReviewUpdateRequest {
  rating: number
  content: string
}

export interface ReviewResponse {
  id: number
  orderId: number
  productId: number
  productName: string
  rating: number
  content: string
  createdAt: string
}

export interface InquiryCreateRequest {
  category: string
  title: string
  content: string
  orderId?: number
}

export interface InquiryResponse {
  id: number
  orderId: number | null
  category: string
  title: string
  content: string
  answer: string | null
  status: InquiryStatus
  createdAt: string
  answeredAt: string | null
}

export interface CouponResponse {
  id: number
  code: string
  name: string
  discountType: DiscountType
  discountValue: number
  minOrderAmount: number
  maxDiscountAmount: number | null
  validFrom: string
  validUntil: string
}

export interface UserCouponResponse {
  id: number
  couponId: number
  code: string
  name: string
  discountType: DiscountType
  discountValue: number
  status: UserCouponStatus
  issuedAt: string
  usedAt: string | null
}

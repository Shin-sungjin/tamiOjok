import { apiClient } from './client'
import type { CouponResponse, PageResponse, UserCouponResponse } from './types'

export async function getAvailableCoupons(page = 0, size = 10): Promise<PageResponse<CouponResponse>> {
  const response = await apiClient.get<PageResponse<CouponResponse>>('/api/v1/coupons', {
    params: { page, size },
  })
  return response.data
}

export async function issueCoupon(couponId: number): Promise<UserCouponResponse> {
  const response = await apiClient.post<UserCouponResponse>(`/api/v1/coupons/${couponId}/issue`)
  return response.data
}

export async function getMyCoupons(page = 0, size = 10): Promise<PageResponse<UserCouponResponse>> {
  const response = await apiClient.get<PageResponse<UserCouponResponse>>('/api/v1/coupons/my', {
    params: { page, size },
  })
  return response.data
}

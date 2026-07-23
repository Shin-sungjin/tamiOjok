import { apiClient } from './client'
import type { OrderCreateRequest, OrderResponse, PageResponse } from './types'

export async function createOrder(request: OrderCreateRequest): Promise<OrderResponse> {
  const response = await apiClient.post<OrderResponse>('/api/v1/orders', request)
  return response.data
}

export async function createOrderFromCart(userCouponId?: number): Promise<OrderResponse> {
  const response = await apiClient.post<OrderResponse>('/api/v1/orders/from-cart', null, {
    params: userCouponId ? { userCouponId } : undefined,
  })
  return response.data
}

export async function getMyOrders(page = 0, size = 10): Promise<PageResponse<OrderResponse>> {
  const response = await apiClient.get<PageResponse<OrderResponse>>('/api/v1/orders', {
    params: { page, size },
  })
  return response.data
}

export async function getMyOrder(orderId: number): Promise<OrderResponse> {
  const response = await apiClient.get<OrderResponse>(`/api/v1/orders/${orderId}`)
  return response.data
}

export async function cancelOrder(orderId: number): Promise<void> {
  await apiClient.delete(`/api/v1/orders/${orderId}`)
}

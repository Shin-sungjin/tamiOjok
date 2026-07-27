import { apiClient } from './client'
import type { DeliveryResponse, DeliveryReturnRequest } from './types'

export async function getDelivery(orderId: number): Promise<DeliveryResponse> {
  const response = await apiClient.get<DeliveryResponse>(`/api/v1/orders/${orderId}/delivery`)
  return response.data
}

export async function requestReturn(orderId: number, request: DeliveryReturnRequest): Promise<void> {
  await apiClient.post(`/api/v1/orders/${orderId}/delivery/return-request`, request)
}

import { apiClient } from './client'
import type { PaymentConfirmRequest, PaymentResponse } from './types'

export async function confirmPayment(request: PaymentConfirmRequest): Promise<PaymentResponse> {
  const response = await apiClient.post<PaymentResponse>('/api/v1/payments/confirm', request)
  return response.data
}

export async function getPayment(orderId: number): Promise<PaymentResponse> {
  const response = await apiClient.get<PaymentResponse>(`/api/v1/payments/orders/${orderId}`)
  return response.data
}

import { apiClient } from './client'
import type { CartItemAddRequest, CartResponse } from './types'

export async function getMyCart(): Promise<CartResponse> {
  const response = await apiClient.get<CartResponse>('/api/v1/cart')
  return response.data
}

export async function addItem(request: CartItemAddRequest): Promise<CartResponse> {
  const response = await apiClient.post<CartResponse>('/api/v1/cart/items', request)
  return response.data
}

export async function updateItemQuantity(cartItemId: number, quantity: number): Promise<CartResponse> {
  const response = await apiClient.put<CartResponse>(`/api/v1/cart/items/${cartItemId}`, { quantity })
  return response.data
}

export async function removeItem(cartItemId: number): Promise<void> {
  await apiClient.delete(`/api/v1/cart/items/${cartItemId}`)
}

export async function clearCart(): Promise<void> {
  await apiClient.delete('/api/v1/cart')
}

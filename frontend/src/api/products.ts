import { apiClient } from './client'
import type { PageResponse, ProductResponse } from './types'

export async function getProducts(
  page = 0,
  size = 12,
  keyword?: string,
): Promise<PageResponse<ProductResponse>> {
  const response = await apiClient.get<PageResponse<ProductResponse>>('/api/v1/products', {
    params: { page, size, keyword: keyword || undefined },
  })
  return response.data
}

export async function getProduct(productId: number): Promise<ProductResponse> {
  const response = await apiClient.get<ProductResponse>(`/api/v1/products/${productId}`)
  return response.data
}

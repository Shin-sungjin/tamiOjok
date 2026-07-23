import { apiClient } from './client'
import type { PageResponse, ReviewCreateRequest, ReviewResponse, ReviewUpdateRequest } from './types'

export async function createReview(request: ReviewCreateRequest): Promise<ReviewResponse> {
  const response = await apiClient.post<ReviewResponse>('/api/v1/reviews', request)
  return response.data
}

export async function getMyReviews(page = 0, size = 10): Promise<PageResponse<ReviewResponse>> {
  const response = await apiClient.get<PageResponse<ReviewResponse>>('/api/v1/reviews', {
    params: { page, size },
  })
  return response.data
}

export async function updateReview(reviewId: number, request: ReviewUpdateRequest): Promise<ReviewResponse> {
  const response = await apiClient.put<ReviewResponse>(`/api/v1/reviews/${reviewId}`, request)
  return response.data
}

export async function deleteReview(reviewId: number): Promise<void> {
  await apiClient.delete(`/api/v1/reviews/${reviewId}`)
}

export async function getProductReviews(
  productId: number,
  page = 0,
  size = 10,
): Promise<PageResponse<ReviewResponse>> {
  const response = await apiClient.get<PageResponse<ReviewResponse>>(`/api/v1/products/${productId}/reviews`, {
    params: { page, size },
  })
  return response.data
}

import { apiClient } from './client'
import type { InquiryCreateRequest, InquiryResponse, PageResponse } from './types'

export async function createInquiry(request: InquiryCreateRequest): Promise<InquiryResponse> {
  const response = await apiClient.post<InquiryResponse>('/api/v1/inquiries', request)
  return response.data
}

export async function getMyInquiries(page = 0, size = 10): Promise<PageResponse<InquiryResponse>> {
  const response = await apiClient.get<PageResponse<InquiryResponse>>('/api/v1/inquiries', {
    params: { page, size },
  })
  return response.data
}

export async function getMyInquiry(inquiryId: number): Promise<InquiryResponse> {
  const response = await apiClient.get<InquiryResponse>(`/api/v1/inquiries/${inquiryId}`)
  return response.data
}

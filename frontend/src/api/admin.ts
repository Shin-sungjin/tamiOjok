import { apiClient } from './client'
import type {
  AdminOrderResponse,
  DashboardSummaryResponse,
  DeliveryCreateRequest,
  DeliveryResponse,
  InquiryResponse,
  InquiryStatus,
  OrderStatus,
  PageResponse,
  ProductCreateRequest,
  ProductResponse,
  ProductStatus,
  ProductUpdateRequest,
} from './types'

export async function getDashboardSummary(): Promise<DashboardSummaryResponse> {
  const response = await apiClient.get<DashboardSummaryResponse>('/api/v1/admin/dashboard/summary')
  return response.data
}

export async function getAdminInquiries(
  status?: InquiryStatus,
  page = 0,
  size = 10,
): Promise<PageResponse<InquiryResponse>> {
  const response = await apiClient.get<PageResponse<InquiryResponse>>('/api/v1/admin/inquiries', {
    params: { status, page, size },
  })
  return response.data
}

export async function answerInquiry(inquiryId: number, answer: string): Promise<InquiryResponse> {
  const response = await apiClient.post<InquiryResponse>(`/api/v1/admin/inquiries/${inquiryId}/answer`, {
    answer,
  })
  return response.data
}

export async function getAdminProducts(
  status?: ProductStatus,
  page = 0,
  size = 20,
): Promise<PageResponse<ProductResponse>> {
  const response = await apiClient.get<PageResponse<ProductResponse>>('/api/v1/admin/products', {
    params: { status, page, size },
  })
  return response.data
}

export async function createProduct(request: ProductCreateRequest): Promise<void> {
  await apiClient.post('/api/v1/admin/products', request)
}

export async function updateProduct(productId: number, request: ProductUpdateRequest): Promise<void> {
  await apiClient.put(`/api/v1/admin/products/${productId}`, request)
}

export async function changeProductStatus(productId: number, status: ProductStatus): Promise<void> {
  await apiClient.put(`/api/v1/admin/products/${productId}/status/${status}`)
}

export async function restockProduct(productId: number, quantity: number): Promise<void> {
  await apiClient.post(`/api/v1/admin/products/${productId}/stock/restock`, { quantity })
}

export async function getAdminOrders(
  status?: OrderStatus,
  page = 0,
  size = 10,
): Promise<PageResponse<AdminOrderResponse>> {
  const response = await apiClient.get<PageResponse<AdminOrderResponse>>('/api/v1/admin/orders', {
    params: { status, page, size },
  })
  return response.data
}

export async function getAdminOrder(orderId: number): Promise<AdminOrderResponse> {
  const response = await apiClient.get<AdminOrderResponse>(`/api/v1/admin/orders/${orderId}`)
  return response.data
}

export async function startPreparingOrder(orderId: number): Promise<void> {
  await apiClient.post(`/api/v1/admin/orders/${orderId}/prepare`)
}

export async function createDelivery(orderId: number, request: DeliveryCreateRequest): Promise<DeliveryResponse> {
  const response = await apiClient.post<DeliveryResponse>(`/api/v1/admin/orders/${orderId}/delivery`, request)
  return response.data
}

import { apiClient } from './client'
import type { AdditionalInfoRequest } from './types'

export async function completeAdditionalInfo(request: AdditionalInfoRequest): Promise<void> {
  await apiClient.post('/api/v1/users/me/additional-info', request)
}

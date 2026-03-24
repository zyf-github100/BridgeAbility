import { apiRequest } from '../lib/http'
import type { PageResponse } from './jobseeker'
import type { ServiceAlert, ServiceAlertStatusPayload } from './service'

export interface AdminRiskQuery {
  page?: number
  pageSize?: number
  status?: string
  level?: number
}

export function getAdminRiskRecords(token: string, query: AdminRiskQuery = {}) {
  const searchParams = new URLSearchParams()

  Object.entries(query).forEach(([key, value]) => {
    if (value === undefined || value === null || value === '') {
      return
    }
    searchParams.set(key, String(value))
  })

  const suffix = searchParams.size > 0 ? `?${searchParams.toString()}` : ''

  return apiRequest<PageResponse<ServiceAlert>>(`/api/admin/risk-records${suffix}`, {
    method: 'GET',
    token,
  })
}

export function updateAdminRiskRecordStatus(
  token: string,
  alertId: string,
  payload: ServiceAlertStatusPayload,
) {
  return apiRequest<ServiceAlert>(`/api/admin/risk-records/${alertId}/status`, {
    method: 'POST',
    token,
    body: JSON.stringify(payload),
  })
}

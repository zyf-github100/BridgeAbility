import { apiRequest } from '../lib/http'

export interface AdminIssueItem {
  id: string
  issueType: 'APPEAL' | 'DATA_CORRECTION'
  sourceRole: string
  sourceUserId?: number | null
  sourceName: string
  title: string
  content: string
  relatedType?: string | null
  relatedId?: string | null
  severityLevel: number
  ticketStatus: 'PENDING' | 'IN_PROGRESS' | 'RESOLVED' | 'REJECTED'
  resolutionNote?: string | null
  handledBy?: string | null
  handledAt?: string | null
  createdAt: string
}

export interface AdminIssueQuery {
  issueType?: 'APPEAL' | 'DATA_CORRECTION'
  status?: 'PENDING' | 'IN_PROGRESS' | 'RESOLVED' | 'REJECTED'
}

export interface AdminIssueStatusPayload {
  targetStatus: 'PENDING' | 'IN_PROGRESS' | 'RESOLVED' | 'REJECTED'
  resolutionNote: string
  handlerName: string
}

export function getAdminIssues(token: string, query: AdminIssueQuery = {}) {
  const searchParams = new URLSearchParams()

  Object.entries(query).forEach(([key, value]) => {
    if (!value) {
      return
    }
    searchParams.set(key, value)
  })

  const suffix = searchParams.size > 0 ? `?${searchParams.toString()}` : ''

  return apiRequest<AdminIssueItem[]>(`/api/admin/issues${suffix}`, {
    method: 'GET',
    token,
  })
}

export function updateAdminIssueStatus(token: string, issueId: string, payload: AdminIssueStatusPayload) {
  return apiRequest<AdminIssueItem>(`/api/admin/issues/${issueId}/status`, {
    method: 'POST',
    token,
    body: JSON.stringify(payload),
  })
}

import { apiRequest } from '../lib/http'

export type NotificationTargetRole =
  | 'ALL'
  | 'ROLE_JOBSEEKER'
  | 'ROLE_ENTERPRISE'
  | 'ROLE_SERVICE_ORG'
  | 'ROLE_ADMIN'

export interface NotificationItem {
  id: string
  type: string
  title: string
  content: string
  targetRole: NotificationTargetRole
  createdAt: string
  read: boolean
}

export interface PublishAnnouncementPayload {
  title: string
  content: string
  targetRole: NotificationTargetRole
}

export interface NotificationQuery {
  read?: boolean
  targetRole?: NotificationTargetRole
}

export function getNotifications(token: string, query: NotificationQuery = {}) {
  const params = new URLSearchParams()
  if (typeof query.read === 'boolean') {
    params.set('read', String(query.read))
  }
  if (query.targetRole) {
    params.set('targetRole', query.targetRole)
  }
  const suffix = params.size > 0 ? `?${params.toString()}` : ''

  return apiRequest<NotificationItem[]>(`/api/notifications${suffix}`, {
    method: 'GET',
    token,
  })
}

export function markNotificationRead(token: string, notificationId: string) {
  return apiRequest<boolean>(`/api/notifications/${notificationId}/read`, {
    method: 'POST',
    token,
  })
}

export function publishSystemAnnouncement(token: string, payload: PublishAnnouncementPayload) {
  return apiRequest<NotificationItem>('/api/admin/notifications', {
    method: 'POST',
    token,
    body: JSON.stringify(payload),
  })
}

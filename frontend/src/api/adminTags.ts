import { apiRequest } from '../lib/http'

export interface TagDictionaryItem {
  id: number
  tagCode: string
  tagName: string
  tagCategory: string
  tagStatus: 'ACTIVE' | 'INACTIVE'
  description: string
  createdAt: string
  updatedAt: string
}

export interface TagDictionaryPayload {
  tagCode: string
  tagName: string
  tagCategory: string
  tagStatus: 'ACTIVE' | 'INACTIVE'
  description?: string
}

export function getAdminTags(token: string) {
  return apiRequest<TagDictionaryItem[]>('/api/admin/tags', {
    method: 'GET',
    token,
  })
}

export function createAdminTag(token: string, payload: TagDictionaryPayload) {
  return apiRequest<TagDictionaryItem>('/api/admin/tags', {
    method: 'POST',
    token,
    body: JSON.stringify(payload),
  })
}

export function updateAdminTag(token: string, tagId: number, payload: TagDictionaryPayload) {
  return apiRequest<TagDictionaryItem>(`/api/admin/tags/${tagId}`, {
    method: 'PUT',
    token,
    body: JSON.stringify(payload),
  })
}

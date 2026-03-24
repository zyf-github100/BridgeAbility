import { apiRequest } from '../lib/http'

export interface PublishedKnowledgeArticle {
  id: string
  title: string
  category: string
  summary: string
  tags: string[]
  publishDate: string
}

export interface AdminKnowledgeArticle {
  id: string
  title: string
  category: string
  summary: string
  content: string
  tags: string[]
  publishStatus: 'PUBLISHED' | 'OFFLINE'
  publishedAt: string
  createdAt: string
  updatedAt: string
}

export interface KnowledgeArticleUpsertPayload {
  title: string
  category: string
  summary: string
  content: string
  tags: string[]
}

export function getAdminKnowledgeArticles(token: string) {
  return apiRequest<AdminKnowledgeArticle[]>('/api/admin/knowledge/articles', {
    method: 'GET',
    token,
  })
}

export function getPublishedKnowledgeArticles() {
  return apiRequest<PublishedKnowledgeArticle[]>('/api/knowledge/articles', {
    method: 'GET',
  })
}

export function createAdminKnowledgeArticle(token: string, payload: KnowledgeArticleUpsertPayload) {
  return apiRequest<AdminKnowledgeArticle>('/api/admin/knowledge/articles', {
    method: 'POST',
    token,
    body: JSON.stringify(payload),
  })
}

export function updateAdminKnowledgeArticle(token: string, articleId: string, payload: KnowledgeArticleUpsertPayload) {
  return apiRequest<AdminKnowledgeArticle>(`/api/admin/knowledge/articles/${articleId}`, {
    method: 'PUT',
    token,
    body: JSON.stringify(payload),
  })
}

export function publishAdminKnowledgeArticle(token: string, articleId: string) {
  return apiRequest<AdminKnowledgeArticle>(`/api/admin/knowledge/articles/${articleId}/publish`, {
    method: 'POST',
    token,
  })
}

export function offlineAdminKnowledgeArticle(token: string, articleId: string) {
  return apiRequest<AdminKnowledgeArticle>(`/api/admin/knowledge/articles/${articleId}/offline`, {
    method: 'POST',
    token,
  })
}

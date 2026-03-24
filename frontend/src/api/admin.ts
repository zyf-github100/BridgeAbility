import { apiRequest } from '../lib/http'
import type { EnterpriseVerificationProfile } from './enterprise'

export interface AdminMetric {
  label: string
  value: string
  hint: string
}

export interface EnterpriseReviewItem {
  userId?: number | null
  company: string
  industry: string
  city: string
  status: string
  note: string
  submittedAt?: string
  materialCount?: number
}

export interface AdminDashboard {
  jobseekerCount: number
  enterpriseCount: number
  publishedJobCount: number
  applicationCount: number
  hiredCount: number
  openAlertCount: number
  metrics: AdminMetric[]
  reviewQueue: EnterpriseReviewItem[]
  auditLogs: string[]
}

export interface AdminUserSummary {
  userId: number
  account: string
  nickname?: string | null
  email?: string | null
  phone?: string | null
  status: number
  statusLabel: string
  roles: string[]
  lastLoginAt?: string | null
  createdAt: string
}

export interface MatchingScoreWeights {
  skill: number
  workMode: number
  communication: number
  environment: number
  accommodation: number
}

export interface MatchingRiskConfig {
  penaltyPerRisk: number
  penaltyPerBlockingRisk: number
  maxPenalty: number
  hardFilteredMaxScore: number
}

export interface MatchingCandidateStageConfig {
  matchScoreWeight: number
  profileCompletionWeight: number
  priorityThreshold: number
  followUpThreshold: number
}

export interface MatchingConfig {
  code: string
  scoreWeights: MatchingScoreWeights
  risk: MatchingRiskConfig
  candidateStage: MatchingCandidateStageConfig
  customized: boolean
  updatedByUserId?: number | null
  updatedAt?: string
}

export function getAdminDashboard(token: string) {
  return apiRequest<AdminDashboard>('/api/admin/dashboard', {
    method: 'GET',
    token,
  })
}

export function getAdminUsers(token: string) {
  return apiRequest<AdminUserSummary[]>('/api/admin/users', {
    method: 'GET',
    token,
  })
}

export function getAdminMatchingConfig(token: string) {
  return apiRequest<MatchingConfig>('/api/admin/matching-config', {
    method: 'GET',
    token,
  })
}

export function updateAdminMatchingConfig(
  token: string,
  payload: {
    scoreWeights: MatchingScoreWeights
    risk: MatchingRiskConfig
    candidateStage: MatchingCandidateStageConfig
  },
) {
  return apiRequest<MatchingConfig>('/api/admin/matching-config', {
    method: 'PUT',
    token,
    body: JSON.stringify(payload),
  })
}

export function resetAdminMatchingConfig(token: string) {
  return apiRequest<MatchingConfig>('/api/admin/matching-config/reset', {
    method: 'POST',
    token,
  })
}

export function getPendingEnterpriseReviews(token: string) {
  return apiRequest<EnterpriseReviewItem[]>('/api/admin/enterprises/pending', {
    method: 'GET',
    token,
  })
}

export function getAuditLogs(token: string) {
  return apiRequest<string[]>('/api/admin/audit-logs', {
    method: 'GET',
    token,
  })
}

export function getEnterpriseReviewDetail(token: string, userId: number) {
  return apiRequest<EnterpriseVerificationProfile>(`/api/admin/enterprises/${userId}/review`, {
    method: 'GET',
    token,
  })
}

export function reviewEnterprise(
  token: string,
  userId: number,
  payload: {
    decision: 'APPROVED' | 'REJECTED'
    note?: string
  },
) {
  return apiRequest<EnterpriseVerificationProfile>(`/api/admin/enterprises/${userId}/review`, {
    method: 'POST',
    token,
    body: JSON.stringify(payload),
  })
}

export type EnterpriseReviewDetail = EnterpriseVerificationProfile

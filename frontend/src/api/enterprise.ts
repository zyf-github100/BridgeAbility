import { apiRequest } from '../lib/http'
import type { PageResponse, ScoreItem } from './jobseeker'

export interface EnterpriseVerificationMaterial {
  id: number
  materialType: string
  materialTypeLabel: string
  originalFileName: string
  contentType: string
  fileSize: number
  note?: string
  uploadedAt: string
}

export interface EnterpriseVerificationProfile {
  userId: number
  companyName: string
  industry: string
  city: string
  unifiedSocialCreditCode: string
  contactName: string
  contactPhone: string
  officeAddress: string
  accessibilityCommitment: string
  verificationStatus: string
  reviewNote?: string
  submittedAt?: string
  reviewedAt?: string
  canPublishJobs: boolean
  publishedJobCount: number
  materials: EnterpriseVerificationMaterial[]
}

export interface EnterpriseVerificationProfileUpsertPayload {
  companyName: string
  industry?: string
  city?: string
  unifiedSocialCreditCode?: string
  contactName?: string
  contactPhone?: string
  officeAddress?: string
  accessibilityCommitment?: string
  submitForReview: boolean
}

export interface EnterpriseJobQuery {
  page?: number
  pageSize?: number
  city?: string
  workMode?: string
  publishStatus?: string
}

export interface EnterpriseCandidateQuery {
  page?: number
  pageSize?: number
  consentGranted?: boolean
}

export interface EnterpriseJobAccessibility {
  onsiteRequired: boolean | null
  remoteSupported: boolean | null
  highFrequencyVoiceRequired: boolean | null
  noisyEnvironment: boolean | null
  longStandingRequired: boolean | null
  textMaterialSupported: boolean | null
  onlineInterviewSupported: boolean | null
  textInterviewSupported: boolean | null
  flexibleScheduleSupported: boolean | null
  accessibleWorkspace: boolean | null
  assistiveSoftwareSupported: boolean | null
}

export interface EnterpriseJobSummary {
  id: string
  title: string
  department: string
  city: string
  workMode: string
  salaryRange: string
  headcount: number
  deadline: string
  publishStatus: string
  stage: string
  matchScore: number
  accessibilityCompletionRate: number
  readyToPublish: boolean
  candidateCount: number
}

export interface EnterpriseJobDetail {
  id: string
  title: string
  companyName: string
  department: string
  city: string
  salaryMin: number
  salaryMax: number
  salaryRange: string
  headcount: number
  description: string
  requirementText: string
  workMode: string
  deadline: string
  interviewMode: string
  publishStatus: string
  stage: string
  matchScore: number
  accessibilityCompletionRate: number
  readyToPublish: boolean
  accessibilityTag: EnterpriseJobAccessibility
  dimensionScores: ScoreItem[]
  reasons: string[]
  risks: string[]
  supports: string[]
  environment: string[]
  applyHint: string
}

export interface EnterpriseJobUpsertPayload {
  title: string
  department: string
  city: string
  salaryMin: number
  salaryMax: number
  headcount: number
  description: string
  requirementText: string
  workMode: string
  deadline: string
  interviewMode: string
  publishStatus: string
  accessibilityTag: EnterpriseJobAccessibility
}

export interface EnterpriseCandidateApplication {
  applicationId: number
  userId?: number
  jobId: string
  jobTitle: string
  companyName: string
  candidateName: string
  city: string
  expectedJob: string
  workModePreference: string
  schoolName: string
  major: string
  intro: string
  profileCompletionRate: number
  matchScore: number
  recommendationStage: string
  recommendationSummary: string
  dimensionScores: ScoreItem[]
  explanationSnapshot: string[]
  status: string
  consentGranted: boolean
  supportVisibility: string
  preferredInterviewMode: string
  coverNote: string
  additionalSupport?: string
  supportSummary: string[]
  submittedAt: string
  latestInterview?: EnterpriseInterviewRecord
  interviewRecords: EnterpriseInterviewRecord[]
  supportRequests: EnterpriseSupportRequest[]
}

export interface EnterpriseInterviewRecord {
  interviewId: number
  interviewTime: string
  interviewMode: string
  interviewerName: string
  inviteNote?: string
  resultStatus: string
  feedbackNote?: string
  rejectReason?: string
  createdAt: string
  updatedAt: string
}

export interface EnterpriseSupportRequest {
  requestType: string
  requestTypeLabel: string
  requestContent: string
}

export interface EnterpriseInterviewInvitePayload {
  applicationId: number
  interviewTime: string
  interviewMode: string
  interviewerName: string
  note?: string
}

export interface EnterpriseInterviewResultPayload {
  applicationId: number
  resultStatus: 'PASS' | 'FAIL'
  applicationStatus?: string
  feedbackNote?: string
  rejectReason?: string
}

export interface EnterpriseApplicationStatusUpdatePayload {
  targetStatus: 'VIEWED' | 'INTERVIEWING' | 'OFFERED' | 'HIRED' | 'REJECTED'
  note?: string
}

export interface EnterpriseStatsBucket {
  code: string
  label: string
  value: number
  hint: string
}

export interface EnterpriseStatsJob {
  jobId: string
  title: string
  publishStatus: string
  stage: string
  accessibilityCompletionRate: number
  candidateCount: number
  interviewingCount: number
  hiredCount: number
  averageMatchScore: number
}

export interface EnterpriseRecruitmentStats {
  totalJobs: number
  publishedJobs: number
  draftJobs: number
  offlineJobs: number
  totalApplications: number
  appliedCount: number
  interviewingCount: number
  offeredCount: number
  hiredCount: number
  rejectedCount: number
  consentGrantedCount: number
  averageMatchScore: number
  averageAccessibilityCompletionRate: number
  publishStatusBreakdown: EnterpriseStatsBucket[]
  applicationStatusBreakdown: EnterpriseStatsBucket[]
  topJobs: EnterpriseStatsJob[]
  insights: string[]
}

export function getEnterpriseProfile(token: string) {
  return apiRequest<EnterpriseVerificationProfile>('/api/enterprise/profile', {
    method: 'GET',
    token,
  })
}

export function saveEnterpriseProfile(token: string, payload: EnterpriseVerificationProfileUpsertPayload) {
  return apiRequest<EnterpriseVerificationProfile>('/api/enterprise/profile', {
    method: 'PUT',
    token,
    body: JSON.stringify(payload),
  })
}

export function uploadEnterpriseVerificationMaterial(
  token: string,
  payload: {
    materialType: string
    note?: string
    file: File
  },
) {
  const formData = new FormData()
  formData.set('materialType', payload.materialType)
  if (payload.note?.trim()) {
    formData.set('note', payload.note.trim())
  }
  formData.set('file', payload.file)

  return apiRequest<EnterpriseVerificationMaterial>('/api/enterprise/profile/materials', {
    method: 'POST',
    token,
    body: formData,
  })
}

export function deleteEnterpriseVerificationMaterial(token: string, materialId: number) {
  return apiRequest<boolean>(`/api/enterprise/profile/materials/${materialId}`, {
    method: 'DELETE',
    token,
  })
}

export function getEnterpriseJobs(token: string, query: EnterpriseJobQuery = {}) {
  return apiRequest<PageResponse<EnterpriseJobSummary>>(
    withQuery('/api/enterprise/jobs', query as Record<string, unknown>),
    {
      method: 'GET',
      token,
    },
  )
}

export function getEnterpriseRecruitmentStats(token: string) {
  return apiRequest<EnterpriseRecruitmentStats>('/api/enterprise/stats', {
    method: 'GET',
    token,
  })
}

export function getEnterpriseJobDetail(token: string, jobId: string) {
  return apiRequest<EnterpriseJobDetail>(`/api/enterprise/jobs/${jobId}`, {
    method: 'GET',
    token,
  })
}

export function createEnterpriseJob(token: string, payload: EnterpriseJobUpsertPayload) {
  return apiRequest<EnterpriseJobDetail>('/api/enterprise/jobs', {
    method: 'POST',
    token,
    body: JSON.stringify(payload),
  })
}

export function updateEnterpriseJob(token: string, jobId: string, payload: EnterpriseJobUpsertPayload) {
  return apiRequest<EnterpriseJobDetail>(`/api/enterprise/jobs/${jobId}`, {
    method: 'PUT',
    token,
    body: JSON.stringify(payload),
  })
}

export function offlineEnterpriseJob(token: string, jobId: string) {
  return apiRequest<EnterpriseJobDetail>(`/api/enterprise/jobs/${jobId}/offline`, {
    method: 'POST',
    token,
  })
}

export function getEnterpriseJobCandidates(
  token: string,
  jobId: string,
  query: EnterpriseCandidateQuery = {},
) {
  return apiRequest<PageResponse<EnterpriseCandidateApplication>>(
    withQuery(`/api/enterprise/jobs/${jobId}/candidates`, query as Record<string, unknown>),
    {
      method: 'GET',
      token,
    },
  )
}

export function inviteEnterpriseInterview(token: string, payload: EnterpriseInterviewInvitePayload) {
  return apiRequest<EnterpriseCandidateApplication>('/api/enterprise/interview/invite', {
    method: 'POST',
    token,
    body: JSON.stringify(payload),
  })
}

export function submitEnterpriseInterviewResult(token: string, payload: EnterpriseInterviewResultPayload) {
  return apiRequest<EnterpriseCandidateApplication>('/api/enterprise/interview/result', {
    method: 'POST',
    token,
    body: JSON.stringify(payload),
  })
}

export function updateEnterpriseApplicationStatus(
  token: string,
  applicationId: number,
  payload: EnterpriseApplicationStatusUpdatePayload,
) {
  return apiRequest<EnterpriseCandidateApplication>(`/api/enterprise/applications/${applicationId}/status`, {
    method: 'POST',
    token,
    body: JSON.stringify(payload),
  })
}

function withQuery(path: string, query: Record<string, unknown>) {
  const searchParams = new URLSearchParams()

  Object.entries(query).forEach(([key, value]) => {
    if (value === undefined || value === null || value === '') {
      return
    }
    searchParams.set(key, String(value))
  })

  const search = searchParams.toString()
  return search ? `${path}?${search}` : path
}

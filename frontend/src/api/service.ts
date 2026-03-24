import { apiRequest } from '../lib/http'
import type { JobseekerProfile, PageResponse, SupportNeeds } from './jobseeker'

export interface ServiceCaseSummary {
  id: string
  name: string
  stage: string
  owner: string
  nextAction: string
  alertLevel: string
  timeline: string[]
  profileAuthorized: boolean
  pendingAlertCount: number
  followupCount: number
  referralCount: number
}

export interface ServiceIntervention {
  id: number
  caseId: string
  interventionType: string
  content: string
  attachmentNote?: string | null
  operatorName: string
  createdAt: string
}

export interface ServiceFollowup {
  id: number
  caseId: string
  jobId?: string | null
  followupStage: string
  adaptationScore: number
  environmentIssue?: string | null
  communicationIssue?: string | null
  supportImplemented: boolean
  leaveRisk: boolean
  needHelp: boolean
  recordStatus: string
  operatorName: string
  dueAt?: string | null
  createdAt: string
  completedAt: string
}

export interface ServiceAlert {
  alertId: string
  caseId?: string | null
  userId: number
  name: string
  alertType: string
  alertLevel: number
  triggerReason: string
  createdAt: string
  alertStatus: string
  resolutionNote?: string | null
  handledBy?: string | null
  handledAt?: string | null
}

export interface ServiceProfileAccess {
  linkedJobseeker: boolean
  profileAuthorized: boolean
  linkedAccount?: string | null
  linkedDisplayName?: string | null
  authorizationNote?: string | null
  authorizationUpdatedBy?: string | null
  authorizationUpdatedAt?: string | null
  jobseekerProfile?: JobseekerProfile | null
  supportNeeds?: SupportNeeds | null
}

export interface ServiceResourceReferral {
  id: number
  caseId: string
  referralType: string
  resourceName: string
  providerName?: string | null
  contactName?: string | null
  contactPhone?: string | null
  scheduledAt?: string | null
  referralStatus: string
  statusNote?: string | null
  operatorName: string
  createdAt: string
  updatedAt: string
}

export interface ServiceCaseDetail extends ServiceCaseSummary {
  intakeNote?: string | null
  profileAccess: ServiceProfileAccess
  interventions: ServiceIntervention[]
  followups: ServiceFollowup[]
  alerts: ServiceAlert[]
  referrals: ServiceResourceReferral[]
}

export interface ServiceAlertQuery {
  page?: number
  pageSize?: number
  status?: string
  level?: number
}

export interface ServiceCaseCreatePayload {
  name: string
  stage: string
  ownerName: string
  nextAction: string
  jobseekerAccount?: string
  intakeNote?: string
  profileAuthorized: boolean
  authorizationNote?: string
  operatorName: string
}

export interface ServiceProfileAccessPayload {
  profileAuthorized: boolean
  authorizationNote?: string
  operatorName: string
}

export interface ServiceInterventionPayload {
  interventionType: string
  content: string
  attachmentNote?: string
  operatorName: string
}

export interface ServiceFollowupPayload {
  jobId?: string
  followupStage: string
  adaptationScore: number
  environmentIssue?: string
  communicationIssue?: string
  supportImplemented: boolean
  leaveRisk: boolean
  needHelp: boolean
  operatorName: string
}

export interface ServiceReferralPayload {
  referralType: string
  resourceName: string
  providerName?: string
  contactName?: string
  contactPhone?: string
  scheduledAt?: string
  statusNote?: string
  operatorName: string
}

export interface ServiceReferralStatusPayload {
  targetStatus: string
  statusNote?: string
  operatorName: string
}

export interface ServiceAlertStatusPayload {
  targetStatus: string
  resolutionNote: string
  operatorName: string
}

export function getServiceCases(token: string) {
  return apiRequest<ServiceCaseSummary[]>('/api/service/cases', {
    method: 'GET',
    token,
  })
}

export function createServiceCase(token: string, payload: ServiceCaseCreatePayload) {
  return apiRequest<ServiceCaseDetail>('/api/service/cases', {
    method: 'POST',
    token,
    body: JSON.stringify(payload),
  })
}

export function getServiceCaseDetail(token: string, caseId: string) {
  return apiRequest<ServiceCaseDetail>(`/api/service/cases/${caseId}`, {
    method: 'GET',
    token,
  })
}

export function updateServiceCaseProfileAccess(
  token: string,
  caseId: string,
  payload: ServiceProfileAccessPayload,
) {
  return apiRequest<ServiceCaseDetail>(`/api/service/cases/${caseId}/profile-access`, {
    method: 'POST',
    token,
    body: JSON.stringify(payload),
  })
}

export function createServiceIntervention(
  token: string,
  caseId: string,
  payload: ServiceInterventionPayload,
) {
  return apiRequest<ServiceCaseDetail>(`/api/service/cases/${caseId}/interventions`, {
    method: 'POST',
    token,
    body: JSON.stringify(payload),
  })
}

export function createServiceFollowup(token: string, caseId: string, payload: ServiceFollowupPayload) {
  return apiRequest<ServiceCaseDetail>(`/api/service/cases/${caseId}/followups`, {
    method: 'POST',
    token,
    body: JSON.stringify(payload),
  })
}

export function createServiceReferral(token: string, caseId: string, payload: ServiceReferralPayload) {
  return apiRequest<ServiceCaseDetail>(`/api/service/cases/${caseId}/referrals`, {
    method: 'POST',
    token,
    body: JSON.stringify(payload),
  })
}

export function updateServiceReferralStatus(
  token: string,
  referralId: number,
  payload: ServiceReferralStatusPayload,
) {
  return apiRequest<ServiceCaseDetail>(`/api/service/referrals/${referralId}/status`, {
    method: 'POST',
    token,
    body: JSON.stringify(payload),
  })
}

export function getServiceAlerts(token: string, query: ServiceAlertQuery = {}) {
  return apiRequest<PageResponse<ServiceAlert>>(withQuery('/api/service/alerts', query as Record<string, unknown>), {
    method: 'GET',
    token,
  })
}

export function updateServiceAlertStatus(
  token: string,
  alertId: string,
  payload: ServiceAlertStatusPayload,
) {
  return apiRequest<ServiceAlert>(`/api/service/alerts/${alertId}/status`, {
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

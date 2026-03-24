import { apiRequest } from '../lib/http'
import { ApiError } from '../lib/http'
import type { ServiceCaseDetail, ServiceCaseSummary } from './service'

export interface PageResponse<T> {
  total: number
  page: number
  pageSize: number
  list: T[]
}

export interface ScoreItem {
  label: string
  value: number
}

export interface Job {
  id: string
  title: string
  company: string
  city: string
  salaryRange: string
  workMode: string
  summary: string
  stage: string
  matchScore: number
  dimensionScores: ScoreItem[]
  reasons: string[]
  risks: string[]
  supports: string[]
  description: string[]
  requirements: string[]
  environment: string[]
  applyHint: string
}

export interface RecommendedJobQuery {
  page?: number
  pageSize?: number
  city?: string
  workMode?: string
  keyword?: string
}

export interface JobApplication {
  applicationId: number
  jobId: string
  jobTitle: string
  companyName: string
  status: string
  consentToShareSupportNeed: boolean
  supportVisibility: string
  preferredInterviewMode: string
  coverNote: string
  additionalSupport?: string
  matchScoreSnapshot: number
  explanationSnapshot: string[]
  latestInterview: JobseekerInterviewRecord | null
  interviewRecords: JobseekerInterviewRecord[]
  submittedAt: string
}

export interface JobseekerInterviewRecord {
  interviewId: number
  interviewTime: string
  interviewMode: string
  interviewerName: string
  inviteNote?: string | null
  resultStatus: string
  feedbackNote?: string | null
  rejectReason?: string | null
  createdAt: string
  updatedAt: string
}

export interface InterviewSupportRequest {
  id: number
  applicationId: number
  requestType: string
  requestTypeLabel: string
  requestContent: string
  requestStatus: string
  createdAt: string
  updatedAt: string
}

export interface CreateInterviewSupportRequestPayload {
  applicationId: number
  requestType: string
  requestContent: string
}

export interface EmploymentFollowup {
  id: number
  jobId: string
  followupStage: string
  adaptationScore: number
  environmentIssue?: string | null
  communicationIssue?: string | null
  supportImplemented: boolean
  leaveRisk: boolean
  needHelp: boolean
  remark?: string | null
  createdAt: string
  updatedAt: string
}

export interface CreateEmploymentFollowupPayload {
  jobId: string
  followupStage: string
  adaptationScore: number
  environmentIssue?: string
  communicationIssue?: string
  supportImplemented: boolean
  leaveRisk: boolean
  needHelp: boolean
  remark?: string
}

export interface ApplyJobPayload {
  coverNote: string
  preferredInterviewMode: string
  supportVisibility?: string
  additionalSupport?: string
}

export interface InterviewCommunicationCard {
  title: string
  subtitle: string
  lines: string[]
  copyText: string
}

export interface SupportNeeds {
  supportVisibility: string
  consentToShareSupportNeed: boolean
  hasAnyNeed: boolean
  textCommunicationPreferred: boolean
  subtitleNeeded: boolean
  remoteInterviewPreferred: boolean
  keyboardOnlyMode: boolean
  highContrastNeeded: boolean
  largeFontNeeded: boolean
  flexibleScheduleNeeded: boolean
  accessibleWorkspaceNeeded: boolean
  assistiveSoftwareNeeded: boolean
  remark?: string
  supportSummary: string[]
  summaryText?: string
  interviewCommunicationCard: InterviewCommunicationCard
  updatedAt: string
}

export interface SaveSupportNeedsPayload {
  supportVisibility: string
  textCommunicationPreferred: boolean
  subtitleNeeded: boolean
  remoteInterviewPreferred: boolean
  keyboardOnlyMode: boolean
  highContrastNeeded: boolean
  largeFontNeeded: boolean
  flexibleScheduleNeeded: boolean
  accessibleWorkspaceNeeded: boolean
  assistiveSoftwareNeeded: boolean
  remark?: string
}

export interface JobseekerSkillTag {
  skillCode: string
  skillName: string
  skillLevel: number
  skillLevelLabel: string
}

export interface JobseekerProjectExperience {
  id?: number
  projectName: string
  roleName: string
  description: string
  startDate?: string
  endDate?: string
  periodLabel: string
}

export interface JobseekerAbilityCard {
  code: string
  title: string
  summary: string
  highlights: string[]
}

export interface JobseekerProfile {
  id: number
  userId: number
  realName: string
  gender?: string
  birthYear?: number
  schoolName?: string
  major?: string
  degree?: string
  graduationYear?: number
  currentCity?: string
  targetCity?: string
  expectedJob?: string
  expectedSalaryMin?: number
  expectedSalaryMax?: number
  workModePreference?: string
  intro?: string
  profileCompletionRate: number
  skillTags: JobseekerSkillTag[]
  projectExperiences: JobseekerProjectExperience[]
  abilityCards: JobseekerAbilityCard[]
}

export interface JobseekerProfileUpsertPayload {
  realName: string
  gender?: string
  birthYear?: number
  schoolName?: string
  major?: string
  degree?: string
  graduationYear?: number
  currentCity?: string
  targetCity?: string
  expectedJob?: string
  expectedSalaryMin?: number
  expectedSalaryMax?: number
  workModePreference?: string
  intro?: string
  skillTags?: Array<{
    skillCode?: string
    skillName: string
    skillLevel: number
  }>
  projectExperiences?: Array<{
    projectName: string
    roleName?: string
    description?: string
    startDate?: string
    endDate?: string
  }>
}

export interface ResumePreview {
  displayName: string
  headline: string
  summary: string
  profileCompletionRate: number
  profile: JobseekerProfile | null
  supportNeeds: SupportNeeds
  recentApplications: JobApplication[]
  strengths: string[]
  suggestions: string[]
  exportFileName: string
  generatedAt: string
}

export type ResumeExportFormat = 'pdf' | 'docx'

export function getCurrentJobseekerProfile(token: string) {
  return apiRequest<JobseekerProfile | null>('/api/jobseeker/profile', {
    method: 'GET',
    token,
  })
}

export function saveCurrentJobseekerProfile(token: string, payload: JobseekerProfileUpsertPayload) {
  return apiRequest<JobseekerProfile>('/api/jobseeker/profile', {
    method: 'PUT',
    token,
    body: JSON.stringify(payload),
  })
}

export function getRecommendedJobs(token: string, query: RecommendedJobQuery = {}) {
  return apiRequest<PageResponse<Job>>(withQuery('/api/jobseeker/recommend-jobs', query), {
    method: 'GET',
    token,
  })
}

export function getRecommendedJobDetail(token: string, jobId: string) {
  return apiRequest<Job>(`/api/jobseeker/recommend-jobs/${jobId}`, {
    method: 'GET',
    token,
  })
}

export function applyToJob(token: string, jobId: string, payload: ApplyJobPayload) {
  return apiRequest<JobApplication>(`/api/jobseeker/apply/${jobId}`, {
    method: 'POST',
    token,
    body: JSON.stringify(payload),
  })
}

export function getCurrentUserApplications(token: string) {
  return apiRequest<JobApplication[]>('/api/jobseeker/applications', {
    method: 'GET',
    token,
  })
}

export function getCurrentInterviewSupportRequests(token: string) {
  return apiRequest<InterviewSupportRequest[]>('/api/jobseeker/interview-support-requests', {
    method: 'GET',
    token,
  })
}

export function createInterviewSupportRequest(token: string, payload: CreateInterviewSupportRequestPayload) {
  return apiRequest<InterviewSupportRequest>('/api/jobseeker/interview-support-request', {
    method: 'POST',
    token,
    body: JSON.stringify(payload),
  })
}

export function getCurrentEmploymentFollowups(token: string) {
  return apiRequest<EmploymentFollowup[]>('/api/jobseeker/followups', {
    method: 'GET',
    token,
  })
}

export function createEmploymentFollowup(token: string, payload: CreateEmploymentFollowupPayload) {
  return apiRequest<EmploymentFollowup>('/api/jobseeker/followup', {
    method: 'POST',
    token,
    body: JSON.stringify(payload),
  })
}

export function getCurrentServiceRecords(token: string) {
  return apiRequest<ServiceCaseSummary[]>('/api/jobseeker/service-records', {
    method: 'GET',
    token,
  })
}

export function getCurrentServiceRecordDetail(token: string, caseId: string) {
  return apiRequest<ServiceCaseDetail>(`/api/jobseeker/service-records/${caseId}`, {
    method: 'GET',
    token,
  })
}

export function getCurrentResumePreview(token: string) {
  return apiRequest<ResumePreview>('/api/jobseeker/resume-preview', {
    method: 'GET',
    token,
  })
}

export async function exportCurrentResume(token: string, format: ResumeExportFormat = 'pdf') {
  const response = await fetch(`/api/jobseeker/resume-export?format=${encodeURIComponent(format)}`, {
    method: 'GET',
    headers: {
      Authorization: `Bearer ${token}`,
    },
  })

  if (!response.ok) {
    try {
      const payload = await response.json()
      throw new ApiError(payload?.message || `请求失败（HTTP ${response.status}）`, response.status, payload?.traceId)
    } catch (error) {
      if (error instanceof ApiError) {
        throw error
      }
      throw new ApiError(`请求失败（HTTP ${response.status}）`, response.status)
    }
  }

  const blob = await response.blob()
  const contentDisposition = response.headers.get('content-disposition') ?? ''
  const fileNameMatch = contentDisposition.match(/filename="([^"]+)"/)

  return {
    blob,
    fileName: fileNameMatch?.[1] ?? `bridgeability-resume.${format}`,
  }
}

export function getCurrentSupportNeeds(token: string) {
  return apiRequest<SupportNeeds>('/api/jobseeker/support-needs', {
    method: 'GET',
    token,
  })
}

export function saveCurrentSupportNeeds(token: string, payload: SaveSupportNeedsPayload) {
  return apiRequest<SupportNeeds>('/api/jobseeker/support-needs', {
    method: 'PUT',
    token,
    body: JSON.stringify(payload),
  })
}

function withQuery(path: string, query: RecommendedJobQuery) {
  const searchParams = new URLSearchParams()

  Object.entries(query).forEach(([key, value]) => {
    if (value === undefined || value === null) {
      return
    }

    const text = String(value).trim()
    if (!text) {
      return
    }

    searchParams.set(key, text)
  })

  const search = searchParams.toString()
  return search ? `${path}?${search}` : path
}

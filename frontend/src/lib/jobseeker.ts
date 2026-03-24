export const workModeLabels: Record<string, string> = {
  FULL_TIME: '全职',
  INTERNSHIP: '实习',
  PART_TIME: '兼职',
  REMOTE: '远程',
  HYBRID: '混合办公',
}

export const jobStageLabels: Record<string, string> = {
  OPEN: '可立即投递',
  PRIORITY: '优先推荐',
  CAUTION: '建议谨慎',
}

export const interviewModeLabels: Record<string, string> = {
  TEXT: '文字面试',
  ONLINE: '线上视频',
  HYBRID: '混合沟通',
  ONSITE: '线下面试',
  WRITTEN_TEST: '笔试/作业评估',
}

export const supportVisibilityLabels: Record<string, string> = {
  SUMMARY: '向企业展示支持需求摘要',
  HIDDEN: '暂不向企业展示支持需求',
}

export const applicationStatusLabels: Record<string, string> = {
  APPLIED: '已投递',
  VIEWED: '企业已查看',
  INTERVIEW: '面试中',
  INTERVIEWING: '面试中',
  REJECTED: '未通过',
  OFFERED: '待录用',
  HIRED: '已录用',
}

export const interviewSupportRequestTypeLabels: Record<string, string> = {
  TEXT_INTERVIEW: '文字面试',
  SUBTITLE: '字幕支持',
  REMOTE_INTERVIEW: '远程面试',
  FLEXIBLE_TIME: '弹性时间',
  OTHER: '其他说明',
}

export const interviewSupportRequestStatusLabels: Record<string, string> = {
  PENDING: '待处理',
  APPROVED: '已采纳',
  REJECTED: '未采纳',
}

export const employmentFollowupStageLabels: Record<string, string> = {
  DAY_7: '7 天跟踪',
  DAY_30: '30 天跟踪',
}

export const interviewResultLabels: Record<string, string> = {
  PENDING: '待反馈',
  PASS: '通过',
  FAIL: '未通过',
}

export const candidateRecommendationLabels: Record<string, string> = {
  PRIORITY: '优先跟进',
  FOLLOW_UP: '建议沟通',
  CAUTION: '谨慎评估',
}

export const scoreDimensionLabels: Record<string, string> = {
  skill: '技能匹配',
  workMode: '工作方式',
  communication: '沟通方式',
  environment: '环境适配',
  accommodation: '支持可落地性',
}

export const interviewModeOptions = [
  { value: 'TEXT', label: interviewModeLabels.TEXT },
  { value: 'ONLINE', label: interviewModeLabels.ONLINE },
  { value: 'HYBRID', label: interviewModeLabels.HYBRID },
]

export const supportVisibilityOptions = [
  { value: 'SUMMARY', label: supportVisibilityLabels.SUMMARY },
  { value: 'HIDDEN', label: supportVisibilityLabels.HIDDEN },
]

export function getWorkModeLabel(value?: string | null) {
  return getLabel(workModeLabels, value, '未填写')
}

export function getJobStageLabel(value?: string | null) {
  return getLabel(jobStageLabels, value, '待评估')
}

export function getInterviewModeLabel(value?: string | null) {
  return getLabel(interviewModeLabels, value, '未选择')
}

export function getSupportVisibilityLabel(value?: string | null) {
  return getLabel(supportVisibilityLabels, value, '未设置')
}

export function getApplicationStatusLabel(value?: string | null) {
  return getLabel(applicationStatusLabels, value, '处理中')
}

export function getInterviewSupportRequestTypeLabel(value?: string | null) {
  return getLabel(interviewSupportRequestTypeLabels, value, '支持申请')
}

export function getInterviewSupportRequestStatusLabel(value?: string | null) {
  return getLabel(interviewSupportRequestStatusLabels, value, '待处理')
}

export function getEmploymentFollowupStageLabel(value?: string | null) {
  return getLabel(employmentFollowupStageLabels, value, '回访跟踪')
}

export function getInterviewResultLabel(value?: string | null) {
  return getLabel(interviewResultLabels, value, '待反馈')
}

export function getCandidateRecommendationLabel(value?: string | null) {
  return getLabel(candidateRecommendationLabels, value, '待评估')
}

export function getScoreDimensionLabel(value?: string | null) {
  return getLabel(scoreDimensionLabels, value, '综合维度')
}

export function formatDateTime(value?: string | null) {
  return value?.trim() || '暂无记录'
}

function getLabel(map: Record<string, string>, value?: string | null, fallback = '未填写') {
  if (!value) {
    return fallback
  }
  return map[value] ?? value
}

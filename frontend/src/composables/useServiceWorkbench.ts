import { computed, reactive, ref, watch } from 'vue'

import {
  createServiceCase,
  createServiceFollowup,
  createServiceIntervention,
  createServiceReferral,
  getServiceAlerts,
  getServiceCaseDetail,
  getServiceCases,
  updateServiceAlertStatus,
  updateServiceCaseProfileAccess,
  updateServiceReferralStatus,
  type ServiceAlert,
  type ServiceCaseDetail,
  type ServiceCaseSummary,
  type ServiceResourceReferral,
} from '../api/service'
import { ApiError } from '../lib/http'
import { useAuthStore } from '../stores/auth'

export interface CaseCreateFormState {
  name: string
  stage: string
  ownerName: string
  nextAction: string
  jobseekerAccount: string
  intakeNote: string
  profileAuthorized: boolean
  authorizationNote: string
  operatorName: string
}

export interface ProfileAccessFormState {
  profileAuthorized: boolean
  authorizationNote: string
  operatorName: string
}

export interface InterventionFormState {
  interventionType: string
  content: string
  attachmentNote: string
  operatorName: string
}

export interface FollowupFormState {
  jobId: string
  followupStage: string
  adaptationScore: number
  environmentIssue: string
  communicationIssue: string
  supportImplemented: boolean
  leaveRisk: boolean
  needHelp: boolean
  operatorName: string
}

export interface ReferralFormState {
  referralType: string
  resourceName: string
  providerName: string
  contactName: string
  contactPhone: string
  scheduledAt: string
  statusNote: string
  operatorName: string
}

export interface AlertActionFormState {
  resolutionNote: string
  operatorName: string
}

export interface ReferralActionFormState {
  statusNote: string
  operatorName: string
}

export type AlertStatusFilter = 'ALL' | 'OPEN' | 'ESCALATED' | 'RESOLVED' | 'CLOSED'
export type AlertLevelFilter = 'ALL' | '1' | '2' | '3'
export type ReferralStatusAction = 'IN_PROGRESS' | 'CONNECTED' | 'COMPLETED' | 'CANCELLED'

export const interventionTypeOptions = [
  { value: 'RESUME_GUIDANCE', label: '简历指导' },
  { value: 'INTERVIEW_GUIDANCE', label: '面试辅导' },
  { value: 'JOB_RECOMMENDATION', label: '岗位推荐' },
  { value: 'PSYCHOLOGICAL_SUPPORT', label: '心理支持' },
  { value: 'TRAINING_REFERRAL', label: '培训转介' },
  { value: 'EMPLOYER_COMMUNICATION', label: '企业沟通' },
  { value: 'ONBOARDING_SUPPORT', label: '入职支持' },
]

export const followupStageOptions = [
  { value: 'DAY_7', label: '7 天回访' },
  { value: 'DAY_30', label: '30 天回访' },
]

export const referralTypeOptions = [
  { value: 'TRAINING', label: '培训资源' },
  { value: 'EMPLOYMENT', label: '就业转介' },
  { value: 'COUNSELING', label: '辅导支持' },
  { value: 'ASSISTIVE_SUPPORT', label: '无障碍支持' },
  { value: 'POLICY_SUPPORT', label: '政策支持' },
  { value: 'OTHER', label: '其他资源' },
]

export const alertStatusOptions: Array<{ value: AlertStatusFilter; label: string }> = [
  { value: 'ALL', label: '全部状态' },
  { value: 'OPEN', label: '待处理' },
  { value: 'ESCALATED', label: '已升级' },
  { value: 'RESOLVED', label: '已处理' },
  { value: 'CLOSED', label: '已关闭' },
]

export const alertLevelOptions: Array<{ value: AlertLevelFilter; label: string }> = [
  { value: 'ALL', label: '全部等级' },
  { value: '3', label: '高风险' },
  { value: '2', label: '中风险' },
  { value: '1', label: '低风险' },
]

function createEmptyCaseCreateForm(): CaseCreateFormState {
  return {
    name: '',
    stage: '初次建档',
    ownerName: '',
    nextAction: '',
    jobseekerAccount: '',
    intakeNote: '',
    profileAuthorized: false,
    authorizationNote: '',
    operatorName: '',
  }
}

function createEmptyProfileAccessForm(): ProfileAccessFormState {
  return {
    profileAuthorized: false,
    authorizationNote: '',
    operatorName: '',
  }
}

function createEmptyInterventionForm(): InterventionFormState {
  return {
    interventionType: 'RESUME_GUIDANCE',
    content: '',
    attachmentNote: '',
    operatorName: '',
  }
}

function createEmptyFollowupForm(): FollowupFormState {
  return {
    jobId: '',
    followupStage: 'DAY_7',
    adaptationScore: 80,
    environmentIssue: '',
    communicationIssue: '',
    supportImplemented: true,
    leaveRisk: false,
    needHelp: false,
    operatorName: '',
  }
}

function createEmptyReferralForm(): ReferralFormState {
  return {
    referralType: 'TRAINING',
    resourceName: '',
    providerName: '',
    contactName: '',
    contactPhone: '',
    scheduledAt: '',
    statusNote: '',
    operatorName: '',
  }
}

function normalizeAlertStatus(status?: string | null) {
  const text = status?.trim()
  if (!text) {
    return 'OPEN'
  }

  const normalized = text.toUpperCase()
  if (normalized === 'PENDING') {
    return 'OPEN'
  }
  if (text.includes('待处理')) {
    return 'OPEN'
  }
  if (text.includes('已处理')) {
    return 'RESOLVED'
  }
  if (text.includes('已关闭')) {
    return 'CLOSED'
  }
  if (text.includes('升级')) {
    return 'ESCALATED'
  }
  return normalized
}

function isActionableAlert(status?: string | null) {
  const normalized = normalizeAlertStatus(status)
  return normalized === 'OPEN' || normalized === 'ESCALATED'
}

function getAlertStatusLabel(status?: string | null) {
  const normalized = normalizeAlertStatus(status)
  if (normalized === 'OPEN') {
    return '待处理'
  }
  if (normalized === 'RESOLVED') {
    return '已处理'
  }
  if (normalized === 'CLOSED') {
    return '已关闭'
  }
  if (normalized === 'ESCALATED') {
    return '已升级'
  }
  return status || '待处理'
}

function getAlertLevelLabel(level: number) {
  if (level >= 3) {
    return '高'
  }
  if (level === 2) {
    return '中'
  }
  if (level === 1) {
    return '低'
  }
  return '未评估'
}

function getCaseAlertLevelLabel(level?: string | null) {
  if (!level?.trim()) {
    return '无'
  }

  const normalized = level.trim().toUpperCase()
  if (normalized === 'HIGH' || level.includes('高')) {
    return '高'
  }
  if (normalized === 'MEDIUM' || level.includes('中')) {
    return '中'
  }
  if (normalized === 'LOW' || level.includes('低')) {
    return '低'
  }
  if (normalized === 'NONE' || level.includes('无')) {
    return '无'
  }
  return level
}

function getInterventionTypeLabel(type: string) {
  return interventionTypeOptions.find((item) => item.value === type)?.label ?? type
}

function getFollowupStageLabel(stage: string) {
  return followupStageOptions.find((item) => item.value === stage)?.label ?? stage
}

function getReferralTypeLabel(type: string) {
  return referralTypeOptions.find((item) => item.value === type)?.label ?? type
}

function getReferralStatusLabel(status: string) {
  switch (status) {
    case 'PLANNED':
      return '已排期'
    case 'IN_PROGRESS':
      return '跟进中'
    case 'CONNECTED':
      return '已对接'
    case 'COMPLETED':
      return '已完成'
    case 'CANCELLED':
      return '已取消'
    default:
      return status
  }
}

function getBooleanLabel(value: boolean) {
  return value ? '是' : '否'
}

export function useServiceWorkbench() {
  const authStore = useAuthStore()

  const serviceCases = ref<ServiceCaseSummary[]>([])
  const alerts = ref<ServiceAlert[]>([])
  const selectedCaseId = ref('')
  const selectedCaseDetail = ref<ServiceCaseDetail | null>(null)
  const alertStatusFilter = ref<AlertStatusFilter>('ALL')
  const alertLevelFilter = ref<AlertLevelFilter>('ALL')
  const isLoading = ref(false)
  const isDetailLoading = ref(false)
  const isCaseCreating = ref(false)
  const isProfileAccessSaving = ref(false)
  const isInterventionSaving = ref(false)
  const isFollowupSaving = ref(false)
  const isReferralSaving = ref(false)
  const activeAlertId = ref('')
  const activeReferralId = ref(0)
  const loadError = ref('')
  const detailError = ref('')
  const actionError = ref('')
  const successMessage = ref('')

  const caseCreateForm = reactive(createEmptyCaseCreateForm())
  const profileAccessForm = reactive(createEmptyProfileAccessForm())
  const interventionForm = reactive(createEmptyInterventionForm())
  const followupForm = reactive(createEmptyFollowupForm())
  const referralForm = reactive(createEmptyReferralForm())
  const alertActionForms = reactive<Record<string, AlertActionFormState>>({})
  const referralActionForms = reactive<Record<number, ReferralActionFormState>>({})

  let detailRequestSerial = 0

  const defaultOperatorName = computed(
    () => authStore.nickname?.trim() || authStore.account?.trim() || '服务专员',
  )

  const selectedCase = computed<ServiceCaseSummary | ServiceCaseDetail | null>(() => {
    if (selectedCaseDetail.value?.id === selectedCaseId.value) {
      return selectedCaseDetail.value
    }
    return serviceCases.value.find((item) => item.id === selectedCaseId.value) ?? null
  })

  const pendingAlerts = computed(() => alerts.value.filter((item) => isActionableAlert(item.alertStatus)))
  const highRiskAlerts = computed(() => pendingAlerts.value.filter((item) => item.alertLevel >= 3))
  const resolvedAlerts = computed(() =>
    alerts.value.filter((item) => !isActionableAlert(item.alertStatus)),
  )
  const filteredAlerts = computed(() =>
    alerts.value.filter((item) => {
      if (
        alertStatusFilter.value !== 'ALL' &&
        normalizeAlertStatus(item.alertStatus) !== alertStatusFilter.value
      ) {
        return false
      }

      if (alertLevelFilter.value !== 'ALL' && item.alertLevel !== Number(alertLevelFilter.value)) {
        return false
      }

      return true
    }),
  )
  const topAlert = computed(
    () => highRiskAlerts.value[0] ?? pendingAlerts.value[0] ?? filteredAlerts.value[0] ?? null,
  )
  const totalFollowupCount = computed(() =>
    serviceCases.value.reduce((total, item) => total + item.followupCount, 0),
  )
  const totalReferralCount = computed(() =>
    serviceCases.value.reduce((total, item) => total + item.referralCount, 0),
  )
  const selectedTimelineCount = computed(() => selectedCase.value?.timeline.length ?? 0)
  const selectedPendingAlertCount = computed(
    () =>
      selectedCaseDetail.value?.alerts.filter((item) => isActionableAlert(item.alertStatus)).length ?? 0,
  )
  const selectedFollowupCount = computed(() => selectedCaseDetail.value?.followups.length ?? 0)
  const selectedReferralCount = computed(() => selectedCaseDetail.value?.referrals.length ?? 0)
  const pageNote = computed(() => {
    if (!serviceCases.value.length) {
      return '当前没有服务个案。'
    }

    return `当前 ${serviceCases.value.length} 个个案，待处理预警 ${pendingAlerts.value.length} 条，资源转介 ${totalReferralCount.value} 项。`
  })

  watch(
    defaultOperatorName,
    (name) => {
      if (!caseCreateForm.operatorName.trim()) {
        caseCreateForm.operatorName = name
      }
      if (!caseCreateForm.ownerName.trim()) {
        caseCreateForm.ownerName = name
      }
      if (!profileAccessForm.operatorName.trim()) {
        profileAccessForm.operatorName = name
      }
      if (!interventionForm.operatorName.trim()) {
        interventionForm.operatorName = name
      }
      if (!followupForm.operatorName.trim()) {
        followupForm.operatorName = name
      }
      if (!referralForm.operatorName.trim()) {
        referralForm.operatorName = name
      }
      Object.values(alertActionForms).forEach((form) => {
        if (!form.operatorName.trim()) {
          form.operatorName = name
        }
      })
      Object.values(referralActionForms).forEach((form) => {
        if (!form.operatorName.trim()) {
          form.operatorName = name
        }
      })
    },
    { immediate: true },
  )

  function resetCaseCreateForm() {
    Object.assign(caseCreateForm, createEmptyCaseCreateForm())
    caseCreateForm.operatorName = defaultOperatorName.value
    caseCreateForm.ownerName = defaultOperatorName.value
  }

  function resetProfileAccessForm(detail?: ServiceCaseDetail | null) {
    Object.assign(profileAccessForm, createEmptyProfileAccessForm())
    profileAccessForm.operatorName = defaultOperatorName.value
    if (!detail) {
      return
    }
    profileAccessForm.profileAuthorized = detail.profileAccess.profileAuthorized
    profileAccessForm.authorizationNote = detail.profileAccess.authorizationNote ?? ''
  }

  function resetInterventionForm() {
    Object.assign(interventionForm, createEmptyInterventionForm())
    interventionForm.operatorName = defaultOperatorName.value
  }

  function resetFollowupForm() {
    Object.assign(followupForm, createEmptyFollowupForm())
    followupForm.operatorName = defaultOperatorName.value
  }

  function resetReferralForm() {
    Object.assign(referralForm, createEmptyReferralForm())
    referralForm.operatorName = defaultOperatorName.value
  }

  function resetCaseForms(detail?: ServiceCaseDetail | null) {
    resetProfileAccessForm(detail)
    resetInterventionForm()
    resetFollowupForm()
    resetReferralForm()
  }

  function clearActionFeedback() {
    actionError.value = ''
    successMessage.value = ''
  }

  function syncSelectedCase(caseList: ServiceCaseSummary[]) {
    if (!caseList.length) {
      selectedCaseId.value = ''
      return ''
    }

    if (!caseList.some((item) => item.id === selectedCaseId.value)) {
      selectedCaseId.value = caseList[0].id
    }

    return selectedCaseId.value
  }

  function syncAlertActionForms(list: ServiceAlert[]) {
    const knownIds = new Set(list.map((item) => item.alertId))

    Object.keys(alertActionForms).forEach((alertId) => {
      if (!knownIds.has(alertId)) {
        delete alertActionForms[alertId]
      }
    })

    list.forEach((alert) => {
      if (!alertActionForms[alert.alertId]) {
        alertActionForms[alert.alertId] = {
          resolutionNote: '',
          operatorName: defaultOperatorName.value,
        }
      }
    })
  }

  function syncReferralActionForms(list: ServiceResourceReferral[]) {
    const knownIds = new Set(list.map((item) => item.id))

    Object.keys(referralActionForms).forEach((referralId) => {
      if (!knownIds.has(Number(referralId))) {
        delete referralActionForms[Number(referralId)]
      }
    })

    list.forEach((referral) => {
      if (!referralActionForms[referral.id]) {
        referralActionForms[referral.id] = {
          statusNote: referral.statusNote ?? '',
          operatorName: defaultOperatorName.value,
        }
      }
    })
  }

  function seedCaseForms(detail: ServiceCaseDetail | null) {
    resetProfileAccessForm(detail)
    if (!detail) {
      return
    }

    if (!followupForm.jobId && detail.followups[0]?.jobId) {
      followupForm.jobId = detail.followups[0].jobId ?? ''
    }

    const hasDay7 = detail.followups.some((item) => item.followupStage === 'DAY_7')
    const hasDay30 = detail.followups.some((item) => item.followupStage === 'DAY_30')

    if (hasDay7 && !hasDay30) {
      followupForm.followupStage = 'DAY_30'
    }
  }

  async function refreshWorkbench() {
    if (!authStore.token) {
      return
    }

    isLoading.value = true
    loadError.value = ''

    try {
      const previousSelectedCaseId = selectedCaseId.value
      const [caseList, alertPage] = await Promise.all([
        getServiceCases(authStore.token),
        getServiceAlerts(authStore.token, { page: 1, pageSize: 100 }),
      ])

      serviceCases.value = caseList
      alerts.value = alertPage.list
      syncAlertActionForms(alerts.value)

      const nextCaseId = syncSelectedCase(caseList)
      if (nextCaseId) {
        if (nextCaseId !== previousSelectedCaseId) {
          resetCaseForms()
        }
        await loadCaseDetail(nextCaseId)
      } else {
        selectedCaseDetail.value = null
        detailError.value = ''
      }
    } catch (error) {
      loadError.value = error instanceof ApiError ? error.message : '服务首页加载失败'
    } finally {
      isLoading.value = false
    }
  }

  async function selectCase(caseId: string) {
    if (!caseId || caseId === selectedCaseId.value) {
      return
    }

    selectedCaseId.value = caseId
    clearActionFeedback()
    resetCaseForms()
    await loadCaseDetail(caseId)
  }

  async function loadCaseDetail(caseId: string) {
    if (!authStore.token) {
      return
    }

    const requestId = ++detailRequestSerial
    isDetailLoading.value = true
    detailError.value = ''
    selectedCaseDetail.value = null

    try {
      const detail = await getServiceCaseDetail(authStore.token, caseId)
      if (requestId !== detailRequestSerial) {
        return
      }
      selectedCaseDetail.value = detail
      seedCaseForms(detail)
      syncReferralActionForms(detail.referrals)
    } catch (error) {
      if (requestId !== detailRequestSerial) {
        return
      }
      detailError.value = error instanceof ApiError ? error.message : '个案详情加载失败'
    } finally {
      if (requestId === detailRequestSerial) {
        isDetailLoading.value = false
      }
    }
  }

  async function submitCreateCase() {
    if (!authStore.token) {
      return
    }

    isCaseCreating.value = true
    clearActionFeedback()

    try {
      const detail = await createServiceCase(authStore.token, {
        name: caseCreateForm.name.trim(),
        stage: caseCreateForm.stage.trim(),
        ownerName: caseCreateForm.ownerName.trim(),
        nextAction: caseCreateForm.nextAction.trim(),
        jobseekerAccount: caseCreateForm.jobseekerAccount.trim() || undefined,
        intakeNote: caseCreateForm.intakeNote.trim() || undefined,
        profileAuthorized: caseCreateForm.profileAuthorized,
        authorizationNote: caseCreateForm.authorizationNote.trim() || undefined,
        operatorName: caseCreateForm.operatorName.trim(),
      })

      selectedCaseId.value = detail.id
      successMessage.value = '服务对象档案已建立。'
      resetCaseCreateForm()
      await refreshWorkbench()
    } catch (error) {
      actionError.value = error instanceof ApiError ? error.message : '新建个案失败'
    } finally {
      isCaseCreating.value = false
    }
  }

  async function submitProfileAccess() {
    if (!authStore.token || !selectedCaseId.value) {
      return
    }

    clearActionFeedback()

    if (profileAccessForm.profileAuthorized && !selectedCaseDetail.value?.profileAccess.linkedJobseeker) {
      actionError.value = '当前个案未绑定求职者账号，不能开启查档授权。'
      return
    }

    isProfileAccessSaving.value = true

    try {
      await updateServiceCaseProfileAccess(authStore.token, selectedCaseId.value, {
        profileAuthorized: profileAccessForm.profileAuthorized,
        authorizationNote: profileAccessForm.authorizationNote.trim() || undefined,
        operatorName: profileAccessForm.operatorName.trim(),
      })

      successMessage.value = profileAccessForm.profileAuthorized
        ? '档案授权已更新，可以查看求职者档案。'
        : '档案授权已撤回。'
      await refreshWorkbench()
    } catch (error) {
      actionError.value = error instanceof ApiError ? error.message : '更新档案授权失败'
    } finally {
      isProfileAccessSaving.value = false
    }
  }

  async function submitIntervention() {
    if (!authStore.token || !selectedCaseId.value) {
      return
    }

    isInterventionSaving.value = true
    clearActionFeedback()

    try {
      await createServiceIntervention(authStore.token, selectedCaseId.value, {
        interventionType: interventionForm.interventionType,
        content: interventionForm.content.trim(),
        attachmentNote: interventionForm.attachmentNote.trim() || undefined,
        operatorName: interventionForm.operatorName.trim(),
      })

      successMessage.value = '干预记录已写入个案时间线。'
      resetInterventionForm()
      await refreshWorkbench()
    } catch (error) {
      actionError.value = error instanceof ApiError ? error.message : '干预记录保存失败'
    } finally {
      isInterventionSaving.value = false
    }
  }

  async function submitFollowup() {
    if (!authStore.token || !selectedCaseId.value) {
      return
    }

    isFollowupSaving.value = true
    clearActionFeedback()

    try {
      await createServiceFollowup(authStore.token, selectedCaseId.value, {
        jobId: followupForm.jobId.trim() || undefined,
        followupStage: followupForm.followupStage,
        adaptationScore: Number(followupForm.adaptationScore),
        environmentIssue: followupForm.environmentIssue.trim() || undefined,
        communicationIssue: followupForm.communicationIssue.trim() || undefined,
        supportImplemented: followupForm.supportImplemented,
        leaveRisk: followupForm.leaveRisk,
        needHelp: followupForm.needHelp,
        operatorName: followupForm.operatorName.trim(),
      })

      successMessage.value = '回访记录已保存，风险预警已按规则同步。'
      resetFollowupForm()
      await refreshWorkbench()
    } catch (error) {
      actionError.value = error instanceof ApiError ? error.message : '回访记录保存失败'
    } finally {
      isFollowupSaving.value = false
    }
  }

  async function submitReferral() {
    if (!authStore.token || !selectedCaseId.value) {
      return
    }

    isReferralSaving.value = true
    clearActionFeedback()

    try {
      await createServiceReferral(authStore.token, selectedCaseId.value, {
        referralType: referralForm.referralType,
        resourceName: referralForm.resourceName.trim(),
        providerName: referralForm.providerName.trim() || undefined,
        contactName: referralForm.contactName.trim() || undefined,
        contactPhone: referralForm.contactPhone.trim() || undefined,
        scheduledAt: referralForm.scheduledAt.trim() || undefined,
        statusNote: referralForm.statusNote.trim() || undefined,
        operatorName: referralForm.operatorName.trim(),
      })

      successMessage.value = '资源转介已纳入个案跟进。'
      resetReferralForm()
      await refreshWorkbench()
    } catch (error) {
      actionError.value = error instanceof ApiError ? error.message : '资源转介保存失败'
    } finally {
      isReferralSaving.value = false
    }
  }

  async function submitReferralStatus(referral: ServiceResourceReferral, targetStatus: ReferralStatusAction) {
    if (!authStore.token) {
      return
    }

    const form = referralActionForms[referral.id]
    if (!form) {
      return
    }

    activeReferralId.value = referral.id
    clearActionFeedback()

    try {
      await updateServiceReferralStatus(authStore.token, referral.id, {
        targetStatus,
        statusNote: form.statusNote.trim() || undefined,
        operatorName: form.operatorName.trim(),
      })

      successMessage.value = '资源转介状态已更新。'
      await refreshWorkbench()
    } catch (error) {
      actionError.value = error instanceof ApiError ? error.message : '更新转介状态失败'
    } finally {
      activeReferralId.value = 0
    }
  }

  async function submitAlertStatus(
    alert: ServiceAlert,
    targetStatus: 'RESOLVED' | 'ESCALATED' | 'CLOSED',
  ) {
    if (!authStore.token) {
      return
    }

    const form = alertActionForms[alert.alertId]
    if (!form) {
      return
    }

    activeAlertId.value = alert.alertId
    clearActionFeedback()

    try {
      await updateServiceAlertStatus(authStore.token, alert.alertId, {
        targetStatus,
        resolutionNote: form.resolutionNote.trim(),
        operatorName: form.operatorName.trim(),
      })

      successMessage.value =
        targetStatus === 'ESCALATED' ? '预警已升级，请继续处理。' : '预警状态已更新。'
      form.resolutionNote = ''
      await refreshWorkbench()
    } catch (error) {
      actionError.value = error instanceof ApiError ? error.message : '预警处理失败'
    } finally {
      activeAlertId.value = ''
    }
  }

  async function initializeWorkbench() {
    resetCaseCreateForm()
    resetCaseForms()
    await refreshWorkbench()
  }

  return {
    interventionTypeOptions,
    followupStageOptions,
    referralTypeOptions,
    alertStatusOptions,
    alertLevelOptions,
    serviceCases,
    alerts,
    selectedCaseId,
    selectedCaseDetail,
    alertStatusFilter,
    alertLevelFilter,
    isLoading,
    isDetailLoading,
    isCaseCreating,
    isProfileAccessSaving,
    isInterventionSaving,
    isFollowupSaving,
    isReferralSaving,
    activeAlertId,
    activeReferralId,
    loadError,
    detailError,
    actionError,
    successMessage,
    caseCreateForm,
    profileAccessForm,
    interventionForm,
    followupForm,
    referralForm,
    alertActionForms,
    referralActionForms,
    selectedCase,
    pendingAlerts,
    highRiskAlerts,
    resolvedAlerts,
    filteredAlerts,
    topAlert,
    totalFollowupCount,
    totalReferralCount,
    selectedTimelineCount,
    selectedPendingAlertCount,
    selectedFollowupCount,
    selectedReferralCount,
    pageNote,
    clearActionFeedback,
    resetCaseCreateForm,
    resetCaseForms,
    refreshWorkbench,
    selectCase,
    loadCaseDetail,
    submitCreateCase,
    submitProfileAccess,
    submitIntervention,
    submitFollowup,
    submitReferral,
    submitReferralStatus,
    submitAlertStatus,
    initializeWorkbench,
    normalizeAlertStatus,
    isActionableAlert,
    getAlertStatusLabel,
    getAlertLevelLabel,
    getCaseAlertLevelLabel,
    getInterventionTypeLabel,
    getFollowupStageLabel,
    getReferralTypeLabel,
    getReferralStatusLabel,
    getBooleanLabel,
  }
}

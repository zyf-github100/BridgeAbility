<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'

import {
  getEnterpriseJobCandidates,
  getEnterpriseJobs,
  inviteEnterpriseInterview,
  submitEnterpriseInterviewResult,
  updateEnterpriseApplicationStatus,
  type EnterpriseCandidateApplication,
  type EnterpriseJobSummary,
} from '../api/enterprise'
import { ApiError } from '../lib/http'
import {
  formatDateTime,
  getApplicationStatusLabel,
  getCandidateRecommendationLabel,
  getInterviewModeLabel,
  getInterviewResultLabel,
  getJobStageLabel,
  getSupportVisibilityLabel,
  getWorkModeLabel,
} from '../lib/jobseeker'
import { useAuthStore } from '../stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const jobs = ref<EnterpriseJobSummary[]>([])
const selectedJobId = ref('')
const candidates = ref<EnterpriseCandidateApplication[]>([])
const selectedCandidateId = ref(0)
const isJobsLoading = ref(false)
const isCandidatesLoading = ref(false)
const isInviteSubmitting = ref(false)
const isResultSubmitting = ref(false)
const isStatusSubmitting = ref(false)
const loadError = ref('')
const actionError = ref('')
const actionSuccess = ref('')

const interviewModeOptions = [
  { value: 'ONSITE', label: '线下面试' },
  { value: 'ONLINE', label: '线上视频面试' },
  { value: 'TEXT', label: '文字面试' },
  { value: 'WRITTEN_TEST', label: '笔试/作业评估' },
]

const passStatusOptions = [
  { value: 'OFFERED', label: '待录用' },
  { value: 'HIRED', label: '已录用' },
]

const manualStatusOptions = [
  { value: 'VIEWED', label: '已查看' },
  { value: 'INTERVIEWING', label: '面试推进中' },
  { value: 'OFFERED', label: '待录用' },
  { value: 'HIRED', label: '已录用' },
  { value: 'REJECTED', label: '未通过' },
]
const invitableApplicationStatuses = ['APPLIED', 'INTERVIEW', 'INTERVIEWING']

const inviteForm = ref({
  interviewTime: '',
  interviewMode: 'ONLINE',
  interviewerName: '',
  note: '',
})

const resultForm = ref({
  resultStatus: 'PASS' as 'PASS' | 'FAIL',
  applicationStatus: 'OFFERED',
  feedbackNote: '',
  rejectReason: '',
})

const statusForm = ref({
  targetStatus: 'VIEWED' as 'VIEWED' | 'INTERVIEWING' | 'OFFERED' | 'HIRED' | 'REJECTED',
})

const routeJobId = computed(() => {
  const pathValue = Array.isArray(route.params.jobId) ? route.params.jobId[0] : route.params.jobId
  if (pathValue) {
    return String(pathValue)
  }
  return typeof route.query.jobId === 'string' ? route.query.jobId : ''
})
const routeApplicationId = computed(() => {
  const value = Array.isArray(route.params.applicationId) ? route.params.applicationId[0] : route.params.applicationId
  return Number(value ?? 0)
})
const selectedJob = computed(() => jobs.value.find((job) => job.id === selectedJobId.value) ?? null)
const selectedCandidate = computed(
  () => candidates.value.find((candidate) => candidate.applicationId === selectedCandidateId.value) ?? null,
)
const pendingInterview = computed(() => {
  const latestInterview = selectedCandidate.value?.latestInterview
  return latestInterview?.resultStatus === 'PENDING' ? latestInterview : null
})
const pendingInterviewCount = computed(() =>
  candidates.value.filter((candidate) => candidate.latestInterview?.resultStatus === 'PENDING').length,
)
const completedInterviewCount = computed(() =>
  candidates.value.filter(
    (candidate) =>
      candidate.latestInterview &&
      ['PASS', 'FAIL'].includes(candidate.latestInterview.resultStatus),
  ).length,
)
const hiredCount = computed(() => candidates.value.filter((candidate) => candidate.status === 'HIRED').length)
const canInvite = computed(() => {
  if (!selectedCandidate.value) {
    return false
  }
  return invitableApplicationStatuses.includes(selectedCandidate.value.status)
})
const inviteDisabledReason = computed(() => {
  if (!selectedCandidate.value) {
    return '请选择候选人后再发送邀约。'
  }
  if (selectedCandidate.value.status === 'VIEWED') {
    return '候选人已进入查看阶段，请先将招聘进度更新为“面试中”，再发送面试邀约。'
  }
  if (!canInvite.value) {
    return '当前状态已结束，不能再次发起面试。'
  }
  return ''
})
const canSubmitResult = computed(() => Boolean(pendingInterview.value))
const canUpdateStatus = computed(() => {
  if (!selectedCandidate.value) {
    return false
  }
  return !['HIRED', 'REJECTED'].includes(selectedCandidate.value.status)
})

watch(
  () => routeJobId.value,
  (value) => {
    if (value && value !== selectedJobId.value) {
      selectedJobId.value = value
    }
  },
)

watch(
  () => routeApplicationId.value,
  (value) => {
    if (value && value !== selectedCandidateId.value) {
      selectedCandidateId.value = value
    }
  },
)

watch(selectedJobId, async (jobId) => {
  if (!jobId) {
    candidates.value = []
    selectedCandidateId.value = 0
    return
  }
  await loadCandidates()
})

watch(candidates, (list) => {
  if (!list.length) {
    selectedCandidateId.value = 0
    return
  }

  if (routeApplicationId.value && list.some((candidate) => candidate.applicationId === routeApplicationId.value)) {
    selectedCandidateId.value = routeApplicationId.value
    return
  }

  if (!list.some((candidate) => candidate.applicationId === selectedCandidateId.value)) {
    selectedCandidateId.value = list[0].applicationId
  }
})

watch(selectedCandidateId, () => {
  hydrateActionForms(selectedCandidate.value)
})

watch(
  () => resultForm.value.resultStatus,
  (value) => {
    if (value === 'FAIL') {
      resultForm.value.applicationStatus = 'REJECTED'
      return
    }
    if (resultForm.value.applicationStatus === 'REJECTED') {
      resultForm.value.applicationStatus = 'OFFERED'
    }
  },
)

async function loadJobs() {
  if (!authStore.token) {
    return
  }

  isJobsLoading.value = true
  loadError.value = ''

  try {
    const page = await getEnterpriseJobs(authStore.token, { page: 1, pageSize: 50 })
    jobs.value = page.list

    if (!page.list.length) {
      selectedJobId.value = ''
      return
    }

    const nextJobId = routeJobId.value && page.list.some((job) => job.id === routeJobId.value)
      ? routeJobId.value
      : page.list[0].id

    if (nextJobId !== selectedJobId.value) {
      selectedJobId.value = nextJobId
      return
    }

    await loadCandidates()
  } catch (error) {
    loadError.value = error instanceof ApiError ? error.message : '岗位列表加载失败'
  } finally {
    isJobsLoading.value = false
  }
}

async function loadCandidates() {
  if (!authStore.token || !selectedJobId.value) {
    return
  }

  isCandidatesLoading.value = true
  loadError.value = ''

  try {
    const page = await getEnterpriseJobCandidates(authStore.token, selectedJobId.value, {
      page: 1,
      pageSize: 50,
    })
    candidates.value = page.list
  } catch (error) {
    loadError.value = error instanceof ApiError ? error.message : '候选人记录加载失败'
  } finally {
    isCandidatesLoading.value = false
  }
}

async function refreshPage() {
  await loadJobs()
}

async function handleSelectJob(jobId: string) {
  await router.replace({
    path: '/enterprise/interviews',
    query: {
      jobId,
    },
  })
}

async function submitInterviewInvite() {
  if (!authStore.token || !selectedCandidate.value) {
    return
  }
  if (!canInvite.value) {
    actionError.value = inviteDisabledReason.value || '当前状态不能发起面试邀约。'
    actionSuccess.value = ''
    return
  }
  if (!inviteForm.value.interviewTime || !inviteForm.value.interviewerName.trim()) {
    actionError.value = '请完整填写面试时间和面试官。'
    actionSuccess.value = ''
    return
  }

  isInviteSubmitting.value = true
  actionError.value = ''
  actionSuccess.value = ''

  try {
    const updatedCandidate = await inviteEnterpriseInterview(authStore.token, {
      applicationId: selectedCandidate.value.applicationId,
      interviewTime: toApiDateTime(inviteForm.value.interviewTime),
      interviewMode: inviteForm.value.interviewMode,
      interviewerName: inviteForm.value.interviewerName.trim(),
      note: inviteForm.value.note.trim() || undefined,
    })
    replaceCandidate(updatedCandidate)
    hydrateActionForms(updatedCandidate)
    actionSuccess.value = '已发起面试邀约，候选人状态已推进到面试中。'
  } catch (error) {
    actionError.value = error instanceof ApiError ? error.message : '面试邀约提交失败'
  } finally {
    isInviteSubmitting.value = false
  }
}

async function submitInterviewResult() {
  if (!authStore.token || !selectedCandidate.value || !pendingInterview.value) {
    return
  }
  if (resultForm.value.resultStatus === 'FAIL' && !resultForm.value.rejectReason.trim()) {
    actionError.value = '记录未通过结果时必须填写未通过原因。'
    actionSuccess.value = ''
    return
  }

  isResultSubmitting.value = true
  actionError.value = ''
  actionSuccess.value = ''

  try {
    const updatedCandidate = await submitEnterpriseInterviewResult(authStore.token, {
      applicationId: selectedCandidate.value.applicationId,
      resultStatus: resultForm.value.resultStatus,
      applicationStatus:
        resultForm.value.resultStatus === 'FAIL' ? 'REJECTED' : resultForm.value.applicationStatus,
      feedbackNote: resultForm.value.feedbackNote.trim() || undefined,
      rejectReason:
        resultForm.value.resultStatus === 'FAIL' ? resultForm.value.rejectReason.trim() : undefined,
    })
    replaceCandidate(updatedCandidate)
    hydrateActionForms(updatedCandidate)
    actionSuccess.value =
      resultForm.value.resultStatus === 'FAIL'
        ? '已记录未通过结果和原因。'
        : '已记录面试结果并更新候选人状态。'
  } catch (error) {
    actionError.value = error instanceof ApiError ? error.message : '面试结果提交失败'
  } finally {
    isResultSubmitting.value = false
  }
}

async function submitManualStatusUpdate() {
  if (!authStore.token || !selectedCandidate.value || !canUpdateStatus.value) {
    return
  }

  isStatusSubmitting.value = true
  actionError.value = ''
  actionSuccess.value = ''

  try {
    const updatedCandidate = await updateEnterpriseApplicationStatus(
      authStore.token,
      selectedCandidate.value.applicationId,
      {
        targetStatus: statusForm.value.targetStatus,
      },
    )
    replaceCandidate(updatedCandidate)
    hydrateActionForms(updatedCandidate)
    actionSuccess.value = `已将候选人状态更新为${getApplicationStatusLabel(updatedCandidate.status)}。`
  } catch (error) {
    actionError.value = error instanceof ApiError ? error.message : '招聘状态更新失败'
  } finally {
    isStatusSubmitting.value = false
  }
}

function replaceCandidate(updatedCandidate: EnterpriseCandidateApplication) {
  candidates.value = candidates.value.map((candidate) =>
    candidate.applicationId === updatedCandidate.applicationId ? updatedCandidate : candidate,
  )
}

function hydrateActionForms(candidate: EnterpriseCandidateApplication | null) {
  actionError.value = ''
  actionSuccess.value = ''

  const latestInterview = candidate?.latestInterview
  const hasPendingInterview = latestInterview?.resultStatus === 'PENDING'

  inviteForm.value = {
    interviewTime: hasPendingInterview ? toDateTimeLocal(latestInterview.interviewTime) : '',
    interviewMode: hasPendingInterview
      ? latestInterview.interviewMode || getDefaultInterviewMode(candidate?.preferredInterviewMode)
      : getDefaultInterviewMode(candidate?.preferredInterviewMode),
    interviewerName: hasPendingInterview ? latestInterview.interviewerName : '',
    note: hasPendingInterview ? latestInterview.inviteNote ?? '' : '',
  }

  resultForm.value = {
    resultStatus: 'PASS',
    applicationStatus: candidate?.status === 'HIRED' ? 'HIRED' : 'OFFERED',
    feedbackNote: '',
    rejectReason: '',
  }
  statusForm.value = {
    targetStatus: getDefaultManualStatus(candidate?.status),
  }
}

function getDefaultInterviewMode(preferredInterviewMode?: string | null) {
  if (preferredInterviewMode === 'TEXT') {
    return 'TEXT'
  }
  if (preferredInterviewMode === 'ONLINE') {
    return 'ONLINE'
  }
  return 'ONLINE'
}

function getDefaultManualStatus(status?: string | null) {
  switch (status) {
    case 'INTERVIEW':
      return 'INTERVIEWING'
    case 'VIEWED':
    case 'INTERVIEWING':
    case 'OFFERED':
    case 'HIRED':
    case 'REJECTED':
      return status
    default:
      return 'VIEWED'
  }
}

function toApiDateTime(value: string) {
  return `${value.replace('T', ' ')}:00`
}

function toDateTimeLocal(value?: string | null) {
  if (!value?.trim()) {
    return ''
  }
  return value.replace(' ', 'T').slice(0, 16)
}

function candidateDetailPath(applicationId: number) {
  return `/enterprise/candidates/${selectedJobId.value}/${applicationId}`
}

function interviewPath(applicationId: number) {
  return `/enterprise/interviews/${selectedJobId.value}/${applicationId}`
}

onMounted(() => {
  void refreshPage()
})
</script>

<template>
  <div class="page-head">
    <div>
      <p class="eyebrow">企业端 / 面试管理</p>
      <h2>面试管理</h2>
    </div>
    <div class="page-actions">
      <RouterLink v-if="selectedJobId" class="secondary-link" :to="`/enterprise/candidates?jobId=${selectedJobId}`">候选人列表</RouterLink>
      <RouterLink v-if="selectedCandidateId" class="secondary-link" :to="candidateDetailPath(selectedCandidateId)">候选人详情</RouterLink>
      <RouterLink v-if="selectedJobId" class="secondary-link" :to="`/enterprise/jobs/${selectedJobId}`">岗位详情</RouterLink>
      <button type="button" class="primary-button" @click="refreshPage">刷新数据</button>
    </div>
  </div>

  <p v-if="loadError" class="status-error" role="alert">{{ loadError }}</p>

  <article class="ledger-panel job-switcher">
    <div class="panel-headline">
      <p class="eyebrow">岗位切换</p>
      <h3>选择岗位后查看候选人</h3>
    </div>
    <p v-if="isJobsLoading" class="status-muted">正在加载岗位...</p>
    <div v-else-if="jobs.length" class="job-switcher-grid">
      <button
        v-for="job in jobs"
        :key="job.id"
        type="button"
        class="job-switcher-item"
        :class="{ 'is-active': job.id === selectedJobId }"
        @click="handleSelectJob(job.id)"
      >
        <div class="row-head">
          <h4>{{ job.title }}</h4>
          <span>{{ getJobStageLabel(job.stage) }}</span>
        </div>
        <p>{{ job.department }} / {{ job.city }} / {{ getWorkModeLabel(job.workMode) }}</p>
      </button>
    </div>
    <p v-else class="status-muted">暂无岗位数据。</p>
  </article>

  <section class="metric-strip">
    <div class="metric-cell">
      <span>当前岗位候选人</span>
      <strong>{{ candidates.length }}</strong>
      <p>{{ selectedJob ? `${selectedJob.title} 下的候选人数` : '选择岗位后查看' }}</p>
    </div>
    <div class="metric-cell">
      <span>待反馈面试</span>
      <strong>{{ pendingInterviewCount }}</strong>
      <p>已有邀约但尚未录入结果</p>
    </div>
    <div class="metric-cell">
      <span>已录结果</span>
      <strong>{{ completedInterviewCount }}</strong>
      <p>最近一轮面试已写入结论</p>
    </div>
    <div class="metric-cell">
      <span>已录用</span>
      <strong>{{ hiredCount }}</strong>
      <p>当前岗位已录用的人数</p>
    </div>
  </section>

  <section class="master-detail">
    <article class="list-pane">
      <div class="panel-headline">
        <p class="eyebrow">候选人列表</p>
        <h3>选择候选人</h3>
      </div>
      <p v-if="isCandidatesLoading" class="status-muted">正在加载候选人记录...</p>
      <p v-else-if="!candidates.length" class="status-muted">这个岗位暂时还没有候选人记录。</p>

      <RouterLink
        v-for="candidate in candidates"
        :key="candidate.applicationId"
        class="select-row"
        :class="{ 'is-selected': candidate.applicationId === selectedCandidateId }"
        :to="interviewPath(candidate.applicationId)"
      >
        <div class="select-score">
          <span>匹配</span>
          <strong>{{ candidate.matchScore }}</strong>
        </div>
        <div class="select-copy">
          <div class="row-head">
            <h4>{{ candidate.candidateName }}</h4>
            <span>{{ getApplicationStatusLabel(candidate.status) }}</span>
          </div>
          <p>{{ candidate.recommendationSummary }}</p>
          <div class="inline-tags">
            <span>{{ getCandidateRecommendationLabel(candidate.recommendationStage) }}</span>
            <span>{{ getSupportVisibilityLabel(candidate.supportVisibility) }}</span>
            <span v-if="candidate.latestInterview">
              {{ getInterviewModeLabel(candidate.latestInterview.interviewMode) }} /
              {{ getInterviewResultLabel(candidate.latestInterview.resultStatus) }}
            </span>
          </div>
        </div>
      </RouterLink>
    </article>

    <article class="detail-pane">
      <template v-if="selectedCandidate">
        <div class="panel-headline">
          <div>
            <p class="eyebrow">流程动作</p>
            <h3>{{ selectedCandidate.candidateName }}</h3>
          </div>
          <span class="status-chip">{{ getApplicationStatusLabel(selectedCandidate.status) }}</span>
        </div>

        <div class="workflow-summary">
          <span>岗位：{{ selectedCandidate.jobTitle }}</span>
          <span>投递时间：{{ formatDateTime(selectedCandidate.submittedAt) }}</span>
          <span>首选面试：{{ getInterviewModeLabel(selectedCandidate.preferredInterviewMode) }}</span>
          <span>支持可见性：{{ getSupportVisibilityLabel(selectedCandidate.supportVisibility) }}</span>
        </div>

        <p v-if="actionError" class="status-error" role="alert">{{ actionError }}</p>
        <p v-if="actionSuccess" class="success-banner" aria-live="polite">{{ actionSuccess }}</p>

        <div class="detail-block workflow-card">
          <div class="row-head">
            <div>
              <p class="eyebrow">状态更新</p>
              <h4>先同步招聘状态</h4>
            </div>
          </div>

          <form class="action-form" @submit.prevent="submitManualStatusUpdate">
            <div class="form-grid">
              <label class="field-block">
                <span>招聘状态</span>
                <select v-model="statusForm.targetStatus" class="field-input">
                  <option v-for="option in manualStatusOptions" :key="option.value" :value="option.value">
                    {{ option.label }}
                  </option>
                </select>
              </label>
            </div>
            <div class="action-row">
              <button type="submit" class="secondary-link" :disabled="isStatusSubmitting || !canUpdateStatus">
                {{ isStatusSubmitting ? '更新中...' : '更新招聘状态' }}
              </button>
              <span v-if="!canUpdateStatus" class="muted-note">终态记录不能手动变更。</span>
            </div>
          </form>
        </div>

        <div class="detail-block workflow-card">
          <div class="row-head">
            <div>
              <p class="eyebrow">面试邀约</p>
              <h4>{{ pendingInterview ? '当前已有待反馈面试' : '发起新一轮面试' }}</h4>
            </div>
            <span class="status-chip">
              {{
                pendingInterview
                  ? `${getInterviewModeLabel(pendingInterview.interviewMode)} / ${formatDateTime(pendingInterview.interviewTime)}`
                  : selectedCandidate.latestInterview
                    ? getInterviewResultLabel(selectedCandidate.latestInterview.resultStatus)
                    : '尚未发起面试'
              }}
            </span>
          </div>

          <form class="action-form" @submit.prevent="submitInterviewInvite">
            <div class="form-grid">
              <label class="field-block">
                <span>面试时间</span>
                <input v-model="inviteForm.interviewTime" class="field-input" type="datetime-local" />
              </label>
              <label class="field-block">
                <span>面试方式</span>
                <select v-model="inviteForm.interviewMode" class="field-input">
                  <option v-for="option in interviewModeOptions" :key="option.value" :value="option.value">
                    {{ option.label }}
                  </option>
                </select>
              </label>
              <label class="field-block">
                <span>面试官</span>
                <input v-model="inviteForm.interviewerName" class="field-input" type="text" />
              </label>
              <label class="field-block field-block-wide">
                <span>邀约说明</span>
                <textarea v-model="inviteForm.note" class="field-input field-textarea" rows="3" />
              </label>
            </div>
            <div class="action-row">
              <button type="submit" class="primary-button" :disabled="isInviteSubmitting || !canInvite">
                {{ isInviteSubmitting ? '提交中...' : pendingInterview ? '更新面试邀约' : '发起面试邀约' }}
              </button>
              <span v-if="!canInvite" class="muted-note">{{ inviteDisabledReason }}</span>
            </div>
          </form>
        </div>

        <div v-if="canSubmitResult" class="detail-block workflow-card">
          <div class="row-head">
            <div>
              <p class="eyebrow">结果记录</p>
              <h4>写回面试结果</h4>
            </div>
          </div>

          <form class="action-form" @submit.prevent="submitInterviewResult">
            <div class="form-grid">
              <label class="field-block">
                <span>面试结果</span>
                <select v-model="resultForm.resultStatus" class="field-input">
                  <option value="PASS">通过</option>
                  <option value="FAIL">未通过</option>
                </select>
              </label>
              <label class="field-block">
                <span>推进状态</span>
                <select
                  v-model="resultForm.applicationStatus"
                  class="field-input"
                  :disabled="resultForm.resultStatus === 'FAIL'"
                >
                  <option v-for="option in passStatusOptions" :key="option.value" :value="option.value">
                    {{ option.label }}
                  </option>
                  <option value="REJECTED">未通过</option>
                </select>
              </label>
              <label class="field-block field-block-wide">
                <span>结果说明</span>
                <textarea v-model="resultForm.feedbackNote" class="field-input field-textarea" rows="3" />
              </label>
              <label v-if="resultForm.resultStatus === 'FAIL'" class="field-block field-block-wide">
                <span>未通过原因</span>
                <textarea v-model="resultForm.rejectReason" class="field-input field-textarea" rows="3" />
              </label>
            </div>
            <div class="action-row">
              <button type="submit" class="primary-button" :disabled="isResultSubmitting">
                {{ isResultSubmitting ? '提交中...' : '记录面试结果' }}
              </button>
            </div>
          </form>
        </div>

        <div class="detail-block">
          <div class="row-head">
            <div>
              <p class="eyebrow">历史记录</p>
              <h4>保留完整面试轨迹</h4>
            </div>
            <RouterLink class="secondary-link" :to="candidateDetailPath(selectedCandidate.applicationId)">查看候选人详情</RouterLink>
          </div>

          <div v-if="selectedCandidate.interviewRecords.length" class="history-list">
            <article v-for="record in selectedCandidate.interviewRecords" :key="record.interviewId" class="history-card">
              <div class="row-head">
                <strong>{{ getInterviewModeLabel(record.interviewMode) }}</strong>
                <span>{{ getInterviewResultLabel(record.resultStatus) }}</span>
              </div>
              <p class="history-meta">
                {{ formatDateTime(record.interviewTime || record.createdAt) }} / {{ record.interviewerName }}
              </p>
              <p v-if="record.inviteNote" class="detail-copy">{{ record.inviteNote }}</p>
              <p v-if="record.feedbackNote" class="detail-copy">{{ record.feedbackNote }}</p>
              <p v-if="record.rejectReason" class="status-error">未通过原因：{{ record.rejectReason }}</p>
            </article>
          </div>
          <p v-else class="status-muted">尚未发起面试。</p>
        </div>
      </template>

      <p v-else class="status-muted">从左侧选择候选人后查看详情。</p>
    </article>
  </section>
</template>

<style scoped>
.job-switcher {
  margin-bottom: 20px;
}

.job-switcher-grid,
.workflow-card,
.action-form,
.history-list {
  display: grid;
  gap: 14px;
}

.job-switcher-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.job-switcher-item {
  padding: 16px;
  border: 1px solid var(--line-strong);
  background: rgba(252, 252, 249, 0.92);
  text-align: left;
}

.job-switcher-item.is-active {
  border-color: var(--brand);
  background: rgba(37, 99, 235, 0.08);
}

.workflow-summary {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  color: var(--muted);
  font-size: 0.94rem;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.field-block {
  display: grid;
  gap: 8px;
}

.field-block-wide {
  grid-column: 1 / -1;
}

.field-input {
  width: 100%;
  min-height: 42px;
  padding: 0 12px;
  border: 1px solid var(--line-strong);
  background: #ffffff;
}

.field-textarea {
  min-height: 96px;
  padding: 12px;
  resize: vertical;
}

.history-list {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.history-card {
  display: grid;
  gap: 8px;
  padding: 14px;
  border: 1px solid var(--line);
  background: rgba(255, 255, 255, 0.7);
}

.history-meta,
.detail-copy,
.status-muted {
  margin: 0;
  line-height: 1.7;
}

.status-chip {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 32px;
  padding: 0 12px;
  background: rgba(37, 99, 235, 0.12);
  color: var(--brand);
  font-weight: 700;
}

.action-row {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
}

.success-banner,
.status-error {
  margin: 0;
}

.success-banner {
  color: var(--success);
  font-weight: 700;
}

.status-error {
  color: var(--error);
  font-weight: 700;
}

.status-muted,
.muted-note {
  color: var(--muted);
}

@media (max-width: 1100px) {
  .job-switcher-grid,
  .form-grid,
  .history-list {
    grid-template-columns: 1fr;
  }
}
</style>

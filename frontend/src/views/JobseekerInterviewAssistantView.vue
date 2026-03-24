<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { RouterLink } from 'vue-router'

import {
  createInterviewSupportRequest,
  getCurrentInterviewSupportRequests,
  getCurrentUserApplications,
  type InterviewSupportRequest,
  type JobApplication,
} from '../api/jobseeker'
import { ApiError } from '../lib/http'
import {
  formatDateTime,
  getApplicationStatusLabel,
  getInterviewModeLabel,
  getInterviewResultLabel,
  getInterviewSupportRequestStatusLabel,
  getInterviewSupportRequestTypeLabel,
} from '../lib/jobseeker'
import { useAuthStore } from '../stores/auth'

interface SupportForm {
  requestType: string
  requestContent: string
}

const authStore = useAuthStore()

const applications = ref<JobApplication[]>([])
const supportRequests = ref<InterviewSupportRequest[]>([])
const supportForms = reactive<Record<number, SupportForm>>({})
const supportSubmitting = reactive<Record<number, boolean>>({})
const supportErrors = reactive<Record<number, string>>({})
const supportSuccess = reactive<Record<number, string>>({})
const isLoading = ref(false)
const loadError = ref('')

const supportTypeOptions = [
  { value: 'TEXT_INTERVIEW', label: '文字面试' },
  { value: 'SUBTITLE', label: '字幕支持' },
  { value: 'REMOTE_INTERVIEW', label: '远程面试' },
  { value: 'FLEXIBLE_TIME', label: '弹性时间' },
  { value: 'OTHER', label: '其他说明' },
]
const supportEligibleStatuses = new Set(['APPLIED', 'VIEWED', 'INTERVIEW', 'INTERVIEWING'])

const eligibleApplications = computed(() =>
  applications.value.filter((item) => supportEligibleStatuses.has(item.status) || item.interviewRecords.length),
)
const interviewCount = computed(() => applications.value.filter((item) => item.interviewRecords.length).length)
const pendingSupportCount = computed(
  () => supportRequests.value.filter((item) => item.requestStatus === 'PENDING').length,
)
const firstEligibleApplicationId = computed(() => eligibleApplications.value[0]?.applicationId ?? 0)

function supportHistory(applicationId: number) {
  return supportRequests.value.filter((item) => item.applicationId === applicationId)
}

function canRequestSupport(application: JobApplication) {
  return supportEligibleStatuses.has(application.status)
}

function syncForms() {
  eligibleApplications.value.forEach((application) => {
    supportForms[application.applicationId] ??= {
      requestType: 'TEXT_INTERVIEW',
      requestContent: '',
    }
  })
}

function scrollToFirstRequest() {
  if (!firstEligibleApplicationId.value) {
    return
  }

  document.getElementById(`interview-request-${firstEligibleApplicationId.value}`)?.scrollIntoView({
    behavior: 'smooth',
    block: 'start',
  })
}

async function loadData() {
  if (!authStore.token) {
    return
  }

  isLoading.value = true
  loadError.value = ''

  try {
    const [applicationList, supportList] = await Promise.all([
      getCurrentUserApplications(authStore.token),
      getCurrentInterviewSupportRequests(authStore.token),
    ])
    applications.value = applicationList
    supportRequests.value = supportList
    syncForms()
  } catch (error) {
    loadError.value = error instanceof ApiError ? error.message : '面试辅助加载失败'
  } finally {
    isLoading.value = false
  }
}

async function submitSupport(application: JobApplication) {
  if (!authStore.token) {
    return
  }

  const form = supportForms[application.applicationId]
  if (!form?.requestContent.trim()) {
    supportErrors[application.applicationId] = '请先填写支持说明。'
    supportSuccess[application.applicationId] = ''
    return
  }

  supportSubmitting[application.applicationId] = true
  supportErrors[application.applicationId] = ''
  supportSuccess[application.applicationId] = ''

  try {
    const saved = await createInterviewSupportRequest(authStore.token, {
      applicationId: application.applicationId,
      requestType: form.requestType,
      requestContent: form.requestContent.trim(),
    })
    supportRequests.value = [saved, ...supportRequests.value.filter((item) => item.id !== saved.id)]
    supportForms[application.applicationId].requestContent = ''
    supportSuccess[application.applicationId] = '面试支持申请已提交。'
  } catch (error) {
    supportErrors[application.applicationId] = error instanceof ApiError ? error.message : '面试支持申请提交失败'
  } finally {
    supportSubmitting[application.applicationId] = false
  }
}

onMounted(() => {
  void loadData()
})
</script>

<template>
  <div class="page-head">
    <div>
      <p class="eyebrow">求职者 / 面试辅助</p>
      <h2>面试辅助</h2>
    </div>
    <div class="page-actions">
      <RouterLink class="secondary-link" to="/applications">投递记录</RouterLink>
      <RouterLink class="secondary-link" to="/jobseeker/support-needs">便利需求</RouterLink>
      <button type="button" class="primary-button" :disabled="!firstEligibleApplicationId" @click="scrollToFirstRequest">
        发起支持申请
      </button>
    </div>
  </div>

  <p v-if="loadError" class="status-error" role="alert">{{ loadError }}</p>
  <p v-else-if="isLoading" class="status-muted">正在加载面试辅助数据...</p>

  <section class="metric-strip">
    <div class="metric-cell">
      <span>面试相关岗位</span>
      <strong>{{ eligibleApplications.length }}</strong>
      <p>已进入面试阶段的岗位。</p>
    </div>
    <div class="metric-cell">
      <span>已有面试记录</span>
      <strong>{{ interviewCount }}</strong>
      <p>企业已经返回面试记录的岗位。</p>
    </div>
    <div class="metric-cell">
      <span>支持申请</span>
      <strong>{{ supportRequests.length }}</strong>
      <p>历史提交的面试支持申请总数。</p>
    </div>
    <div class="metric-cell">
      <span>待处理申请</span>
      <strong>{{ pendingSupportCount }}</strong>
      <p>等待企业或服务方确认的申请。</p>
    </div>
  </section>

  <article v-if="!isLoading && !eligibleApplications.length" class="ledger-panel">
    <div class="panel-headline">
      <p class="eyebrow">当前状态</p>
      <h3>还没有进入面试辅助阶段的岗位</h3>
    </div>
    <ul class="detail-list">
      <li>先去“推荐岗位”继续投递，或在“投递记录”里关注企业是否已查看申请。</li>
      <li>当岗位进入“面试中”或已产生面试记录后，这里会自动显示对应卡片。</li>
      <li>如需提前说明沟通偏好，可先完善便利需求。</li>
    </ul>
  </article>

  <section v-else class="interview-list">
    <article
      v-for="application in eligibleApplications"
      :id="`interview-request-${application.applicationId}`"
      :key="application.applicationId"
      class="ledger-panel interview-card"
    >
      <div class="record-head">
        <div>
          <p class="eyebrow">面试岗位</p>
          <div class="row-head">
            <h3>{{ application.jobTitle }}</h3>
            <span>{{ application.companyName }}</span>
          </div>
          <p class="body-copy">
            状态：{{ getApplicationStatusLabel(application.status) }} ·
            提交：{{ formatDateTime(application.submittedAt) }} ·
            面试偏好：{{ getInterviewModeLabel(application.preferredInterviewMode) }}
          </p>
        </div>
        <span class="score-chip">申请 {{ supportHistory(application.applicationId).length }}</span>
      </div>

      <div class="interview-grid">
        <section class="detail-block">
          <p class="eyebrow">当前面试状态</p>
          <template v-if="application.latestInterview">
            <h4>{{ getInterviewResultLabel(application.latestInterview.resultStatus) }}</h4>
            <p class="body-copy">时间：{{ formatDateTime(application.latestInterview.interviewTime) }}</p>
            <p class="body-copy">形式：{{ getInterviewModeLabel(application.latestInterview.interviewMode) }}</p>
            <p class="body-copy">面试官：{{ application.latestInterview.interviewerName || '待补充' }}</p>
            <p v-if="application.latestInterview.inviteNote" class="body-copy">
              邀约说明：{{ application.latestInterview.inviteNote }}
            </p>
            <p v-if="application.latestInterview.feedbackNote" class="body-copy">
              面试反馈：{{ application.latestInterview.feedbackNote }}
            </p>
          </template>
          <p v-else class="status-muted">企业尚未写回正式面试记录，但该岗位已进入面试阶段。</p>
        </section>

        <section class="detail-block">
          <p class="eyebrow">支持申请</p>
          <p v-if="supportSuccess[application.applicationId]" class="status-success">{{ supportSuccess[application.applicationId] }}</p>
          <p v-if="supportErrors[application.applicationId]" class="status-error">{{ supportErrors[application.applicationId] }}</p>
          <form v-if="canRequestSupport(application)" class="workflow-form" @submit.prevent="submitSupport(application)">
            <label class="form-field">
              <span>支持类型</span>
              <select v-model="supportForms[application.applicationId].requestType" class="field-control">
                <option v-for="option in supportTypeOptions" :key="option.value" :value="option.value">
                  {{ option.label }}
                </option>
              </select>
            </label>
            <label class="form-field">
              <span>支持说明</span>
              <textarea
                v-model="supportForms[application.applicationId].requestContent"
                rows="4"
                class="field-control textarea-control"
                placeholder="补充你希望企业或服务机构配合的面试方式、沟通节奏或材料要求。"
              />
            </label>
            <button type="submit" class="primary-button" :disabled="supportSubmitting[application.applicationId]">
              {{ supportSubmitting[application.applicationId] ? '提交中...' : '提交支持申请' }}
            </button>
          </form>
          <p v-else class="status-muted">当前状态下不可再新增面试支持申请。</p>
        </section>
      </div>

      <div class="detail-block">
        <p class="eyebrow">申请历史</p>
        <div v-if="supportHistory(application.applicationId).length" class="history-stack">
          <article v-for="item in supportHistory(application.applicationId)" :key="item.id" class="history-card">
            <div class="row-head">
              <h4>{{ item.requestTypeLabel || getInterviewSupportRequestTypeLabel(item.requestType) }}</h4>
              <span class="score-chip">{{ getInterviewSupportRequestStatusLabel(item.requestStatus) }}</span>
            </div>
            <p class="body-copy">{{ item.requestContent }}</p>
            <p class="status-muted">{{ formatDateTime(item.createdAt) }}</p>
          </article>
        </div>
        <p v-else class="status-muted">还没有提交过面试支持申请。</p>
      </div>

      <div class="detail-actions">
        <RouterLink class="secondary-link" :to="`/jobs/${application.jobId}`">岗位详情</RouterLink>
        <RouterLink class="secondary-link" to="/jobseeker/onboarding-feedback">入职反馈</RouterLink>
      </div>
    </article>
  </section>
</template>

<style scoped>
.interview-list,
.interview-card,
.interview-grid,
.workflow-form,
.history-stack,
.form-field {
  display: grid;
  gap: 16px;
}

.interview-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.record-head {
  display: flex;
  justify-content: space-between;
  gap: 14px;
  flex-wrap: wrap;
  align-items: start;
}

.history-card {
  display: grid;
  gap: 8px;
  padding: 14px;
  border: 1px solid var(--line);
  background: rgba(252, 252, 249, 0.92);
}

.status-error {
  margin: 0 0 16px;
  color: var(--error);
  font-weight: 700;
}

.status-success {
  color: var(--success);
  font-weight: 700;
}

.status-muted {
  color: var(--muted);
}

@media (max-width: 960px) {
  .interview-grid {
    grid-template-columns: 1fr;
  }
}
</style>

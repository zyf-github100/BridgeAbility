<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { RouterLink } from 'vue-router'

import {
  createEmploymentFollowup,
  getCurrentEmploymentFollowups,
  getCurrentUserApplications,
  type EmploymentFollowup,
  type JobApplication,
} from '../api/jobseeker'
import { ApiError } from '../lib/http'
import {
  formatDateTime,
  getApplicationStatusLabel,
  getEmploymentFollowupStageLabel,
  getInterviewModeLabel,
  getInterviewResultLabel,
} from '../lib/jobseeker'
import { useAuthStore } from '../stores/auth'

interface FollowupForm {
  followupStage: string
  adaptationScore: number
  environmentIssue: string
  communicationIssue: string
  supportImplemented: 'true' | 'false'
  leaveRisk: 'true' | 'false'
  needHelp: 'true' | 'false'
  remark: string
}

const authStore = useAuthStore()

const applications = ref<JobApplication[]>([])
const followups = ref<EmploymentFollowup[]>([])
const followupForms = reactive<Record<string, FollowupForm>>({})
const submittingStates = reactive<Record<string, boolean>>({})
const successMessages = reactive<Record<string, string>>({})
const errorMessages = reactive<Record<string, string>>({})
const isLoading = ref(false)
const loadError = ref('')

const followupStageOptions = [
  { value: 'DAY_7', label: '7 天跟踪' },
  { value: 'DAY_30', label: '30 天跟踪' },
]
const booleanOptions = [
  { value: 'true', label: '是' },
  { value: 'false', label: '否' },
]

const hiredApplications = computed(() =>
  applications.value.filter((item) => item.status === 'HIRED'),
)
const offeredApplications = computed(() =>
  applications.value.filter((item) => item.status === 'OFFERED'),
)
const day7Count = computed(() => followups.value.filter((item) => item.followupStage === 'DAY_7').length)
const day30Count = computed(() => followups.value.filter((item) => item.followupStage === 'DAY_30').length)

function followupHistory(jobId: string) {
  return followups.value.filter((item) => item.jobId === jobId)
}

function nextFollowupStage(jobId: string) {
  return followupHistory(jobId).some((item) => item.followupStage === 'DAY_7') ? 'DAY_30' : 'DAY_7'
}

function toBoolean(value: 'true' | 'false') {
  return value === 'true'
}

function optionalText(value: string) {
  const trimmed = value.trim()
  return trimmed || undefined
}

function syncForms() {
  hiredApplications.value.forEach((application) => {
    followupForms[application.jobId] ??= {
      followupStage: nextFollowupStage(application.jobId),
      adaptationScore: 80,
      environmentIssue: '',
      communicationIssue: '',
      supportImplemented: 'true',
      leaveRisk: 'false',
      needHelp: 'false',
      remark: '',
    }
  })
}

async function loadData() {
  if (!authStore.token) {
    return
  }

  isLoading.value = true
  loadError.value = ''
  try {
    const [applicationList, followupList] = await Promise.all([
      getCurrentUserApplications(authStore.token),
      getCurrentEmploymentFollowups(authStore.token),
    ])
    applications.value = applicationList
    followups.value = followupList
    syncForms()
  } catch (error) {
    loadError.value = error instanceof ApiError ? error.message : '入职反馈加载失败'
  } finally {
    isLoading.value = false
  }
}

async function submitFollowup(application: JobApplication) {
  if (!authStore.token) {
    return
  }

  const form = followupForms[application.jobId]
  if (!form || form.adaptationScore < 0 || form.adaptationScore > 100) {
    errorMessages[application.jobId] = '适应评分需为 0-100。'
    successMessages[application.jobId] = ''
    return
  }

  submittingStates[application.jobId] = true
  errorMessages[application.jobId] = ''
  successMessages[application.jobId] = ''

  try {
    const saved = await createEmploymentFollowup(authStore.token, {
      jobId: application.jobId,
      followupStage: form.followupStage,
      adaptationScore: Math.round(form.adaptationScore),
      environmentIssue: optionalText(form.environmentIssue),
      communicationIssue: optionalText(form.communicationIssue),
      supportImplemented: toBoolean(form.supportImplemented),
      leaveRisk: toBoolean(form.leaveRisk),
      needHelp: toBoolean(form.needHelp),
      remark: optionalText(form.remark),
    })

    followups.value = [
      saved,
      ...followups.value.filter(
        (item) => !(item.jobId === saved.jobId && item.followupStage === saved.followupStage),
      ),
    ]

    followupForms[application.jobId] = {
      ...followupForms[application.jobId],
      followupStage: nextFollowupStage(application.jobId),
      environmentIssue: '',
      communicationIssue: '',
      remark: '',
    }
    successMessages[application.jobId] = `已提交${getEmploymentFollowupStageLabel(saved.followupStage)}反馈。`
  } catch (error) {
    errorMessages[application.jobId] = error instanceof ApiError ? error.message : '入职反馈提交失败'
  } finally {
    submittingStates[application.jobId] = false
  }
}

onMounted(() => {
  void loadData()
})
</script>

<template>
  <div class="page-head">
    <div>
      <p class="eyebrow">求职者 / 入职跟进</p>
      <h2>入职反馈</h2>
    </div>
    <div class="page-actions">
      <RouterLink class="secondary-link" to="/applications">返回投递记录</RouterLink>
      <RouterLink class="secondary-link" to="/jobseeker/service-records">服务支持记录</RouterLink>
      <button type="button" class="primary-button" @click="loadData">刷新</button>
    </div>
  </div>

  <p v-if="loadError" class="status-error" role="alert">{{ loadError }}</p>

  <section class="metric-strip">
    <div class="metric-cell">
      <span>已录用岗位</span>
      <strong>{{ hiredApplications.length }}</strong>
      <p>可以进入正式入职反馈的岗位</p>
    </div>
    <div class="metric-cell">
      <span>待录用岗位</span>
      <strong>{{ offeredApplications.length }}</strong>
      <p>还未进入正式反馈阶段</p>
    </div>
    <div class="metric-cell">
      <span>7 天反馈</span>
      <strong>{{ day7Count }}</strong>
      <p>已提交的首轮适应记录</p>
    </div>
    <div class="metric-cell">
      <span>30 天反馈</span>
      <strong>{{ day30Count }}</strong>
      <p>已提交的稳定期反馈</p>
    </div>
  </section>

  <article v-if="isLoading" class="ledger-panel">
    <p class="status-muted">正在加载入职反馈数据...</p>
  </article>

  <article v-else-if="!hiredApplications.length" class="ledger-panel">
    <div class="panel-headline">
      <div>
        <p class="eyebrow">当前状态</p>
        <h3>还没有进入入职反馈阶段的岗位</h3>
      </div>
    </div>
    <ul class="detail-list">
      <li>先在“投递与跟踪”里关注企业面试结果和录用状态。</li>
      <li>当岗位进入“已录用”后，这里会显示 7 天和 30 天反馈表单。</li>
      <li>收到录用后，再在这里记录适应情况和需要帮助的事项。</li>
    </ul>
  </article>

  <section v-else class="feedback-list">
    <article v-for="application in hiredApplications" :key="application.applicationId" class="ledger-panel feedback-card">
      <div class="record-head">
        <div>
          <p class="eyebrow">已录用岗位</p>
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
        <span class="match-chip">历史反馈 {{ followupHistory(application.jobId).length }}</span>
      </div>

      <div class="feedback-grid">
        <section class="detail-block">
          <p class="eyebrow">当前操作</p>
          <p v-if="successMessages[application.jobId]" class="status-success">{{ successMessages[application.jobId] }}</p>
          <p v-if="errorMessages[application.jobId]" class="status-error">{{ errorMessages[application.jobId] }}</p>
          <form class="workflow-form" @submit.prevent="submitFollowup(application)">
            <label class="form-field">
              <span>反馈阶段</span>
              <select v-model="followupForms[application.jobId].followupStage" class="field-control">
                <option v-for="option in followupStageOptions" :key="option.value" :value="option.value">{{ option.label }}</option>
              </select>
            </label>
            <label class="form-field">
              <span>适应评分</span>
              <input
                v-model.number="followupForms[application.jobId].adaptationScore"
                type="number"
                min="0"
                max="100"
                class="field-control"
              />
            </label>
            <div class="form-pair">
              <label class="form-field">
                <span>支持落实</span>
                <select v-model="followupForms[application.jobId].supportImplemented" class="field-control">
                  <option v-for="option in booleanOptions" :key="option.value" :value="option.value">{{ option.label }}</option>
                </select>
              </label>
              <label class="form-field">
                <span>离职风险</span>
                <select v-model="followupForms[application.jobId].leaveRisk" class="field-control">
                  <option v-for="option in booleanOptions" :key="option.value" :value="option.value">{{ option.label }}</option>
                </select>
              </label>
              <label class="form-field">
                <span>需要帮助</span>
                <select v-model="followupForms[application.jobId].needHelp" class="field-control">
                  <option v-for="option in booleanOptions" :key="option.value" :value="option.value">{{ option.label }}</option>
                </select>
              </label>
            </div>
            <label class="form-field">
              <span>环境问题</span>
              <textarea v-model="followupForms[application.jobId].environmentIssue" rows="2" class="field-control textarea-control" />
            </label>
            <label class="form-field">
              <span>沟通问题</span>
              <textarea v-model="followupForms[application.jobId].communicationIssue" rows="2" class="field-control textarea-control" />
            </label>
            <label class="form-field">
              <span>备注</span>
              <textarea v-model="followupForms[application.jobId].remark" rows="2" class="field-control textarea-control" />
            </label>
            <button type="submit" class="primary-button" :disabled="submittingStates[application.jobId]">
              {{ submittingStates[application.jobId] ? '提交中...' : '提交入职反馈' }}
            </button>
          </form>
        </section>

        <section class="detail-block">
          <p class="eyebrow">历史记录</p>
          <div v-if="followupHistory(application.jobId).length" class="history-stack">
            <article v-for="item in followupHistory(application.jobId)" :key="`${item.jobId}-${item.followupStage}`" class="history-card">
              <div class="row-head">
                <h4>{{ getEmploymentFollowupStageLabel(item.followupStage) }}</h4>
                <span class="score-chip">适应 {{ item.adaptationScore }}</span>
              </div>
              <p class="body-copy">
                {{ item.supportImplemented ? '支持已落实' : '支持待落实' }} /
                {{ item.needHelp ? '需要继续协助' : '暂无额外协助' }}
              </p>
              <p v-if="item.environmentIssue" class="body-copy">环境问题：{{ item.environmentIssue }}</p>
              <p v-if="item.communicationIssue" class="body-copy">沟通问题：{{ item.communicationIssue }}</p>
              <p v-if="item.remark" class="body-copy">备注：{{ item.remark }}</p>
              <p class="history-meta">{{ formatDateTime(item.updatedAt) }}</p>
            </article>
          </div>
          <p v-else class="status-muted">还没有提交过入职反馈。</p>

          <div v-if="application.latestInterview" class="detail-block latest-interview">
            <p class="eyebrow">最近一次面试记录</p>
            <h4>{{ getInterviewResultLabel(application.latestInterview.resultStatus) }}</h4>
            <p class="body-copy">时间：{{ formatDateTime(application.latestInterview.interviewTime) }}</p>
            <p class="body-copy">形式：{{ getInterviewModeLabel(application.latestInterview.interviewMode) }}</p>
            <p v-if="application.latestInterview.feedbackNote" class="body-copy">
              反馈：{{ application.latestInterview.feedbackNote }}
            </p>
          </div>

          <div class="detail-actions">
            <RouterLink class="secondary-link" :to="`/jobs/${application.jobId}`">岗位详情</RouterLink>
            <RouterLink class="secondary-link" to="/applications">投递记录</RouterLink>
          </div>
        </section>
      </div>
    </article>
  </section>
</template>

<style scoped>
.feedback-list,
.feedback-card,
.workflow-form,
.history-stack {
  display: grid;
  gap: 18px;
}

.feedback-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.record-head {
  display: flex;
  justify-content: space-between;
  gap: 14px;
  flex-wrap: wrap;
  align-items: start;
}

.form-field,
.form-pair {
  display: grid;
  gap: 10px;
}

.form-pair {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.field-control {
  width: 100%;
  border: 1px solid var(--line-strong);
  padding: 12px 14px;
  background: #fff;
}

.history-card {
  display: grid;
  gap: 8px;
  padding: 14px;
  border: 1px solid var(--line);
  background: rgba(255, 255, 255, 0.76);
}

.match-chip {
  padding: 10px 14px;
  border: 1px solid var(--line-strong);
  background: rgba(37, 99, 235, 0.08);
  color: var(--heading);
}

.history-meta,
.status-muted {
  color: var(--muted);
}

.status-error {
  color: var(--error);
  font-weight: 700;
}

.status-success {
  color: var(--success);
  font-weight: 700;
}

.latest-interview {
  margin-top: 14px;
}

@media (max-width: 980px) {
  .feedback-grid,
  .form-pair {
    grid-template-columns: 1fr;
  }
}
</style>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'

import {
  getEnterpriseJobCandidates,
  getEnterpriseJobs,
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
const totalJobs = ref(0)
const selectedJobId = ref('')
const candidates = ref<EnterpriseCandidateApplication[]>([])
const consentFilter = ref<'ALL' | 'true' | 'false'>('ALL')
const isJobsLoading = ref(false)
const isCandidatesLoading = ref(false)
const loadError = ref('')

const routeJobId = computed(() => (typeof route.query.jobId === 'string' ? route.query.jobId : ''))
const selectedJob = computed(() => jobs.value.find((job) => job.id === selectedJobId.value) ?? null)
const consentGrantedCount = computed(() => candidates.value.filter((candidate) => candidate.consentGranted).length)
const interviewingCount = computed(() =>
  candidates.value.filter((candidate) => ['INTERVIEW', 'INTERVIEWING'].includes(candidate.status)).length,
)
const withInterviewRecordCount = computed(() => candidates.value.filter((candidate) => candidate.latestInterview).length)

watch(
  () => routeJobId.value,
  (value) => {
    if (value && value !== selectedJobId.value) {
      selectedJobId.value = value
    }
  },
)

watch(selectedJobId, async (jobId) => {
  if (!jobId) {
    candidates.value = []
    return
  }

  if (jobId !== routeJobId.value) {
    await router.replace({
      query: {
        ...route.query,
        jobId,
      },
    })
  }

  await loadCandidates()
})

watch(consentFilter, async () => {
  if (!selectedJobId.value) {
    return
  }
  await loadCandidates()
})

async function loadJobs() {
  if (!authStore.token) {
    return
  }

  isJobsLoading.value = true
  loadError.value = ''

  try {
    const page = await getEnterpriseJobs(authStore.token, { page: 1, pageSize: 50 })
    jobs.value = page.list
    totalJobs.value = page.total

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
      consentGranted: consentFilter.value === 'ALL' ? undefined : consentFilter.value === 'true',
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
      <p class="eyebrow">企业端 / 候选人列表</p>
      <h2>候选人列表</h2>
    </div>
    <div class="page-actions">
      <RouterLink v-if="selectedJobId" class="secondary-link" :to="`/enterprise/jobs/${selectedJobId}`">岗位详情</RouterLink>
      <RouterLink v-if="selectedJobId" class="secondary-link" :to="`/enterprise/jobs/${selectedJobId}/edit`">编辑岗位</RouterLink>
      <RouterLink class="secondary-link" to="/enterprise/interviews">面试管理</RouterLink>
      <button type="button" class="primary-button" @click="refreshPage">刷新列表</button>
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
        @click="selectedJobId = job.id"
      >
        <div class="row-head">
          <h4>{{ job.title }}</h4>
          <span>{{ getJobStageLabel(job.stage) }}</span>
        </div>
        <p>{{ job.department }} / {{ job.city }} / {{ getWorkModeLabel(job.workMode) }}</p>
        <div class="inline-tags">
          <span>{{ job.candidateCount }} 份投递</span>
          <span>{{ job.accessibilityCompletionRate }}% 标签完成</span>
        </div>
      </button>
    </div>
    <p v-else class="status-muted">暂无岗位数据。</p>
  </article>

  <section class="metric-strip">
    <div class="metric-cell">
      <span>岗位总数</span>
      <strong>{{ totalJobs }}</strong>
      <p>企业端当前可查看的全部岗位</p>
    </div>
    <div class="metric-cell">
      <span>当前岗位候选人</span>
      <strong>{{ candidates.length }}</strong>
      <p>{{ selectedJob ? `${selectedJob.title} 的候选人记录数` : '选择岗位后查看' }}</p>
    </div>
    <div class="metric-cell">
      <span>已授权支持摘要</span>
      <strong>{{ consentGrantedCount }}</strong>
      <p>可直接查看支持需求摘要的候选人数</p>
    </div>
    <div class="metric-cell">
      <span>进入面试环节</span>
      <strong>{{ interviewingCount }}</strong>
      <p>可继续跳转到面试管理页处理的候选人数</p>
    </div>
  </section>

  <div class="filter-strip">
    <span>当前岗位：{{ selectedJob ? selectedJob.title : '未选择' }}</span>
    <span>岗位阶段：{{ selectedJob ? getJobStageLabel(selectedJob.stage) : '--' }}</span>
    <span>已有面试记录：{{ withInterviewRecordCount }}</span>
    <label class="inline-filter">
      <span>授权状态</span>
      <select v-model="consentFilter" class="inline-select">
        <option value="ALL">全部</option>
        <option value="true">已授权</option>
        <option value="false">未授权</option>
      </select>
    </label>
  </div>

  <article class="ledger-panel">
    <div class="panel-headline">
      <div>
        <p class="eyebrow">候选人列表</p>
        <h3>从列表进入详情页和面试页</h3>
      </div>
    </div>

    <p v-if="isCandidatesLoading" class="status-muted">正在加载候选人记录...</p>
    <div v-else-if="candidates.length" class="candidate-grid">
      <article v-for="candidate in candidates" :key="candidate.applicationId" class="candidate-card">
        <div class="row-head">
          <div>
            <h4>{{ candidate.candidateName }}</h4>
            <p class="candidate-subtitle">
              {{ candidate.expectedJob }} / {{ candidate.city }} / {{ formatDateTime(candidate.submittedAt) }}
            </p>
          </div>
          <span class="status-chip">{{ getApplicationStatusLabel(candidate.status) }}</span>
        </div>

        <div class="inline-tags">
          <span>匹配分 {{ candidate.matchScore }}</span>
          <span>{{ getCandidateRecommendationLabel(candidate.recommendationStage) }}</span>
          <span>{{ getSupportVisibilityLabel(candidate.supportVisibility) }}</span>
          <span>{{ candidate.consentGranted ? '已授权支持摘要' : '未授权支持摘要' }}</span>
          <span>{{ getWorkModeLabel(candidate.workModePreference) }}</span>
        </div>

        <p class="detail-copy">{{ candidate.recommendationSummary }}</p>
        <p class="detail-copy">{{ candidate.coverNote }}</p>
        <p v-if="candidate.latestInterview" class="status-muted">
          最近面试：{{ getInterviewModeLabel(candidate.latestInterview.interviewMode) }} /
          {{ getInterviewResultLabel(candidate.latestInterview.resultStatus) }}
        </p>

        <div class="action-row compact-actions">
          <RouterLink class="secondary-link" :to="candidateDetailPath(candidate.applicationId)">候选人详情</RouterLink>
          <RouterLink class="secondary-link" :to="interviewPath(candidate.applicationId)">面试管理</RouterLink>
        </div>
      </article>
    </div>
    <p v-else class="status-muted">这个岗位暂时还没有候选人记录。</p>
  </article>
</template>

<style scoped>
.job-switcher {
  margin-bottom: 20px;
}

.job-switcher-grid,
.candidate-grid {
  display: grid;
  gap: 16px;
}

.job-switcher-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.candidate-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.job-switcher-item,
.candidate-card {
  display: grid;
  gap: 12px;
  padding: 18px;
  border: 1px solid var(--line-strong);
  background: rgba(252, 252, 249, 0.92);
  text-align: left;
}

.job-switcher-item.is-active {
  border-color: var(--brand);
  background: rgba(37, 99, 235, 0.08);
}

.candidate-subtitle,
.detail-copy,
.status-muted {
  margin: 8px 0 0;
  line-height: 1.7;
}

.status-chip {
  display: inline-flex;
  align-items: center;
  padding: 4px 10px;
  border: 1px solid var(--line-strong);
  background: rgba(47, 111, 237, 0.08);
  color: var(--brand);
}

.compact-actions {
  margin-top: 0;
}

.status-error {
  color: var(--error);
  font-weight: 700;
}

.status-muted {
  color: var(--muted);
}

@media (max-width: 1100px) {
  .job-switcher-grid,
  .candidate-grid {
    grid-template-columns: 1fr;
  }
}
</style>

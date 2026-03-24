<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { RouterLink } from 'vue-router'

import {
  getEnterpriseJobs,
  getEnterpriseProfile,
  type EnterpriseJobSummary,
  type EnterpriseVerificationProfile,
} from '../api/enterprise'
import { ApiError } from '../lib/http'
import { getJobStageLabel, getWorkModeLabel } from '../lib/jobseeker'
import { useAuthStore } from '../stores/auth'

const authStore = useAuthStore()

const jobs = ref<EnterpriseJobSummary[]>([])
const totalJobs = ref(0)
const enterpriseProfile = ref<EnterpriseVerificationProfile | null>(null)
const publishStatusFilter = ref('ALL')
const isJobsLoading = ref(false)
const isProfileLoading = ref(false)
const loadError = ref('')
const profileError = ref('')

const publishedCount = computed(() => jobs.value.filter((job) => job.publishStatus === 'PUBLISHED').length)
const readyCount = computed(() => jobs.value.filter((job) => job.readyToPublish).length)
const withCandidateCount = computed(() => jobs.value.filter((job) => job.candidateCount > 0).length)
const canPublishJobs = computed(() => enterpriseProfile.value?.canPublishJobs ?? false)

function getPublishStatusLabel(value: string) {
  switch (value) {
    case 'PUBLISHED':
      return '已发布'
    case 'OFFLINE':
      return '已下线'
    default:
      return '草稿'
  }
}

async function loadJobs() {
  if (!authStore.token) {
    return
  }

  isJobsLoading.value = true
  loadError.value = ''

  try {
    const page = await getEnterpriseJobs(authStore.token, {
      page: 1,
      pageSize: 50,
      publishStatus: publishStatusFilter.value === 'ALL' ? undefined : publishStatusFilter.value,
    })
    jobs.value = page.list
    totalJobs.value = page.total
  } catch (error) {
    loadError.value = error instanceof ApiError ? error.message : '岗位列表加载失败'
  } finally {
    isJobsLoading.value = false
  }
}

async function loadEnterpriseProfile() {
  if (!authStore.token) {
    return
  }

  isProfileLoading.value = true
  profileError.value = ''

  try {
    enterpriseProfile.value = await getEnterpriseProfile(authStore.token)
  } catch (error) {
    profileError.value = error instanceof ApiError ? error.message : '企业认证状态加载失败'
  } finally {
    isProfileLoading.value = false
  }
}

async function refreshPage() {
  await Promise.all([loadJobs(), loadEnterpriseProfile()])
}

watch(publishStatusFilter, async () => {
  await loadJobs()
})

onMounted(() => {
  void refreshPage()
})
</script>

<template>
  <div class="page-head">
    <div>
      <p class="eyebrow">企业端 / 岗位管理</p>
      <h2>岗位管理</h2>
    </div>
    <div class="page-actions">
      <RouterLink class="secondary-link" to="/enterprise/verification">企业认证</RouterLink>
      <RouterLink class="secondary-link" to="/enterprise/stats">招聘统计</RouterLink>
      <RouterLink class="primary-button" to="/enterprise/jobs/publish">发布岗位</RouterLink>
    </div>
  </div>

  <p v-if="profileError" class="status-error" role="alert">{{ profileError }}</p>
  <p v-if="loadError" class="status-error" role="alert">{{ loadError }}</p>

  <article class="ledger-panel publishing-gate">
    <div class="panel-headline">
      <div>
        <p class="eyebrow">发布前检查</p>
        <h3>{{ canPublishJobs ? '认证已通过，可以进入发布与详情页推进岗位' : '认证未通过，当前只能保存草稿' }}</h3>
      </div>
      <RouterLink class="secondary-link" to="/enterprise/verification">查看认证资料</RouterLink>
    </div>

    <p v-if="isProfileLoading" class="status-muted">正在同步企业认证状态...</p>

    <div v-else class="gate-grid">
      <div class="detail-block">
        <p class="eyebrow">认证摘要</p>
        <ul class="detail-list compact-list">
          <li>企业名称：{{ enterpriseProfile?.companyName || '未填写' }}</li>
          <li>认证状态：{{ enterpriseProfile?.verificationStatus || 'DRAFT' }}</li>
          <li>可否发布岗位：{{ canPublishJobs ? '可以发布' : '仅可保存草稿' }}</li>
          <li>已发布岗位：{{ enterpriseProfile?.publishedJobCount ?? 0 }}</li>
        </ul>
        <p v-if="enterpriseProfile?.reviewNote" class="detail-copy">{{ enterpriseProfile.reviewNote }}</p>
      </div>

      <div class="detail-block">
        <p class="eyebrow">常用入口</p>
        <ul class="detail-list compact-list">
          <li>查看岗位详情，核对岗位内容、推荐理由和候选人情况。</li>
          <li>前往发布岗位，维护岗位信息和无障碍标签。</li>
          <li>打开候选人列表，继续跟进投递与面试进度。</li>
        </ul>
      </div>
    </div>
  </article>

  <section class="metric-strip">
    <div class="metric-cell">
      <span>岗位总数</span>
      <strong>{{ totalJobs }}</strong>
      <p>当前账号下全部岗位</p>
    </div>
    <div class="metric-cell">
      <span>已发布</span>
      <strong>{{ publishedCount }}</strong>
      <p>当前筛选结果中的已发布岗位</p>
    </div>
    <div class="metric-cell">
      <span>可直接发布</span>
      <strong>{{ readyCount }}</strong>
      <p>无障碍标签已补齐的岗位</p>
    </div>
    <div class="metric-cell">
      <span>已有候选人</span>
      <strong>{{ withCandidateCount }}</strong>
      <p>可继续进入候选人列表推进的岗位</p>
    </div>
  </section>

  <div class="filter-strip">
    <label class="inline-filter">
      <span>发布状态</span>
      <select v-model="publishStatusFilter" class="inline-select">
        <option value="ALL">全部</option>
        <option value="PUBLISHED">已发布</option>
        <option value="DRAFT">草稿</option>
        <option value="OFFLINE">已下线</option>
      </select>
    </label>
    <span>筛选结果：{{ jobs.length }}</span>
    <span>可发布：{{ canPublishJobs ? '是' : '否' }}</span>
  </div>

  <article class="ledger-panel">
    <div class="panel-headline">
      <div>
        <p class="eyebrow">岗位列表</p>
        <h3>查看岗位与进入相关页面</h3>
      </div>
      <button type="button" class="primary-button" @click="refreshPage">刷新数据</button>
    </div>

    <p v-if="isJobsLoading" class="status-muted">正在加载岗位列表...</p>
    <div v-else-if="jobs.length" class="job-board">
      <article v-for="job in jobs" :key="job.id" class="job-card">
        <div class="row-head">
          <div>
            <h4>{{ job.title }}</h4>
            <p class="job-subtitle">{{ job.department }} / {{ job.city }} / {{ getWorkModeLabel(job.workMode) }}</p>
          </div>
          <span class="status-chip">{{ getPublishStatusLabel(job.publishStatus) }}</span>
        </div>

        <div class="inline-tags">
          <span>{{ job.salaryRange }}</span>
          <span>{{ job.headcount }} 人</span>
          <span>{{ job.candidateCount }} 份投递</span>
          <span>{{ job.accessibilityCompletionRate }}% 标注完成</span>
          <span>{{ getJobStageLabel(job.stage) }}</span>
        </div>

        <p class="detail-copy">
          当前岗位{{ job.readyToPublish ? '已补齐发布前所需标签' : '仍有标签待补齐' }}，
          匹配基准分 {{ job.matchScore }}。
        </p>

        <div class="action-row compact-actions">
          <RouterLink class="secondary-link" :to="`/enterprise/jobs/${job.id}`">岗位详情</RouterLink>
          <RouterLink class="secondary-link" :to="`/enterprise/jobs/${job.id}/edit`">编辑发布</RouterLink>
          <RouterLink class="secondary-link" :to="`/enterprise/candidates?jobId=${job.id}`">候选人列表</RouterLink>
          <RouterLink class="secondary-link" :to="`/enterprise/interviews?jobId=${job.id}`">面试管理</RouterLink>
        </div>
      </article>
    </div>
    <p v-else class="status-muted">当前筛选条件下还没有岗位。</p>
  </article>
</template>

<style scoped>
.publishing-gate {
  margin-bottom: 20px;
}

.gate-grid,
.job-board {
  display: grid;
  gap: 16px;
}

.gate-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.job-board {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.job-card {
  display: grid;
  gap: 14px;
  padding: 18px;
  border: 1px solid var(--line-strong);
  background: rgba(252, 252, 249, 0.92);
}

.job-subtitle,
.detail-copy {
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

.status-error,
.status-muted {
  margin: 0 0 16px;
}

.status-error {
  color: var(--error);
  font-weight: 700;
}

.status-muted {
  color: var(--muted);
}

@media (max-width: 1100px) {
  .gate-grid,
  .job-board {
    grid-template-columns: 1fr;
  }
}
</style>

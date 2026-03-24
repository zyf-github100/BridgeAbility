<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'

import {
  getEnterpriseJobs,
  getEnterpriseProfile,
  getEnterpriseRecruitmentStats,
  type EnterpriseJobSummary,
  type EnterpriseRecruitmentStats,
  type EnterpriseVerificationProfile,
} from '../api/enterprise'
import { ApiError } from '../lib/http'
import { getJobStageLabel, getWorkModeLabel } from '../lib/jobseeker'
import { useAuthStore } from '../stores/auth'

const authStore = useAuthStore()

const profile = ref<EnterpriseVerificationProfile | null>(null)
const stats = ref<EnterpriseRecruitmentStats | null>(null)
const jobs = ref<EnterpriseJobSummary[]>([])
const isLoading = ref(false)
const loadError = ref('')

const featuredJobs = computed(() => jobs.value.slice(0, 4))
const verificationStatusLabel = computed(() => {
  switch (profile.value?.verificationStatus) {
    case 'APPROVED':
      return '认证通过'
    case 'PENDING':
      return '审核中'
    case 'REJECTED':
      return '已驳回'
    default:
      return '待提交'
  }
})
const nextActions = computed(() => {
  if (!profile.value || !stats.value) {
    return []
  }

  const actions = [
    profile.value.canPublishJobs
      ? '认证已通过，可直接进入岗位管理补齐草稿并发布。'
      : '请先完善企业认证资料；认证通过后即可正式发布岗位。',
    stats.value.draftJobs > 0
      ? `当前仍有 ${stats.value.draftJobs} 个草稿岗位，完善无障碍信息后即可继续发布。`
      : '当前没有草稿岗位，可以继续跟进候选人进展。',
    stats.value.interviewingCount > 0
      ? `有 ${stats.value.interviewingCount} 位候选人正在面试中，可前往候选人列表更新结果。`
      : '当前没有进行中的面试，可关注岗位曝光和投递转化。',
  ]

  return actions
})

async function loadConsole() {
  if (!authStore.token) {
    return
  }

  isLoading.value = true
  loadError.value = ''
  try {
    const [profileData, statsData, jobsPage] = await Promise.all([
      getEnterpriseProfile(authStore.token),
      getEnterpriseRecruitmentStats(authStore.token),
      getEnterpriseJobs(authStore.token, { page: 1, pageSize: 8 }),
    ])
    profile.value = profileData
    stats.value = statsData
    jobs.value = jobsPage.list
  } catch (error) {
    loadError.value = error instanceof ApiError ? error.message : '企业首页加载失败'
  } finally {
    isLoading.value = false
  }
}

onMounted(() => {
  void loadConsole()
})
</script>

<template>
  <div class="page-head">
    <div>
      <p class="eyebrow">企业端 / 首页</p>
      <h2>企业首页</h2>
    </div>
    <div class="page-actions">
      <RouterLink class="secondary-link" to="/enterprise/verification">企业认证</RouterLink>
      <RouterLink class="secondary-link" to="/enterprise/jobs">岗位管理</RouterLink>
      <RouterLink class="secondary-link" to="/enterprise/candidates">候选人列表</RouterLink>
      <RouterLink class="secondary-link" to="/enterprise/interviews">面试管理</RouterLink>
      <button type="button" class="primary-button" @click="loadConsole">刷新首页</button>
    </div>
  </div>

  <p v-if="loadError" class="status-error" role="alert">{{ loadError }}</p>
  <p v-else-if="isLoading" class="status-muted">正在加载企业首页数据...</p>

  <template v-if="profile && stats && !isLoading">
    <section class="metric-strip">
      <div class="metric-cell">
        <span>认证状态</span>
        <strong>{{ verificationStatusLabel }}</strong>
        <p>{{ profile.reviewNote || '认证状态决定岗位是否可以正式发布。' }}</p>
      </div>
      <div class="metric-cell">
        <span>已发布岗位</span>
        <strong>{{ stats.publishedJobs }}</strong>
        <p>草稿 {{ stats.draftJobs }} / 下线 {{ stats.offlineJobs }}</p>
      </div>
      <div class="metric-cell">
        <span>候选人投递</span>
        <strong>{{ stats.totalApplications }}</strong>
        <p>面试中 {{ stats.interviewingCount }} / 已录用 {{ stats.hiredCount }}</p>
      </div>
      <div class="metric-cell">
        <span>标注完成度</span>
        <strong>{{ stats.averageAccessibilityCompletionRate }}%</strong>
        <p>平均匹配分 {{ stats.averageMatchScore }}</p>
      </div>
    </section>

    <section class="content-columns">
      <article class="ledger-panel console-panel">
        <div class="panel-headline">
          <div>
            <p class="eyebrow">首页摘要</p>
            <h3>先处理这些关键事项</h3>
          </div>
        </div>

        <div class="summary-grid">
          <div class="detail-block">
            <p class="eyebrow">企业资料</p>
            <ul class="detail-list">
              <li>企业名称：{{ profile.companyName || '未填写' }}</li>
              <li>所在城市：{{ profile.city || '未填写' }}</li>
              <li>可否发布岗位：{{ profile.canPublishJobs ? '可以发布' : '仅可保存草稿' }}</li>
              <li>已上传资料：{{ profile.materials.length }}</li>
            </ul>
          </div>

          <div class="detail-block">
            <p class="eyebrow">下一步建议</p>
            <ul class="detail-list">
              <li v-for="item in nextActions" :key="item">{{ item }}</li>
            </ul>
          </div>
        </div>

        <div class="action-grid">
          <RouterLink class="route-item" to="/enterprise/jobs">
            <strong>进入岗位管理</strong>
            <span>查看和维护岗位信息</span>
          </RouterLink>
          <RouterLink class="route-item" to="/enterprise/candidates">
            <strong>进入候选人列表</strong>
            <span>查看候选人并跟进投递进度</span>
          </RouterLink>
          <RouterLink class="route-item" to="/enterprise/interviews">
            <strong>进入面试管理</strong>
            <span>处理面试邀约、状态和结果</span>
          </RouterLink>
        </div>
      </article>

      <article class="ledger-panel console-panel">
        <div class="panel-headline">
          <div>
            <p class="eyebrow">重点岗位</p>
            <h3>优先查看最近在推进的岗位</h3>
          </div>
        </div>

        <div v-if="featuredJobs.length" class="job-list">
          <article v-for="job in featuredJobs" :key="job.id" class="job-card">
            <div class="row-head">
              <h4>{{ job.title }}</h4>
              <span>{{ job.publishStatus }}</span>
            </div>
            <p>{{ job.department }} / {{ job.city }} / {{ getWorkModeLabel(job.workMode) }}</p>
            <div class="inline-tags">
              <span>{{ job.salaryRange }}</span>
              <span>{{ job.candidateCount }} 份投递</span>
              <span>{{ job.accessibilityCompletionRate }}% 标注完成</span>
            </div>
            <p class="body-copy">阶段：{{ getJobStageLabel(job.stage) }}</p>
            <div class="compact-actions">
              <RouterLink class="secondary-link" :to="`/enterprise/jobs/${job.id}`">岗位详情</RouterLink>
              <RouterLink class="secondary-link" :to="`/enterprise/candidates?jobId=${job.id}`">候选人列表</RouterLink>
            </div>
          </article>
        </div>
        <p v-else class="status-muted">当前还没有岗位数据。</p>

        <div class="detail-block">
          <p class="eyebrow">数据摘要</p>
          <ul class="detail-list">
            <li v-for="item in stats.insights.slice(0, 4)" :key="item">{{ item }}</li>
          </ul>
        </div>
      </article>
    </section>
  </template>
</template>

<style scoped>
.console-panel,
.job-list,
.summary-grid,
.action-grid {
  display: grid;
  gap: 16px;
}

.summary-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.action-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.job-card {
  display: grid;
  gap: 10px;
  padding: 18px;
  border: 1px solid var(--line);
  background: rgba(255, 255, 255, 0.76);
}

.job-card p {
  margin: 0;
  line-height: 1.75;
}

.compact-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
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

@media (max-width: 980px) {
  .summary-grid,
  .action-grid {
    grid-template-columns: 1fr;
  }
}
</style>

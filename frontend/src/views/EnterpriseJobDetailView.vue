<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { RouterLink, useRoute } from 'vue-router'

import ScoreBar from '../components/ScoreBar.vue'
import {
  getEnterpriseJobCandidates,
  getEnterpriseJobDetail,
  type EnterpriseCandidateApplication,
  type EnterpriseJobDetail,
} from '../api/enterprise'
import { ApiError } from '../lib/http'
import {
  formatDateTime,
  getApplicationStatusLabel,
  getCandidateRecommendationLabel,
  getInterviewResultLabel,
  getJobStageLabel,
  getScoreDimensionLabel,
  getWorkModeLabel,
} from '../lib/jobseeker'
import { useAuthStore } from '../stores/auth'

const route = useRoute()
const authStore = useAuthStore()

const jobDetail = ref<EnterpriseJobDetail | null>(null)
const candidatePreview = ref<EnterpriseCandidateApplication[]>([])
const totalCandidates = ref(0)
const isLoading = ref(false)
const loadError = ref('')

const jobId = computed(() => {
  const value = Array.isArray(route.params.jobId) ? route.params.jobId[0] : route.params.jobId
  return value ?? ''
})

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

async function loadPageData() {
  if (!authStore.token || !jobId.value) {
    return
  }

  isLoading.value = true
  loadError.value = ''

  try {
    const [detail, candidatesPage] = await Promise.all([
      getEnterpriseJobDetail(authStore.token, jobId.value),
      getEnterpriseJobCandidates(authStore.token, jobId.value, { page: 1, pageSize: 4 }),
    ])
    jobDetail.value = detail
    candidatePreview.value = candidatesPage.list
    totalCandidates.value = candidatesPage.total
  } catch (error) {
    loadError.value = error instanceof ApiError ? error.message : '企业岗位详情加载失败'
  } finally {
    isLoading.value = false
  }
}

watch(
  () => jobId.value,
  async () => {
    await loadPageData()
  },
  { immediate: true },
)

onMounted(() => {
  void loadPageData()
})
</script>

<template>
  <div class="page-head">
    <div>
      <p class="eyebrow">企业端 / 岗位详情</p>
      <h2>{{ jobDetail?.title || '企业岗位详情' }}</h2>
    </div>
    <div class="page-actions">
      <RouterLink class="secondary-link" to="/enterprise/jobs">返回岗位列表</RouterLink>
      <RouterLink v-if="jobId" class="secondary-link" :to="`/enterprise/jobs/${jobId}/edit`">编辑发布</RouterLink>
      <RouterLink v-if="jobId" class="secondary-link" :to="`/enterprise/candidates?jobId=${jobId}`">候选人列表</RouterLink>
      <RouterLink v-if="jobId" class="primary-button" :to="`/enterprise/interviews?jobId=${jobId}`">面试管理</RouterLink>
    </div>
  </div>

  <p v-if="loadError" class="status-error" role="alert">{{ loadError }}</p>
  <p v-else-if="isLoading" class="status-muted">正在加载岗位详情...</p>

  <template v-if="jobDetail && !isLoading">
    <section class="metric-strip">
      <div class="metric-cell">
        <span>发布状态</span>
        <strong>{{ getPublishStatusLabel(jobDetail.publishStatus) }}</strong>
        <p>当前岗位的对外可见状态</p>
      </div>
      <div class="metric-cell">
        <span>岗位阶段</span>
        <strong>{{ getJobStageLabel(jobDetail.stage) }}</strong>
        <p>当前岗位的招聘推进级别</p>
      </div>
      <div class="metric-cell">
        <span>匹配基准分</span>
        <strong>{{ jobDetail.matchScore }}</strong>
        <p>推荐系统对该岗位的基础评分</p>
      </div>
    <div class="metric-cell">
      <span>候选人投递</span>
      <strong>{{ totalCandidates }}</strong>
      <p>可查看当前岗位的候选人与面试进展</p>
    </div>
    </section>

    <section class="content-columns">
      <article class="ledger-panel">
        <div class="panel-headline">
          <div>
            <p class="eyebrow">岗位概览</p>
            <h3>{{ jobDetail.companyName }}</h3>
          </div>
        </div>

        <div class="detail-block">
          <ul class="detail-list compact-list">
            <li>部门：{{ jobDetail.department }}</li>
            <li>工作地点：{{ jobDetail.city }}</li>
            <li>工作方式：{{ getWorkModeLabel(jobDetail.workMode) }}</li>
            <li>薪资范围：{{ jobDetail.salaryRange }}</li>
            <li>招聘人数：{{ jobDetail.headcount }}</li>
            <li>截止日期：{{ jobDetail.deadline }}</li>
            <li>标签完成度：{{ jobDetail.accessibilityCompletionRate }}%</li>
          </ul>
        </div>

        <div class="detail-block">
          <p class="eyebrow">岗位职责</p>
          <p class="detail-copy">{{ jobDetail.description }}</p>
        </div>

        <div class="detail-block">
          <p class="eyebrow">任职要求</p>
          <p class="detail-copy">{{ jobDetail.requirementText }}</p>
        </div>

        <div class="action-grid">
          <RouterLink class="route-item" :to="`/enterprise/jobs/${jobDetail.id}/edit`">
            <strong>进入发布页</strong>
            <span>维护岗位字段和无障碍标签</span>
          </RouterLink>
          <RouterLink class="route-item" :to="`/enterprise/candidates?jobId=${jobDetail.id}`">
            <strong>查看候选人</strong>
            <span>进入该岗位下的候选人列表</span>
          </RouterLink>
          <RouterLink class="route-item" :to="`/enterprise/interviews?jobId=${jobDetail.id}`">
            <strong>进入面试页</strong>
            <span>继续处理面试邀约和结果记录</span>
          </RouterLink>
        </div>
      </article>

      <article class="ledger-panel analytics-panel">
        <div class="panel-headline">
          <div>
            <p class="eyebrow">推荐与风险</p>
            <h3>岗位对外呈现摘要</h3>
          </div>
        </div>

        <div class="score-stack">
          <ScoreBar
            v-for="score in jobDetail.dimensionScores"
            :key="score.label"
            :label="getScoreDimensionLabel(score.label)"
            :value="score.value"
          />
        </div>

        <div class="detail-block">
          <p class="eyebrow">推荐理由</p>
          <ul class="detail-list">
            <li v-for="item in jobDetail.reasons" :key="item">{{ item }}</li>
          </ul>
        </div>

        <div class="detail-block">
          <p class="eyebrow">风险提示</p>
          <ul class="detail-list">
            <li v-for="item in jobDetail.risks" :key="item">{{ item }}</li>
          </ul>
        </div>

        <div class="detail-block">
          <p class="eyebrow">支持信息</p>
          <ul class="detail-list">
            <li v-for="item in jobDetail.supports" :key="item">{{ item }}</li>
          </ul>
        </div>

        <div class="detail-block">
          <p class="eyebrow">环境摘要</p>
          <ul class="detail-list">
            <li v-for="item in jobDetail.environment" :key="item">{{ item }}</li>
          </ul>
        </div>

        <div class="detail-block">
          <p class="eyebrow">投递提示</p>
          <p class="detail-copy">{{ jobDetail.applyHint }}</p>
        </div>
      </article>
    </section>

    <article class="ledger-panel candidate-panel">
      <div class="panel-headline">
        <div>
          <p class="eyebrow">候选人样本</p>
          <h3>从岗位详情直接跳到候选人详情和面试管理</h3>
        </div>
        <RouterLink class="secondary-link" :to="`/enterprise/candidates?jobId=${jobDetail.id}`">查看完整列表</RouterLink>
      </div>

      <div v-if="candidatePreview.length" class="candidate-grid">
        <article v-for="candidate in candidatePreview" :key="candidate.applicationId" class="candidate-card">
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
            <span>档案 {{ candidate.profileCompletionRate }}%</span>
            <span>{{ candidate.consentGranted ? '已授权支持摘要' : '未授权支持摘要' }}</span>
          </div>

          <p class="detail-copy">{{ candidate.recommendationSummary }}</p>
          <p v-if="candidate.latestInterview" class="status-muted">
            最近面试：{{ getInterviewResultLabel(candidate.latestInterview.resultStatus) }} /
            {{ formatDateTime(candidate.latestInterview.interviewTime || candidate.latestInterview.createdAt) }}
          </p>

          <div class="action-row compact-actions">
            <RouterLink class="secondary-link" :to="`/enterprise/candidates/${jobDetail.id}/${candidate.applicationId}`">
              候选人详情
            </RouterLink>
            <RouterLink class="secondary-link" :to="`/enterprise/interviews/${jobDetail.id}/${candidate.applicationId}`">
              面试管理
            </RouterLink>
          </div>
        </article>
      </div>
      <p v-else class="status-muted">当前岗位还没有候选人投递。</p>
    </article>
  </template>
</template>

<style scoped>
.analytics-panel,
.score-stack,
.candidate-grid,
.action-grid {
  display: grid;
  gap: 16px;
}

.candidate-panel {
  margin-top: 20px;
}

.candidate-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.candidate-card {
  display: grid;
  gap: 12px;
  padding: 18px;
  border: 1px solid var(--line);
  background: rgba(255, 255, 255, 0.76);
}

.candidate-subtitle,
.detail-copy,
.status-muted {
  margin: 8px 0 0;
  line-height: 1.7;
}

.action-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
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
  .candidate-grid,
  .action-grid {
    grid-template-columns: 1fr;
  }
}
</style>

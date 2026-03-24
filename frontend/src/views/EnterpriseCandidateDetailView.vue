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
  getInterviewModeLabel,
  getInterviewResultLabel,
  getScoreDimensionLabel,
  getSupportVisibilityLabel,
  getWorkModeLabel,
} from '../lib/jobseeker'
import { useAuthStore } from '../stores/auth'

const route = useRoute()
const authStore = useAuthStore()

const jobDetail = ref<EnterpriseJobDetail | null>(null)
const candidate = ref<EnterpriseCandidateApplication | null>(null)
const isLoading = ref(false)
const loadError = ref('')

const jobId = computed(() => {
  const value = Array.isArray(route.params.jobId) ? route.params.jobId[0] : route.params.jobId
  return value ?? ''
})
const applicationId = computed(() => {
  const value = Array.isArray(route.params.applicationId) ? route.params.applicationId[0] : route.params.applicationId
  return Number(value ?? 0)
})

async function loadPageData() {
  if (!authStore.token || !jobId.value || !applicationId.value) {
    loadError.value = '缺少岗位或候选人参数，请从候选人列表进入。'
    return
  }

  isLoading.value = true
  loadError.value = ''

  try {
    const [detail, candidatesPage] = await Promise.all([
      getEnterpriseJobDetail(authStore.token, jobId.value),
      getEnterpriseJobCandidates(authStore.token, jobId.value, { page: 1, pageSize: 50 }),
    ])
    jobDetail.value = detail
    candidate.value = candidatesPage.list.find((item) => item.applicationId === applicationId.value) ?? null

    if (!candidate.value) {
      loadError.value = '当前岗位下未找到对应候选人，请返回候选人列表重新选择。'
    }
  } catch (error) {
    loadError.value = error instanceof ApiError ? error.message : '候选人详情加载失败'
  } finally {
    isLoading.value = false
  }
}

watch(
  () => [jobId.value, applicationId.value],
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
      <p class="eyebrow">企业端 / 候选人详情</p>
      <h2>{{ candidate?.candidateName || '候选人详情' }}</h2>
    </div>
    <div class="page-actions">
      <RouterLink class="secondary-link" :to="`/enterprise/candidates?jobId=${jobId}`">返回候选人列表</RouterLink>
      <RouterLink class="secondary-link" :to="`/enterprise/jobs/${jobId}`">岗位详情</RouterLink>
      <RouterLink class="primary-button" :to="`/enterprise/interviews/${jobId}/${applicationId}`">面试管理</RouterLink>
    </div>
  </div>

  <p v-if="loadError" class="status-error" role="alert">{{ loadError }}</p>
  <p v-else-if="isLoading" class="status-muted">正在加载候选人详情...</p>

  <template v-if="candidate && jobDetail && !isLoading">
    <section class="metric-strip">
      <div class="metric-cell">
        <span>当前状态</span>
        <strong>{{ getApplicationStatusLabel(candidate.status) }}</strong>
        <p>当前候选人在招聘流程中的位置</p>
      </div>
      <div class="metric-cell">
        <span>匹配分</span>
        <strong>{{ candidate.matchScore }}</strong>
        <p>与岗位 {{ jobDetail.title }} 的匹配结果</p>
      </div>
      <div class="metric-cell">
        <span>档案完成度</span>
        <strong>{{ candidate.profileCompletionRate }}%</strong>
        <p>用于评估候选人资料完整程度</p>
      </div>
      <div class="metric-cell">
        <span>支持摘要权限</span>
        <strong>{{ candidate.consentGranted ? '已授权' : '未授权' }}</strong>
        <p>{{ getSupportVisibilityLabel(candidate.supportVisibility) }}</p>
      </div>
    </section>

    <section class="content-columns">
      <article class="ledger-panel">
        <div class="panel-headline">
          <div>
            <p class="eyebrow">候选人概览</p>
            <h3>{{ candidate.expectedJob }}</h3>
          </div>
          <span class="status-chip">{{ getCandidateRecommendationLabel(candidate.recommendationStage) }}</span>
        </div>

        <div class="detail-block">
          <ul class="detail-list compact-list">
            <li>候选人：{{ candidate.candidateName }}</li>
            <li>意向岗位：{{ candidate.expectedJob }}</li>
            <li>所在城市：{{ candidate.city }}</li>
            <li>工作方式偏好：{{ getWorkModeLabel(candidate.workModePreference) }}</li>
            <li>首选面试方式：{{ getInterviewModeLabel(candidate.preferredInterviewMode) }}</li>
            <li>投递时间：{{ formatDateTime(candidate.submittedAt) }}</li>
          </ul>
        </div>

        <div class="score-stack">
          <ScoreBar label="岗位匹配分" :value="candidate.matchScore" />
          <ScoreBar label="档案完整度" :value="candidate.profileCompletionRate" />
          <ScoreBar label="沟通可执行度" :value="candidate.consentGranted ? 90 : 56" />
          <ScoreBar
            v-for="score in candidate.dimensionScores"
            :key="score.label"
            :label="getScoreDimensionLabel(score.label)"
            :value="score.value"
          />
        </div>

        <div class="detail-block">
          <p class="eyebrow">推荐摘要</p>
          <p class="detail-copy">{{ candidate.recommendationSummary }}</p>
          <ul class="detail-list">
            <li v-for="item in candidate.explanationSnapshot" :key="item">{{ item }}</li>
          </ul>
        </div>

        <div class="detail-block">
          <p class="eyebrow">投递说明</p>
          <p class="detail-copy">{{ candidate.coverNote }}</p>
        </div>

        <div class="action-grid">
          <RouterLink class="route-item" :to="`/enterprise/jobs/${jobId}`">
            <strong>返回岗位详情</strong>
            <span>回到岗位页查看岗位信息和其它候选人样本</span>
          </RouterLink>
          <RouterLink class="route-item" :to="`/enterprise/interviews/${jobId}/${applicationId}`">
            <strong>进入面试管理</strong>
            <span>处理状态、邀约和结果记录</span>
          </RouterLink>
        </div>
      </article>

      <article class="ledger-panel detail-stack">
        <div class="detail-block">
          <p class="eyebrow">候选人档案</p>
          <ul class="detail-list">
            <li>学校：{{ candidate.schoolName || '企业不可见' }}</li>
            <li>专业：{{ candidate.major || '企业不可见' }}</li>
            <li>个人简介：{{ candidate.intro || '仅展示与招聘相关的最小必要信息。' }}</li>
          </ul>
        </div>

        <div class="detail-block">
          <p class="eyebrow">支持申请</p>
          <div v-if="candidate.supportRequests.length" class="request-list">
            <article
              v-for="request in candidate.supportRequests"
              :key="`${request.requestType}-${request.requestContent}`"
              class="request-card"
            >
              <strong>{{ request.requestTypeLabel }}</strong>
              <p>{{ request.requestContent }}</p>
            </article>
          </div>
          <ul v-else class="detail-list">
            <li v-for="item in candidate.supportSummary" :key="item">{{ item }}</li>
          </ul>
        </div>

        <div v-if="candidate.additionalSupport" class="detail-block">
          <p class="eyebrow">补充支持说明</p>
          <p class="detail-copy">{{ candidate.additionalSupport }}</p>
        </div>

        <div class="detail-block">
          <p class="eyebrow">面试记录摘要</p>
          <div v-if="candidate.interviewRecords.length" class="history-list">
            <article v-for="record in candidate.interviewRecords" :key="record.interviewId" class="history-card">
              <div class="row-head">
                <strong>{{ getInterviewModeLabel(record.interviewMode) }}</strong>
                <span>{{ getInterviewResultLabel(record.resultStatus) }}</span>
              </div>
              <p class="detail-copy">{{ formatDateTime(record.interviewTime || record.createdAt) }}</p>
              <p v-if="record.feedbackNote" class="detail-copy">{{ record.feedbackNote }}</p>
              <p v-if="record.rejectReason" class="status-error">未通过原因：{{ record.rejectReason }}</p>
            </article>
          </div>
          <p v-else class="status-muted">尚未发起面试。</p>
        </div>
      </article>
    </section>
  </template>
</template>

<style scoped>
.score-stack,
.detail-stack,
.request-list,
.history-list,
.action-grid {
  display: grid;
  gap: 16px;
}

.request-list,
.history-list,
.action-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.request-card,
.history-card {
  display: grid;
  gap: 8px;
  padding: 14px;
  border: 1px solid var(--line);
  background: rgba(255, 255, 255, 0.7);
}

.detail-copy,
.status-muted {
  margin: 0;
  line-height: 1.8;
  white-space: pre-line;
}

.status-chip {
  display: inline-flex;
  align-items: center;
  padding: 4px 10px;
  border: 1px solid var(--line-strong);
  background: rgba(47, 111, 237, 0.08);
  color: var(--brand);
}

.status-error {
  color: var(--error);
  font-weight: 700;
}

.status-muted {
  color: var(--muted);
}

@media (max-width: 1100px) {
  .request-list,
  .history-list,
  .action-grid {
    grid-template-columns: 1fr;
  }
}
</style>

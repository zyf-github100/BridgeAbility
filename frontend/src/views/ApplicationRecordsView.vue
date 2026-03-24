<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'

import { getCurrentUserApplications, type JobApplication } from '../api/jobseeker'
import { ApiError } from '../lib/http'
import {
  formatDateTime,
  getApplicationStatusLabel,
  getInterviewModeLabel,
  getInterviewResultLabel,
  getSupportVisibilityLabel,
} from '../lib/jobseeker'
import { useAuthStore } from '../stores/auth'

const authStore = useAuthStore()

const applications = ref<JobApplication[]>([])
const isLoading = ref(false)
const loadError = ref('')

const interviewCount = computed(() => applications.value.filter((item) => item.interviewRecords.length).length)
const offeredCount = computed(() => applications.value.filter((item) => item.status === 'OFFERED').length)
const hiredCount = computed(() => applications.value.filter((item) => item.status === 'HIRED').length)
const latestUpdatedAt = computed(() => {
  const values = [
    ...applications.value.map((item) => item.submittedAt),
    ...applications.value.flatMap((item) =>
      item.interviewRecords.flatMap((record) => [record.updatedAt, record.createdAt]),
    ),
  ].filter(Boolean)

  return values.length ? [...values].sort().at(-1) ?? '' : ''
})

async function loadApplications() {
  if (!authStore.token) {
    return
  }

  isLoading.value = true
  loadError.value = ''

  try {
    applications.value = await getCurrentUserApplications(authStore.token)
  } catch (error) {
    loadError.value = error instanceof ApiError ? error.message : '投递记录加载失败'
  } finally {
    isLoading.value = false
  }
}

onMounted(() => {
  void loadApplications()
})
</script>

<template>
  <div class="page-head">
    <div>
      <p class="eyebrow">求职者 / 投递记录</p>
      <h2>投递记录</h2>
    </div>
    <div class="page-actions">
      <RouterLink class="secondary-link" to="/jobseeker/interview-assistance">面试辅助</RouterLink>
      <RouterLink class="secondary-link" to="/jobseeker/onboarding-feedback">入职反馈</RouterLink>
      <RouterLink class="primary-button" to="/jobs">继续投递岗位</RouterLink>
    </div>
  </div>

  <p v-if="loadError" class="status-error" role="alert">{{ loadError }}</p>
  <p v-else-if="isLoading" class="status-muted">正在加载投递记录...</p>

  <section class="metric-strip">
    <div class="metric-cell">
      <span>累计投递</span>
      <strong>{{ applications.length }}</strong>
      <p>所有已提交过的岗位申请。</p>
    </div>
    <div class="metric-cell">
      <span>已有面试</span>
      <strong>{{ interviewCount }}</strong>
      <p>可直接跳到“面试辅助”继续跟进。</p>
    </div>
    <div class="metric-cell">
      <span>待录用</span>
      <strong>{{ offeredCount }}</strong>
      <p>已经收到录用前反馈的岗位。</p>
    </div>
    <div class="metric-cell">
      <span>已录用</span>
      <strong>{{ hiredCount }}</strong>
      <p>{{ latestUpdatedAt ? `最近动作：${formatDateTime(latestUpdatedAt)}` : '暂无最近动作' }}</p>
    </div>
  </section>

  <article v-if="!isLoading && !applications.length" class="ledger-panel">
    <div class="panel-headline">
      <p class="eyebrow">当前状态</p>
      <h3>还没有投递记录</h3>
    </div>
    <ul class="detail-list">
      <li>先去“推荐岗位”查看系统推荐结果。</li>
      <li>投递成功后，这里会回流企业查看、面试和录用状态。</li>
      <li>需要面试支持时打开“面试辅助”，已录用后再填写“入职反馈”。</li>
    </ul>
  </article>

  <section v-else class="records-grid">
    <article v-for="application in applications" :key="application.applicationId" class="ledger-panel record-card">
      <div class="record-head">
        <div>
          <p class="eyebrow">投递岗位</p>
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
        <span class="score-chip">匹配 {{ application.matchScoreSnapshot }}</span>
      </div>

      <div class="record-section">
        <p class="eyebrow">投递说明</p>
        <p class="record-copy">{{ application.coverNote }}</p>
        <p v-if="application.additionalSupport" class="record-copy">需求摘要：{{ application.additionalSupport }}</p>
        <p class="status-muted">展示范围：{{ getSupportVisibilityLabel(application.supportVisibility) }}</p>
      </div>

      <div class="workflow-grid">
        <section class="detail-block">
          <p class="eyebrow">推荐理由</p>
          <ul class="detail-list">
            <li v-for="item in application.explanationSnapshot" :key="item">{{ item }}</li>
          </ul>
        </section>

        <section class="detail-block">
          <p class="eyebrow">最近一次面试</p>
          <template v-if="application.latestInterview">
            <h4>{{ getInterviewResultLabel(application.latestInterview.resultStatus) }}</h4>
            <p class="record-copy">时间：{{ formatDateTime(application.latestInterview.interviewTime) }}</p>
            <p class="record-copy">形式：{{ getInterviewModeLabel(application.latestInterview.interviewMode) }}</p>
            <p class="record-copy">面试官：{{ application.latestInterview.interviewerName || '待补充' }}</p>
            <p v-if="application.latestInterview.feedbackNote" class="record-copy">
              反馈：{{ application.latestInterview.feedbackNote }}
            </p>
          </template>
          <p v-else class="status-muted">还没有新的面试记录。</p>
        </section>
      </div>

      <div class="detail-actions">
        <RouterLink class="secondary-link" :to="`/jobs/${application.jobId}`">岗位详情</RouterLink>
        <RouterLink class="secondary-link" to="/jobseeker/interview-assistance">面试辅助</RouterLink>
        <RouterLink
          v-if="application.status === 'OFFERED' || application.status === 'HIRED'"
          class="secondary-link"
          to="/jobseeker/onboarding-feedback"
        >
          入职反馈
        </RouterLink>
      </div>
    </article>
  </section>
</template>

<style scoped>
.records-grid,
.record-card,
.workflow-grid {
  display: grid;
  gap: 18px;
}

.workflow-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.record-head {
  display: flex;
  justify-content: space-between;
  gap: 14px;
  flex-wrap: wrap;
  align-items: start;
}

.record-section {
  display: grid;
  gap: 8px;
}

.record-copy {
  margin: 0;
  line-height: 1.8;
  white-space: pre-line;
}

.status-error {
  margin: 0 0 16px;
  color: var(--error);
  font-weight: 700;
}

.status-muted {
  color: var(--muted);
}

@media (max-width: 960px) {
  .workflow-grid {
    grid-template-columns: 1fr;
  }
}
</style>

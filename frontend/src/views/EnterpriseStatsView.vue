<script setup lang="ts">
import { onMounted, ref } from 'vue'

import {
  getEnterpriseRecruitmentStats,
  type EnterpriseRecruitmentStats,
} from '../api/enterprise'
import { ApiError } from '../lib/http'
import { getJobStageLabel } from '../lib/jobseeker'
import { useAuthStore } from '../stores/auth'

const authStore = useAuthStore()

const isLoading = ref(false)
const loadError = ref('')
const stats = ref<EnterpriseRecruitmentStats | null>(null)

async function loadStats() {
  if (!authStore.token) {
    return
  }

  isLoading.value = true
  loadError.value = ''

  try {
    stats.value = await getEnterpriseRecruitmentStats(authStore.token)
  } catch (error) {
    loadError.value = error instanceof ApiError ? error.message : '招聘统计加载失败'
  } finally {
    isLoading.value = false
  }
}

function getPublishStatusLabel(value: string) {
  switch (value) {
    case 'PUBLISHED':
      return '已发布'
    case 'DRAFT':
      return '草稿'
    case 'OFFLINE':
      return '已下线'
    default:
      return value || '未知'
  }
}

onMounted(() => {
  loadStats()
})
</script>

<template>
  <div class="page-head">
    <div>
      <p class="eyebrow">企业端 / 招聘统计</p>
      <h2>招聘统计</h2>
    </div>
    <div class="page-actions">
      <button type="button" class="primary-button" @click="loadStats">刷新统计</button>
    </div>
  </div>

  <p v-if="loadError" class="status-error" role="alert">{{ loadError }}</p>
  <p v-if="isLoading" class="status-muted">正在汇总招聘统计...</p>

  <template v-if="stats && !isLoading">
    <section class="metric-strip">
      <div class="metric-cell">
        <span>岗位总数</span>
        <strong>{{ stats.totalJobs }}</strong>
        <p>当前企业端可见的全部岗位数量。</p>
      </div>
      <div class="metric-cell">
        <span>累计投递</span>
        <strong>{{ stats.totalApplications }}</strong>
        <p>已进入统计口径的候选人投递总量。</p>
      </div>
      <div class="metric-cell">
        <span>面试中</span>
        <strong>{{ stats.interviewingCount }}</strong>
        <p>正在推进面试安排或结果确认的候选人。</p>
      </div>
        <div class="metric-cell">
          <span>已录用</span>
          <strong>{{ stats.hiredCount }}</strong>
          <p>已经完成录用的候选人数量。</p>
        </div>
    </section>

    <section class="content-columns">
      <article class="ledger-panel">
        <div class="panel-headline">
          <div>
            <p class="eyebrow">岗位状态</p>
            <h3>发布节奏与岗位准备度</h3>
          </div>
        </div>
        <div class="bucket-list">
          <article v-for="bucket in stats.publishStatusBreakdown" :key="bucket.code" class="bucket-card">
            <div class="row-head">
              <h4>{{ bucket.label }}</h4>
              <span>{{ bucket.value }}</span>
            </div>
            <p>{{ bucket.hint }}</p>
          </article>
        </div>

        <div class="detail-block">
          <p class="eyebrow">质量指标</p>
          <ul class="detail-list">
            <li>岗位平均无障碍标注完成度：{{ stats.averageAccessibilityCompletionRate }}%</li>
            <li>候选人平均匹配分：{{ stats.averageMatchScore }}</li>
            <li>草稿岗位：{{ stats.draftJobs }} 个</li>
            <li>已下线岗位：{{ stats.offlineJobs }} 个</li>
          </ul>
        </div>
      </article>

      <article class="ledger-panel">
        <div class="panel-headline">
          <div>
            <p class="eyebrow">候选人分布</p>
            <h3>当前招聘流程推进情况</h3>
          </div>
        </div>
        <div class="bucket-list">
          <article v-for="bucket in stats.applicationStatusBreakdown" :key="bucket.code" class="bucket-card">
            <div class="row-head">
              <h4>{{ bucket.label }}</h4>
              <span>{{ bucket.value }}</span>
            </div>
            <p>{{ bucket.hint }}</p>
          </article>
        </div>

        <div class="detail-block">
          <p class="eyebrow">统计解读</p>
          <ul class="detail-list">
            <li v-for="item in stats.insights" :key="item">{{ item }}</li>
          </ul>
        </div>

        <div class="detail-block">
          <p class="eyebrow">补充指标</p>
          <ul class="detail-list">
            <li>待处理投递：{{ stats.appliedCount }}</li>
            <li>待录用：{{ stats.offeredCount }}</li>
            <li>未通过：{{ stats.rejectedCount }}</li>
            <li>授权展示便利需求摘要：{{ stats.consentGrantedCount }}</li>
          </ul>
        </div>
      </article>
    </section>

    <article class="ledger-panel leaderboard-panel">
      <div class="panel-headline">
        <div>
          <p class="eyebrow">岗位表现</p>
          <h3>优先关注这些岗位的推进节奏</h3>
        </div>
      </div>

      <div v-if="stats.topJobs.length" class="table-shell">
        <table>
          <thead>
            <tr>
              <th>岗位</th>
              <th>发布状态</th>
              <th>招聘阶段</th>
              <th>投递数</th>
              <th>面试中</th>
              <th>已录用</th>
              <th>匹配分</th>
              <th>标注完成度</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="job in stats.topJobs" :key="job.jobId">
              <td>{{ job.title }}</td>
              <td>{{ getPublishStatusLabel(job.publishStatus) }}</td>
              <td>{{ getJobStageLabel(job.stage) }}</td>
              <td>{{ job.candidateCount }}</td>
              <td>{{ job.interviewingCount }}</td>
              <td>{{ job.hiredCount }}</td>
              <td>{{ job.averageMatchScore }}</td>
              <td>{{ job.accessibilityCompletionRate }}%</td>
            </tr>
          </tbody>
        </table>
      </div>
      <p v-else class="status-muted">当前还没有可展示的岗位统计。</p>
    </article>
  </template>
</template>

<style scoped>
.bucket-list {
  display: grid;
  gap: 14px;
}

.bucket-card {
  display: grid;
  gap: 10px;
  padding: 16px;
  border: 1px solid var(--line);
  background: rgba(255, 255, 255, 0.72);
}

.bucket-card p {
  margin: 0;
  line-height: 1.8;
}

.bucket-card span {
  font-family: var(--serif);
  font-size: 1.8rem;
  color: var(--heading);
}

.leaderboard-panel {
  margin-top: 20px;
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
</style>

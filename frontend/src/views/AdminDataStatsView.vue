<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'

import {
  getAdminDashboard,
  getAuditLogs,
  getPendingEnterpriseReviews,
  type AdminDashboard,
} from '../api/admin'
import { getAdminIssues } from '../api/adminIssues'
import { getAdminRiskRecords } from '../api/adminRisks'
import { ApiError } from '../lib/http'
import { useAuthStore } from '../stores/auth'

const authStore = useAuthStore()

const dashboard = ref<AdminDashboard | null>(null)
const pendingReviewCount = ref(0)
const auditLogCount = ref(0)
const issueCount = ref(0)
const pendingIssueCount = ref(0)
const riskCount = ref(0)
const openRiskCount = ref(0)
const isLoading = ref(false)
const loadError = ref('')

const userTotal = computed(() => (dashboard.value?.jobseekerCount ?? 0) + (dashboard.value?.enterpriseCount ?? 0))
const hireRateHint = computed(() => {
  if (!dashboard.value || dashboard.value.applicationCount === 0) {
    return '暂无投递数据'
  }
  return `${Math.round((dashboard.value.hiredCount * 100) / dashboard.value.applicationCount)}%`
})

async function loadStats() {
  if (!authStore.token) {
    return
  }

  isLoading.value = true
  loadError.value = ''

  try {
    const [dashboardData, reviewQueue, auditLogs, risks, issues] = await Promise.all([
      getAdminDashboard(authStore.token),
      getPendingEnterpriseReviews(authStore.token),
      getAuditLogs(authStore.token),
      getAdminRiskRecords(authStore.token, { page: 1, pageSize: 100 }),
      getAdminIssues(authStore.token),
    ])

    dashboard.value = dashboardData
    pendingReviewCount.value = reviewQueue.length
    auditLogCount.value = auditLogs.length
    riskCount.value = risks.total
    openRiskCount.value = risks.list.filter((item) => item.alertStatus === 'OPEN' || item.alertStatus === 'ESCALATED').length
    issueCount.value = issues.length
    pendingIssueCount.value = issues.filter(
      (item) => item.ticketStatus === 'PENDING' || item.ticketStatus === 'IN_PROGRESS',
    ).length
  } catch (error) {
    loadError.value = error instanceof ApiError ? error.message : '数据统计页加载失败'
  } finally {
    isLoading.value = false
  }
}

onMounted(() => {
  void loadStats()
})
</script>

<template>
  <div class="page-head">
    <div>
      <p class="eyebrow">管理端 / 数据统计</p>
      <h2>数据统计</h2>
    </div>
    <div class="page-actions">
      <RouterLink class="secondary-link" to="/admin/reviews">企业审核</RouterLink>
      <RouterLink class="secondary-link" to="/admin/logs">日志管理</RouterLink>
      <button type="button" class="primary-button" @click="loadStats">刷新统计</button>
    </div>
  </div>

  <p v-if="loadError" class="status-error" role="alert">{{ loadError }}</p>
  <p v-else-if="isLoading && !dashboard" class="status-muted">正在汇总平台数据...</p>

  <template v-if="dashboard && !isLoading">
    <section class="metric-strip">
      <div class="metric-cell">
        <span>平台用户总量</span>
        <strong>{{ userTotal }}</strong>
        <p>求职者 {{ dashboard.jobseekerCount }} / 企业 {{ dashboard.enterpriseCount }}</p>
      </div>
      <div class="metric-cell">
        <span>已发布岗位</span>
        <strong>{{ dashboard.publishedJobCount }}</strong>
        <p>当前对外可见岗位总数</p>
      </div>
      <div class="metric-cell">
        <span>累计投递</span>
        <strong>{{ dashboard.applicationCount }}</strong>
        <p>录用转化提示：{{ hireRateHint }}</p>
      </div>
      <div class="metric-cell">
        <span>已入职</span>
        <strong>{{ dashboard.hiredCount }}</strong>
        <p>完成入职的人数</p>
      </div>
    </section>

    <section class="content-columns stats-columns">
      <article class="ledger-panel">
        <div class="panel-headline">
          <div>
            <p class="eyebrow">平台指标</p>
            <h3>核心业务数据</h3>
          </div>
        </div>

        <div class="bucket-list">
          <article v-for="metric in dashboard.metrics" :key="metric.label" class="bucket-card">
            <div class="row-head">
              <h4>{{ metric.label }}</h4>
              <span>{{ metric.value }}</span>
            </div>
            <p>{{ metric.hint }}</p>
          </article>
        </div>
      </article>

      <article class="ledger-panel">
        <div class="panel-headline">
          <div>
            <p class="eyebrow">治理指标</p>
            <h3>审核、日志、风险与工单</h3>
          </div>
        </div>

        <div class="detail-block">
          <ul class="detail-list">
            <li>待审企业：{{ pendingReviewCount }}</li>
            <li>审计日志：{{ auditLogCount }}</li>
            <li>风险记录总数：{{ riskCount }}</li>
            <li>活跃风险：{{ openRiskCount }}</li>
            <li>工单总数：{{ issueCount }}</li>
            <li>待处理工单：{{ pendingIssueCount }}</li>
            <li>活跃预警：{{ dashboard.openAlertCount }}</li>
          </ul>
        </div>

        <div class="action-grid">
          <RouterLink class="route-item" to="/admin/reviews">
            <strong>进入审核页</strong>
            <span>处理企业审核积压</span>
          </RouterLink>
          <RouterLink class="route-item" to="/admin/logs">
            <strong>进入日志页</strong>
            <span>追溯关键动作留痕</span>
          </RouterLink>
          <RouterLink class="route-item" to="/admin/risk-records">
            <strong>进入风险页</strong>
            <span>继续处理风险记录</span>
          </RouterLink>
          <RouterLink class="route-item" to="/admin/issues">
            <strong>进入工单页</strong>
            <span>继续处理申诉与纠错</span>
          </RouterLink>
        </div>
      </article>
    </section>
  </template>
</template>

<style scoped>
.stats-columns,
.bucket-list,
.action-grid {
  display: grid;
  gap: 16px;
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

.action-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
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
  .action-grid {
    grid-template-columns: 1fr;
  }
}
</style>

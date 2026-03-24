<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'

import {
  getAdminDashboard,
  type AdminDashboard,
  type AdminMetric,
} from '../api/admin'
import { ApiError } from '../lib/http'
import { useAuthStore } from '../stores/auth'

const authStore = useAuthStore()

const dashboard = ref<AdminDashboard | null>(null)
const isLoading = ref(false)
const loadError = ref('')

const metrics = computed<AdminMetric[]>(() => {
  if (dashboard.value?.metrics.length) {
    return dashboard.value.metrics
  }

  if (!dashboard.value) {
    return []
  }

  return [
    {
      label: '待审企业',
      value: String(dashboard.value.reviewQueue.length),
      hint: '等待进入企业审核页处理',
    },
    {
      label: '已发布岗位',
      value: String(dashboard.value.publishedJobCount),
      hint: '当前平台对外可见岗位数',
    },
    {
      label: '活跃预警',
      value: String(dashboard.value.openAlertCount),
      hint: '风险记录页中待处理的预警数',
    },
    {
      label: '已入职',
      value: String(dashboard.value.hiredCount),
      hint: '累计完成入职的人数',
    },
  ]
})

const pendingReviewCount = computed(() => dashboard.value?.reviewQueue.length ?? 0)
const recentAuditLogs = computed(() => dashboard.value?.auditLogs.slice(0, 4) ?? [])

async function loadDashboard() {
  if (!authStore.token) {
    return
  }

  isLoading.value = true
  loadError.value = ''

  try {
    dashboard.value = await getAdminDashboard(authStore.token)
  } catch (error) {
    loadError.value = error instanceof ApiError ? error.message : '管理首页加载失败'
  } finally {
    isLoading.value = false
  }
}

onMounted(() => {
  void loadDashboard()
})
</script>

<template>
  <div class="page-head">
    <div>
      <p class="eyebrow">管理端 / 首页</p>
      <h2>管理后台</h2>
    </div>
    <div class="page-actions">
      <button type="button" class="primary-button" @click="loadDashboard">刷新摘要</button>
    </div>
  </div>

  <p v-if="loadError" class="status-error" role="alert">{{ loadError }}</p>
  <p v-else-if="isLoading && !dashboard" class="status-muted">正在加载管理摘要...</p>

  <section v-if="metrics.length" class="metric-strip">
    <div v-for="metric in metrics" :key="metric.label" class="metric-cell">
      <span>{{ metric.label }}</span>
      <strong>{{ metric.value }}</strong>
      <p>{{ metric.hint }}</p>
    </div>
  </section>

  <section class="content-columns admin-home-columns">
    <article class="ledger-panel">
      <div class="panel-headline">
        <div>
          <p class="eyebrow">快捷入口</p>
          <h3>进入对应管理模块</h3>
        </div>
      </div>

      <div class="route-grid">
        <RouterLink class="route-item" to="/admin/reviews">
          <strong>企业审核</strong>
          <span>处理企业认证资料与审核决定</span>
        </RouterLink>
        <RouterLink class="route-item" to="/admin/users">
          <strong>用户管理</strong>
          <span>查看平台账号、角色与联系信息</span>
        </RouterLink>
        <RouterLink class="route-item" to="/admin/matching-engine">
          <strong>匹配引擎</strong>
          <span>调整推荐权重、风险惩罚和候选人阶段阈值</span>
        </RouterLink>
        <RouterLink class="route-item" to="/admin/tags">
          <strong>标签字典</strong>
          <span>维护平台标签与字典项</span>
        </RouterLink>
        <RouterLink class="route-item" to="/admin/knowledge">
          <strong>内容管理</strong>
          <span>维护知识库与政策内容</span>
        </RouterLink>
        <RouterLink class="route-item" to="/admin/logs">
          <strong>日志管理</strong>
          <span>查看审计日志和关键动作留痕</span>
        </RouterLink>
        <RouterLink class="route-item" to="/admin/stats">
          <strong>数据统计</strong>
          <span>查看平台核心指标与管理概况</span>
        </RouterLink>
      </div>
    </article>

    <article class="ledger-panel summary-panel">
      <div class="panel-headline">
        <div>
          <p class="eyebrow">当前重点</p>
          <h3>待处理事项</h3>
        </div>
      </div>

      <div class="detail-block">
        <p class="eyebrow">审核摘要</p>
        <ul class="detail-list">
          <li>待审企业：{{ pendingReviewCount }}</li>
          <li>企业数：{{ dashboard?.enterpriseCount ?? 0 }}</li>
          <li>求职者数：{{ dashboard?.jobseekerCount ?? 0 }}</li>
        </ul>
      </div>

      <div class="detail-block">
        <p class="eyebrow">最近日志</p>
        <ul v-if="recentAuditLogs.length" class="detail-list">
          <li v-for="log in recentAuditLogs" :key="log">{{ log }}</li>
        </ul>
        <p v-else class="status-muted">当前还没有日志摘要。</p>
      </div>

      <div class="action-row">
        <RouterLink class="secondary-link" to="/admin/reviews">进入审核页</RouterLink>
        <RouterLink class="secondary-link" to="/admin/matching-engine">进入引擎页</RouterLink>
        <RouterLink class="secondary-link" to="/admin/logs">进入日志页</RouterLink>
        <RouterLink class="secondary-link" to="/admin/stats">进入数据页</RouterLink>
      </div>
    </article>
  </section>
</template>

<style scoped>
.admin-home-columns,
.route-grid,
.summary-panel {
  display: grid;
  gap: 16px;
}

.route-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.action-row {
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
  .route-grid {
    grid-template-columns: 1fr;
  }
}
</style>

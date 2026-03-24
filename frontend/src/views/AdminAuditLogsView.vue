<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'

import {
  getAdminDashboard,
  getAuditLogs,
} from '../api/admin'
import { ApiError } from '../lib/http'
import { useAuthStore } from '../stores/auth'

const authStore = useAuthStore()

const auditLogs = ref<string[]>([])
const keyword = ref('')
const isLoading = ref(false)
const loadError = ref('')
const pendingReviewCount = ref(0)
const openAlertCount = ref(0)

const visibleLogs = computed(() => {
  const text = keyword.value.trim().toLowerCase()
  if (!text) {
    return auditLogs.value
  }
  return auditLogs.value.filter((item) => item.toLowerCase().includes(text))
})

async function loadLogs() {
  if (!authStore.token) {
    return
  }

  isLoading.value = true
  loadError.value = ''

  try {
    const [logs, dashboard] = await Promise.all([
      getAuditLogs(authStore.token),
      getAdminDashboard(authStore.token),
    ])
    auditLogs.value = logs
    pendingReviewCount.value = dashboard.reviewQueue.length
    openAlertCount.value = dashboard.openAlertCount
  } catch (error) {
    loadError.value = error instanceof ApiError ? error.message : '日志管理页加载失败'
  } finally {
    isLoading.value = false
  }
}

onMounted(() => {
  void loadLogs()
})
</script>

<template>
  <div class="page-head">
    <div>
      <p class="eyebrow">管理端 / 日志管理</p>
      <h2>日志管理</h2>
    </div>
    <div class="page-actions">
      <RouterLink class="secondary-link" to="/admin/reviews">企业审核</RouterLink>
      <RouterLink class="secondary-link" to="/admin/risk-records">风险记录</RouterLink>
      <button type="button" class="primary-button" @click="loadLogs">刷新日志</button>
    </div>
  </div>

  <p v-if="loadError" class="status-error" role="alert">{{ loadError }}</p>

  <section class="metric-strip">
    <div class="metric-cell">
      <span>日志总数</span>
      <strong>{{ auditLogs.length }}</strong>
      <p>当前审计日志接口返回的全部记录</p>
    </div>
    <div class="metric-cell">
      <span>待审企业</span>
      <strong>{{ pendingReviewCount }}</strong>
      <p>可跳到企业审核页继续处理</p>
    </div>
    <div class="metric-cell">
      <span>活跃预警</span>
      <strong>{{ openAlertCount }}</strong>
      <p>可前往风险记录页查看待处理项</p>
    </div>
    <div class="metric-cell">
      <span>筛选结果</span>
      <strong>{{ visibleLogs.length }}</strong>
      <p>当前关键词命中的日志数</p>
    </div>
  </section>

  <article class="ledger-panel log-panel">
    <div class="panel-headline">
      <div>
        <p class="eyebrow">审计日志</p>
        <h3>关键动作留痕</h3>
      </div>
    </div>

    <div class="filter-strip">
      <label class="inline-filter">
        <span>搜索</span>
        <input v-model="keyword" class="inline-input" type="text" placeholder="按企业、用户、动作关键词筛选" />
      </label>
      <RouterLink class="secondary-link" to="/admin/issues">查看申诉纠错</RouterLink>
    </div>

    <p v-if="isLoading" class="status-muted">正在加载日志...</p>
    <ol v-else-if="visibleLogs.length" class="log-list">
      <li v-for="log in visibleLogs" :key="log" class="log-item">{{ log }}</li>
    </ol>
    <p v-else class="status-muted">当前没有匹配的日志记录。</p>
  </article>
</template>

<style scoped>
.log-panel,
.log-list {
  display: grid;
  gap: 16px;
}

.inline-filter {
  display: inline-flex;
  gap: 10px;
  align-items: center;
}

.inline-input {
  min-height: 40px;
  padding: 0 12px;
  border: 1px solid var(--line-strong);
  background: #fff;
}

.log-list {
  margin: 0;
  padding-left: 20px;
}

.log-item {
  padding: 12px 0;
  border-bottom: 1px solid var(--line);
  line-height: 1.8;
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

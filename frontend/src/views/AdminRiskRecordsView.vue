<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'

import {
  getAdminRiskRecords,
  updateAdminRiskRecordStatus,
} from '../api/adminRisks'
import type { ServiceAlert } from '../api/service'
import { ApiError } from '../lib/http'
import { formatDateTime } from '../lib/jobseeker'
import { useAuthStore } from '../stores/auth'

type RiskStatus = '' | 'OPEN' | 'ESCALATED' | 'RESOLVED' | 'CLOSED'
type RiskLevel = '' | '1' | '2' | '3'

interface RiskActionForm {
  resolutionNote: string
  operatorName: string
}

const authStore = useAuthStore()

const alerts = ref<ServiceAlert[]>([])
const total = ref(0)
const isLoading = ref(false)
const activeAlertId = ref('')
const loadError = ref('')
const actionError = ref('')
const actionSuccess = ref('')
const statusFilter = ref<RiskStatus>('')
const levelFilter = ref<RiskLevel>('')
const actionForms = reactive<Record<string, RiskActionForm>>({})

const openCount = computed(() =>
  alerts.value.filter((item) => item.alertStatus === 'OPEN' || item.alertStatus === 'ESCALATED').length,
)
const highRiskCount = computed(() => alerts.value.filter((item) => item.alertLevel >= 3).length)
const handledCount = computed(() =>
  alerts.value.filter((item) => item.alertStatus === 'RESOLVED' || item.alertStatus === 'CLOSED').length,
)

function getDefaultOperatorName() {
  return authStore.nickname || authStore.account || 'admin'
}

function ensureActionForm(alertId: string) {
  if (!actionForms[alertId]) {
    actionForms[alertId] = {
      resolutionNote: '',
      operatorName: getDefaultOperatorName(),
    }
  }
  return actionForms[alertId]
}

function patchActionForms(nextAlerts: ServiceAlert[]) {
  nextAlerts.forEach((alert) => {
    const form = ensureActionForm(alert.alertId)
    if (!form.operatorName) {
      form.operatorName = getDefaultOperatorName()
    }
  })
}

function getAlertLevelLabel(level: number) {
  if (level >= 3) {
    return '高'
  }
  if (level === 2) {
    return '中'
  }
  return '低'
}

function getAlertStatusLabel(status: string) {
  switch (status) {
    case 'OPEN':
      return '待处理'
    case 'ESCALATED':
      return '已升级'
    case 'RESOLVED':
      return '已处理'
    case 'CLOSED':
      return '已关闭'
    default:
      return status
  }
}

function isActionable(status: string) {
  return status === 'OPEN' || status === 'ESCALATED'
}

async function loadRiskRecords() {
  if (!authStore.token) {
    return
  }

  isLoading.value = true
  loadError.value = ''

  try {
    const response = await getAdminRiskRecords(authStore.token, {
      page: 1,
      pageSize: 100,
      status: statusFilter.value || undefined,
      level: levelFilter.value ? Number(levelFilter.value) : undefined,
    })
    alerts.value = response.list
    total.value = response.total
    patchActionForms(response.list)
  } catch (error) {
    loadError.value = error instanceof ApiError ? error.message : '风险记录加载失败'
  } finally {
    isLoading.value = false
  }
}

async function submitAlertStatus(alert: ServiceAlert, targetStatus: 'RESOLVED' | 'ESCALATED' | 'CLOSED') {
  if (!authStore.token) {
    return
  }

  const form = ensureActionForm(alert.alertId)
  activeAlertId.value = alert.alertId
  actionError.value = ''
  actionSuccess.value = ''

  try {
    const updated = await updateAdminRiskRecordStatus(authStore.token, alert.alertId, {
      targetStatus,
      resolutionNote: form.resolutionNote.trim(),
      operatorName: form.operatorName.trim() || getDefaultOperatorName(),
    })
    alerts.value = alerts.value.map((item) => (item.alertId === updated.alertId ? updated : item))
    actionSuccess.value =
      targetStatus === 'ESCALATED' ? '风险记录已升级。' : targetStatus === 'CLOSED' ? '风险记录已关闭。' : '风险记录已标记为已处理。'
  } catch (error) {
    actionError.value = error instanceof ApiError ? error.message : '风险记录更新失败'
  } finally {
    activeAlertId.value = ''
  }
}

onMounted(() => {
  loadRiskRecords()
})
</script>

<template>
  <div class="page-head">
    <div>
      <p class="eyebrow">管理端 / 风险记录</p>
      <h2>风险记录管理</h2>
    </div>
    <div class="page-actions">
      <button type="button" class="primary-button" @click="loadRiskRecords">刷新风险记录</button>
    </div>
  </div>

  <section class="metric-strip">
    <div class="metric-cell">
      <span>记录总数</span>
      <strong>{{ total }}</strong>
    </div>
    <div class="metric-cell">
      <span>待处理</span>
      <strong>{{ openCount }}</strong>
    </div>
    <div class="metric-cell">
      <span>高风险</span>
      <strong>{{ highRiskCount }}</strong>
    </div>
    <div class="metric-cell">
      <span>已处理</span>
      <strong>{{ handledCount }}</strong>
    </div>
  </section>

  <article class="ledger-panel">
    <div class="panel-headline">
      <div>
        <p class="eyebrow">风险处理队列</p>
        <h3>按状态和等级筛选</h3>
      </div>
    </div>

    <p v-if="actionSuccess" class="success-banner" aria-live="polite">{{ actionSuccess }}</p>
    <p v-if="actionError" class="status-error" role="alert">{{ actionError }}</p>
    <p v-if="loadError" class="status-error" role="alert">{{ loadError }}</p>

    <div class="filter-row">
      <label class="inline-filter">
        <span>状态</span>
        <select v-model="statusFilter" class="inline-select" @change="loadRiskRecords">
          <option value="">全部</option>
          <option value="OPEN">待处理</option>
          <option value="ESCALATED">已升级</option>
          <option value="RESOLVED">已处理</option>
          <option value="CLOSED">已关闭</option>
        </select>
      </label>
      <label class="inline-filter">
        <span>等级</span>
        <select v-model="levelFilter" class="inline-select" @change="loadRiskRecords">
          <option value="">全部</option>
          <option value="3">高风险</option>
          <option value="2">中风险</option>
          <option value="1">低风险</option>
        </select>
      </label>
    </div>

    <div v-if="alerts.length" class="risk-list">
      <article
        v-for="alert in alerts"
        :key="alert.alertId"
        class="risk-card"
        :class="{ 'is-urgent': alert.alertLevel >= 3 }"
      >
        <div class="row-head">
          <div>
            <p class="eyebrow">#{{ alert.alertId }}</p>
            <h4>{{ alert.name }}</h4>
          </div>
          <span class="status-chip">{{ getAlertStatusLabel(alert.alertStatus) }}</span>
        </div>

        <p class="detail-copy">{{ alert.triggerReason }}</p>

        <div class="inline-tags">
          <span>{{ alert.alertType }}</span>
          <span>{{ getAlertLevelLabel(alert.alertLevel) }}风险</span>
          <span>{{ formatDateTime(alert.createdAt) }}</span>
          <span v-if="alert.caseId">个案 {{ alert.caseId }}</span>
        </div>

        <p v-if="alert.resolutionNote" class="status-muted">处理说明：{{ alert.resolutionNote }}</p>
        <p v-if="alert.handledAt" class="status-muted">
          处理人：{{ alert.handledBy || '未记录' }} / {{ formatDateTime(alert.handledAt) }}
        </p>

        <div v-if="isActionable(alert.alertStatus)" class="action-shell">
          <label class="field-block field-span">
            <span>处理说明</span>
            <textarea
              v-model="actionForms[alert.alertId].resolutionNote"
              class="field-input field-textarea"
              placeholder="填写已协调资源、升级原因或关闭依据"
            />
          </label>
          <label class="field-block">
            <span>处理人</span>
            <input v-model="actionForms[alert.alertId].operatorName" class="field-input" type="text" />
          </label>
          <div class="action-row">
            <button
              type="button"
              class="primary-button"
              :disabled="activeAlertId === alert.alertId"
              @click="submitAlertStatus(alert, 'RESOLVED')"
            >
              {{ activeAlertId === alert.alertId ? '处理中...' : '标记已处理' }}
            </button>
            <button
              type="button"
              class="secondary-link"
              :disabled="activeAlertId === alert.alertId"
              @click="submitAlertStatus(alert, 'ESCALATED')"
            >
              升级处理
            </button>
            <button
              type="button"
              class="secondary-link"
              :disabled="activeAlertId === alert.alertId"
              @click="submitAlertStatus(alert, 'CLOSED')"
            >
              关闭记录
            </button>
          </div>
        </div>
      </article>
    </div>
    <p v-else-if="isLoading" class="status-muted">正在同步风险记录...</p>
    <p v-else class="status-muted">当前筛选条件下没有风险记录。</p>
  </article>
</template>

<style scoped>
.filter-row,
.risk-list {
  display: grid;
}

.filter-row {
  grid-template-columns: repeat(2, minmax(0, max-content));
  gap: 12px;
  margin-bottom: 16px;
}

.inline-filter {
  display: inline-flex;
  align-items: center;
  gap: 10px;
}

.inline-select,
.field-input {
  width: 100%;
  min-height: 44px;
  padding: 0 14px;
  border: 1px solid var(--line-strong);
  background: #ffffff;
}

.risk-list {
  gap: 12px;
}

.risk-card {
  display: grid;
  gap: 14px;
  padding: 18px;
  border: 1px solid var(--line);
  background: rgba(252, 252, 249, 0.92);
}

.risk-card.is-urgent {
  border-color: rgba(180, 35, 24, 0.32);
  background: rgba(180, 35, 24, 0.04);
}

.action-shell {
  display: grid;
  gap: 12px;
  padding-top: 14px;
  border-top: 1px solid var(--line);
}

.field-block {
  display: grid;
  gap: 8px;
}

.field-span {
  grid-column: 1 / -1;
}

.field-textarea {
  min-height: 112px;
  padding: 14px;
  resize: vertical;
}

.detail-copy {
  margin: 0;
  line-height: 1.8;
  white-space: pre-line;
}

.action-row {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.status-chip {
  display: inline-flex;
  align-items: center;
  padding: 4px 10px;
  border: 1px solid var(--line-strong);
  background: rgba(47, 111, 237, 0.08);
  color: var(--brand);
}

.status-error,
.status-muted,
.success-banner {
  margin: 0;
}

.status-error {
  color: var(--error);
  font-weight: 700;
}

.status-muted {
  color: var(--muted);
}

@media (max-width: 900px) {
  .filter-row {
    grid-template-columns: 1fr;
  }
}
</style>

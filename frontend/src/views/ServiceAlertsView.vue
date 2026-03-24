<script setup lang="ts">
import { computed, onMounted, reactive } from 'vue'
import { RouterLink } from 'vue-router'

import { formatDateTime } from '../lib/jobseeker'
import { useServiceWorkbench } from '../composables/useServiceWorkbench'

const workbench = reactive(useServiceWorkbench())

const resolvedAlertCount = computed(() => workbench.resolvedAlerts.length)

onMounted(() => {
  void workbench.initializeWorkbench()
})
</script>

<template>
  <div class="page-head">
    <div>
      <p class="eyebrow">服务机构 / 预警处理</p>
      <h2>预警处理</h2>
    </div>
    <div class="page-actions">
      <button type="button" class="primary-button" @click="workbench.refreshWorkbench">刷新预警</button>
    </div>
  </div>

  <section class="metric-strip">
    <div class="metric-cell">
      <span>全部预警</span>
      <strong>{{ workbench.alerts.length }}</strong>
    </div>
    <div class="metric-cell">
      <span>待处理预警</span>
      <strong>{{ workbench.pendingAlerts.length }}</strong>
    </div>
    <div class="metric-cell">
      <span>高风险预警</span>
      <strong>{{ workbench.highRiskAlerts.length }}</strong>
    </div>
    <div class="metric-cell">
      <span>已处理预警</span>
      <strong>{{ resolvedAlertCount }}</strong>
    </div>
  </section>

  <section class="content-columns">
    <article class="hero-panel">
      <div class="panel-headline">
        <p class="eyebrow">最高优先级</p>
        <h3>{{ workbench.topAlert ? workbench.topAlert.name : '当前没有需要处理的预警' }}</h3>
      </div>

      <template v-if="workbench.topAlert">
        <p class="body-copy">{{ workbench.topAlert.triggerReason }}</p>
        <div class="inline-tags detail-tags">
          <span>{{ workbench.topAlert.alertType }}</span>
          <span>{{ workbench.getAlertLevelLabel(workbench.topAlert.alertLevel) }}风险</span>
          <span>{{ workbench.getAlertStatusLabel(workbench.topAlert.alertStatus) }}</span>
          <span>{{ formatDateTime(workbench.topAlert.createdAt) }}</span>
        </div>
        <div class="page-actions">
          <RouterLink
            v-if="workbench.topAlert.caseId"
            class="secondary-link"
            :to="`/service/cases/${workbench.topAlert.caseId}`"
          >
            查看对应个案
          </RouterLink>
        </div>
      </template>
      <p v-else class="status-muted">全部预警已完成处理或当前没有风险信号。</p>
    </article>

  </section>

  <article class="ledger-panel">
    <div class="panel-headline">
      <p class="eyebrow">预警队列</p>
      <h3>按状态和等级快速处理</h3>
    </div>

    <p v-if="workbench.successMessage" class="success-banner" aria-live="polite">
      {{ workbench.successMessage }}
    </p>
    <p v-if="workbench.actionError" class="status-error" role="alert">{{ workbench.actionError }}</p>
    <p v-if="workbench.loadError" class="status-error" role="alert">{{ workbench.loadError }}</p>

    <div class="alert-filter-row">
      <label class="inline-filter">
        <span>状态</span>
        <select v-model="workbench.alertStatusFilter" class="inline-select">
          <option v-for="option in workbench.alertStatusOptions" :key="option.value" :value="option.value">
            {{ option.label }}
          </option>
        </select>
      </label>
      <label class="inline-filter">
        <span>等级</span>
        <select v-model="workbench.alertLevelFilter" class="inline-select">
          <option v-for="option in workbench.alertLevelOptions" :key="option.value" :value="option.value">
            {{ option.label }}
          </option>
        </select>
      </label>
    </div>

    <div v-if="workbench.filteredAlerts.length" class="alert-feed">
      <article
        v-for="alert in workbench.filteredAlerts"
        :key="alert.alertId"
        class="alert-card"
        :class="{ 'is-urgent': alert.alertLevel >= 3 }"
      >
        <div class="row-head">
          <h4>{{ alert.name }}</h4>
          <span>{{ workbench.getAlertStatusLabel(alert.alertStatus) }}</span>
        </div>
        <p class="detail-copy">{{ alert.triggerReason }}</p>
        <div class="inline-tags">
          <span>{{ alert.alertType }}</span>
          <span>{{ workbench.getAlertLevelLabel(alert.alertLevel) }}风险</span>
          <span>{{ formatDateTime(alert.createdAt) }}</span>
          <span v-if="alert.caseId">{{ alert.caseId }}</span>
        </div>
        <p v-if="alert.resolutionNote" class="status-muted">处理说明：{{ alert.resolutionNote }}</p>
        <p v-if="alert.handledAt" class="status-muted">
          处理人：{{ alert.handledBy || '未记录' }} / {{ formatDateTime(alert.handledAt) }}
        </p>

        <div class="action-row compact">
          <RouterLink
            v-if="alert.caseId"
            class="secondary-link"
            :to="`/service/cases/${alert.caseId}`"
          >
            查看个案
          </RouterLink>
        </div>

        <div v-if="workbench.isActionableAlert(alert.alertStatus)" class="alert-action-shell">
          <label class="field-block field-block-wide">
            <span>处理说明</span>
            <textarea
              v-model="workbench.alertActionForms[alert.alertId].resolutionNote"
              class="field-input field-textarea"
              placeholder="填写已协调资源、处理结果，或升级给上级/企业的原因"
            />
          </label>
          <label class="field-block">
            <span>处理人</span>
            <input v-model="workbench.alertActionForms[alert.alertId].operatorName" class="field-input" type="text" />
          </label>
          <div class="action-row compact">
            <button
              type="button"
              class="primary-button"
              :disabled="workbench.activeAlertId === alert.alertId"
              @click="workbench.submitAlertStatus(alert, 'RESOLVED')"
            >
              {{ workbench.activeAlertId === alert.alertId ? '处理中...' : '标记已处理' }}
            </button>
            <button
              type="button"
              class="secondary-link"
              :disabled="workbench.activeAlertId === alert.alertId"
              @click="workbench.submitAlertStatus(alert, 'ESCALATED')"
            >
              升级处理
            </button>
            <button
              type="button"
              class="secondary-link"
              :disabled="workbench.activeAlertId === alert.alertId"
              @click="workbench.submitAlertStatus(alert, 'CLOSED')"
            >
              关闭预警
            </button>
          </div>
        </div>
      </article>
    </div>
    <p v-else-if="workbench.isLoading" class="status-muted">正在同步预警队列...</p>
    <p v-else class="status-muted">当前筛选下没有需要展示的预警。</p>
  </article>
</template>

<style scoped>
.detail-tags {
  margin-bottom: 18px;
}

.note-stack {
  display: grid;
  gap: 12px;
}

.note-card {
  padding: 16px;
  border: 1px solid var(--line);
  background: rgba(252, 252, 249, 0.92);
}

.note-card span {
  display: block;
  margin-bottom: 8px;
  font-family: var(--mono);
  font-size: 0.78rem;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--muted);
}

.note-card strong {
  display: block;
  margin-bottom: 8px;
  font-family: var(--serif);
  font-size: 1.35rem;
  color: var(--heading);
}

.alert-filter-row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
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
  border-radius: 12px;
  background: #ffffff;
}

.field-textarea {
  min-height: 112px;
  padding: 14px;
  resize: vertical;
}

.alert-feed {
  display: grid;
  gap: 12px;
}

.alert-card {
  padding: 16px;
  border: 1px solid var(--line);
  background: rgba(252, 252, 249, 0.92);
}

.alert-card.is-urgent {
  border-color: rgba(180, 35, 24, 0.32);
  background: rgba(180, 35, 24, 0.04);
}

.alert-action-shell {
  display: grid;
  gap: 12px;
  margin-top: 14px;
  padding-top: 14px;
  border-top: 1px solid var(--line);
}

.detail-copy {
  margin: 10px 0 0;
  line-height: 1.8;
  white-space: pre-line;
}

.field-block {
  display: grid;
  gap: 8px;
}

.field-block-wide {
  grid-column: 1 / -1;
}

.action-row {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.action-row.compact {
  margin-top: 0;
}

.success-banner,
.status-error,
.status-muted {
  margin: 0;
}

.success-banner {
  padding: 12px 14px;
}

.status-error {
  color: var(--error);
  font-weight: 700;
}

.status-muted {
  color: var(--muted);
}
</style>

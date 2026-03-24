<script setup lang="ts">
import { computed, onMounted, reactive } from 'vue'
import { RouterLink } from 'vue-router'

import { formatDateTime } from '../lib/jobseeker'
import { useServiceWorkbench } from '../composables/useServiceWorkbench'

const workbench = reactive(useServiceWorkbench())

const focusCases = computed(() => workbench.serviceCases.slice(0, 4))
const focusAlerts = computed(() => workbench.pendingAlerts.slice(0, 4))
const selectedCaseBasePath = computed(() =>
  workbench.selectedCase ? `/service/cases/${workbench.selectedCase.id}` : '/service/cases',
)
const selectedInterventionPath = computed(() =>
  workbench.selectedCase ? `/service/cases/${workbench.selectedCase.id}/interventions` : '/service/cases',
)
const selectedFollowupPath = computed(() =>
  workbench.selectedCase ? `/service/cases/${workbench.selectedCase.id}/followups` : '/service/cases',
)
const selectedReferralPath = computed(() =>
  workbench.selectedCase ? `/service/cases/${workbench.selectedCase.id}/referrals` : '/service/cases',
)

onMounted(() => {
  void workbench.initializeWorkbench()
})
</script>

<template>
  <div class="page-head">
    <div>
      <p class="eyebrow">服务机构 / 首页</p>
      <h2>服务首页</h2>
    </div>
    <div class="page-actions">
      <RouterLink class="secondary-link" to="/service/cases">服务对象列表</RouterLink>
      <button type="button" class="primary-button" @click="workbench.refreshWorkbench">刷新首页</button>
    </div>
  </div>

  <p v-if="workbench.loadError" class="status-error" role="alert">{{ workbench.loadError }}</p>
  <p
    v-else-if="workbench.isLoading && !workbench.serviceCases.length && !workbench.alerts.length"
    class="status-muted"
  >
    正在同步首页数据...
  </p>

  <section class="metric-strip">
    <div class="metric-cell">
      <span>活跃个案</span>
      <strong>{{ workbench.serviceCases.length }}</strong>
      <p>当前正在跟进的服务对象数量。</p>
    </div>
    <div class="metric-cell">
      <span>待处理预警</span>
      <strong>{{ workbench.pendingAlerts.length }}</strong>
      <p>需要优先处理的风险预警。</p>
    </div>
    <div class="metric-cell">
      <span>高风险预警</span>
      <strong>{{ workbench.highRiskAlerts.length }}</strong>
      <p>可前往预警页查看并处理风险提醒。</p>
    </div>
    <div class="metric-cell">
      <span>累计回访</span>
      <strong>{{ workbench.totalFollowupCount }}</strong>
      <p>所有个案累计回访总数。</p>
    </div>
  </section>

  <section class="content-columns">
    <article class="hero-panel">
      <div class="panel-headline">
        <p class="eyebrow">今日焦点</p>
        <h3>{{ workbench.selectedCase ? `${workbench.selectedCase.name} 的下一步` : '从列表中查看今天重点跟进的个案' }}</h3>
      </div>

      <template v-if="workbench.selectedCase">
        <p class="body-copy">
          {{ workbench.selectedCase.stage }} / 负责人：{{ workbench.selectedCase.owner }} / 下一步：{{
            workbench.selectedCase.nextAction
          }}
        </p>
        <div class="inline-tags overview-tags">
          <span>当前风险：{{ workbench.getCaseAlertLevelLabel(workbench.selectedCase.alertLevel) }}</span>
          <span>待处理预警：{{ workbench.selectedPendingAlertCount }}</span>
          <span>时间线：{{ workbench.selectedTimelineCount }}</span>
        </div>
      </template>
      <p v-else class="status-muted">当前没有可聚焦的服务对象。</p>

      <div class="route-list route-grid">
        <RouterLink class="route-item" to="/service/cases">
          <span>对象列表</span>
          <strong>查看服务对象列表</strong>
          <p class="body-copy">先查看服务对象列表，再进入对应页面继续跟进。</p>
        </RouterLink>
        <RouterLink class="route-item" :to="selectedCaseBasePath">
          <span>个案详情</span>
          <strong>查看建档与授权</strong>
          <p class="body-copy">集中查看个案基础信息、授权状态和时间线。</p>
        </RouterLink>
        <RouterLink class="route-item" :to="selectedInterventionPath">
          <span>干预记录</span>
          <strong>写入支持动作</strong>
          <p class="body-copy">记录辅导、沟通、入职支持等动作。</p>
        </RouterLink>
        <RouterLink class="route-item" to="/service/alerts">
          <span>预警处理</span>
          <strong>查看风险预警</strong>
          <p class="body-copy">按状态和等级查看风险信号，并及时跟进。</p>
        </RouterLink>
        <RouterLink class="route-item" :to="selectedFollowupPath">
          <span>回访管理</span>
          <strong>维护 7 天与 30 天回访</strong>
          <p class="body-copy">登记 7 天和 30 天回访结果。</p>
        </RouterLink>
        <RouterLink class="route-item" :to="selectedReferralPath">
          <span>资源转介</span>
          <strong>安排转介与排期</strong>
          <p class="body-copy">管理转介计划、排期与进展。</p>
        </RouterLink>
      </div>

      <div v-if="workbench.topAlert" class="alert-banner">
        <strong>{{ workbench.getAlertLevelLabel(workbench.topAlert.alertLevel) }}</strong>
        <p>{{ workbench.topAlert.name }}：{{ workbench.topAlert.triggerReason }}</p>
      </div>
    </article>

  </section>

  <section class="content-columns">
    <article class="ledger-panel">
      <div class="panel-headline">
        <p class="eyebrow">重点对象</p>
        <h3>进入对应个案页面</h3>
      </div>

      <div v-if="focusCases.length" class="preview-stack">
        <article v-for="item in focusCases" :key="item.id" class="preview-card">
          <div class="row-head">
            <h4>{{ item.name }}</h4>
            <span>{{ item.id }}</span>
          </div>
          <p class="body-copy">{{ item.stage }} / 负责人：{{ item.owner }}</p>
          <p class="body-copy">{{ item.nextAction }}</p>
          <div class="inline-tags">
            <span>{{ item.pendingAlertCount }} 条预警</span>
            <span>{{ item.followupCount }} 次回访</span>
            <span>{{ workbench.getCaseAlertLevelLabel(item.alertLevel) }}风险</span>
          </div>
          <div class="detail-actions">
            <RouterLink class="secondary-link" :to="`/service/cases/${item.id}`">详情</RouterLink>
            <RouterLink class="secondary-link" :to="`/service/cases/${item.id}/interventions`">干预</RouterLink>
            <RouterLink class="secondary-link" :to="`/service/cases/${item.id}/followups`">回访</RouterLink>
          </div>
        </article>
      </div>
      <p v-else class="status-muted">当前没有服务对象数据。</p>
    </article>

    <article class="ledger-panel">
      <div class="panel-headline">
        <p class="eyebrow">待办预警</p>
        <h3>先处理最紧急的几条</h3>
      </div>

      <div v-if="focusAlerts.length" class="preview-stack">
        <RouterLink
          v-for="alert in focusAlerts"
          :key="alert.alertId"
          class="preview-card"
          to="/service/alerts"
        >
          <div class="row-head">
            <h4>{{ alert.name }}</h4>
            <span>{{ workbench.getAlertStatusLabel(alert.alertStatus) }}</span>
          </div>
          <p class="body-copy">{{ alert.triggerReason }}</p>
          <div class="inline-tags">
            <span>{{ alert.alertType }}</span>
            <span>{{ workbench.getAlertLevelLabel(alert.alertLevel) }}风险</span>
            <span>{{ formatDateTime(alert.createdAt) }}</span>
          </div>
        </RouterLink>
      </div>
      <p v-else class="status-muted">当前没有待办预警。</p>
    </article>
  </section>
</template>

<style scoped>
.route-grid,
.overview-note-grid,
.preview-stack {
  display: grid;
  gap: 14px;
}

.route-grid,
.overview-note-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.overview-note-card,
.preview-card {
  padding: 18px;
  border: 1px solid var(--line-strong);
  background: rgba(252, 252, 249, 0.88);
}

.overview-note-card span {
  display: block;
  margin-bottom: 10px;
  font-family: var(--mono);
  font-size: 0.78rem;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--muted);
}

.overview-note-card strong {
  display: block;
  margin-bottom: 8px;
  font-family: var(--serif);
  font-size: 1.45rem;
  color: var(--heading);
}

.overview-tags {
  margin-bottom: 18px;
}

.status-error {
  margin: 0 0 16px;
  color: var(--error);
  font-weight: 700;
}

.status-muted {
  color: var(--muted);
}

@media (max-width: 1180px) {
  .route-grid,
  .overview-note-grid {
    grid-template-columns: 1fr;
  }
}
</style>

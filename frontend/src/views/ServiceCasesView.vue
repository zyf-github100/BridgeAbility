<script setup lang="ts">
import { onMounted, reactive } from 'vue'
import { RouterLink } from 'vue-router'

import { useServiceWorkbench } from '../composables/useServiceWorkbench'

const workbench = reactive(useServiceWorkbench())

onMounted(() => {
  void workbench.initializeWorkbench()
})
</script>

<template>
  <div class="page-head">
    <div>
      <p class="eyebrow">服务机构 / 服务对象列表</p>
      <h2>服务对象列表</h2>
    </div>
    <div class="page-actions">
      <RouterLink class="secondary-link" to="/service">返回首页</RouterLink>
      <button
        type="submit"
        form="service-case-create-form"
        class="primary-button"
        :disabled="workbench.isCaseCreating"
      >
        {{ workbench.isCaseCreating ? '建档中...' : '新增服务对象' }}
      </button>
    </div>
  </div>

  <p v-if="workbench.successMessage" class="success-banner" aria-live="polite">{{ workbench.successMessage }}</p>
  <p v-if="workbench.actionError" class="status-error" role="alert">{{ workbench.actionError }}</p>
  <p v-if="workbench.loadError" class="status-error" role="alert">{{ workbench.loadError }}</p>

  <section class="metric-strip">
    <div class="metric-cell">
      <span>服务对象</span>
      <strong>{{ workbench.serviceCases.length }}</strong>
      <p>{{ workbench.pageNote }}</p>
    </div>
    <div class="metric-cell">
      <span>待处理预警</span>
      <strong>{{ workbench.pendingAlerts.length }}</strong>
      <p>高风险 {{ workbench.highRiskAlerts.length }} 条。</p>
    </div>
    <div class="metric-cell">
      <span>回访记录</span>
      <strong>{{ workbench.totalFollowupCount }}</strong>
      <p>所有个案累计回访记录。</p>
    </div>
    <div class="metric-cell">
      <span>资源转介</span>
      <strong>{{ workbench.totalReferralCount }}</strong>
      <p>服务对象累计转介记录。</p>
    </div>
  </section>

  <section class="workspace-grid service-grid">
    <article class="ledger-panel">
      <div class="panel-headline">
        <p class="eyebrow">新建对象</p>
        <h3>录入服务对象基础信息</h3>
      </div>

      <form id="service-case-create-form" class="form-grid compact-form" @submit.prevent="workbench.submitCreateCase">
        <label class="field-block">
          <span>服务对象姓名</span>
          <input v-model="workbench.caseCreateForm.name" class="field-input" type="text" placeholder="例如：陈同学" />
        </label>
        <label class="field-block">
          <span>当前阶段</span>
          <input v-model="workbench.caseCreateForm.stage" class="field-input" type="text" placeholder="例如：初次建档" />
        </label>
        <label class="field-block">
          <span>负责人</span>
          <input v-model="workbench.caseCreateForm.ownerName" class="field-input" type="text" />
        </label>
        <label class="field-block">
          <span>下一步动作</span>
          <input v-model="workbench.caseCreateForm.nextAction" class="field-input" type="text" placeholder="例如：一周内完成需求评估" />
        </label>
        <label class="field-block">
          <span>绑定求职者账号</span>
          <input
            v-model="workbench.caseCreateForm.jobseekerAccount"
            class="field-input"
            type="text"
            placeholder="可选，授权查档时推荐填写"
          />
        </label>
        <label class="field-block">
          <span>操作人</span>
          <input v-model="workbench.caseCreateForm.operatorName" class="field-input" type="text" />
        </label>
        <label class="field-block field-block-wide">
          <span>建档说明</span>
          <textarea
            v-model="workbench.caseCreateForm.intakeNote"
            class="field-input field-textarea"
            placeholder="记录来源、重点困难、初步服务计划。"
          />
        </label>
        <label class="field-block field-block-wide checkbox-block">
          <input v-model="workbench.caseCreateForm.profileAuthorized" type="checkbox" />
          <span>建档时同步开通求职者档案查看授权</span>
        </label>
        <label class="field-block field-block-wide">
          <span>授权说明</span>
          <textarea
            v-model="workbench.caseCreateForm.authorizationNote"
            class="field-input field-textarea"
            placeholder="例如：经本人同意，用于开展就业辅导和资源转介。"
          />
        </label>
        <div class="action-row">
          <button type="submit" class="primary-button" :disabled="workbench.isCaseCreating">
            {{ workbench.isCaseCreating ? '建档中...' : '建立服务对象档案' }}
          </button>
          <button type="button" class="toggle-button" @click="workbench.resetCaseCreateForm">重置表单</button>
        </div>
      </form>
    </article>

    <article class="ledger-panel">
      <div class="panel-headline">
        <p class="eyebrow">对象列表</p>
        <h3>查看服务对象并进入处理页面</h3>
      </div>

      <p v-if="workbench.isLoading" class="status-muted">正在同步服务对象列表...</p>
      <div v-else-if="workbench.serviceCases.length" class="case-ledger">
        <article v-for="item in workbench.serviceCases" :key="item.id" class="case-row service-case-card">
          <div>
            <strong>{{ item.name }}</strong>
            <p>{{ item.id }} / {{ item.stage }}</p>
            <div class="inline-tags">
              <span>{{ item.pendingAlertCount }} 条预警</span>
              <span>{{ item.followupCount }} 次回访</span>
              <span>{{ item.referralCount }} 项转介</span>
            </div>
          </div>
          <div>
            <span>负责人：{{ item.owner }}</span>
            <p>{{ item.nextAction }}</p>
          </div>
          <div class="case-flag">
            <span>风险</span>
            <strong>{{ workbench.getCaseAlertLevelLabel(item.alertLevel) }}</strong>
            <small>{{ item.profileAuthorized ? '已授权查档' : '未授权查档' }}</small>
          </div>
          <div class="card-actions">
            <RouterLink class="secondary-link" :to="`/service/cases/${item.id}`">个案详情</RouterLink>
            <RouterLink class="secondary-link" :to="`/service/cases/${item.id}/interventions`">干预记录</RouterLink>
            <RouterLink class="secondary-link" :to="`/service/cases/${item.id}/followups`">回访管理</RouterLink>
            <RouterLink class="secondary-link" :to="`/service/cases/${item.id}/referrals`">资源转介</RouterLink>
          </div>
        </article>
      </div>
      <p v-else class="status-muted">当前没有服务对象记录。</p>
    </article>
  </section>
</template>

<style scoped>
.service-grid {
  align-items: start;
}

.compact-form,
.card-actions {
  margin-top: 18px;
}

.service-case-card {
  padding: 18px 0;
}

.card-actions,
.form-grid {
  display: grid;
  gap: 14px;
}

.form-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.field-block {
  display: grid;
  gap: 8px;
}

.field-block-wide {
  grid-column: 1 / -1;
}

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

.checkbox-block {
  grid-template-columns: auto 1fr;
  align-items: center;
}

.card-actions {
  grid-column: 1 / -1;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  padding-top: 14px;
}

.action-row {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.status-error {
  margin: 0 0 16px;
  color: var(--error);
  font-weight: 700;
}

.status-muted {
  color: var(--muted);
}

@media (max-width: 1080px) {
  .form-grid,
  .card-actions {
    grid-template-columns: 1fr;
  }
}
</style>

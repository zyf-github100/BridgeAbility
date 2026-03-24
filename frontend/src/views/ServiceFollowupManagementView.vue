<script setup lang="ts">
import { RouterLink } from 'vue-router'

import { useServiceCasePage } from '../composables/useServiceCasePage'
import { formatDateTime } from '../lib/jobseeker'

const { workbench, caseBasePath, refreshCasePage } = useServiceCasePage()

async function submitFollowup() {
  await workbench.submitFollowup()
}
</script>

<template>
  <div class="page-head">
    <div>
      <p class="eyebrow">服务机构 / 回访管理</p>
      <h2>{{ workbench.selectedCase?.name || '回访管理' }}</h2>
    </div>
    <div class="page-actions">
      <RouterLink class="secondary-link" :to="caseBasePath">个案详情</RouterLink>
      <RouterLink class="secondary-link" to="/service/alerts">预警处理</RouterLink>
      <button
        type="submit"
        form="followup-form"
        class="primary-button"
        :disabled="workbench.isFollowupSaving || !workbench.selectedCaseDetail"
      >
        {{ workbench.isFollowupSaving ? '保存中...' : '保存回访记录' }}
      </button>
    </div>
  </div>

  <p v-if="workbench.successMessage" class="success-banner" aria-live="polite">{{ workbench.successMessage }}</p>
  <p v-if="workbench.actionError" class="status-error" role="alert">{{ workbench.actionError }}</p>
  <p v-if="workbench.detailError" class="status-error" role="alert">{{ workbench.detailError }}</p>

  <section class="metric-strip">
    <div class="metric-cell">
      <span>历史回访</span>
      <strong>{{ workbench.selectedCaseDetail?.followups.length ?? 0 }}</strong>
      <p>当前个案已记录的回访内容。</p>
    </div>
    <div class="metric-cell">
      <span>待处理预警</span>
      <strong>{{ workbench.selectedPendingAlertCount }}</strong>
      <p>高风险情况可前往预警处理。</p>
    </div>
    <div class="metric-cell">
      <span>当前阶段</span>
      <strong>{{ workbench.followupForm.followupStage === 'DAY_30' ? '30 天' : '7 天' }}</strong>
      <p>根据已有记录自动推荐下一次回访阶段。</p>
    </div>
    <div class="metric-cell">
      <span>资源转介</span>
      <strong>{{ workbench.selectedReferralCount }}</strong>
      <p>若回访暴露支持缺口，可直接转到资源转介页。</p>
    </div>
  </section>

  <section class="content-columns">
    <article class="ledger-panel">
      <div class="panel-headline">
        <p class="eyebrow">新增回访</p>
        <h3>登记 7 天或 30 天回访结果</h3>
      </div>

      <form id="followup-form" class="form-grid" @submit.prevent="submitFollowup">
        <label class="field-block">
          <span>回访阶段</span>
          <select v-model="workbench.followupForm.followupStage" class="field-input">
            <option v-for="option in workbench.followupStageOptions" :key="option.value" :value="option.value">
              {{ option.label }}
            </option>
          </select>
        </label>
        <label class="field-block">
          <span>记录人</span>
          <input v-model="workbench.followupForm.operatorName" class="field-input" type="text" />
        </label>
        <label class="field-block">
          <span>关联岗位 ID</span>
          <input v-model="workbench.followupForm.jobId" class="field-input" type="text" placeholder="可选，用于标记当前入职岗位" />
        </label>
        <label class="field-block">
          <span>适应评分</span>
          <input v-model.number="workbench.followupForm.adaptationScore" class="field-input" type="number" min="0" max="100" />
        </label>
        <label class="field-block">
          <span>支持是否落实</span>
          <select v-model="workbench.followupForm.supportImplemented" class="field-input">
            <option :value="true">已落实</option>
            <option :value="false">未落实</option>
          </select>
        </label>
        <label class="field-block">
          <span>是否存在离职风险</span>
          <select v-model="workbench.followupForm.leaveRisk" class="field-input">
            <option :value="false">无明显风险</option>
            <option :value="true">存在风险</option>
          </select>
        </label>
        <label class="field-block">
          <span>是否需要继续帮助</span>
          <select v-model="workbench.followupForm.needHelp" class="field-input">
            <option :value="false">暂不需要</option>
            <option :value="true">需要继续介入</option>
          </select>
        </label>
        <label class="field-block field-block-wide">
          <span>环境问题</span>
          <textarea
            v-model="workbench.followupForm.environmentIssue"
            class="field-input field-textarea"
            placeholder="例如：照明、通道、设备、工作位等问题。"
          />
        </label>
        <label class="field-block field-block-wide">
          <span>沟通问题</span>
          <textarea
            v-model="workbench.followupForm.communicationIssue"
            class="field-input field-textarea"
            placeholder="例如：会议语速过快、任务说明过于口头化。"
          />
        </label>
      </form>
    </article>

    <article class="ledger-panel">
      <div class="panel-headline">
        <p class="eyebrow">历史回访</p>
        <h3>按回访阶段记录反馈与风险</h3>
      </div>

      <div v-if="workbench.selectedCaseDetail?.followups.length" class="record-stack">
        <article v-for="item in workbench.selectedCaseDetail.followups" :key="item.id" class="record-card">
          <div class="row-head">
            <h4>{{ workbench.getFollowupStageLabel(item.followupStage) }}</h4>
            <span>{{ formatDateTime(item.completedAt || item.dueAt || item.createdAt) }}</span>
          </div>
          <div class="inline-tags">
            <span>适应 {{ item.adaptationScore }}</span>
            <span>支持落实：{{ workbench.getBooleanLabel(item.supportImplemented) }}</span>
            <span>离职风险：{{ workbench.getBooleanLabel(item.leaveRisk) }}</span>
            <span>需要帮助：{{ workbench.getBooleanLabel(item.needHelp) }}</span>
            <span>{{ item.recordStatus }}</span>
          </div>
          <p v-if="item.environmentIssue" class="detail-copy">环境问题：{{ item.environmentIssue }}</p>
          <p v-if="item.communicationIssue" class="detail-copy">沟通问题：{{ item.communicationIssue }}</p>
          <p class="status-muted">记录人：{{ item.operatorName }}</p>
        </article>
      </div>
      <p v-else class="status-muted">当前还没有回访记录。</p>

      <div class="detail-actions">
        <button type="button" class="secondary-link" @click="refreshCasePage">刷新回访</button>
        <RouterLink class="secondary-link" :to="`${caseBasePath}/referrals`">资源转介</RouterLink>
      </div>
    </article>
  </section>
</template>

<style scoped>
.record-stack,
.form-grid {
  display: grid;
  gap: 14px;
}

.form-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.record-card {
  display: grid;
  gap: 10px;
  padding: 16px;
  border: 1px solid var(--line);
  background: rgba(252, 252, 249, 0.92);
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

.detail-copy {
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
  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>

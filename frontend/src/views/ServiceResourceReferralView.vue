<script setup lang="ts">
import { RouterLink } from 'vue-router'

import { useServiceCasePage } from '../composables/useServiceCasePage'
import { formatDateTime } from '../lib/jobseeker'

const { workbench, caseBasePath, refreshCasePage } = useServiceCasePage()

const referralStatusActions = [
  { value: 'IN_PROGRESS', label: '开始跟进' },
  { value: 'CONNECTED', label: '已对接' },
  { value: 'COMPLETED', label: '已完成' },
  { value: 'CANCELLED', label: '取消转介' },
] as const

async function submitReferral() {
  await workbench.submitReferral()
}
</script>

<template>
  <div class="page-head">
    <div>
      <p class="eyebrow">服务机构 / 资源转介</p>
      <h2>{{ workbench.selectedCase?.name || '资源转介' }}</h2>
    </div>
    <div class="page-actions">
      <RouterLink class="secondary-link" :to="caseBasePath">个案详情</RouterLink>
      <RouterLink class="secondary-link" :to="`${caseBasePath}/followups`">回访管理</RouterLink>
      <button
        type="submit"
        form="referral-form"
        class="primary-button"
        :disabled="workbench.isReferralSaving || !workbench.selectedCaseDetail"
      >
        {{ workbench.isReferralSaving ? '保存中...' : '新增资源转介' }}
      </button>
    </div>
  </div>

  <p v-if="workbench.successMessage" class="success-banner" aria-live="polite">{{ workbench.successMessage }}</p>
  <p v-if="workbench.actionError" class="status-error" role="alert">{{ workbench.actionError }}</p>
  <p v-if="workbench.detailError" class="status-error" role="alert">{{ workbench.detailError }}</p>

  <section class="metric-strip">
    <div class="metric-cell">
      <span>转介总数</span>
      <strong>{{ workbench.selectedCaseDetail?.referrals.length ?? 0 }}</strong>
      <p>当前个案已有的资源转介记录。</p>
    </div>
    <div class="metric-cell">
      <span>进行中</span>
      <strong>{{ workbench.selectedCaseDetail?.referrals.filter((item) => item.referralStatus === 'IN_PROGRESS').length ?? 0 }}</strong>
      <p>已经进入跟进或对接阶段的转介。</p>
    </div>
    <div class="metric-cell">
      <span>待处理预警</span>
      <strong>{{ workbench.selectedPendingAlertCount }}</strong>
      <p>必要时可与预警页配合处理。</p>
    </div>
    <div class="metric-cell">
      <span>回访记录</span>
      <strong>{{ workbench.selectedFollowupCount }}</strong>
      <p>可结合回访结果继续调整转介方案。</p>
    </div>
  </section>

  <section class="content-columns">
    <article class="ledger-panel">
      <div class="panel-headline">
        <p class="eyebrow">新增转介</p>
        <h3>安排培训、就业与支持资源</h3>
      </div>

      <form id="referral-form" class="form-grid" @submit.prevent="submitReferral">
        <label class="field-block">
          <span>转介类型</span>
          <select v-model="workbench.referralForm.referralType" class="field-input">
            <option v-for="option in workbench.referralTypeOptions" :key="option.value" :value="option.value">
              {{ option.label }}
            </option>
          </select>
        </label>
        <label class="field-block">
          <span>资源名称</span>
          <input v-model="workbench.referralForm.resourceName" class="field-input" type="text" placeholder="例如：无障碍培训营" />
        </label>
        <label class="field-block">
          <span>服务机构 / 提供方</span>
          <input v-model="workbench.referralForm.providerName" class="field-input" type="text" placeholder="例如：市残联培训中心" />
        </label>
        <label class="field-block">
          <span>联系人</span>
          <input v-model="workbench.referralForm.contactName" class="field-input" type="text" />
        </label>
        <label class="field-block">
          <span>联系电话</span>
          <input v-model="workbench.referralForm.contactPhone" class="field-input" type="text" />
        </label>
        <label class="field-block">
          <span>排期时间</span>
          <input v-model="workbench.referralForm.scheduledAt" class="field-input" type="text" placeholder="例如：2026-04-08 14:00:00" />
        </label>
        <label class="field-block">
          <span>操作人</span>
          <input v-model="workbench.referralForm.operatorName" class="field-input" type="text" />
        </label>
        <label class="field-block field-block-wide">
          <span>转介说明</span>
          <textarea
            v-model="workbench.referralForm.statusNote"
            class="field-input field-textarea"
            placeholder="记录转介目标、接收要求、跟进重点。"
          />
        </label>
      </form>
    </article>

    <article class="ledger-panel">
      <div class="panel-headline">
        <p class="eyebrow">转介历史</p>
        <h3>跟进转介状态与结果</h3>
      </div>

      <div v-if="workbench.selectedCaseDetail?.referrals.length" class="record-stack">
        <article v-for="referral in workbench.selectedCaseDetail.referrals" :key="referral.id" class="record-card">
          <div class="row-head">
            <h4>{{ workbench.getReferralTypeLabel(referral.referralType) }} / {{ referral.resourceName }}</h4>
            <span>{{ workbench.getReferralStatusLabel(referral.referralStatus) }}</span>
          </div>
          <div class="inline-tags">
            <span v-if="referral.providerName">机构：{{ referral.providerName }}</span>
            <span v-if="referral.contactName">联系人：{{ referral.contactName }}</span>
            <span v-if="referral.contactPhone">电话：{{ referral.contactPhone }}</span>
            <span>{{ referral.scheduledAt ? `排期：${formatDateTime(referral.scheduledAt)}` : '未排期' }}</span>
          </div>
          <p v-if="referral.statusNote" class="detail-copy">{{ referral.statusNote }}</p>
          <p class="status-muted">
            记录人：{{ referral.operatorName }} / 创建：{{ formatDateTime(referral.createdAt) }} / 更新：{{ formatDateTime(referral.updatedAt) }}
          </p>

          <div v-if="workbench.referralActionForms[referral.id]" class="status-form">
            <textarea
              v-model="workbench.referralActionForms[referral.id].statusNote"
              class="field-input field-textarea field-textarea-sm"
              placeholder="补充对接情况、服务反馈或未成行原因。"
            />
            <div class="action-row">
              <button
                v-for="action in referralStatusActions"
                :key="action.value"
                type="button"
                class="toggle-button"
                :disabled="workbench.activeReferralId === referral.id || action.value === referral.referralStatus"
                @click="workbench.submitReferralStatus(referral, action.value)"
              >
                {{ workbench.activeReferralId === referral.id ? '提交中...' : action.label }}
              </button>
            </div>
          </div>
        </article>
      </div>
      <p v-else class="status-muted">暂未安排资源转介。</p>

      <div class="detail-actions">
        <button type="button" class="secondary-link" @click="refreshCasePage">刷新转介</button>
        <RouterLink class="secondary-link" to="/service/alerts">预警处理</RouterLink>
      </div>
    </article>
  </section>
</template>

<style scoped>
.record-stack,
.form-grid,
.status-form {
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

.field-textarea-sm {
  min-height: 88px;
}

.action-row {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
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

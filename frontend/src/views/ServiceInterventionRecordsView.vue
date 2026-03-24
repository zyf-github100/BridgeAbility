<script setup lang="ts">
import { RouterLink } from 'vue-router'

import { useServiceCasePage } from '../composables/useServiceCasePage'
import { formatDateTime } from '../lib/jobseeker'

const { workbench, caseBasePath, refreshCasePage } = useServiceCasePage()

async function submitIntervention() {
  await workbench.submitIntervention()
}
</script>

<template>
  <div class="page-head">
    <div>
      <p class="eyebrow">服务机构 / 干预记录</p>
      <h2>{{ workbench.selectedCase?.name || '干预记录' }}</h2>
    </div>
    <div class="page-actions">
      <RouterLink class="secondary-link" :to="caseBasePath">个案详情</RouterLink>
      <RouterLink class="secondary-link" :to="`${caseBasePath}/followups`">回访管理</RouterLink>
      <button
        type="submit"
        form="intervention-form"
        class="primary-button"
        :disabled="workbench.isInterventionSaving || !workbench.selectedCaseDetail"
      >
        {{ workbench.isInterventionSaving ? '保存中...' : '保存干预记录' }}
      </button>
    </div>
  </div>

  <p v-if="workbench.successMessage" class="success-banner" aria-live="polite">{{ workbench.successMessage }}</p>
  <p v-if="workbench.actionError" class="status-error" role="alert">{{ workbench.actionError }}</p>
  <p v-if="workbench.detailError" class="status-error" role="alert">{{ workbench.detailError }}</p>

  <section class="metric-strip">
    <div class="metric-cell">
      <span>干预记录</span>
      <strong>{{ workbench.selectedCaseDetail?.interventions.length ?? 0 }}</strong>
      <p>记录机构已执行的支持动作。</p>
    </div>
    <div class="metric-cell">
      <span>下一步动作</span>
      <strong>{{ workbench.selectedCase?.nextAction || '待选择' }}</strong>
      <p>便于本次干预与后续动作衔接。</p>
    </div>
    <div class="metric-cell">
      <span>回访</span>
      <strong>{{ workbench.selectedFollowupCount }}</strong>
      <p>必要时可继续转到回访管理页。</p>
    </div>
    <div class="metric-cell">
      <span>转介</span>
      <strong>{{ workbench.selectedReferralCount }}</strong>
      <p>需要外部资源时可跳转到资源转介页。</p>
    </div>
  </section>

  <section class="content-columns">
    <article class="ledger-panel">
      <div class="panel-headline">
        <p class="eyebrow">新增干预</p>
        <h3>记录机构已执行的支持动作</h3>
      </div>

      <form id="intervention-form" class="form-grid" @submit.prevent="submitIntervention">
        <label class="field-block">
          <span>干预类型</span>
          <select v-model="workbench.interventionForm.interventionType" class="field-input">
            <option v-for="option in workbench.interventionTypeOptions" :key="option.value" :value="option.value">
              {{ option.label }}
            </option>
          </select>
        </label>
        <label class="field-block">
          <span>执行人</span>
          <input v-model="workbench.interventionForm.operatorName" class="field-input" type="text" />
        </label>
        <label class="field-block field-block-wide">
          <span>干预内容</span>
          <textarea
            v-model="workbench.interventionForm.content"
            class="field-input field-textarea"
            placeholder="例如：完成简历改版、企业沟通、面试模拟或入职支持。"
          />
        </label>
        <label class="field-block field-block-wide">
          <span>附件说明</span>
          <input
            v-model="workbench.interventionForm.attachmentNote"
            class="field-input"
            type="text"
            placeholder="可选，例如：简历版本 v2 / 面试纪要"
          />
        </label>
      </form>
    </article>

    <article class="ledger-panel">
      <div class="panel-headline">
        <p class="eyebrow">历史干预</p>
        <h3>当前个案的干预记录时间序列</h3>
      </div>

      <div v-if="workbench.selectedCaseDetail?.interventions.length" class="record-stack">
        <article
          v-for="item in workbench.selectedCaseDetail.interventions"
          :key="item.id"
          class="record-card"
        >
          <div class="row-head">
            <h4>{{ workbench.getInterventionTypeLabel(item.interventionType) }}</h4>
            <span>{{ formatDateTime(item.createdAt) }}</span>
          </div>
          <p class="detail-copy">{{ item.content }}</p>
          <p v-if="item.attachmentNote" class="status-muted">附件说明：{{ item.attachmentNote }}</p>
          <p class="status-muted">执行人：{{ item.operatorName }}</p>
        </article>
      </div>
      <p v-else class="status-muted">还没有写入过干预记录。</p>

      <div class="detail-actions">
        <button type="button" class="secondary-link" @click="refreshCasePage">刷新记录</button>
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
  min-height: 120px;
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

<script setup lang="ts">
import { computed } from 'vue'
import { RouterLink } from 'vue-router'

import { useServiceCasePage } from '../composables/useServiceCasePage'
import { formatDateTime } from '../lib/jobseeker'

const { workbench, caseBasePath, refreshCasePage } = useServiceCasePage()

const selectedProfileAccess = computed(() => workbench.selectedCaseDetail?.profileAccess ?? null)
const selectedProfile = computed(() => selectedProfileAccess.value?.jobseekerProfile ?? null)
const selectedSupportNeeds = computed(() => selectedProfileAccess.value?.supportNeeds ?? null)
const hasLinkedJobseeker = computed(() => Boolean(selectedProfileAccess.value?.linkedJobseeker))
const canManageProfileAccess = computed(() => Boolean(workbench.selectedCaseDetail && hasLinkedJobseeker.value))

async function submitProfileAccess() {
  await workbench.submitProfileAccess()
}
</script>

<template>
  <div class="page-head">
    <div>
      <p class="eyebrow">服务机构 / 个案详情</p>
      <h2>{{ workbench.selectedCase?.name || '个案详情' }}</h2>
    </div>
    <div class="page-actions">
      <RouterLink class="secondary-link" to="/service/cases">返回对象列表</RouterLink>
      <RouterLink class="secondary-link" :to="`${caseBasePath}/interventions`">干预记录</RouterLink>
      <button
        type="submit"
        form="profile-access-form"
        class="primary-button"
        :disabled="workbench.isProfileAccessSaving || !canManageProfileAccess"
      >
        {{ workbench.isProfileAccessSaving ? '保存中...' : '保存档案授权' }}
      </button>
    </div>
  </div>

  <p v-if="workbench.successMessage" class="success-banner" aria-live="polite">{{ workbench.successMessage }}</p>
  <p v-if="workbench.actionError" class="status-error" role="alert">{{ workbench.actionError }}</p>
  <p v-if="workbench.loadError" class="status-error" role="alert">{{ workbench.loadError }}</p>
  <p v-if="workbench.detailError" class="status-error" role="alert">{{ workbench.detailError }}</p>

  <section class="metric-strip">
    <div class="metric-cell">
      <span>当前阶段</span>
      <strong>{{ workbench.selectedCase?.stage || '待选择' }}</strong>
      <p>{{ workbench.selectedCase?.nextAction || '请选择个案后查看下一步动作。' }}</p>
    </div>
    <div class="metric-cell">
      <span>预警</span>
      <strong>{{ workbench.selectedPendingAlertCount }}</strong>
      <p>查看当前个案关联的风险预警。</p>
    </div>
    <div class="metric-cell">
      <span>回访</span>
      <strong>{{ workbench.selectedFollowupCount }}</strong>
      <p>查看当前个案的回访记录。</p>
    </div>
    <div class="metric-cell">
      <span>转介</span>
      <strong>{{ workbench.selectedReferralCount }}</strong>
      <p>查看当前个案的转介安排。</p>
    </div>
  </section>

  <article v-if="workbench.isDetailLoading" class="ledger-panel">
    <p class="status-muted">正在加载个案详情...</p>
  </article>

  <template v-else-if="workbench.selectedCaseDetail">
    <section class="content-columns">
      <article class="ledger-panel">
        <div class="panel-headline">
          <p class="eyebrow">建档信息</p>
          <h3>{{ workbench.selectedCaseDetail.id }}</h3>
        </div>

        <div class="detail-meta-grid">
          <div>
            <span class="meta-label">负责人</span>
            <strong>{{ workbench.selectedCaseDetail.owner }}</strong>
          </div>
          <div>
            <span class="meta-label">阶段</span>
            <strong>{{ workbench.selectedCaseDetail.stage }}</strong>
          </div>
          <div>
            <span class="meta-label">下一步</span>
            <strong>{{ workbench.selectedCaseDetail.nextAction }}</strong>
          </div>
          <div>
            <span class="meta-label">风险等级</span>
            <strong>{{ workbench.getCaseAlertLevelLabel(workbench.selectedCaseDetail.alertLevel) }}</strong>
          </div>
        </div>

        <p v-if="workbench.selectedCaseDetail.intakeNote" class="detail-copy">{{ workbench.selectedCaseDetail.intakeNote }}</p>
      </article>

      <article class="ledger-panel">
        <div class="panel-headline">
          <p class="eyebrow">快捷导航</p>
          <h3>查看更多服务内容</h3>
        </div>
        <div class="route-list route-grid">
          <RouterLink class="route-item" :to="`${caseBasePath}/interventions`">
            <span>干预</span>
            <strong>记录机构动作</strong>
            <p class="body-copy">记录辅导、沟通和支持动作。</p>
          </RouterLink>
          <RouterLink class="route-item" to="/service/alerts">
            <span>预警</span>
            <strong>处理风险预警</strong>
            <p class="body-copy">查看关联预警和处理状态。</p>
          </RouterLink>
          <RouterLink class="route-item" :to="`${caseBasePath}/followups`">
            <span>回访</span>
            <strong>维护回访管理</strong>
            <p class="body-copy">记录 7 天和 30 天回访反馈。</p>
          </RouterLink>
          <RouterLink class="route-item" :to="`${caseBasePath}/referrals`">
            <span>转介</span>
            <strong>安排资源转介</strong>
            <p class="body-copy">管理资源安排与跟进进展。</p>
          </RouterLink>
        </div>
      </article>
    </section>

    <section class="content-columns">
      <article class="ledger-panel">
        <div class="panel-headline">
          <p class="eyebrow">档案授权</p>
          <h3>控制机构是否可以查看求职者档案</h3>
        </div>

        <form id="profile-access-form" class="form-grid" @submit.prevent="submitProfileAccess">
          <label class="field-block field-block-wide checkbox-block">
            <input
              v-model="workbench.profileAccessForm.profileAuthorized"
              type="checkbox"
              :disabled="!canManageProfileAccess"
            />
            <span>允许查看经授权的求职者档案与支持需求</span>
          </label>
          <p v-if="!hasLinkedJobseeker" class="status-muted field-block-wide">
            需先在建档时绑定求职者账号，才能开启查档授权。
          </p>
          <label class="field-block">
            <span>操作人</span>
            <input v-model="workbench.profileAccessForm.operatorName" class="field-input" type="text" />
          </label>
          <label class="field-block field-block-wide">
            <span>授权说明</span>
            <textarea
              v-model="workbench.profileAccessForm.authorizationNote"
              class="field-input field-textarea"
              placeholder="记录授权来源、适用范围或撤回原因。"
            />
          </label>
        </form>
      </article>

      <aside class="side-stack">
        <div class="detail-block">
          <p class="eyebrow">当前授权状态</p>
          <h3>{{ selectedProfileAccess?.profileAuthorized ? '已授权查档' : '未授权查档' }}</h3>
          <div class="inline-tags">
            <span>{{ selectedProfileAccess?.linkedJobseeker ? '已绑定求职者账号' : '未绑定求职者账号' }}</span>
            <span v-if="selectedProfileAccess?.linkedAccount">账号：{{ selectedProfileAccess.linkedAccount }}</span>
          </div>
          <p v-if="selectedProfileAccess?.authorizationNote" class="detail-copy">{{ selectedProfileAccess.authorizationNote }}</p>
          <p v-if="selectedProfileAccess?.authorizationUpdatedAt" class="status-muted">
            最近更新：{{ formatDateTime(selectedProfileAccess.authorizationUpdatedAt) }}
            / {{ selectedProfileAccess.authorizationUpdatedBy || '系统' }}
          </p>
        </div>

        <div class="detail-block">
          <p class="eyebrow">求职者档案摘要</p>
          <template v-if="selectedProfileAccess?.profileAuthorized && selectedProfile">
            <ul class="detail-list">
              <li>姓名：{{ selectedProfile.realName || selectedProfileAccess?.linkedDisplayName || '未填写' }}</li>
              <li>学校 / 专业：{{ selectedProfile.schoolName || '未填写' }} / {{ selectedProfile.major || '未填写' }}</li>
              <li>目标岗位：{{ selectedProfile.expectedJob || '未填写' }}</li>
              <li>意向城市：{{ selectedProfile.targetCity || selectedProfile.currentCity || '未填写' }}</li>
              <li>档案完整度：{{ selectedProfile.profileCompletionRate }}%</li>
            </ul>
          </template>
          <p v-else class="status-muted">当前还没有授权查看求职者档案摘要。</p>
        </div>

        <div class="detail-block">
          <p class="eyebrow">支持需求摘要</p>
          <template v-if="selectedSupportNeeds?.supportSummary?.length">
            <ul class="detail-list">
              <li v-for="item in selectedSupportNeeds.supportSummary" :key="item">{{ item }}</li>
            </ul>
            <p v-if="selectedSupportNeeds.remark" class="detail-copy">{{ selectedSupportNeeds.remark }}</p>
          </template>
          <p v-else class="status-muted">当前没有可查看的支持需求摘要。</p>
        </div>
      </aside>
    </section>

    <section class="content-columns">
      <article class="ledger-panel">
        <div class="panel-headline">
          <p class="eyebrow">时间线</p>
          <h3>当前个案过程记录</h3>
        </div>
        <ol v-if="workbench.selectedCaseDetail.timeline.length" class="timeline-list">
          <li v-for="line in workbench.selectedCaseDetail.timeline" :key="line">
            <span>记录</span>
            <div>
              <strong>{{ line }}</strong>
              <p>{{ workbench.selectedCaseDetail.id }}</p>
            </div>
          </li>
        </ol>
        <p v-else class="status-muted">当前还没有时间线记录。</p>
      </article>

      <article class="ledger-panel">
        <div class="panel-headline">
          <p class="eyebrow">关联预警</p>
          <h3>当前关联预警</h3>
        </div>
        <div v-if="workbench.selectedCaseDetail.alerts.length" class="record-stack">
          <article v-for="item in workbench.selectedCaseDetail.alerts" :key="item.alertId" class="record-card">
            <div class="row-head">
              <h4>{{ item.alertType }}</h4>
              <span>{{ workbench.getAlertStatusLabel(item.alertStatus) }}</span>
            </div>
            <div class="inline-tags">
              <span>{{ workbench.getAlertLevelLabel(item.alertLevel) }}风险</span>
              <span>{{ formatDateTime(item.createdAt) }}</span>
            </div>
            <p class="detail-copy">{{ item.triggerReason }}</p>
            <p v-if="item.resolutionNote" class="status-muted">处理说明：{{ item.resolutionNote }}</p>
          </article>
        </div>
        <p v-else class="status-muted">当前个案没有关联预警。</p>
        <div class="detail-actions">
          <button type="button" class="secondary-link" @click="refreshCasePage">刷新详情</button>
          <RouterLink class="secondary-link" to="/service/alerts">打开预警处理</RouterLink>
        </div>
      </article>
    </section>
  </template>

  <article v-else class="ledger-panel">
    <p class="status-muted">没有可查看的个案，请先回到对象列表选择服务对象。</p>
  </article>
</template>

<style scoped>
.route-grid,
.detail-meta-grid,
.record-stack,
.side-stack,
.form-grid {
  display: grid;
  gap: 14px;
}

.route-grid,
.detail-meta-grid,
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

.meta-label {
  display: block;
  margin-bottom: 4px;
  color: var(--muted);
  font-size: 0.9rem;
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

@media (max-width: 1080px) {
  .route-grid,
  .detail-meta-grid,
  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>

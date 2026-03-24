<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'

import { getAdminIssues, updateAdminIssueStatus, type AdminIssueItem } from '../api/adminIssues'
import { ApiError } from '../lib/http'
import { formatDateTime } from '../lib/jobseeker'
import { useAuthStore } from '../stores/auth'

type IssueType = '' | 'APPEAL' | 'DATA_CORRECTION'
type IssueStatus = '' | 'PENDING' | 'IN_PROGRESS' | 'RESOLVED' | 'REJECTED'

interface IssueActionForm {
  resolutionNote: string
  handlerName: string
}

const authStore = useAuthStore()

const issues = ref<AdminIssueItem[]>([])
const isLoading = ref(false)
const activeIssueId = ref('')
const loadError = ref('')
const actionError = ref('')
const actionSuccess = ref('')
const issueTypeFilter = ref<IssueType>('')
const issueStatusFilter = ref<IssueStatus>('')
const actionForms = reactive<Record<string, IssueActionForm>>({})

const pendingCount = computed(() =>
  issues.value.filter((item) => item.ticketStatus === 'PENDING' || item.ticketStatus === 'IN_PROGRESS').length,
)
const resolvedCount = computed(() => issues.value.filter((item) => item.ticketStatus === 'RESOLVED').length)
const highSeverityCount = computed(() => issues.value.filter((item) => item.severityLevel >= 3).length)

function getDefaultHandlerName() {
  return authStore.nickname || authStore.account || 'admin'
}

function ensureActionForm(issueId: string) {
  if (!actionForms[issueId]) {
    actionForms[issueId] = {
      resolutionNote: '',
      handlerName: getDefaultHandlerName(),
    }
  }
  return actionForms[issueId]
}

function patchActionForms(nextIssues: AdminIssueItem[]) {
  nextIssues.forEach((issue) => {
    const form = ensureActionForm(issue.id)
    if (!form.handlerName) {
      form.handlerName = getDefaultHandlerName()
    }
  })
}

function getIssueTypeLabel(type: string) {
  switch (type) {
    case 'APPEAL':
      return '异常申诉'
    case 'DATA_CORRECTION':
      return '数据纠错'
    default:
      return type
  }
}

function getIssueStatusLabel(status: string) {
  switch (status) {
    case 'PENDING':
      return '待处理'
    case 'IN_PROGRESS':
      return '处理中'
    case 'RESOLVED':
      return '已解决'
    case 'REJECTED':
      return '已驳回'
    default:
      return status
  }
}

function getRoleLabel(role: string) {
  switch (role) {
    case 'ROLE_JOBSEEKER':
      return '求职者'
    case 'ROLE_ENTERPRISE':
      return '企业'
    case 'ROLE_SERVICE_ORG':
      return '服务机构'
    case 'ROLE_ADMIN':
      return '管理员'
    default:
      return role
  }
}

function getSeverityLabel(level: number) {
  if (level >= 3) {
    return '高优先级'
  }
  if (level === 2) {
    return '中优先级'
  }
  return '一般'
}

function isActionable(status: string) {
  return status === 'PENDING' || status === 'IN_PROGRESS'
}

async function loadIssues() {
  if (!authStore.token) {
    return
  }

  isLoading.value = true
  loadError.value = ''

  try {
    issues.value = await getAdminIssues(authStore.token, {
      issueType: issueTypeFilter.value || undefined,
      status: issueStatusFilter.value || undefined,
    })
    patchActionForms(issues.value)
  } catch (error) {
    loadError.value = error instanceof ApiError ? error.message : '申诉与纠错工单加载失败'
  } finally {
    isLoading.value = false
  }
}

async function submitIssueStatus(issue: AdminIssueItem, targetStatus: 'IN_PROGRESS' | 'RESOLVED' | 'REJECTED') {
  if (!authStore.token) {
    return
  }

  const form = ensureActionForm(issue.id)
  activeIssueId.value = issue.id
  actionError.value = ''
  actionSuccess.value = ''

  try {
    const updated = await updateAdminIssueStatus(authStore.token, issue.id, {
      targetStatus,
      resolutionNote: form.resolutionNote.trim(),
      handlerName: form.handlerName.trim() || getDefaultHandlerName(),
    })
    issues.value = issues.value.map((item) => (item.id === updated.id ? updated : item))
    actionSuccess.value =
      targetStatus === 'IN_PROGRESS'
        ? '工单已进入处理中。'
        : targetStatus === 'RESOLVED'
          ? '工单已标记为已解决。'
          : '工单已驳回。'
  } catch (error) {
    actionError.value = error instanceof ApiError ? error.message : '工单状态更新失败'
  } finally {
    activeIssueId.value = ''
  }
}

onMounted(() => {
  loadIssues()
})
</script>

<template>
  <div class="page-head">
    <div>
      <p class="eyebrow">管理端 / 申诉与纠错</p>
      <h2>异常申诉与数据纠错</h2>
    </div>
    <div class="page-actions">
      <button type="button" class="primary-button" @click="loadIssues">刷新工单</button>
    </div>
  </div>

  <section class="metric-strip">
    <div class="metric-cell">
      <span>工单总数</span>
      <strong>{{ issues.length }}</strong>
    </div>
    <div class="metric-cell">
      <span>待处理</span>
      <strong>{{ pendingCount }}</strong>
    </div>
    <div class="metric-cell">
      <span>高优先级</span>
      <strong>{{ highSeverityCount }}</strong>
    </div>
    <div class="metric-cell">
      <span>已解决</span>
      <strong>{{ resolvedCount }}</strong>
    </div>
  </section>

  <article class="ledger-panel">
    <div class="panel-headline">
      <div>
        <p class="eyebrow">工单队列</p>
        <h3>处理申诉与纠错事项</h3>
      </div>
    </div>

    <p v-if="actionSuccess" class="success-banner" aria-live="polite">{{ actionSuccess }}</p>
    <p v-if="actionError" class="status-error" role="alert">{{ actionError }}</p>
    <p v-if="loadError" class="status-error" role="alert">{{ loadError }}</p>

    <div class="filter-row">
      <label class="inline-filter">
        <span>类型</span>
        <select v-model="issueTypeFilter" class="inline-select" @change="loadIssues">
          <option value="">全部</option>
          <option value="APPEAL">异常申诉</option>
          <option value="DATA_CORRECTION">数据纠错</option>
        </select>
      </label>
      <label class="inline-filter">
        <span>状态</span>
        <select v-model="issueStatusFilter" class="inline-select" @change="loadIssues">
          <option value="">全部</option>
          <option value="PENDING">待处理</option>
          <option value="IN_PROGRESS">处理中</option>
          <option value="RESOLVED">已解决</option>
          <option value="REJECTED">已驳回</option>
        </select>
      </label>
    </div>

    <div v-if="issues.length" class="issue-list">
      <article
        v-for="issue in issues"
        :key="issue.id"
        class="issue-card"
        :class="{ 'is-urgent': issue.severityLevel >= 3 }"
      >
        <div class="row-head">
          <div>
            <p class="eyebrow">#{{ issue.id }}</p>
            <h4>{{ issue.title }}</h4>
          </div>
          <span class="status-chip">{{ getIssueStatusLabel(issue.ticketStatus) }}</span>
        </div>

        <p class="detail-copy">{{ issue.content }}</p>

        <div class="inline-tags">
          <span>{{ getIssueTypeLabel(issue.issueType) }}</span>
          <span>{{ getRoleLabel(issue.sourceRole) }} / {{ issue.sourceName }}</span>
          <span>{{ getSeverityLabel(issue.severityLevel) }}</span>
          <span>{{ formatDateTime(issue.createdAt) }}</span>
        </div>

        <p v-if="issue.relatedType || issue.relatedId" class="status-muted">
          关联对象：{{ issue.relatedType || '未指定' }} / {{ issue.relatedId || '未指定' }}
        </p>
        <p v-if="issue.resolutionNote" class="status-muted">处理说明：{{ issue.resolutionNote }}</p>
        <p v-if="issue.handledAt" class="status-muted">
          处理人：{{ issue.handledBy || '未记录' }} / {{ formatDateTime(issue.handledAt) }}
        </p>

        <div v-if="isActionable(issue.ticketStatus)" class="action-shell">
          <label class="field-block field-span">
            <span>处理说明</span>
            <textarea
              v-model="actionForms[issue.id].resolutionNote"
              class="field-input field-textarea"
              placeholder="填写核查过程、处理结论或驳回原因"
            />
          </label>
          <label class="field-block">
            <span>处理人</span>
            <input v-model="actionForms[issue.id].handlerName" class="field-input" type="text" />
          </label>
          <div class="action-row">
            <button
              type="button"
              class="primary-button"
              :disabled="activeIssueId === issue.id"
              @click="submitIssueStatus(issue, 'IN_PROGRESS')"
            >
              {{ activeIssueId === issue.id ? '处理中...' : '标记处理中' }}
            </button>
            <button
              type="button"
              class="secondary-link"
              :disabled="activeIssueId === issue.id"
              @click="submitIssueStatus(issue, 'RESOLVED')"
            >
              标记已解决
            </button>
            <button
              type="button"
              class="secondary-link"
              :disabled="activeIssueId === issue.id"
              @click="submitIssueStatus(issue, 'REJECTED')"
            >
              驳回工单
            </button>
          </div>
        </div>
      </article>
    </div>
    <p v-else-if="isLoading" class="status-muted">正在同步工单队列...</p>
    <p v-else class="status-muted">当前筛选条件下没有工单。</p>
  </article>
</template>

<style scoped>
.filter-row,
.issue-list {
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

.issue-list {
  gap: 12px;
}

.issue-card {
  display: grid;
  gap: 14px;
  padding: 18px;
  border: 1px solid var(--line);
  background: rgba(252, 252, 249, 0.92);
}

.issue-card.is-urgent {
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

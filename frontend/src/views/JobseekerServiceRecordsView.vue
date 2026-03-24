<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'

import { getCurrentServiceRecordDetail, getCurrentServiceRecords } from '../api/jobseeker'
import type { ServiceCaseDetail, ServiceCaseSummary } from '../api/service'
import { ApiError } from '../lib/http'
import { formatDateTime } from '../lib/jobseeker'
import { useAuthStore } from '../stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const serviceCases = ref<ServiceCaseSummary[]>([])
const selectedCaseId = ref('')
const selectedCaseDetail = ref<ServiceCaseDetail | null>(null)
const isLoading = ref(false)
const isDetailLoading = ref(false)
const loadError = ref('')
const detailError = ref('')

let activeDetailRequest = 0

const selectedCase = computed<ServiceCaseSummary | ServiceCaseDetail | null>(() => {
  if (selectedCaseDetail.value?.id === selectedCaseId.value) {
    return selectedCaseDetail.value
  }
  return serviceCases.value.find((item) => item.id === selectedCaseId.value) ?? null
})

const pendingAlertCount = computed(() => {
  if (selectedCaseDetail.value) {
    return selectedCaseDetail.value.alerts.filter((item) =>
      ['OPEN', 'PENDING', 'ESCALATED'].includes((item.alertStatus || '').toUpperCase()),
    ).length
  }
  return selectedCase.value?.pendingAlertCount ?? 0
})

const followupCount = computed(
  () => selectedCaseDetail.value?.followups.length ?? selectedCase.value?.followupCount ?? 0,
)

const timelineCount = computed(
  () => selectedCaseDetail.value?.timeline.length ?? selectedCase.value?.timeline.length ?? 0,
)

const interventionTypeLabels: Record<string, string> = {
  RESUME_GUIDANCE: '简历指导',
  INTERVIEW_GUIDANCE: '面试辅导',
  JOB_RECOMMENDATION: '岗位推荐',
  PSYCHOLOGICAL_SUPPORT: '心理支持',
  TRAINING_REFERRAL: '培训转介',
  EMPLOYER_COMMUNICATION: '企业沟通',
  ONBOARDING_SUPPORT: '入职支持',
}

const followupStageLabels: Record<string, string> = {
  DAY_7: '7 天回访',
  DAY_30: '30 天回访',
}

function getRouteCaseId() {
  return typeof route.query.caseId === 'string' ? route.query.caseId : ''
}

function getAlertLevelLabel(value?: string | null) {
  const normalized = (value || '').trim().toUpperCase()
  if (normalized === 'HIGH') return '高'
  if (normalized === 'MEDIUM') return '中'
  if (normalized === 'LOW') return '低'
  if (normalized === 'NONE') return '无'
  return value || '无'
}

function getAlertStatusLabel(value?: string | null) {
  const normalized = (value || '').trim().toUpperCase()
  if (normalized === 'ESCALATED') return '已升级'
  if (normalized === 'RESOLVED' || normalized === 'HANDLED') return '已处理'
  if (normalized === 'CLOSED') return '已关闭'
  if (normalized === 'OPEN' || normalized === 'PENDING') return '待处理'
  return value || '待处理'
}

function getAlertSeverityLabel(level: number) {
  if (level >= 3) return '高'
  if (level === 2) return '中'
  if (level === 1) return '低'
  return '未评级'
}

function getInterventionTypeLabel(value?: string | null) {
  if (!value) return '未标注'
  return interventionTypeLabels[value] ?? value
}

function getFollowupStageLabel(value?: string | null) {
  if (!value) return '回访'
  return followupStageLabels[value] ?? value
}

function getBooleanLabel(value: boolean) {
  return value ? '是' : '否'
}

async function loadCases() {
  if (!authStore.token) {
    return
  }

  isLoading.value = true
  loadError.value = ''

  try {
    serviceCases.value = await getCurrentServiceRecords(authStore.token)
  } catch (error) {
    loadError.value = error instanceof ApiError ? error.message : '服务支持记录加载失败'
  } finally {
    isLoading.value = false
  }
}

async function loadCaseDetail(caseId: string) {
  if (!authStore.token || !caseId) {
    selectedCaseDetail.value = null
    return
  }

  const requestId = ++activeDetailRequest
  isDetailLoading.value = true
  detailError.value = ''

  try {
    const detail = await getCurrentServiceRecordDetail(authStore.token, caseId)
    if (requestId === activeDetailRequest) {
      selectedCaseDetail.value = detail
    }
  } catch (error) {
    if (requestId !== activeDetailRequest) {
      return
    }
    selectedCaseDetail.value = null
    detailError.value = error instanceof ApiError ? error.message : '个案详情加载失败'
  } finally {
    if (requestId === activeDetailRequest) {
      isDetailLoading.value = false
    }
  }
}

async function selectCase(caseId: string, syncRoute = true) {
  if (!caseId) {
    selectedCaseId.value = ''
    selectedCaseDetail.value = null
    return
  }

  selectedCaseId.value = caseId
  await loadCaseDetail(caseId)

  if (!syncRoute) {
    return
  }

  await router.replace({
    query: {
      ...route.query,
      caseId,
    },
  })
}

async function refreshView() {
  await loadCases()

  if (!serviceCases.value.length) {
    selectedCaseId.value = ''
    selectedCaseDetail.value = null
    return
  }

  const caseId = serviceCases.value.some((item) => item.id === selectedCaseId.value)
    ? selectedCaseId.value
    : serviceCases.value[0].id

  await selectCase(caseId, false)
}

async function initializeView() {
  await loadCases()

  if (!serviceCases.value.length) {
    return
  }

  const routeCaseId = getRouteCaseId()
  const initialCaseId = serviceCases.value.some((item) => item.id === routeCaseId)
    ? routeCaseId
    : serviceCases.value[0].id

  await selectCase(initialCaseId, false)

  if (initialCaseId !== routeCaseId) {
    await router.replace({
      query: {
        ...route.query,
        caseId: initialCaseId,
      },
    })
  }
}

watch(
  () => route.query.caseId,
  async (value) => {
    const caseId = typeof value === 'string' ? value : ''
    if (!caseId || caseId === selectedCaseId.value) {
      return
    }
    if (!serviceCases.value.some((item) => item.id === caseId)) {
      return
    }
    await selectCase(caseId, false)
  },
)

onMounted(() => {
  void initializeView()
})
</script>

<template>
  <div class="page-head">
    <div>
      <p class="eyebrow">求职者 / 服务支持记录</p>
      <h2>服务支持记录</h2>
    </div>
    <div class="page-actions">
      <RouterLink class="secondary-link" to="/applications">返回投递与跟踪</RouterLink>
      <button type="button" class="primary-button" @click="refreshView">刷新</button>
    </div>
  </div>

  <section class="metric-strip">
    <div class="metric-cell">
      <span>服务个案</span>
      <strong>{{ serviceCases.length }}</strong>
      <p>已关联的服务支持记录</p>
    </div>
    <div class="metric-cell">
      <span>待处理预警</span>
      <strong>{{ pendingAlertCount }}</strong>
      <p>{{ selectedCase ? `${selectedCase.name} 的待处理预警` : '选择个案后显示' }}</p>
    </div>
    <div class="metric-cell">
      <span>回访记录</span>
      <strong>{{ followupCount }}</strong>
      <p>{{ selectedCase ? `${selectedCase.name} 的回访总数` : '选择个案后显示' }}</p>
    </div>
    <div class="metric-cell">
      <span>时间线</span>
      <strong>{{ timelineCount }}</strong>
      <p>{{ selectedCase ? `${selectedCase.name} 的过程记录` : '选择个案后显示' }}</p>
    </div>
  </section>

  <section class="workspace-grid">
    <article class="ledger-panel">
      <div class="panel-headline">
        <p class="eyebrow">个案列表</p>
        <h3>查看服务机构的支持进展</h3>
      </div>

      <p v-if="loadError" class="status-error" role="alert">{{ loadError }}</p>
      <p v-else-if="isLoading" class="status-muted">正在加载...</p>
      <div v-else-if="serviceCases.length" class="case-ledger">
        <button
          v-for="item in serviceCases"
          :key="item.id"
          type="button"
          class="case-row service-case-button"
          :class="{ 'is-selected': item.id === selectedCaseId }"
          @click="selectCase(item.id)"
        >
          <div>
            <strong>{{ item.name }}</strong>
            <p>{{ item.id }} / {{ item.stage }}</p>
            <div class="inline-tags">
              <span>{{ item.pendingAlertCount }} 条预警</span>
              <span>{{ item.followupCount }} 次回访</span>
            </div>
          </div>
          <div>
            <span>负责人：{{ item.owner }}</span>
            <p>{{ item.nextAction }}</p>
          </div>
          <div class="case-flag">
            <span>风险</span>
            <strong>{{ getAlertLevelLabel(item.alertLevel) }}</strong>
          </div>
        </button>
      </div>
      <p v-else class="status-muted">当前没有服务支持记录。</p>
    </article>

    <article class="hero-panel">
      <div class="panel-headline">
        <p class="eyebrow">当前个案</p>
        <h3>{{ selectedCase ? selectedCase.name : '选择一个个案' }}</h3>
      </div>

      <template v-if="selectedCase">
        <p class="record-copy">
          {{ selectedCase.stage }} / 负责人：{{ selectedCase.owner }} / 下一步：{{ selectedCase.nextAction }}
        </p>
        <div class="inline-tags detail-tags">
          <span>风险：{{ getAlertLevelLabel(selectedCase.alertLevel) }}</span>
          <span>待处理预警：{{ pendingAlertCount }}</span>
          <span>回访：{{ followupCount }}</span>
        </div>
      </template>
      <p v-else class="status-muted">暂无可查看个案。</p>
    </article>
  </section>

  <section class="content-columns">
    <article class="ledger-panel">
      <div class="panel-headline">
        <p class="eyebrow">个案详情</p>
        <h3>{{ selectedCase ? selectedCase.name : '选择个案' }}</h3>
      </div>

      <p v-if="detailError" class="status-error" role="alert">{{ detailError }}</p>
      <p v-else-if="isDetailLoading" class="status-muted">正在加载详情...</p>
      <template v-else-if="selectedCaseDetail">
        <div class="detail-block">
          <p class="eyebrow">时间线</p>
          <ol v-if="selectedCaseDetail.timeline.length" class="timeline-list">
            <li v-for="line in selectedCaseDetail.timeline" :key="line">
              <span>记录</span>
              <div>
                <strong>{{ line }}</strong>
                <p>{{ selectedCaseDetail.id }}</p>
              </div>
            </li>
          </ol>
          <p v-else class="status-muted">暂无时间线记录。</p>
        </div>

        <div class="detail-block">
          <p class="eyebrow">机构干预</p>
          <div v-if="selectedCaseDetail.interventions.length" class="record-stack">
            <article v-for="item in selectedCaseDetail.interventions" :key="item.id" class="record-card">
              <div class="row-head">
                <h4>{{ getInterventionTypeLabel(item.interventionType) }}</h4>
                <span>{{ formatDateTime(item.createdAt) }}</span>
              </div>
              <p class="record-copy">{{ item.content }}</p>
              <p v-if="item.attachmentNote" class="status-muted">附件：{{ item.attachmentNote }}</p>
              <p class="status-muted">执行人：{{ item.operatorName }}</p>
            </article>
          </div>
          <p v-else class="status-muted">暂无干预记录。</p>
        </div>

        <div class="detail-block">
          <p class="eyebrow">回访记录</p>
          <div v-if="selectedCaseDetail.followups.length" class="record-stack">
            <article v-for="item in selectedCaseDetail.followups" :key="item.id" class="record-card">
              <div class="row-head">
                <h4>{{ getFollowupStageLabel(item.followupStage) }}</h4>
                <span>{{ formatDateTime(item.completedAt || item.dueAt || item.createdAt) }}</span>
              </div>
              <div class="inline-tags">
                <span>适应 {{ item.adaptationScore }}</span>
                <span>支持落实：{{ getBooleanLabel(item.supportImplemented) }}</span>
                <span>离职风险：{{ getBooleanLabel(item.leaveRisk) }}</span>
                <span>需要帮助：{{ getBooleanLabel(item.needHelp) }}</span>
              </div>
              <p v-if="item.environmentIssue" class="record-copy">环境：{{ item.environmentIssue }}</p>
              <p v-if="item.communicationIssue" class="record-copy">沟通：{{ item.communicationIssue }}</p>
              <p class="status-muted">记录人：{{ item.operatorName }}</p>
            </article>
          </div>
          <p v-else class="status-muted">暂无回访记录。</p>
        </div>

        <div class="detail-block">
          <p class="eyebrow">预警记录</p>
          <div v-if="selectedCaseDetail.alerts.length" class="record-stack">
            <article v-for="item in selectedCaseDetail.alerts" :key="item.alertId" class="record-card">
              <div class="row-head">
                <h4>{{ item.alertType }}</h4>
                <span>{{ getAlertStatusLabel(item.alertStatus) }}</span>
              </div>
              <div class="inline-tags">
                <span>{{ getAlertSeverityLabel(item.alertLevel) }}风险</span>
                <span>{{ formatDateTime(item.createdAt) }}</span>
              </div>
              <p class="record-copy">{{ item.triggerReason }}</p>
              <p v-if="item.resolutionNote" class="status-muted">处理说明：{{ item.resolutionNote }}</p>
            </article>
          </div>
          <p v-else class="status-muted">暂无预警记录。</p>
        </div>
      </template>
      <p v-else class="status-muted">从左侧选择个案后查看详情。</p>
    </article>

    <aside class="side-stack">
      <div v-if="selectedCase" class="detail-block">
        <p class="eyebrow">个案摘要</p>
        <ul class="summary-list">
          <li>
            <span>负责人</span>
            <strong>{{ selectedCase.owner }}</strong>
          </li>
          <li>
            <span>下一步</span>
            <strong>{{ selectedCase.nextAction }}</strong>
          </li>
          <li>
            <span>风险等级</span>
            <strong>{{ getAlertLevelLabel(selectedCase.alertLevel) }}</strong>
          </li>
        </ul>
      </div>

      <div class="detail-block">
        <p class="eyebrow">建议动作</p>
        <ol class="detail-list ordered-list">
          <li>回到“投递与跟踪”补充最新反馈。</li>
          <li>如果预警还在处理中，尽快联系服务机构确认进展。</li>
          <li>更新便利需求，保持企业与机构看到的信息一致。</li>
        </ol>
      </div>
    </aside>
  </section>
</template>

<style scoped>
.service-case-button {
  width: 100%;
  padding: 16px 0;
  background: transparent;
  border: 0;
  text-align: left;
}

.service-case-button.is-selected {
  background: rgba(37, 99, 235, 0.08);
}

.service-case-button:hover {
  background: rgba(37, 99, 235, 0.05);
}

.detail-tags {
  margin-bottom: 18px;
}

.record-stack,
.summary-list,
.side-stack {
  display: grid;
  gap: 12px;
}

.record-card {
  padding: 16px;
  border: 1px solid var(--line);
  background: rgba(252, 252, 249, 0.92);
}

.record-copy {
  margin: 0;
  line-height: 1.8;
  white-space: pre-line;
}

.status-error {
  color: var(--error);
  font-weight: 700;
}

.status-muted {
  color: var(--muted);
}

.summary-list {
  margin: 0;
  padding: 0;
  list-style: none;
}

.summary-list li {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  padding-top: 12px;
  border-top: 1px solid var(--line);
}

.summary-list li:first-child {
  padding-top: 0;
  border-top: 0;
}

.summary-list strong {
  font-family: var(--serif);
  color: var(--heading);
  font-size: 1.05rem;
}

.ordered-list {
  padding-left: 20px;
}
</style>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'

import ScoreBar from '../components/ScoreBar.vue'
import {
  createEnterpriseJob,
  getEnterpriseJobDetail,
  getEnterpriseProfile,
  offlineEnterpriseJob,
  updateEnterpriseJob,
  type EnterpriseJobAccessibility,
  type EnterpriseJobDetail,
  type EnterpriseJobUpsertPayload,
  type EnterpriseVerificationProfile,
} from '../api/enterprise'
import { ApiError } from '../lib/http'
import {
  getJobStageLabel,
  getScoreDimensionLabel,
  getWorkModeLabel,
} from '../lib/jobseeker'
import { useAuthStore } from '../stores/auth'

type TriStateValue = '' | 'true' | 'false'

interface JobFormState {
  title: string
  department: string
  city: string
  salaryMin: number
  salaryMax: number
  headcount: number
  description: string
  requirementText: string
  workMode: string
  deadline: string
  interviewMode: string
  accessibility: Record<keyof EnterpriseJobAccessibility, TriStateValue>
}

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const enterpriseProfile = ref<EnterpriseVerificationProfile | null>(null)
const selectedJobDetail = ref<EnterpriseJobDetail | null>(null)
const isProfileLoading = ref(false)
const isDetailLoading = ref(false)
const isSaving = ref(false)
const isOfflining = ref(false)
const loadError = ref('')
const saveError = ref('')
const profileError = ref('')
const successMessage = ref('')

const triStateOptions = [
  { value: '', label: '待补充' },
  { value: 'true', label: '是' },
  { value: 'false', label: '否' },
]

const workModeOptions = [
  { value: 'FULL_TIME', label: '全职' },
  { value: 'PART_TIME', label: '兼职' },
  { value: 'INTERNSHIP', label: '实习' },
  { value: 'REMOTE', label: '远程' },
  { value: 'HYBRID', label: '混合办公' },
]

const interviewModeOptions = [
  { value: 'TEXT', label: '文字或书面沟通' },
  { value: 'ONLINE', label: '线上视频面试' },
  { value: 'HYBRID', label: '先书面后补充沟通' },
]

const accessibilityFields: Array<{
  key: keyof EnterpriseJobAccessibility
  label: string
}> = [
  { key: 'onsiteRequired', label: '是否必须线下到岗' },
  { key: 'remoteSupported', label: '是否支持远程/混合办公' },
  { key: 'highFrequencyVoiceRequired', label: '是否要求高频语音沟通' },
  { key: 'noisyEnvironment', label: '是否涉及高噪音环境' },
  { key: 'longStandingRequired', label: '是否需要长时间站立' },
  { key: 'textMaterialSupported', label: '是否支持书面材料' },
  { key: 'onlineInterviewSupported', label: '是否支持线上面试' },
  { key: 'textInterviewSupported', label: '是否支持文字/书面面试' },
  { key: 'flexibleScheduleSupported', label: '是否支持弹性时间' },
  { key: 'accessibleWorkspace', label: '办公区是否无障碍可达' },
  { key: 'assistiveSoftwareSupported', label: '是否可提供辅助软件或设备' },
]

const form = reactive<JobFormState>(createEmptyForm())

const jobId = computed(() => {
  const value = Array.isArray(route.params.jobId) ? route.params.jobId[0] : route.params.jobId
  return value ?? ''
})
const isEditMode = computed(() => Boolean(jobId.value))
const missingAccessibilityCount = computed(() => Object.values(form.accessibility).filter((value) => value === '').length)
const accessibilityCompletionRate = computed(() => {
  const values = Object.values(form.accessibility)
  const filled = values.filter((value) => value !== '').length
  return Math.round((filled * 100) / values.length)
})
const readyToPublish = computed(() => missingAccessibilityCount.value === 0)
const canPublishJobs = computed(() => enterpriseProfile.value?.canPublishJobs ?? false)
const verificationStatusLabel = computed(() => {
  switch (enterpriseProfile.value?.verificationStatus ?? 'DRAFT') {
    case 'APPROVED':
      return '已通过'
    case 'PENDING':
      return '审核中'
    case 'REJECTED':
      return '已驳回'
    default:
      return '待提交'
  }
})

watch(
  () => jobId.value,
  async (value) => {
    if (!value) {
      selectedJobDetail.value = null
      Object.assign(form, createEmptyForm())
      return
    }
    await loadJobDetail(value)
  },
  { immediate: true },
)

async function loadEnterpriseProfile() {
  if (!authStore.token) {
    return
  }

  isProfileLoading.value = true
  profileError.value = ''

  try {
    enterpriseProfile.value = await getEnterpriseProfile(authStore.token)
  } catch (error) {
    profileError.value = error instanceof ApiError ? error.message : '企业认证状态加载失败'
  } finally {
    isProfileLoading.value = false
  }
}

async function loadJobDetail(targetJobId: string) {
  if (!authStore.token) {
    return
  }

  isDetailLoading.value = true
  loadError.value = ''

  try {
    const detail = await getEnterpriseJobDetail(authStore.token, targetJobId)
    selectedJobDetail.value = detail
    fillForm(detail)
  } catch (error) {
    loadError.value = error instanceof ApiError ? error.message : '岗位详情加载失败'
  } finally {
    isDetailLoading.value = false
  }
}

async function refreshPage() {
  await loadEnterpriseProfile()
  if (jobId.value) {
    await loadJobDetail(jobId.value)
  }
}

function fillForm(detail: EnterpriseJobDetail) {
  form.title = detail.title
  form.department = detail.department
  form.city = detail.city
  form.salaryMin = detail.salaryMin
  form.salaryMax = detail.salaryMax
  form.headcount = detail.headcount
  form.description = detail.description
  form.requirementText = detail.requirementText
  form.workMode = detail.workMode
  form.deadline = detail.deadline
  form.interviewMode = detail.interviewMode
  form.accessibility = {
    onsiteRequired: toTriState(detail.accessibilityTag.onsiteRequired),
    remoteSupported: toTriState(detail.accessibilityTag.remoteSupported),
    highFrequencyVoiceRequired: toTriState(detail.accessibilityTag.highFrequencyVoiceRequired),
    noisyEnvironment: toTriState(detail.accessibilityTag.noisyEnvironment),
    longStandingRequired: toTriState(detail.accessibilityTag.longStandingRequired),
    textMaterialSupported: toTriState(detail.accessibilityTag.textMaterialSupported),
    onlineInterviewSupported: toTriState(detail.accessibilityTag.onlineInterviewSupported),
    textInterviewSupported: toTriState(detail.accessibilityTag.textInterviewSupported),
    flexibleScheduleSupported: toTriState(detail.accessibilityTag.flexibleScheduleSupported),
    accessibleWorkspace: toTriState(detail.accessibilityTag.accessibleWorkspace),
    assistiveSoftwareSupported: toTriState(detail.accessibilityTag.assistiveSoftwareSupported),
  }
}

async function saveJob(nextStatus: 'DRAFT' | 'PUBLISHED') {
  if (!authStore.token) {
    return
  }

  if (nextStatus === 'PUBLISHED' && !canPublishJobs.value) {
    saveError.value = '企业认证尚未通过，当前只能保存草稿，不能发布岗位。'
    successMessage.value = ''
    return
  }

  if (nextStatus === 'PUBLISHED' && !readyToPublish.value) {
    saveError.value = '请先补齐无障碍标签，再正式发布岗位。'
    successMessage.value = ''
    return
  }

  isSaving.value = true
  saveError.value = ''
  successMessage.value = ''

  try {
    const payload = buildPayload(nextStatus)
    const detail = isEditMode.value
      ? await updateEnterpriseJob(authStore.token, jobId.value, payload)
      : await createEnterpriseJob(authStore.token, payload)

    selectedJobDetail.value = detail
    fillForm(detail)
    successMessage.value = nextStatus === 'PUBLISHED' ? '岗位已发布，可继续查看岗位详情或进入候选人列表。' : '岗位草稿已保存。'
    if (!isEditMode.value) {
      await router.replace(`/enterprise/jobs/${detail.id}/edit`)
    }
  } catch (error) {
    saveError.value = error instanceof ApiError ? error.message : '岗位保存失败'
  } finally {
    isSaving.value = false
  }
}

async function offlineCurrentJob() {
  if (!authStore.token || !jobId.value) {
    return
  }

  isOfflining.value = true
  saveError.value = ''
  successMessage.value = ''

  try {
    const detail = await offlineEnterpriseJob(authStore.token, jobId.value)
    selectedJobDetail.value = detail
    fillForm(detail)
    successMessage.value = '岗位已下线，求职者将不再看到该岗位。'
  } catch (error) {
    saveError.value = error instanceof ApiError ? error.message : '岗位下线失败'
  } finally {
    isOfflining.value = false
  }
}

function buildPayload(publishStatus: 'DRAFT' | 'PUBLISHED'): EnterpriseJobUpsertPayload {
  return {
    title: form.title.trim(),
    department: form.department.trim(),
    city: form.city.trim(),
    salaryMin: Number(form.salaryMin),
    salaryMax: Number(form.salaryMax),
    headcount: Number(form.headcount),
    description: form.description.trim(),
    requirementText: form.requirementText.trim(),
    workMode: form.workMode,
    deadline: form.deadline,
    interviewMode: form.interviewMode,
    publishStatus,
    accessibilityTag: {
      onsiteRequired: fromTriState(form.accessibility.onsiteRequired),
      remoteSupported: fromTriState(form.accessibility.remoteSupported),
      highFrequencyVoiceRequired: fromTriState(form.accessibility.highFrequencyVoiceRequired),
      noisyEnvironment: fromTriState(form.accessibility.noisyEnvironment),
      longStandingRequired: fromTriState(form.accessibility.longStandingRequired),
      textMaterialSupported: fromTriState(form.accessibility.textMaterialSupported),
      onlineInterviewSupported: fromTriState(form.accessibility.onlineInterviewSupported),
      textInterviewSupported: fromTriState(form.accessibility.textInterviewSupported),
      flexibleScheduleSupported: fromTriState(form.accessibility.flexibleScheduleSupported),
      accessibleWorkspace: fromTriState(form.accessibility.accessibleWorkspace),
      assistiveSoftwareSupported: fromTriState(form.accessibility.assistiveSoftwareSupported),
    },
  }
}

function createEmptyForm(): JobFormState {
  return {
    title: '',
    department: '',
    city: '',
    salaryMin: 5000,
    salaryMax: 7000,
    headcount: 1,
    description: '',
    requirementText: '',
    workMode: 'HYBRID',
    deadline: new Date(Date.now() + 1000 * 60 * 60 * 24 * 30).toISOString().slice(0, 10),
    interviewMode: 'ONLINE',
    accessibility: {
      onsiteRequired: '',
      remoteSupported: '',
      highFrequencyVoiceRequired: '',
      noisyEnvironment: '',
      longStandingRequired: '',
      textMaterialSupported: '',
      onlineInterviewSupported: '',
      textInterviewSupported: '',
      flexibleScheduleSupported: '',
      accessibleWorkspace: '',
      assistiveSoftwareSupported: '',
    },
  }
}

function toTriState(value: boolean | null): TriStateValue {
  if (value === true) {
    return 'true'
  }
  if (value === false) {
    return 'false'
  }
  return ''
}

function fromTriState(value: TriStateValue) {
  if (value === 'true') {
    return true
  }
  if (value === 'false') {
    return false
  }
  return null
}

onMounted(() => {
  void loadEnterpriseProfile()
})
</script>

<template>
  <div class="page-head">
    <div>
      <p class="eyebrow">企业端 / {{ isEditMode ? '编辑岗位' : '发布岗位' }}</p>
      <h2>{{ isEditMode ? '发布岗位' : '创建岗位草稿' }}</h2>
    </div>
    <div class="page-actions">
      <RouterLink class="secondary-link" to="/enterprise/jobs">返回岗位列表</RouterLink>
      <RouterLink v-if="jobId" class="secondary-link" :to="`/enterprise/jobs/${jobId}`">岗位详情</RouterLink>
      <RouterLink v-if="jobId" class="secondary-link" :to="`/enterprise/candidates?jobId=${jobId}`">候选人列表</RouterLink>
      <button type="button" class="primary-button" @click="refreshPage">刷新数据</button>
    </div>
  </div>

  <p v-if="profileError" class="status-error" role="alert">{{ profileError }}</p>
  <p v-if="loadError" class="status-error" role="alert">{{ loadError }}</p>

  <article class="ledger-panel publishing-gate">
    <div class="panel-headline">
      <div>
        <p class="eyebrow">发布前检查</p>
        <h3>{{ canPublishJobs ? '企业认证已通过，可继续发布岗位' : '企业认证未通过，当前只能保存草稿' }}</h3>
      </div>
      <RouterLink class="secondary-link" to="/enterprise/verification">去完善认证</RouterLink>
    </div>

    <p v-if="isProfileLoading" class="status-muted">正在加载企业认证状态...</p>

    <div v-else class="gate-grid">
      <div class="detail-block">
        <p class="eyebrow">认证状态</p>
        <ul class="detail-list compact-list">
          <li>认证状态：{{ verificationStatusLabel }}</li>
          <li>可否发布岗位：{{ canPublishJobs ? '可以发布' : '仅可保存草稿' }}</li>
          <li>已发布岗位：{{ enterpriseProfile?.publishedJobCount ?? 0 }}</li>
          <li>已上传资料：{{ enterpriseProfile?.materials.length ?? 0 }}</li>
        </ul>
        <p v-if="enterpriseProfile?.reviewNote" class="detail-copy">{{ enterpriseProfile.reviewNote }}</p>
      </div>

      <div class="detail-block">
        <p class="eyebrow">当前编辑状态</p>
        <ul class="detail-list compact-list">
          <li>模式：{{ isEditMode ? '编辑已有岗位' : '新建岗位' }}</li>
          <li>无障碍标签完成度：{{ accessibilityCompletionRate }}%</li>
          <li>待补充字段：{{ missingAccessibilityCount }}</li>
          <li>当前岗位：{{ selectedJobDetail?.title || '尚未保存' }}</li>
        </ul>
      </div>
    </div>
  </article>

  <section class="management-grid">
    <article class="detail-pane">
      <div class="panel-headline">
        <p class="eyebrow">岗位表单</p>
        <h3>{{ isEditMode ? '编辑岗位并控制发布状态' : '创建岗位草稿' }}</h3>
      </div>

      <p v-if="successMessage" class="success-banner" aria-live="polite">{{ successMessage }}</p>
      <p v-if="saveError" class="status-error" role="alert">{{ saveError }}</p>
      <p v-if="isDetailLoading" class="status-muted">正在加载岗位详情...</p>

      <div class="form-grid">
        <label class="field-block">
          <span>岗位名称</span>
          <input v-model="form.title" class="field-input" type="text" />
        </label>
        <label class="field-block">
          <span>所属部门</span>
          <input v-model="form.department" class="field-input" type="text" />
        </label>
        <label class="field-block">
          <span>工作地点</span>
          <input v-model="form.city" class="field-input" type="text" />
        </label>
        <label class="field-block">
          <span>招聘人数</span>
          <input v-model.number="form.headcount" class="field-input" type="number" min="1" max="999" />
        </label>
        <label class="field-block">
          <span>最低薪资</span>
          <input v-model.number="form.salaryMin" class="field-input" type="number" min="1000" step="500" />
        </label>
        <label class="field-block">
          <span>最高薪资</span>
          <input v-model.number="form.salaryMax" class="field-input" type="number" min="1000" step="500" />
        </label>
        <label class="field-block">
          <span>工作方式</span>
          <select v-model="form.workMode" class="field-input">
            <option v-for="option in workModeOptions" :key="option.value" :value="option.value">
              {{ option.label }}
            </option>
          </select>
        </label>
        <label class="field-block">
          <span>面试方式</span>
          <select v-model="form.interviewMode" class="field-input">
            <option v-for="option in interviewModeOptions" :key="option.value" :value="option.value">
              {{ option.label }}
            </option>
          </select>
        </label>
        <label class="field-block field-block-wide">
          <span>截止日期</span>
          <input v-model="form.deadline" class="field-input" type="date" />
        </label>
        <label class="field-block field-block-wide">
          <span>岗位职责</span>
          <textarea v-model="form.description" class="field-input field-textarea" />
        </label>
        <label class="field-block field-block-wide">
          <span>任职要求</span>
          <textarea v-model="form.requirementText" class="field-input field-textarea" />
        </label>
      </div>

      <div class="panel-headline panel-headline-tight">
        <p class="eyebrow">无障碍标签</p>
        <h3>补齐后再正式发布</h3>
      </div>

      <div class="accessibility-grid">
        <label v-for="field in accessibilityFields" :key="field.key" class="accessibility-item">
          <span>{{ field.label }}</span>
          <select v-model="form.accessibility[field.key]" class="field-input">
            <option v-for="option in triStateOptions" :key="option.value" :value="option.value">
              {{ option.label }}
            </option>
          </select>
        </label>
      </div>

      <div class="action-row">
        <button type="button" class="secondary-link" :disabled="isSaving" @click="saveJob('DRAFT')">
          {{ isSaving ? '保存中...' : '保存草稿' }}
        </button>
        <button
          type="button"
          class="primary-button"
          :disabled="isSaving || !canPublishJobs || !readyToPublish"
          @click="saveJob('PUBLISHED')"
        >
          {{ isSaving ? '发布中...' : '保存并发布' }}
        </button>
        <button
          v-if="isEditMode && selectedJobDetail?.publishStatus !== 'OFFLINE'"
          type="button"
          class="secondary-link"
          :disabled="isOfflining"
          @click="offlineCurrentJob"
        >
          {{ isOfflining ? '处理中...' : '下线岗位' }}
        </button>
      </div>
    </article>

    <article class="list-pane preview-pane">
      <div class="panel-headline">
        <p class="eyebrow">发布预览</p>
        <h3>{{ selectedJobDetail ? selectedJobDetail.title : '先保存一次岗位后查看预览' }}</h3>
      </div>

      <template v-if="selectedJobDetail">
        <p class="body-copy">
          {{ selectedJobDetail.companyName }} / {{ selectedJobDetail.city }} /
          {{ getWorkModeLabel(selectedJobDetail.workMode) }} / {{ selectedJobDetail.salaryRange }}
        </p>

        <div class="metric-strip preview-metrics">
          <div class="metric-cell">
            <span>发布状态</span>
            <strong>{{ selectedJobDetail.publishStatus }}</strong>
          </div>
          <div class="metric-cell">
            <span>岗位阶段</span>
            <strong>{{ getJobStageLabel(selectedJobDetail.stage) }}</strong>
          </div>
          <div class="metric-cell">
            <span>匹配分</span>
            <strong>{{ selectedJobDetail.matchScore }}</strong>
          </div>
          <div class="metric-cell">
            <span>标签完成度</span>
            <strong>{{ selectedJobDetail.accessibilityCompletionRate }}%</strong>
          </div>
        </div>

        <div class="score-stack">
          <ScoreBar
            v-for="score in selectedJobDetail.dimensionScores"
            :key="score.label"
            :label="getScoreDimensionLabel(score.label)"
            :value="score.value"
          />
        </div>

        <div class="detail-block">
          <p class="eyebrow">推荐理由</p>
          <ul class="detail-list">
            <li v-for="item in selectedJobDetail.reasons" :key="item">{{ item }}</li>
          </ul>
        </div>

        <div class="detail-block">
          <p class="eyebrow">风险提示</p>
          <ul class="detail-list">
            <li v-for="item in selectedJobDetail.risks" :key="item">{{ item }}</li>
          </ul>
        </div>

        <div class="detail-block">
          <p class="eyebrow">支持信息</p>
          <ul class="detail-list">
            <li v-for="item in selectedJobDetail.supports" :key="item">{{ item }}</li>
          </ul>
        </div>

        <div class="detail-block">
          <p class="eyebrow">环境摘要</p>
          <ul class="detail-list">
            <li v-for="item in selectedJobDetail.environment" :key="item">{{ item }}</li>
          </ul>
        </div>

        <div class="detail-block">
          <p class="eyebrow">投递提示</p>
          <p class="detail-copy">{{ selectedJobDetail.applyHint }}</p>
        </div>

        <div class="action-grid">
          <RouterLink class="route-item" :to="`/enterprise/jobs/${selectedJobDetail.id}`">
            <strong>打开岗位详情</strong>
            <span>查看岗位详情页中的岗位概览和候选人样本</span>
          </RouterLink>
          <RouterLink class="route-item" :to="`/enterprise/candidates?jobId=${selectedJobDetail.id}`">
            <strong>进入候选人列表</strong>
            <span>继续推进当前岗位的候选人筛选</span>
          </RouterLink>
        </div>
      </template>

      <p v-else class="status-muted">新建岗位后，这里会显示对求职者可见的发布预览。</p>
    </article>
  </section>
</template>

<style scoped>
.publishing-gate {
  margin-bottom: 20px;
}

.gate-grid,
.management-grid,
.form-grid,
.accessibility-grid,
.preview-pane,
.score-stack,
.action-grid {
  display: grid;
  gap: 16px;
}

.gate-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.management-grid {
  grid-template-columns: minmax(0, 1.45fr) minmax(320px, 0.95fr);
}

.form-grid,
.accessibility-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.field-block {
  display: grid;
  gap: 8px;
}

.field-block-wide {
  grid-column: 1 / -1;
}

.field-textarea {
  min-height: 112px;
  resize: vertical;
}

.panel-headline-tight {
  margin-top: 22px;
}

.accessibility-item {
  display: grid;
  gap: 6px;
  padding: 14px;
  border: 1px solid var(--line);
  background: rgba(255, 255, 255, 0.62);
}

.preview-metrics {
  margin-top: 6px;
}

.action-row {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 20px;
}

.action-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.success-banner,
.status-error,
.status-muted {
  margin: 0 0 14px;
}

.success-banner {
  padding: 12px 14px;
  color: var(--success);
  background: var(--success-surface);
}

.status-error {
  color: var(--error);
  font-weight: 700;
}

.status-muted {
  color: var(--muted);
}

@media (max-width: 1180px) {
  .gate-grid,
  .management-grid,
  .form-grid,
  .accessibility-grid,
  .action-grid {
    grid-template-columns: 1fr;
  }
}
</style>

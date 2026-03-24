<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { RouterLink, useRoute } from 'vue-router'

import ErrorSummary from '../components/ErrorSummary.vue'
import ScoreBar from '../components/ScoreBar.vue'
import {
  applyToJob,
  getCurrentSupportNeeds,
  getCurrentUserApplications,
  getRecommendedJobDetail,
  type Job,
  type JobApplication,
  type SupportNeeds,
} from '../api/jobseeker'
import { ApiError } from '../lib/http'
import {
  formatDateTime,
  getApplicationStatusLabel,
  getInterviewModeLabel,
  getScoreDimensionLabel,
  getSupportVisibilityLabel,
  getWorkModeLabel,
  interviewModeOptions,
} from '../lib/jobseeker'
import { useAuthStore } from '../stores/auth'

const route = useRoute()
const authStore = useAuthStore()

const job = ref<Job | null>(null)
const currentApplication = ref<JobApplication | null>(null)
const supportNeeds = ref<SupportNeeds | null>(null)
const isLoading = ref(false)
const isSubmitting = ref(false)
const loadError = ref('')
const submitError = ref('')
const successMessage = ref('')
const submitted = ref(false)

const form = reactive({
  coverNote: '',
  preferredInterviewMode: 'TEXT',
})

const jobId = computed(() => {
  const value = Array.isArray(route.params.jobId) ? route.params.jobId[0] : route.params.jobId
  return value ?? ''
})

const supportSummaryPreview = computed(() => supportNeeds.value?.supportSummary ?? [])
const supportScopeLabel = computed(() =>
  getSupportVisibilityLabel(supportNeeds.value?.supportVisibility),
)
const supportUpdatedAt = computed(() =>
  supportNeeds.value?.updatedAt ? formatDateTime(supportNeeds.value.updatedAt) : '尚未保存',
)
const interviewCard = computed(() => supportNeeds.value?.interviewCommunicationCard ?? null)

const errors = computed(() => {
  const nextErrors: Array<{ id: string; label: string; message: string }> = []

  if (form.coverNote.trim().length < 24) {
    nextErrors.push({
      id: 'cover-note',
      label: '投递说明',
      message: '请至少填写 24 个字，说明你和岗位的匹配点。',
    })
  }

  if (!form.preferredInterviewMode) {
    nextErrors.push({
      id: 'preferred-interview-mode',
      label: '首选面试方式',
      message: '请选择一个首选面试方式。',
    })
  }

  return nextErrors
})

async function loadPageData() {
  if (!authStore.token || !jobId.value) {
    return
  }

  isLoading.value = true
  loadError.value = ''
  submitError.value = ''

  try {
    const [jobDetail, applications, supportProfile] = await Promise.all([
      getRecommendedJobDetail(authStore.token, jobId.value),
      getCurrentUserApplications(authStore.token),
      getCurrentSupportNeeds(authStore.token),
    ])
    job.value = jobDetail
    supportNeeds.value = supportProfile
    currentApplication.value =
      applications.find((application) => application.jobId === jobId.value) ?? null
  } catch (error) {
    loadError.value = error instanceof ApiError ? error.message : '岗位详情加载失败'
  } finally {
    isLoading.value = false
  }
}

async function submitApplication() {
  submitted.value = true
  submitError.value = ''
  successMessage.value = ''

  if (errors.value.length > 0 || !authStore.token || !job.value || currentApplication.value) {
    return
  }

  isSubmitting.value = true

  try {
    currentApplication.value = await applyToJob(authStore.token, job.value.id, {
      coverNote: form.coverNote.trim(),
      preferredInterviewMode: form.preferredInterviewMode,
    })
    successMessage.value = '投递成功。'
  } catch (error) {
    if (error instanceof ApiError && error.code === 4008) {
      submitError.value = '该岗位已经投递过了。'
      await syncCurrentApplication()
    } else {
      submitError.value = error instanceof ApiError ? error.message : '投递失败，请稍后重试。'
    }
  } finally {
    isSubmitting.value = false
  }
}

async function syncCurrentApplication() {
  if (!authStore.token || !jobId.value) {
    return
  }

  const applications = await getCurrentUserApplications(authStore.token)
  currentApplication.value =
    applications.find((application) => application.jobId === jobId.value) ?? null
}

watch(
  () => jobId.value,
  () => {
    loadPageData()
  },
)

onMounted(() => {
  loadPageData()
})
</script>

<template>
  <div v-if="loadError" class="detail-block">
    <p class="status-error" role="alert">{{ loadError }}</p>
  </div>

  <div v-else-if="isLoading" class="detail-block">
    <p class="status-muted">正在加载岗位详情...</p>
  </div>

  <template v-else-if="job">
    <div class="page-head">
      <div>
        <p class="eyebrow">求职者 / 推荐岗位 / 投递</p>
        <h2>{{ job.title }}</h2>
        <p class="body-copy">
          {{ job.company }} · {{ job.city }} · {{ job.salaryRange }} ·
          {{ getWorkModeLabel(job.workMode) }}
        </p>
      </div>
      <div class="page-actions">
        <RouterLink class="secondary-link" to="/jobs">返回推荐岗位</RouterLink>
        <RouterLink class="secondary-link" to="/jobseeker/support-needs">便利需求</RouterLink>
        <RouterLink class="toggle-button" to="/jobseeker/interview-assistance">面试辅助</RouterLink>
        <span class="score-chip">匹配分 {{ job.matchScore }}</span>
      </div>
    </div>

    <section class="detail-layout">
      <article class="detail-main">
        <div class="detail-block">
          <p class="eyebrow">岗位说明</p>
          <h3>职责与任务</h3>
          <ul class="detail-list">
            <li v-for="item in job.description" :key="item">{{ item }}</li>
          </ul>
        </div>

        <div class="detail-block">
          <p class="eyebrow">任职要求</p>
          <ul class="detail-list">
            <li v-for="item in job.requirements" :key="item">{{ item }}</li>
          </ul>
        </div>

        <div class="detail-block">
          <p class="eyebrow">办公环境与支持</p>
          <ul class="detail-list">
            <li v-for="item in job.environment" :key="item">{{ item }}</li>
          </ul>
        </div>

        <div class="detail-block">
          <p class="eyebrow">匹配解释</p>
          <div class="score-stack">
            <ScoreBar
              v-for="score in job.dimensionScores"
              :key="score.label"
              :label="getScoreDimensionLabel(score.label)"
              :value="score.value"
            />
          </div>
        </div>

        <div class="split-ledger">
          <div class="detail-block">
            <p class="eyebrow">推荐理由</p>
            <ul class="detail-list">
              <li v-for="reason in job.reasons" :key="reason">{{ reason }}</li>
            </ul>
          </div>
          <div class="detail-block">
            <p class="eyebrow">风险提示</p>
            <ul class="detail-list">
              <li v-for="risk in job.risks" :key="risk">{{ risk }}</li>
            </ul>
          </div>
        </div>
      </article>

      <aside class="detail-sidebar">
        <div class="detail-block">
          <p class="eyebrow">投递入口</p>
          <h3>{{ currentApplication ? '该岗位已投递' : '先确认沟通重点，再提交申请' }}</h3>
          <p class="body-copy">{{ job.applyHint }}</p>
        </div>

        <div class="detail-block support-preview-card">
          <p class="eyebrow">便利需求档案</p>
          <h3>{{ supportNeeds?.hasAnyNeed ? '已保存沟通与支持偏好' : '尚未保存具体便利需求' }}</h3>
          <p class="body-copy">
            授权范围：{{ supportScopeLabel }}<br />
            最近保存：{{ supportUpdatedAt }}
          </p>
          <ul v-if="supportSummaryPreview.length" class="detail-list">
            <li v-for="item in supportSummaryPreview" :key="item">{{ item }}</li>
          </ul>
          <p v-else class="body-copy detail-tip">
            当前还没有填写具体便利需求。你仍可投递岗位；如需企业提前做好配合，可先补充这部分信息。
          </p>
          <RouterLink class="secondary-link" to="/jobseeker/support-needs">去维护便利需求</RouterLink>
          <RouterLink class="secondary-link" to="/jobseeker/interview-assistance">打开面试辅助</RouterLink>
        </div>

        <div v-if="interviewCard" class="detail-block support-preview-card">
          <p class="eyebrow">面试沟通卡</p>
          <h3>{{ interviewCard.title }}</h3>
          <p class="body-copy">{{ interviewCard.subtitle }}</p>
          <ul class="detail-list">
            <li v-for="line in interviewCard.lines" :key="line">{{ line }}</li>
          </ul>
        </div>

        <p v-if="successMessage" class="success-banner" aria-live="polite">{{ successMessage }}</p>
        <p v-if="submitError" class="status-error" role="alert">{{ submitError }}</p>

        <template v-if="currentApplication">
          <div class="detail-block applied-card">
            <p class="eyebrow">当前状态</p>
            <h3>{{ getApplicationStatusLabel(currentApplication.status) }}</h3>
            <p class="body-copy">
              提交时间：{{ formatDateTime(currentApplication.submittedAt) }}<br />
              面试偏好：{{ getInterviewModeLabel(currentApplication.preferredInterviewMode) }}<br />
              授权范围：{{ getSupportVisibilityLabel(currentApplication.supportVisibility) }}
            </p>
            <div class="detail-copy">
              <strong>投递说明</strong>
              <p>{{ currentApplication.coverNote }}</p>
            </div>
            <div v-if="currentApplication.additionalSupport" class="detail-copy">
              <strong>便利需求摘要</strong>
              <p>{{ currentApplication.additionalSupport }}</p>
            </div>
            <RouterLink class="secondary-link" to="/applications">查看投递记录</RouterLink>
            <RouterLink class="secondary-link" to="/jobseeker/interview-assistance">打开面试辅助</RouterLink>
          </div>
        </template>

        <template v-else>
          <ErrorSummary :errors="submitted ? errors : []" />

          <form class="form-shell" novalidate @submit.prevent="submitApplication">
            <div class="field">
              <label class="field-label" for="cover-note">
                投递说明
                <span class="required-mark" aria-hidden="true">*</span>
              </label>
              <p class="field-hint" id="cover-note-hint">
                用 1 到 2 段话说明你的匹配点。
              </p>
              <textarea
                id="cover-note"
                v-model="form.coverNote"
                rows="5"
                class="field-control textarea-control"
                :aria-invalid="submitted && errors.some((error) => error.id === 'cover-note')"
                aria-describedby="cover-note-hint cover-note-error"
              />
              <p
                v-if="submitted && errors.some((error) => error.id === 'cover-note')"
                id="cover-note-error"
                class="field-error"
              >
                请至少填写 24 个字，说明你和岗位的匹配点。
              </p>
            </div>

            <fieldset class="fieldset">
              <legend class="field-label" id="preferred-interview-mode">
                首选面试方式
                <span class="required-mark" aria-hidden="true">*</span>
              </legend>
              <p class="field-hint">选择你希望优先采用的沟通方式。</p>
              <label
                v-for="option in interviewModeOptions"
                :key="option.value"
                class="choice-row"
                :for="`interview-mode-${option.value}`"
              >
                <input
                  :id="`interview-mode-${option.value}`"
                  v-model="form.preferredInterviewMode"
                  type="radio"
                  name="preferred-interview-mode"
                  :value="option.value"
                />
                <span>{{ option.label }}</span>
              </label>
              <p
                v-if="submitted && errors.some((error) => error.id === 'preferred-interview-mode')"
                class="field-error"
              >
                请选择一个首选面试方式。
              </p>
            </fieldset>

            <button type="submit" class="primary-button" :disabled="isSubmitting">
              {{ isSubmitting ? '提交中...' : '提交投递' }}
            </button>
          </form>
        </template>
      </aside>
    </section>
  </template>
</template>

<style scoped>
.status-error,
.status-muted {
  margin: 0;
}

.status-error {
  color: var(--error);
  font-weight: 700;
}

.status-muted {
  color: var(--muted);
}

.applied-card,
.support-preview-card {
  display: grid;
  gap: 14px;
}

.detail-copy {
  display: grid;
  gap: 8px;
}

.detail-copy strong {
  color: var(--heading);
}

.detail-copy p,
.detail-tip {
  margin: 0;
  line-height: 1.8;
  white-space: pre-line;
}
</style>

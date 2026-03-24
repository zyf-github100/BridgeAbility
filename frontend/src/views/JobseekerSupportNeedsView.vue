<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { RouterLink } from 'vue-router'

import {
  getCurrentSupportNeeds,
  saveCurrentSupportNeeds,
  type SupportNeeds,
} from '../api/jobseeker'
import { ApiError } from '../lib/http'
import {
  formatDateTime,
  getSupportVisibilityLabel,
  supportVisibilityOptions,
} from '../lib/jobseeker'
import { useAuthStore } from '../stores/auth'

interface SupportForm {
  supportVisibility: string
  textCommunicationPreferred: boolean
  subtitleNeeded: boolean
  remoteInterviewPreferred: boolean
  keyboardOnlyMode: boolean
  highContrastNeeded: boolean
  largeFontNeeded: boolean
  flexibleScheduleNeeded: boolean
  accessibleWorkspaceNeeded: boolean
  assistiveSoftwareNeeded: boolean
  remark: string
}

type SupportToggleKey = Exclude<keyof SupportForm, 'supportVisibility' | 'remark'>

const authStore = useAuthStore()

const supportFields: Array<{ key: SupportToggleKey; label: string; hint: string }> = [
  { key: 'textCommunicationPreferred', label: '优先文字沟通', hint: '适合先用文字确认信息，再进入语音或视频环节。' },
  { key: 'subtitleNeeded', label: '需要字幕支持', hint: '适用于线上会议、宣讲或面试时需要同步字幕或文字。' },
  { key: 'remoteInterviewPreferred', label: '更适合远程面试', hint: '优先选择线上流程，减少线下切换成本。' },
  { key: 'keyboardOnlyMode', label: '依赖纯键盘操作', hint: '流程、表单或测试环境需要兼容键盘完成。' },
  { key: 'highContrastNeeded', label: '需要高对比材料', hint: '文档、题目或说明尽量使用高对比度配色。' },
  { key: 'largeFontNeeded', label: '需要大字号材料', hint: '提前提供更易阅读的材料版本。' },
  { key: 'flexibleScheduleNeeded', label: '需要弹性时间', hint: '希望签到、笔试或沟通环节预留更灵活时间。' },
  { key: 'accessibleWorkspaceNeeded', label: '需要无障碍场地', hint: '线下面试或办公场所需确认通行与设施。' },
  { key: 'assistiveSoftwareNeeded', label: '可能需要辅助软件或设备', hint: '提前确认设备接口、读屏或其他辅助工具支持。' },
]

const isLoading = ref(false)
const isSaving = ref(false)
const loadError = ref('')
const saveError = ref('')
const saveSuccess = ref('')
const copyStatus = ref('')
const supportNeeds = ref<SupportNeeds | null>(null)

const supportForm = reactive<SupportForm>({
  supportVisibility: 'HIDDEN',
  textCommunicationPreferred: false,
  subtitleNeeded: false,
  remoteInterviewPreferred: false,
  keyboardOnlyMode: false,
  highContrastNeeded: false,
  largeFontNeeded: false,
  flexibleScheduleNeeded: false,
  accessibleWorkspaceNeeded: false,
  assistiveSoftwareNeeded: false,
  remark: '',
})

const supportSummaryPreview = computed(() => supportNeeds.value?.supportSummary ?? [])
const supportScopeLabel = computed(() =>
  getSupportVisibilityLabel(supportNeeds.value?.supportVisibility ?? supportForm.supportVisibility),
)
const supportUpdatedText = computed(() =>
  supportNeeds.value?.updatedAt ? formatDateTime(supportNeeds.value.updatedAt) : '尚未保存',
)
const interviewCard = computed(() => supportNeeds.value?.interviewCommunicationCard ?? null)

function applySupportForm(data: SupportNeeds) {
  supportForm.supportVisibility = data.supportVisibility || 'HIDDEN'
  supportForm.textCommunicationPreferred = data.textCommunicationPreferred
  supportForm.subtitleNeeded = data.subtitleNeeded
  supportForm.remoteInterviewPreferred = data.remoteInterviewPreferred
  supportForm.keyboardOnlyMode = data.keyboardOnlyMode
  supportForm.highContrastNeeded = data.highContrastNeeded
  supportForm.largeFontNeeded = data.largeFontNeeded
  supportForm.flexibleScheduleNeeded = data.flexibleScheduleNeeded
  supportForm.accessibleWorkspaceNeeded = data.accessibleWorkspaceNeeded
  supportForm.assistiveSoftwareNeeded = data.assistiveSoftwareNeeded
  supportForm.remark = data.remark ?? ''
}

function normalizeOptional(value: string) {
  const trimmed = value.trim()
  return trimmed || undefined
}

async function loadSupportNeeds() {
  if (!authStore.token) {
    return
  }

  isLoading.value = true
  loadError.value = ''

  try {
    const data = await getCurrentSupportNeeds(authStore.token)
    supportNeeds.value = data
    applySupportForm(data)
  } catch (error) {
    loadError.value = error instanceof ApiError ? error.message : '便利需求加载失败'
  } finally {
    isLoading.value = false
  }
}

async function saveSupportProfile() {
  if (!authStore.token) {
    return
  }

  isSaving.value = true
  saveError.value = ''
  saveSuccess.value = ''
  copyStatus.value = ''

  try {
    const saved = await saveCurrentSupportNeeds(authStore.token, {
      supportVisibility: supportForm.supportVisibility,
      textCommunicationPreferred: supportForm.textCommunicationPreferred,
      subtitleNeeded: supportForm.subtitleNeeded,
      remoteInterviewPreferred: supportForm.remoteInterviewPreferred,
      keyboardOnlyMode: supportForm.keyboardOnlyMode,
      highContrastNeeded: supportForm.highContrastNeeded,
      largeFontNeeded: supportForm.largeFontNeeded,
      flexibleScheduleNeeded: supportForm.flexibleScheduleNeeded,
      accessibleWorkspaceNeeded: supportForm.accessibleWorkspaceNeeded,
      assistiveSoftwareNeeded: supportForm.assistiveSoftwareNeeded,
      remark: normalizeOptional(supportForm.remark),
    })
    supportNeeds.value = saved
    applySupportForm(saved)
    saveSuccess.value = '便利需求档案已保存。'
  } catch (error) {
    saveError.value = error instanceof ApiError ? error.message : '便利需求保存失败'
  } finally {
    isSaving.value = false
  }
}

async function copyInterviewCard() {
  if (!interviewCard.value?.copyText) {
    return
  }

  try {
    await navigator.clipboard.writeText(interviewCard.value.copyText)
    copyStatus.value = '面试沟通卡已复制，可直接发送给企业或服务机构。'
  } catch {
    copyStatus.value = '当前环境不支持自动复制，请手动复制沟通卡内容。'
  }
}

onMounted(() => {
  void loadSupportNeeds()
})
</script>

<template>
  <div class="page-head">
    <div>
      <p class="eyebrow">求职者 / 便利需求</p>
      <h2>便利需求档案</h2>
    </div>
    <div class="page-actions">
      <RouterLink class="secondary-link" to="/jobseeker/profile">基础档案</RouterLink>
      <RouterLink class="secondary-link" to="/jobseeker/interview-assistance">面试辅助</RouterLink>
      <button
        type="submit"
        form="support-needs-form"
        class="primary-button"
        :disabled="isSaving || isLoading"
      >
        {{ isSaving ? '保存中...' : '保存便利需求' }}
      </button>
    </div>
  </div>

  <p v-if="loadError" class="status-error" role="alert">{{ loadError }}</p>
  <p v-if="saveSuccess" class="success-banner" aria-live="polite">{{ saveSuccess }}</p>
  <p v-if="saveError" class="status-error" role="alert">{{ saveError }}</p>
  <p v-if="copyStatus" class="status-muted" aria-live="polite">{{ copyStatus }}</p>

  <section class="content-columns support-columns">
    <article class="ledger-panel">
      <p class="eyebrow">需求设置</p>
      <h3>确定企业可见范围与沟通偏好</h3>

      <form id="support-needs-form" class="form-shell" novalidate @submit.prevent="saveSupportProfile">
        <fieldset class="fieldset">
          <legend class="field-label">企业可见范围</legend>
          <p class="field-hint">选择对企业展示的边界。</p>
          <label v-for="option in supportVisibilityOptions" :key="option.value" class="choice-row">
            <input v-model="supportForm.supportVisibility" type="radio" :value="option.value" name="support-visibility" />
            <span>{{ option.label }}</span>
          </label>
        </fieldset>

        <section class="option-grid">
          <label v-for="field in supportFields" :key="field.key" class="support-option">
            <div class="support-option-head">
              <input v-model="supportForm[field.key]" type="checkbox" />
              <span>{{ field.label }}</span>
            </div>
            <p class="field-hint">{{ field.hint }}</p>
          </label>
        </section>

        <div class="field">
          <label class="field-label" for="support-remark">补充说明</label>
          <textarea
            id="support-remark"
            v-model="supportForm.remark"
            rows="5"
            class="field-control textarea-control"
            placeholder="补充说明具体场景、沟通方式或资源要求。"
          />
        </div>
      </form>
    </article>

    <aside class="side-stack">
      <div class="detail-block support-preview-card">
        <p class="eyebrow">当前摘要</p>
        <h3>{{ supportNeeds?.hasAnyNeed ? '已生成需求摘要' : '尚未形成摘要内容' }}</h3>
        <p class="body-copy">
          展示范围：{{ supportScopeLabel }}<br />
          最近保存：{{ supportUpdatedText }}
        </p>
        <ul v-if="supportSummaryPreview.length" class="detail-list">
          <li v-for="item in supportSummaryPreview" :key="item">{{ item }}</li>
        </ul>
        <p v-else class="body-copy detail-tip">
          完善需求后，这里会显示对外沟通重点。
        </p>
      </div>

      <div class="detail-block support-preview-card">
        <p class="eyebrow">面试沟通卡</p>
        <h3>{{ interviewCard?.title ?? '面试沟通卡' }}</h3>
        <p class="body-copy">{{ interviewCard?.subtitle }}</p>
        <ul class="detail-list">
          <li v-for="line in interviewCard?.lines ?? []" :key="line">{{ line }}</li>
        </ul>
        <button type="button" class="toggle-button" @click="copyInterviewCard">复制沟通卡</button>
      </div>
    </aside>
  </section>
</template>

<style scoped>
.support-columns,
.side-stack,
.option-grid,
.support-option-head {
  display: grid;
}

.side-stack,
.option-grid {
  gap: 16px;
}

.option-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.support-option {
  display: grid;
  gap: 10px;
  padding: 16px;
  border: 1px solid var(--line);
  background: rgba(252, 252, 249, 0.92);
}

.support-option-head {
  grid-template-columns: auto minmax(0, 1fr);
  gap: 10px;
  align-items: start;
  font-weight: 700;
  color: var(--heading);
}

.support-preview-card {
  display: grid;
  gap: 12px;
}

.status-error,
.status-muted {
  margin: 0 0 16px;
}

.status-error {
  color: var(--error);
  font-weight: 700;
}

.status-muted {
  color: var(--muted);
}

@media (max-width: 960px) {
  .option-grid {
    grid-template-columns: 1fr;
  }
}
</style>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { RouterLink } from 'vue-router'

import {
  deleteEnterpriseVerificationMaterial,
  getEnterpriseProfile,
  saveEnterpriseProfile,
  uploadEnterpriseVerificationMaterial,
  type EnterpriseVerificationProfile,
} from '../api/enterprise'
import { ApiError } from '../lib/http'
import { useAuthStore } from '../stores/auth'

interface VerificationFormState {
  companyName: string
  industry: string
  city: string
  unifiedSocialCreditCode: string
  contactName: string
  contactPhone: string
  officeAddress: string
  accessibilityCommitment: string
}

interface LoadEnterpriseProfileOptions {
  syncForm?: boolean
}

const authStore = useAuthStore()

const enterpriseProfile = ref<EnterpriseVerificationProfile | null>(null)
const isProfileLoading = ref(false)
const isProfileSaving = ref(false)
const isMaterialUploading = ref(false)
const profileError = ref('')
const profileSuccess = ref('')
const materialType = ref('BUSINESS_LICENSE')
const materialNote = ref('')
const selectedMaterialFile = ref<File | null>(null)
const materialInputKey = ref(0)

const materialTypeOptions = [
  { value: 'BUSINESS_LICENSE', label: '营业执照' },
  { value: 'ACCESSIBILITY_POLICY', label: '无障碍承诺' },
  { value: 'LEGAL_REPRESENTATIVE_ID', label: '法人/经办人材料' },
  { value: 'OTHER', label: '其他资料' },
]

const verificationForm = reactive<VerificationFormState>(createEmptyVerificationForm())

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

const canPublishJobs = computed(() => enterpriseProfile.value?.canPublishJobs ?? false)

async function loadEnterpriseProfile(options: LoadEnterpriseProfileOptions = {}) {
  if (!authStore.token) {
    return
  }

  isProfileLoading.value = true
  profileError.value = ''

  try {
    const profile = await getEnterpriseProfile(authStore.token)
    enterpriseProfile.value = profile
    if (options.syncForm !== false) {
      fillVerificationForm(profile)
    }
  } catch (error) {
    profileError.value = error instanceof ApiError ? error.message : '企业认证资料加载失败'
  } finally {
    isProfileLoading.value = false
  }
}

async function refreshPage() {
  await loadEnterpriseProfile()
}

function fillVerificationForm(profile: EnterpriseVerificationProfile) {
  verificationForm.companyName = profile.companyName ?? ''
  verificationForm.industry = profile.industry ?? ''
  verificationForm.city = profile.city ?? ''
  verificationForm.unifiedSocialCreditCode = profile.unifiedSocialCreditCode ?? ''
  verificationForm.contactName = profile.contactName ?? ''
  verificationForm.contactPhone = profile.contactPhone ?? ''
  verificationForm.officeAddress = profile.officeAddress ?? ''
  verificationForm.accessibilityCommitment = profile.accessibilityCommitment ?? ''
}

async function saveVerificationProfile(submitForReview: boolean) {
  if (!authStore.token) {
    return
  }

  isProfileSaving.value = true
  profileError.value = ''
  profileSuccess.value = ''

  try {
    const profile = await saveEnterpriseProfile(authStore.token, {
      companyName: verificationForm.companyName.trim(),
      industry: verificationForm.industry.trim() || undefined,
      city: verificationForm.city.trim() || undefined,
      unifiedSocialCreditCode: verificationForm.unifiedSocialCreditCode.trim() || undefined,
      contactName: verificationForm.contactName.trim() || undefined,
      contactPhone: verificationForm.contactPhone.trim() || undefined,
      officeAddress: verificationForm.officeAddress.trim() || undefined,
      accessibilityCommitment: verificationForm.accessibilityCommitment.trim() || undefined,
      submitForReview,
    })
    enterpriseProfile.value = profile
    fillVerificationForm(profile)
    profileSuccess.value = submitForReview
      ? '企业认证资料已提交，等待管理员审核。'
      : '企业认证资料已保存为草稿。'
  } catch (error) {
    profileError.value = error instanceof ApiError ? error.message : '企业认证资料保存失败'
  } finally {
    isProfileSaving.value = false
  }
}

function onMaterialFileChange(event: Event) {
  const target = event.target as HTMLInputElement | null
  selectedMaterialFile.value = target?.files?.[0] ?? null
}

async function uploadMaterial() {
  if (!authStore.token || !selectedMaterialFile.value) {
    profileError.value = '请选择要上传的认证资料。'
    return
  }

  isMaterialUploading.value = true
  profileError.value = ''
  profileSuccess.value = ''

  try {
    await uploadEnterpriseVerificationMaterial(authStore.token, {
      materialType: materialType.value,
      note: materialNote.value.trim() || undefined,
      file: selectedMaterialFile.value,
    })
    await loadEnterpriseProfile({ syncForm: false })
    materialNote.value = ''
    selectedMaterialFile.value = null
    materialInputKey.value += 1
    profileSuccess.value = '认证材料上传成功。'
  } catch (error) {
    profileError.value = error instanceof ApiError ? error.message : '认证材料上传失败'
  } finally {
    isMaterialUploading.value = false
  }
}

async function removeMaterial(materialId: number) {
  if (!authStore.token) {
    return
  }

  profileError.value = ''
  profileSuccess.value = ''

  try {
    await deleteEnterpriseVerificationMaterial(authStore.token, materialId)
    await loadEnterpriseProfile({ syncForm: false })
    profileSuccess.value = '认证材料已移除。'
  } catch (error) {
    profileError.value = error instanceof ApiError ? error.message : '认证资料删除失败'
  }
}

async function downloadMaterial(materialId: number, fileName: string) {
  if (!authStore.token) {
    return
  }

  const response = await fetch(`/api/enterprise/profile/materials/${materialId}/download`, {
    method: 'GET',
    headers: {
      Authorization: `Bearer ${authStore.token}`,
    },
  })

  if (!response.ok) {
    profileError.value = `资料下载失败 (${response.status})`
    return
  }

  const blob = await response.blob()
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = fileName
  link.click()
  URL.revokeObjectURL(url)
}

function createEmptyVerificationForm(): VerificationFormState {
  return {
    companyName: '',
    industry: '',
    city: '',
    unifiedSocialCreditCode: '',
    contactName: '',
    contactPhone: '',
    officeAddress: '',
    accessibilityCommitment: '',
  }
}

onMounted(() => {
  refreshPage()
})
</script>

<template>
  <div class="page-head">
    <div>
      <p class="eyebrow">企业端 / 企业认证</p>
      <h2>企业认证</h2>
    </div>
    <div class="page-actions">
      <RouterLink to="/enterprise/jobs" class="secondary-link">岗位管理</RouterLink>
      <button type="button" class="primary-button" @click="refreshPage">刷新数据</button>
    </div>
  </div>

  <section class="content-columns verification-columns">
    <article class="ledger-panel">
      <div class="panel-headline">
        <p class="eyebrow">企业认证资料</p>
        <h3>基础信息</h3>
      </div>
      <p v-if="profileSuccess" class="success-banner" aria-live="polite">{{ profileSuccess }}</p>
      <p v-if="profileError" class="status-error" role="alert">{{ profileError }}</p>
      <p v-if="isProfileLoading" class="status-muted">正在加载企业认证资料...</p>

      <div class="form-grid">
        <label class="field-block">
          <span>企业名称</span>
          <input v-model="verificationForm.companyName" class="field-input" type="text" />
        </label>
        <label class="field-block">
          <span>行业</span>
          <input v-model="verificationForm.industry" class="field-input" type="text" />
        </label>
        <label class="field-block">
          <span>所在城市</span>
          <input v-model="verificationForm.city" class="field-input" type="text" />
        </label>
        <label class="field-block">
          <span>统一社会信用代码</span>
          <input v-model="verificationForm.unifiedSocialCreditCode" class="field-input" type="text" />
        </label>
        <label class="field-block">
          <span>联系人</span>
          <input v-model="verificationForm.contactName" class="field-input" type="text" />
        </label>
        <label class="field-block">
          <span>联系电话</span>
          <input v-model="verificationForm.contactPhone" class="field-input" type="text" />
        </label>
        <label class="field-block field-block-wide">
          <span>办公地址</span>
          <input v-model="verificationForm.officeAddress" class="field-input" type="text" />
        </label>
        <label class="field-block field-block-wide">
          <span>无障碍承诺</span>
          <textarea v-model="verificationForm.accessibilityCommitment" class="field-input field-textarea" />
        </label>
      </div>

      <div class="panel-headline panel-headline-tight">
        <p class="eyebrow">认证资料上传</p>
        <h3>认证材料</h3>
      </div>
      <div class="upload-row">
        <label class="field-block">
          <span>材料类型</span>
          <select v-model="materialType" class="field-input">
            <option v-for="option in materialTypeOptions" :key="option.value" :value="option.value">
              {{ option.label }}
            </option>
          </select>
        </label>
        <label class="field-block">
          <span>材料说明（选填）</span>
          <input v-model="materialNote" class="field-input" type="text" />
        </label>
        <label class="field-block field-block-wide">
          <span>选择文件</span>
          <input
            :key="materialInputKey"
            class="field-input"
            type="file"
            accept=".pdf,.doc,.docx,.jpg,.jpeg,.png,application/pdf,application/msword,application/vnd.openxmlformats-officedocument.wordprocessingml.document,image/jpeg,image/png"
            @change="onMaterialFileChange"
          />
        </label>
      </div>
      <p class="status-muted">支持 PDF、Word、JPG、PNG 格式，单个文件不超过 10 MB。</p>
      <div class="action-row">
        <button type="button" class="secondary-link" :disabled="isMaterialUploading" @click="uploadMaterial">
          {{ isMaterialUploading ? '上传中...' : '上传材料' }}
        </button>
        <button type="button" class="secondary-link" :disabled="isProfileSaving" @click="saveVerificationProfile(false)">
          {{ isProfileSaving ? '保存中...' : '保存草稿' }}
        </button>
        <button type="button" class="primary-button" :disabled="isProfileSaving" @click="saveVerificationProfile(true)">
          {{ isProfileSaving ? '提交中...' : '提交审核' }}
        </button>
      </div>
    </article>

    <article class="list-pane">
      <div class="panel-headline">
        <p class="eyebrow">认证状态</p>
        <h3>{{ enterpriseProfile?.companyName || '尚未填写企业认证资料' }}</h3>
      </div>
      <div class="detail-block">
        <p class="eyebrow">当前进度</p>
        <ul class="detail-list compact-list">
          <li>认证状态：{{ verificationStatusLabel }}</li>
          <li>可否发布岗位：{{ canPublishJobs ? '可以发布' : '仅可保存草稿' }}</li>
          <li>已发布岗位：{{ enterpriseProfile?.publishedJobCount ?? 0 }}</li>
          <li>已上传资料：{{ enterpriseProfile?.materials.length ?? 0 }}</li>
          <li v-if="enterpriseProfile?.submittedAt">提交时间：{{ enterpriseProfile.submittedAt }}</li>
          <li v-if="enterpriseProfile?.reviewedAt">审核时间：{{ enterpriseProfile.reviewedAt }}</li>
        </ul>
        <p v-if="enterpriseProfile?.reviewNote" class="detail-copy">{{ enterpriseProfile.reviewNote }}</p>
      </div>

      <div class="detail-block">
        <p class="eyebrow">资料列表</p>
        <div v-if="enterpriseProfile?.materials.length" class="material-list">
          <article v-for="material in enterpriseProfile.materials" :key="material.id" class="material-card">
            <div class="row-head">
              <h4>{{ material.originalFileName }}</h4>
              <span>{{ material.materialTypeLabel }}</span>
            </div>
            <p>{{ material.note || '无补充说明' }}</p>
            <div class="inline-tags">
              <span>{{ material.uploadedAt }}</span>
              <span>{{ Math.max(1, Math.round(material.fileSize / 1024)) }} KB</span>
            </div>
            <div class="action-row compact-actions">
              <button type="button" class="secondary-link" @click="downloadMaterial(material.id, material.originalFileName)">
                下载
              </button>
              <button type="button" class="secondary-link" @click="removeMaterial(material.id)">移除</button>
            </div>
          </article>
        </div>
        <p v-else class="status-muted">还没有上传任何企业认证资料。</p>
      </div>
    </article>
  </section>

  <section class="metric-strip">
    <div class="metric-cell">
      <span>认证状态</span>
      <strong>{{ verificationStatusLabel }}</strong>
    </div>
    <div class="metric-cell">
      <span>已发布岗位</span>
      <strong>{{ enterpriseProfile?.publishedJobCount ?? 0 }}</strong>
    </div>
    <div class="metric-cell">
      <span>资料数量</span>
      <strong>{{ enterpriseProfile?.materials.length ?? 0 }}</strong>
    </div>
    <div class="metric-cell">
      <span>下一步</span>
      <strong>{{ canPublishJobs ? '前往岗位管理' : '继续补充资料' }}</strong>
    </div>
  </section>
</template>

<style scoped>
.verification-columns,
.material-list,
.upload-row {
  display: grid;
}

.verification-columns,
.material-list,
.upload-row {
  gap: 16px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
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

.material-card {
  padding: 16px;
  border: 1px solid var(--line);
  background: rgba(255, 255, 255, 0.62);
}

.material-card p,
.detail-copy {
  margin: 10px 0 0;
  line-height: 1.7;
}

.compact-actions {
  margin-top: 12px;
}

.action-row {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 20px;
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
  .verification-columns,
  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>

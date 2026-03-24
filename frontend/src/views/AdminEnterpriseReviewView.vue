<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'

import {
  getAdminDashboard,
  getEnterpriseReviewDetail,
  getPendingEnterpriseReviews,
  reviewEnterprise,
  type EnterpriseReviewDetail,
  type EnterpriseReviewItem,
} from '../api/admin'
import { ApiError } from '../lib/http'
import { useAuthStore } from '../stores/auth'

const authStore = useAuthStore()

const reviewQueue = ref<EnterpriseReviewItem[]>([])
const selectedReview = ref<EnterpriseReviewDetail | null>(null)
const selectedReviewUserId = ref<number | null>(null)
const reviewNote = ref('')
const isLoading = ref(false)
const isDetailLoading = ref(false)
const isSubmitting = ref(false)
const loadError = ref('')
const detailError = ref('')
const actionError = ref('')
const actionSuccess = ref('')
const dashboardOpenAlertCount = ref(0)

const pendingReviewCount = computed(() => reviewQueue.value.length)
const materialCount = computed(() => selectedReview.value?.materials.length ?? 0)

async function refreshPage() {
  if (!authStore.token) {
    return
  }

  isLoading.value = true
  loadError.value = ''

  try {
    const [dashboardData, pendingData] = await Promise.all([
      getAdminDashboard(authStore.token),
      getPendingEnterpriseReviews(authStore.token),
    ])

    dashboardOpenAlertCount.value = dashboardData.openAlertCount
    reviewQueue.value = pendingData

    const nextSelectedId =
      selectedReviewUserId.value && pendingData.some((item) => item.userId === selectedReviewUserId.value)
        ? selectedReviewUserId.value
        : pendingData.find((item) => item.userId)?.userId ?? null

    selectedReviewUserId.value = nextSelectedId
    if (nextSelectedId) {
      await loadReviewDetail(nextSelectedId)
    } else {
      selectedReview.value = null
      reviewNote.value = ''
    }
  } catch (error) {
    loadError.value = error instanceof ApiError ? error.message : '企业审核页加载失败'
  } finally {
    isLoading.value = false
  }
}

async function loadReviewDetail(userId: number) {
  if (!authStore.token) {
    return
  }

  isDetailLoading.value = true
  detailError.value = ''
  actionError.value = ''
  actionSuccess.value = ''

  try {
    selectedReview.value = await getEnterpriseReviewDetail(authStore.token, userId)
    selectedReviewUserId.value = userId
    reviewNote.value = selectedReview.value.reviewNote ?? ''
  } catch (error) {
    selectedReview.value = null
    detailError.value = error instanceof ApiError ? error.message : '审核详情加载失败'
  } finally {
    isDetailLoading.value = false
  }
}

async function submitDecision(decision: 'APPROVED' | 'REJECTED') {
  if (!authStore.token || !selectedReview.value) {
    return
  }

  isSubmitting.value = true
  actionError.value = ''
  actionSuccess.value = ''

  try {
    const updated = await reviewEnterprise(authStore.token, selectedReview.value.userId, {
      decision,
      note: reviewNote.value.trim() || undefined,
    })
    selectedReview.value = updated
    actionSuccess.value = decision === 'APPROVED' ? '企业认证已通过，可发布岗位。' : '企业认证已驳回，等待企业补充后再次提交。'
    await refreshPage()
  } catch (error) {
    actionError.value = error instanceof ApiError ? error.message : '审核动作提交失败'
  } finally {
    isSubmitting.value = false
  }
}

async function downloadMaterial(materialId: number, fileName: string) {
  if (!authStore.token || !selectedReview.value) {
    return
  }

  const response = await fetch(
    `/api/admin/enterprises/${selectedReview.value.userId}/materials/${materialId}/download`,
    {
      method: 'GET',
      headers: {
        Authorization: `Bearer ${authStore.token}`,
      },
    },
  )

  if (!response.ok) {
    actionError.value = `资料下载失败 (${response.status})`
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

onMounted(() => {
  void refreshPage()
})
</script>

<template>
  <div class="page-head">
    <div>
      <p class="eyebrow">管理端 / 企业审核</p>
      <h2>企业审核</h2>
    </div>
    <div class="page-actions">
      <RouterLink class="secondary-link" to="/admin/logs">日志管理</RouterLink>
      <RouterLink class="secondary-link" to="/admin/stats">数据统计</RouterLink>
      <button type="button" class="primary-button" @click="refreshPage">刷新审核页</button>
    </div>
  </div>

  <p v-if="loadError" class="status-error" role="alert">{{ loadError }}</p>
  <p v-if="actionSuccess" class="success-banner" aria-live="polite">{{ actionSuccess }}</p>

  <section class="metric-strip">
    <div class="metric-cell">
      <span>待审企业</span>
      <strong>{{ pendingReviewCount }}</strong>
      <p>待进入审核详情处理的企业数量</p>
    </div>
    <div class="metric-cell">
      <span>已选企业资料数</span>
      <strong>{{ materialCount }}</strong>
      <p>当前选中企业已上传的审核材料数</p>
    </div>
    <div class="metric-cell">
      <span>活跃预警</span>
      <strong>{{ dashboardOpenAlertCount }}</strong>
      <p>可前往风险记录页查看待处理项</p>
    </div>
    <div class="metric-cell">
      <span>当前选中</span>
      <strong>{{ selectedReview?.companyName || '未选择' }}</strong>
      <p>从左侧名单查看企业资料与审核进度</p>
    </div>
  </section>

  <section class="content-columns admin-columns">
    <article class="ledger-panel">
      <div class="panel-headline">
        <p class="eyebrow">审核队列</p>
        <h3>待审清单</h3>
      </div>

      <p v-if="isLoading && !reviewQueue.length" class="status-muted">正在加载审核队列...</p>
      <div v-else-if="reviewQueue.length" class="review-list">
        <button
          v-for="item in reviewQueue"
          :key="`${item.company}-${item.userId ?? 'catalog'}`"
          type="button"
          class="review-card"
          :class="{ 'is-active': item.userId && item.userId === selectedReviewUserId }"
          @click="item.userId && loadReviewDetail(item.userId)"
        >
          <div class="row-head">
            <h4>{{ item.company }}</h4>
            <span class="status-chip">{{ item.status }}</span>
          </div>
          <p>{{ item.industry || '未填写行业' }} / {{ item.city || '未填写城市' }}</p>
          <p>{{ item.note }}</p>
          <div class="inline-meta">
            <span v-if="item.submittedAt">提交于 {{ item.submittedAt }}</span>
            <span v-if="item.materialCount">资料 {{ item.materialCount }} 份</span>
            <span v-if="!item.userId">静态回填数据</span>
          </div>
        </button>
      </div>
      <p v-else class="status-muted">当前没有待审核企业。</p>
    </article>

    <article class="ledger-panel review-detail">
      <div class="panel-headline">
        <p class="eyebrow">审核详情</p>
        <h3>{{ selectedReview ? selectedReview.companyName : '选择一条企业认证申请' }}</h3>
      </div>

      <p v-if="detailError" class="status-error" role="alert">{{ detailError }}</p>
      <p v-else-if="isDetailLoading" class="status-muted">正在加载审核详情...</p>

      <template v-else-if="selectedReview">
        <div class="detail-block">
          <p class="eyebrow">企业资料</p>
          <ul class="detail-list compact-list">
            <li>企业名称：{{ selectedReview.companyName }}</li>
            <li>行业：{{ selectedReview.industry || '未填写' }}</li>
            <li>城市：{{ selectedReview.city || '未填写' }}</li>
            <li>统一社会信用代码：{{ selectedReview.unifiedSocialCreditCode || '未填写' }}</li>
            <li>联系人：{{ selectedReview.contactName || '未填写' }}</li>
            <li>联系电话：{{ selectedReview.contactPhone || '未填写' }}</li>
            <li>办公地址：{{ selectedReview.officeAddress || '未填写' }}</li>
            <li>当前状态：{{ selectedReview.verificationStatus }}</li>
          </ul>
        </div>

        <div class="detail-block">
          <p class="eyebrow">无障碍承诺</p>
          <p class="detail-copy">{{ selectedReview.accessibilityCommitment || '未填写无障碍承诺。' }}</p>
        </div>

        <div class="detail-block">
          <p class="eyebrow">上传资料</p>
          <div v-if="selectedReview.materials.length" class="material-list">
            <article v-for="material in selectedReview.materials" :key="material.id" class="material-card">
              <div class="row-head">
                <h4>{{ material.originalFileName }}</h4>
                <span>{{ material.materialTypeLabel }}</span>
              </div>
              <p>{{ material.note || '无补充说明' }}</p>
              <div class="inline-meta">
                <span>{{ material.uploadedAt }}</span>
                <span>{{ Math.max(1, Math.round(material.fileSize / 1024)) }} KB</span>
              </div>
              <button
                type="button"
                class="secondary-link"
                @click="downloadMaterial(material.id, material.originalFileName)"
              >
                下载资料
              </button>
            </article>
          </div>
          <p v-else class="status-muted">该企业尚未上传认证资料。</p>
        </div>

        <div class="detail-block">
          <p class="eyebrow">审核动作</p>
          <p v-if="actionError" class="status-error" role="alert">{{ actionError }}</p>
          <label class="field-label" for="review-note">审核备注</label>
          <textarea
            id="review-note"
            v-model="reviewNote"
            rows="4"
            class="field-control textarea-control"
            placeholder="填写通过条件或驳回原因。"
          />
          <div class="action-row">
            <button type="button" class="primary-button" :disabled="isSubmitting" @click="submitDecision('APPROVED')">
              {{ isSubmitting ? '提交中...' : '审核通过' }}
            </button>
            <button type="button" class="secondary-link" :disabled="isSubmitting" @click="submitDecision('REJECTED')">
              {{ isSubmitting ? '提交中...' : '驳回并退回企业' }}
            </button>
          </div>
        </div>
      </template>

      <p v-else class="status-muted">选择一家待审核企业后，这里会显示完整资料和审核操作。</p>
    </article>
  </section>
</template>

<style scoped>
.admin-columns,
.review-list,
.material-list,
.review-detail {
  display: grid;
  gap: 16px;
}

.review-card,
.material-card {
  padding: 16px;
  border: 1px solid var(--line-strong);
  background: rgba(252, 252, 249, 0.92);
  text-align: left;
}

.review-card.is-active {
  border-color: var(--brand);
  background: rgba(37, 99, 235, 0.08);
}

.status-chip {
  display: inline-flex;
  align-items: center;
  padding: 4px 10px;
  border: 1px solid var(--line-strong);
  background: rgba(37, 99, 235, 0.08);
  color: var(--heading);
}

.inline-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 10px;
  color: var(--muted);
  font-size: 0.84rem;
}

.field-label {
  display: block;
  margin-bottom: 8px;
  font-weight: 700;
}

.field-control {
  width: 100%;
  border: 2px solid var(--line-strong);
  padding: 12px 14px;
  background: #ffffff;
  color: var(--body);
}

.textarea-control {
  resize: vertical;
}

.action-row {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 16px;
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

@media (max-width: 1100px) {
  .admin-columns {
    grid-template-columns: 1fr;
  }
}
</style>

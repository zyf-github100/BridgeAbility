<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'

import {
  getAdminMatchingConfig,
  resetAdminMatchingConfig,
  updateAdminMatchingConfig,
  type MatchingConfig,
} from '../api/admin'
import { ApiError } from '../lib/http'
import { useAuthStore } from '../stores/auth'

const authStore = useAuthStore()

const config = ref<MatchingConfig | null>(null)
const isLoading = ref(false)
const isSaving = ref(false)
const loadError = ref('')
const actionError = ref('')
const actionSuccess = ref('')

const form = reactive({
  scoreWeights: {
    skill: 34,
    workMode: 19,
    communication: 18,
    environment: 14,
    accommodation: 15,
  },
  risk: {
    penaltyPerRisk: 5,
    penaltyPerBlockingRisk: 12,
    maxPenalty: 40,
    hardFilteredMaxScore: 24,
  },
  candidateStage: {
    matchScoreWeight: 7,
    profileCompletionWeight: 3,
    priorityThreshold: 85,
    followUpThreshold: 68,
  },
})

const totalWeight = computed(
  () =>
    form.scoreWeights.skill +
    form.scoreWeights.workMode +
    form.scoreWeights.communication +
    form.scoreWeights.environment +
    form.scoreWeights.accommodation,
)

const sourceLabel = computed(() => (config.value?.customized ? '已使用后台自定义参数' : '当前使用 application.yml 默认参数'))

function applyConfig(next: MatchingConfig) {
  config.value = next
  form.scoreWeights.skill = next.scoreWeights.skill
  form.scoreWeights.workMode = next.scoreWeights.workMode
  form.scoreWeights.communication = next.scoreWeights.communication
  form.scoreWeights.environment = next.scoreWeights.environment
  form.scoreWeights.accommodation = next.scoreWeights.accommodation

  form.risk.penaltyPerRisk = next.risk.penaltyPerRisk
  form.risk.penaltyPerBlockingRisk = next.risk.penaltyPerBlockingRisk
  form.risk.maxPenalty = next.risk.maxPenalty
  form.risk.hardFilteredMaxScore = next.risk.hardFilteredMaxScore

  form.candidateStage.matchScoreWeight = next.candidateStage.matchScoreWeight
  form.candidateStage.profileCompletionWeight = next.candidateStage.profileCompletionWeight
  form.candidateStage.priorityThreshold = next.candidateStage.priorityThreshold
  form.candidateStage.followUpThreshold = next.candidateStage.followUpThreshold
}

async function loadConfig() {
  if (!authStore.token) {
    return
  }

  isLoading.value = true
  loadError.value = ''

  try {
    const next = await getAdminMatchingConfig(authStore.token)
    applyConfig(next)
  } catch (error) {
    loadError.value = error instanceof ApiError ? error.message : '匹配引擎配置加载失败'
  } finally {
    isLoading.value = false
  }
}

async function saveConfig() {
  if (!authStore.token) {
    return
  }
  if (totalWeight.value <= 0) {
    actionError.value = '评分权重总和必须大于 0。'
    actionSuccess.value = ''
    return
  }
  if (form.candidateStage.followUpThreshold > form.candidateStage.priorityThreshold) {
    actionError.value = '跟进阈值不能高于优先阈值。'
    actionSuccess.value = ''
    return
  }

  isSaving.value = true
  actionError.value = ''
  actionSuccess.value = ''

  try {
    const next = await updateAdminMatchingConfig(authStore.token, {
      scoreWeights: { ...form.scoreWeights },
      risk: { ...form.risk },
      candidateStage: { ...form.candidateStage },
    })
    applyConfig(next)
    actionSuccess.value = '匹配引擎参数已更新，新的推荐请求会立即使用这组配置。'
  } catch (error) {
    actionError.value = error instanceof ApiError ? error.message : '匹配引擎配置保存失败'
  } finally {
    isSaving.value = false
  }
}

async function resetConfig() {
  if (!authStore.token) {
    return
  }
  if (!window.confirm('确定恢复为默认配置吗？当前后台自定义参数会被清空。')) {
    return
  }

  isSaving.value = true
  actionError.value = ''
  actionSuccess.value = ''

  try {
    const next = await resetAdminMatchingConfig(authStore.token)
    applyConfig(next)
    actionSuccess.value = '匹配引擎已恢复为默认配置。'
  } catch (error) {
    actionError.value = error instanceof ApiError ? error.message : '恢复默认配置失败'
  } finally {
    isSaving.value = false
  }
}

onMounted(() => {
  void loadConfig()
})
</script>

<template>
  <div class="page-head">
    <div>
      <p class="eyebrow">管理端 / 匹配引擎</p>
      <h2>匹配引擎参数</h2>
      <p class="body-copy">在这里调整推荐权重、风险惩罚和候选人阶段阈值。保存后会立即影响新的推荐结果和企业候选人列表。</p>
    </div>
    <div class="page-actions">
      <button type="button" class="secondary-link" @click="resetConfig">恢复默认</button>
      <button type="button" class="primary-button" @click="loadConfig">刷新配置</button>
    </div>
  </div>

  <p v-if="loadError" class="status-error" role="alert">{{ loadError }}</p>
  <p v-if="actionSuccess" class="success-banner" aria-live="polite">{{ actionSuccess }}</p>
  <p v-if="actionError" class="status-error" role="alert">{{ actionError }}</p>
  <p v-else-if="isLoading && !config" class="status-muted">正在加载匹配引擎配置...</p>

  <section class="metric-strip">
    <div class="metric-cell">
      <span>权重总和</span>
      <strong>{{ totalWeight }}</strong>
      <p>系统会自动按总权重归一化计算，不要求必须等于 100。</p>
    </div>
    <div class="metric-cell">
      <span>硬冲突封顶分</span>
      <strong>{{ form.risk.hardFilteredMaxScore }}</strong>
      <p>命中硬过滤条件后，岗位最高不会超过这个分数。</p>
    </div>
    <div class="metric-cell">
      <span>优先阈值</span>
      <strong>{{ form.candidateStage.priorityThreshold }}</strong>
      <p>企业端候选人列表达到这个综合评分时进入“优先”。</p>
    </div>
    <div class="metric-cell">
      <span>当前来源</span>
      <strong>{{ config?.customized ? '后台自定义' : '默认配置' }}</strong>
      <p>{{ sourceLabel }}</p>
    </div>
  </section>

  <section class="content-columns engine-columns">
    <article class="ledger-panel">
      <div class="panel-headline">
        <div>
          <p class="eyebrow">评分权重</p>
          <h3>五维匹配分</h3>
        </div>
      </div>

      <p class="helper-copy">可优先微调“技能 / 工作方式 / 沟通支持”三组参数，再观察推荐排序变化。</p>

      <div class="form-shell">
        <label class="field">
          <span class="field-label">技能匹配</span>
          <input v-model.number="form.scoreWeights.skill" class="field-control" type="number" min="0" step="1" />
        </label>
        <label class="field">
          <span class="field-label">工作方式</span>
          <input v-model.number="form.scoreWeights.workMode" class="field-control" type="number" min="0" step="1" />
        </label>
        <label class="field">
          <span class="field-label">沟通支持</span>
          <input v-model.number="form.scoreWeights.communication" class="field-control" type="number" min="0" step="1" />
        </label>
        <label class="field">
          <span class="field-label">环境适配</span>
          <input v-model.number="form.scoreWeights.environment" class="field-control" type="number" min="0" step="1" />
        </label>
        <label class="field">
          <span class="field-label">便利支持</span>
          <input v-model.number="form.scoreWeights.accommodation" class="field-control" type="number" min="0" step="1" />
        </label>
      </div>
    </article>

    <article class="ledger-panel">
      <div class="panel-headline">
        <div>
          <p class="eyebrow">风险与阶段</p>
          <h3>惩罚项和候选人分层</h3>
        </div>
      </div>

      <div class="form-shell">
        <label class="field">
          <span class="field-label">普通风险扣分</span>
          <input v-model.number="form.risk.penaltyPerRisk" class="field-control" type="number" min="0" step="1" />
        </label>
        <label class="field">
          <span class="field-label">硬冲突扣分</span>
          <input v-model.number="form.risk.penaltyPerBlockingRisk" class="field-control" type="number" min="0" step="1" />
        </label>
        <label class="field">
          <span class="field-label">风险总扣分上限</span>
          <input v-model.number="form.risk.maxPenalty" class="field-control" type="number" min="0" step="1" />
        </label>
        <label class="field">
          <span class="field-label">硬冲突封顶分</span>
          <input v-model.number="form.risk.hardFilteredMaxScore" class="field-control" type="number" min="0" max="96" step="1" />
        </label>
        <label class="field">
          <span class="field-label">匹配分权重</span>
          <input v-model.number="form.candidateStage.matchScoreWeight" class="field-control" type="number" min="0" step="1" />
        </label>
        <label class="field">
          <span class="field-label">档案完成度权重</span>
          <input v-model.number="form.candidateStage.profileCompletionWeight" class="field-control" type="number" min="0" step="1" />
        </label>
        <label class="field">
          <span class="field-label">优先阈值</span>
          <input v-model.number="form.candidateStage.priorityThreshold" class="field-control" type="number" min="0" max="96" step="1" />
        </label>
        <label class="field">
          <span class="field-label">跟进阈值</span>
          <input v-model.number="form.candidateStage.followUpThreshold" class="field-control" type="number" min="0" max="96" step="1" />
        </label>
      </div>

      <div class="detail-block meta-block">
        <p class="eyebrow">元信息</p>
        <ul class="detail-list">
          <li>配置代码：{{ config?.code ?? 'default' }}</li>
          <li>最后更新人：{{ config?.updatedByUserId ?? '默认配置' }}</li>
          <li>最后更新时间：{{ config?.updatedAt || '尚未从后台保存过' }}</li>
        </ul>
      </div>

      <div class="action-row">
        <button type="button" class="primary-button" :disabled="isSaving" @click="saveConfig">
          {{ isSaving ? '保存中...' : '保存配置' }}
        </button>
      </div>
    </article>
  </section>
</template>

<style scoped>
.engine-columns,
.form-shell {
  display: grid;
  gap: 16px;
}

.helper-copy {
  margin: 0;
  line-height: 1.8;
  color: var(--muted);
}

.form-shell {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.field {
  display: grid;
  gap: 8px;
}

.field-label {
  color: var(--heading);
  font-weight: 700;
}

.field-control {
  width: 100%;
  min-height: 46px;
  padding: 0 14px;
  border: 1px solid var(--line-strong);
  background: #ffffff;
}

.meta-block {
  margin-top: 8px;
}

.action-row {
  display: flex;
  flex-wrap: wrap;
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

@media (max-width: 980px) {
  .form-shell {
    grid-template-columns: 1fr;
  }
}
</style>

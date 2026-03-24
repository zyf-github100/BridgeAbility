<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'

import {
  createAdminKnowledgeArticle,
  getAdminKnowledgeArticles,
  offlineAdminKnowledgeArticle,
  publishAdminKnowledgeArticle,
  updateAdminKnowledgeArticle,
  type AdminKnowledgeArticle,
} from '../api/knowledge'
import { ApiError } from '../lib/http'
import { useAuthStore } from '../stores/auth'

const authStore = useAuthStore()

const articles = ref<AdminKnowledgeArticle[]>([])
const selectedArticleId = ref<string | null>(null)
const isLoading = ref(false)
const isSaving = ref(false)
const isSwitchingStatus = ref(false)
const loadError = ref('')
const actionError = ref('')
const actionSuccess = ref('')

const form = reactive({
  title: '',
  category: '',
  summary: '',
  content: '',
  tagsText: '',
})

const selectedArticle = computed(
  () => articles.value.find((item) => item.id === selectedArticleId.value) ?? null,
)

const publishedCount = computed(
  () => articles.value.filter((item) => item.publishStatus === 'PUBLISHED').length,
)
const offlineCount = computed(
  () => articles.value.filter((item) => item.publishStatus === 'OFFLINE').length,
)
const latestUpdatedAt = computed(() => articles.value[0]?.updatedAt || '暂无更新')
function applyForm(article: AdminKnowledgeArticle | null) {
  form.title = article?.title ?? ''
  form.category = article?.category ?? ''
  form.summary = article?.summary ?? ''
  form.content = article?.content ?? ''
  form.tagsText = article?.tags.join('，') ?? ''
}

function resetForCreate() {
  selectedArticleId.value = null
  actionError.value = ''
  actionSuccess.value = ''
  applyForm(null)
}

function selectArticle(article: AdminKnowledgeArticle) {
  selectedArticleId.value = article.id
  actionError.value = ''
  actionSuccess.value = ''
  applyForm(article)
}

function upsertLocalArticle(article: AdminKnowledgeArticle) {
  const index = articles.value.findIndex((item) => item.id === article.id)
  if (index === -1) {
    articles.value = [article, ...articles.value]
  } else {
    const next = [...articles.value]
    next.splice(index, 1, article)
    articles.value = next
  }

  articles.value = [...articles.value].sort((left, right) => right.updatedAt.localeCompare(left.updatedAt))
  selectedArticleId.value = article.id
  applyForm(article)
}

function parseTags(input: string) {
  return input
    .split(/[\n,，、]/)
    .map((item) => item.trim())
    .filter(Boolean)
}

async function loadArticles() {
  if (!authStore.token) {
    return
  }

  isLoading.value = true
  loadError.value = ''

  try {
    const data = await getAdminKnowledgeArticles(authStore.token)
    articles.value = data

    if (selectedArticleId.value) {
      const current = data.find((item) => item.id === selectedArticleId.value)
      if (current) {
        applyForm(current)
      } else {
        resetForCreate()
      }
    } else if (data[0]) {
      selectArticle(data[0])
    }
  } catch (error) {
    loadError.value = error instanceof ApiError ? error.message : '知识库列表加载失败'
  } finally {
    isLoading.value = false
  }
}

async function saveArticle() {
  if (!authStore.token) {
    return
  }

  const currentArticle = selectedArticle.value
  const wasEditing = Boolean(currentArticle)
  isSaving.value = true
  actionError.value = ''
  actionSuccess.value = ''

  try {
    const payload = {
      title: form.title.trim(),
      category: form.category.trim(),
      summary: form.summary.trim(),
      content: form.content.trim(),
      tags: parseTags(form.tagsText),
    }

    const article = wasEditing && currentArticle
      ? await updateAdminKnowledgeArticle(authStore.token, currentArticle.id, payload)
      : await createAdminKnowledgeArticle(authStore.token, payload)

    upsertLocalArticle(article)
    actionSuccess.value = wasEditing ? '文章内容已更新。' : '新文章已创建，当前为下线状态。'
  } catch (error) {
    actionError.value = error instanceof ApiError ? error.message : '文章保存失败'
  } finally {
    isSaving.value = false
  }
}

async function switchPublishStatus(nextStatus: 'PUBLISHED' | 'OFFLINE') {
  if (!authStore.token || !selectedArticle.value) {
    return
  }

  isSwitchingStatus.value = true
  actionError.value = ''
  actionSuccess.value = ''

  try {
    const article =
      nextStatus === 'PUBLISHED'
        ? await publishAdminKnowledgeArticle(authStore.token, selectedArticle.value.id)
        : await offlineAdminKnowledgeArticle(authStore.token, selectedArticle.value.id)

    upsertLocalArticle(article)
    actionSuccess.value = nextStatus === 'PUBLISHED' ? '文章已发布。' : '文章已下线。'
  } catch (error) {
    actionError.value = error instanceof ApiError ? error.message : '文章状态更新失败'
  } finally {
    isSwitchingStatus.value = false
  }
}

function getStatusLabel(status: string) {
  return status === 'PUBLISHED' ? '已发布' : '已下线'
}

onMounted(() => {
  loadArticles()
})
</script>

<template>
  <div class="page-head">
    <div>
      <p class="eyebrow">政策内容管理</p>
      <h2>知识库管理</h2>
    </div>
    <div class="page-actions">
      <button type="button" class="secondary-link" @click="resetForCreate">新建文章</button>
      <button type="button" class="primary-button" @click="loadArticles">刷新列表</button>
    </div>
  </div>

  <p v-if="loadError" class="status-error" role="alert">{{ loadError }}</p>

  <section class="metric-strip">
    <div class="metric-cell">
      <span>文章总数</span>
      <strong>{{ articles.length }}</strong>
    </div>
    <div class="metric-cell">
      <span>已发布</span>
      <strong>{{ publishedCount }}</strong>
    </div>
    <div class="metric-cell">
      <span>已下线</span>
      <strong>{{ offlineCount }}</strong>
    </div>
    <div class="metric-cell">
      <span>最近更新</span>
      <strong>{{ selectedArticle ? selectedArticle.updatedAt : latestUpdatedAt }}</strong>
    </div>
  </section>

  <section class="content-columns knowledge-columns">
    <article class="ledger-panel">
      <div class="panel-headline">
        <div>
          <p class="eyebrow">文章列表</p>
          <h3>管理端内容清单</h3>
        </div>
      </div>

      <p v-if="isLoading && !articles.length" class="status-muted">正在加载文章...</p>
      <div v-else-if="articles.length" class="article-list">
        <button
          v-for="article in articles"
          :key="article.id"
          type="button"
          class="article-card"
          :class="{ 'is-active': article.id === selectedArticleId }"
          @click="selectArticle(article)"
        >
          <div class="row-head">
            <div>
              <p class="eyebrow">#{{ article.id }}</p>
              <h4>{{ article.title }}</h4>
            </div>
            <span class="status-chip" :class="article.publishStatus === 'PUBLISHED' ? 'is-published' : 'is-offline'">
              {{ getStatusLabel(article.publishStatus) }}
            </span>
          </div>
          <p class="detail-copy">{{ article.summary }}</p>
          <div class="inline-tags">
            <span>{{ article.category }}</span>
            <span v-for="tag in article.tags" :key="`${article.id}-${tag}`">{{ tag }}</span>
          </div>
          <div class="inline-meta">
            <span>更新于 {{ article.updatedAt || '暂无' }}</span>
            <span v-if="article.publishedAt">发布于 {{ article.publishedAt }}</span>
          </div>
        </button>
      </div>
      <p v-else class="status-muted">当前还没有文章，先创建一篇内容。</p>
    </article>

    <article class="ledger-panel editor-panel">
      <div class="panel-headline">
        <div>
          <p class="eyebrow">编辑区</p>
          <h3>{{ selectedArticle ? '编辑文章' : '新增文章' }}</h3>
        </div>
      </div>

      <p v-if="actionSuccess" class="success-banner" aria-live="polite">{{ actionSuccess }}</p>
      <p v-if="actionError" class="status-error" role="alert">{{ actionError }}</p>

      <div class="form-shell">
        <label class="field">
          <span class="field-label">文章标题</span>
          <input v-model="form.title" class="field-control" maxlength="255" placeholder="例如：用工政策更新说明" />
        </label>

        <label class="field">
          <span class="field-label">分类</span>
          <input v-model="form.category" class="field-control" maxlength="64" placeholder="例如：招聘支持" />
        </label>

        <label class="field">
          <span class="field-label">摘要</span>
          <textarea
            v-model="form.summary"
            class="field-control textarea-control"
            rows="3"
            maxlength="500"
            placeholder="给列表页和管理端看的简要说明"
          />
        </label>

        <label class="field">
          <span class="field-label">标签</span>
          <input
            v-model="form.tagsText"
            class="field-control"
            placeholder="多个标签用逗号、顿号或换行分隔"
          />
        </label>

        <label class="field">
          <span class="field-label">正文内容</span>
          <textarea
            v-model="form.content"
            class="field-control textarea-control content-input"
            rows="10"
            placeholder="输入完整政策内容、操作指引或模板正文"
          />
        </label>
      </div>

      <div class="action-row">
        <button type="button" class="primary-button" :disabled="isSaving" @click="saveArticle">
          {{ isSaving ? '保存中...' : selectedArticle ? '保存修改' : '创建文章' }}
        </button>
        <button
          v-if="selectedArticle && selectedArticle.publishStatus !== 'PUBLISHED'"
          type="button"
          class="secondary-link"
          :disabled="isSwitchingStatus"
          @click="switchPublishStatus('PUBLISHED')"
        >
          {{ isSwitchingStatus ? '处理中...' : '发布文章' }}
        </button>
        <button
          v-if="selectedArticle && selectedArticle.publishStatus === 'PUBLISHED'"
          type="button"
          class="secondary-link"
          :disabled="isSwitchingStatus"
          @click="switchPublishStatus('OFFLINE')"
        >
          {{ isSwitchingStatus ? '处理中...' : '下线文章' }}
        </button>
      </div>
    </article>
  </section>
</template>

<style scoped>
.knowledge-columns,
.article-list,
.editor-panel {
  display: grid;
}

.knowledge-columns {
  align-items: start;
}

.article-list,
.editor-panel {
  gap: 16px;
}

.article-card {
  display: grid;
  gap: 12px;
  padding: 16px;
  border: 1px solid var(--line-strong);
  background: rgba(252, 252, 249, 0.92);
  text-align: left;
}

.article-card.is-active {
  border-color: var(--brand);
  background: rgba(37, 99, 235, 0.08);
}

.status-chip {
  display: inline-flex;
  align-items: center;
  padding: 4px 10px;
  border: 1px solid var(--line-strong);
  font-size: 0.84rem;
  font-weight: 700;
}

.status-chip.is-published {
  background: rgba(36, 91, 74, 0.12);
  color: var(--success);
}

.status-chip.is-offline {
  background: rgba(183, 121, 31, 0.12);
  color: #8a5b10;
}

.detail-copy {
  margin: 0;
  line-height: 1.8;
  white-space: pre-line;
}

.inline-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  color: var(--muted);
  font-size: 0.84rem;
}

.content-input {
  min-height: 280px;
}

.action-row {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.success-banner,
.status-error,
.status-muted {
  margin: 0;
}
</style>

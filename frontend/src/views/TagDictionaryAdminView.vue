<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'

import {
  createAdminTag,
  getAdminTags,
  updateAdminTag,
  type TagDictionaryItem,
} from '../api/adminTags'
import { ApiError } from '../lib/http'
import { useAuthStore } from '../stores/auth'

const authStore = useAuthStore()

const tags = ref<TagDictionaryItem[]>([])
const selectedTagId = ref<number | null>(null)
const isLoading = ref(false)
const isSaving = ref(false)
const loadError = ref('')
const actionError = ref('')
const actionSuccess = ref('')

const form = reactive({
  tagCode: '',
  tagName: '',
  tagCategory: '',
  tagStatus: 'ACTIVE' as 'ACTIVE' | 'INACTIVE',
  description: '',
})

const selectedTag = computed(
  () => tags.value.find((item) => item.id === selectedTagId.value) ?? null,
)
const activeCount = computed(() => tags.value.filter((item) => item.tagStatus === 'ACTIVE').length)
const inactiveCount = computed(() => tags.value.filter((item) => item.tagStatus === 'INACTIVE').length)
const categoryCount = computed(() => new Set(tags.value.map((item) => item.tagCategory)).size)

function applyForm(tag: TagDictionaryItem | null) {
  form.tagCode = tag?.tagCode ?? ''
  form.tagName = tag?.tagName ?? ''
  form.tagCategory = tag?.tagCategory ?? ''
  form.tagStatus = tag?.tagStatus ?? 'ACTIVE'
  form.description = tag?.description ?? ''
}

function resetForCreate() {
  selectedTagId.value = null
  actionError.value = ''
  actionSuccess.value = ''
  applyForm(null)
}

function selectTag(tag: TagDictionaryItem) {
  selectedTagId.value = tag.id
  actionError.value = ''
  actionSuccess.value = ''
  applyForm(tag)
}

function upsertLocalTag(tag: TagDictionaryItem) {
  const index = tags.value.findIndex((item) => item.id === tag.id)
  if (index === -1) {
    tags.value = [tag, ...tags.value]
  } else {
    const next = [...tags.value]
    next.splice(index, 1, tag)
    tags.value = next
  }

  tags.value = [...tags.value].sort((left, right) => {
    if (left.tagStatus !== right.tagStatus) {
      return left.tagStatus === 'ACTIVE' ? -1 : 1
    }
    return left.tagCode.localeCompare(right.tagCode)
  })
  selectedTagId.value = tag.id
  applyForm(tag)
}

async function loadTags() {
  if (!authStore.token) {
    return
  }

  isLoading.value = true
  loadError.value = ''

  try {
    tags.value = await getAdminTags(authStore.token)
    if (selectedTagId.value) {
      const current = tags.value.find((item) => item.id === selectedTagId.value)
      if (current) {
        applyForm(current)
      } else {
        resetForCreate()
      }
    } else if (tags.value[0]) {
      selectTag(tags.value[0])
    }
  } catch (error) {
    loadError.value = error instanceof ApiError ? error.message : '标签字典加载失败'
  } finally {
    isLoading.value = false
  }
}

async function saveTag() {
  if (!authStore.token) {
    return
  }

  const isEditing = Boolean(selectedTag.value)
  const payload = {
    tagCode: form.tagCode.trim(),
    tagName: form.tagName.trim(),
    tagCategory: form.tagCategory.trim(),
    tagStatus: form.tagStatus,
    description: form.description.trim(),
  }

  isSaving.value = true
  actionError.value = ''
  actionSuccess.value = ''

  try {
    const tag = selectedTag.value
      ? await updateAdminTag(authStore.token, selectedTag.value.id, payload)
      : await createAdminTag(authStore.token, payload)
    upsertLocalTag(tag)
    actionSuccess.value = isEditing ? '标签已更新。' : '标签已创建。'
  } catch (error) {
    actionError.value = error instanceof ApiError ? error.message : '标签保存失败'
  } finally {
    isSaving.value = false
  }
}

onMounted(() => {
  loadTags()
})
</script>

<template>
  <div class="page-head">
    <div>
      <p class="eyebrow">管理端 / 标签字典</p>
      <h2>标签字典管理</h2>
    </div>
    <div class="page-actions">
      <button type="button" class="secondary-link" @click="resetForCreate">新建标签</button>
      <button type="button" class="primary-button" @click="loadTags">刷新列表</button>
    </div>
  </div>

  <p v-if="loadError" class="status-error" role="alert">{{ loadError }}</p>

  <section class="metric-strip">
    <div class="metric-cell">
      <span>标签总数</span>
      <strong>{{ tags.length }}</strong>
    </div>
    <div class="metric-cell">
      <span>启用中</span>
      <strong>{{ activeCount }}</strong>
    </div>
    <div class="metric-cell">
      <span>已停用</span>
      <strong>{{ inactiveCount }}</strong>
    </div>
    <div class="metric-cell">
      <span>分类数</span>
      <strong>{{ categoryCount }}</strong>
    </div>
  </section>

  <section class="content-columns tag-columns">
    <article class="ledger-panel">
      <div class="panel-headline">
        <div>
          <p class="eyebrow">字典列表</p>
          <h3>当前标签</h3>
        </div>
      </div>

      <p v-if="isLoading && !tags.length" class="status-muted">正在加载标签...</p>
      <div v-else-if="tags.length" class="tag-list">
        <button
          v-for="tag in tags"
          :key="tag.id"
          type="button"
          class="tag-card"
          :class="{ 'is-active': tag.id === selectedTagId }"
          @click="selectTag(tag)"
        >
          <div class="row-head">
            <div>
              <p class="eyebrow">{{ tag.tagCode }}</p>
              <h4>{{ tag.tagName }}</h4>
            </div>
            <span class="status-chip" :class="tag.tagStatus === 'ACTIVE' ? 'is-active-chip' : 'is-inactive-chip'">
              {{ tag.tagStatus === 'ACTIVE' ? '启用中' : '已停用' }}
            </span>
          </div>
          <p class="detail-copy">{{ tag.description || '暂无补充说明。' }}</p>
          <div class="inline-tags">
            <span>{{ tag.tagCategory }}</span>
            <span>{{ tag.updatedAt || '暂无更新时间' }}</span>
          </div>
        </button>
      </div>
      <p v-else class="status-muted">当前还没有标签，先创建一个字典项。</p>
    </article>

    <article class="ledger-panel editor-panel">
      <div class="panel-headline">
        <div>
          <p class="eyebrow">编辑区</p>
          <h3>{{ selectedTag ? '编辑标签' : '新建标签' }}</h3>
        </div>
      </div>

      <p v-if="actionSuccess" class="success-banner" aria-live="polite">{{ actionSuccess }}</p>
      <p v-if="actionError" class="status-error" role="alert">{{ actionError }}</p>

      <div class="form-shell">
        <label class="field">
          <span class="field-label">标签编码</span>
          <input v-model="form.tagCode" class="field-control" maxlength="64" placeholder="例如：text-first" />
        </label>

        <label class="field">
          <span class="field-label">标签名称</span>
          <input v-model="form.tagName" class="field-control" maxlength="64" placeholder="例如：文字沟通优先" />
        </label>

        <label class="field">
          <span class="field-label">标签分类</span>
          <input v-model="form.tagCategory" class="field-control" maxlength="32" placeholder="例如：ACCESSIBILITY" />
        </label>

        <label class="field">
          <span class="field-label">状态</span>
          <select v-model="form.tagStatus" class="field-control">
            <option value="ACTIVE">启用</option>
            <option value="INACTIVE">停用</option>
          </select>
        </label>

        <label class="field field-span">
          <span class="field-label">说明</span>
          <textarea
            v-model="form.description"
            class="field-control textarea-control"
            rows="5"
            maxlength="255"
            placeholder="补充标签适用场景、使用范围或管理说明"
          />
        </label>
      </div>

      <div class="action-row">
        <button type="button" class="primary-button" :disabled="isSaving" @click="saveTag">
          {{ isSaving ? '保存中...' : selectedTag ? '保存修改' : '创建标签' }}
        </button>
      </div>
    </article>
  </section>
</template>

<style scoped>
.tag-columns,
.tag-list,
.editor-panel {
  display: grid;
}

.tag-columns {
  align-items: start;
}

.tag-list,
.editor-panel {
  gap: 16px;
}

.tag-card {
  display: grid;
  gap: 12px;
  padding: 16px;
  border: 1px solid var(--line-strong);
  background: rgba(252, 252, 249, 0.92);
  text-align: left;
}

.tag-card.is-active {
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

.status-chip.is-active-chip {
  background: rgba(36, 91, 74, 0.12);
  color: var(--success);
}

.status-chip.is-inactive-chip {
  background: rgba(183, 121, 31, 0.12);
  color: #8a5b10;
}

.detail-copy {
  margin: 0;
  line-height: 1.8;
  white-space: pre-line;
}

.form-shell {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.field {
  display: grid;
  gap: 8px;
}

.field-span {
  grid-column: 1 / -1;
}

.field-control {
  width: 100%;
  min-height: 46px;
  padding: 0 14px;
  border: 1px solid var(--line-strong);
  background: #ffffff;
}

.textarea-control {
  min-height: 140px;
  padding: 14px;
  resize: vertical;
}

.action-row {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
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
  .form-shell {
    grid-template-columns: 1fr;
  }
}
</style>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'

import {
  getNotifications,
  markNotificationRead,
  publishSystemAnnouncement,
  type NotificationItem,
  type NotificationTargetRole,
} from '../api/notifications'
import { ApiError } from '../lib/http'
import { useAuthStore } from '../stores/auth'

type FilterMode = 'ALL' | 'UNREAD' | 'READ'
type RoleFilterMode = 'ALL_VIEW' | NotificationTargetRole

const authStore = useAuthStore()

const filterMode = ref<FilterMode>('ALL')
const roleFilterMode = ref<RoleFilterMode>('ALL_VIEW')
const isLoading = ref(false)
const isPublishing = ref(false)
const loadError = ref('')
const actionError = ref('')
const actionSuccess = ref('')
const notifications = ref<NotificationItem[]>([])
const readingIds = ref<string[]>([])

const announcementForm = reactive<{
  title: string
  content: string
  targetRole: NotificationTargetRole
}>({
  title: '',
  content: '',
  targetRole: 'ALL',
})

const isAdmin = computed(() => authStore.roles.includes('ROLE_ADMIN'))
const unreadCount = computed(() => notifications.value.filter((item) => !item.read).length)
const readCount = computed(() => notifications.value.filter((item) => item.read).length)
const latestNotification = computed(() => notifications.value[0] ?? null)

const filteredNotifications = computed(() => {
  return notifications.value.filter((item) => {
    if (filterMode.value === 'UNREAD' && item.read) {
      return false
    }
    if (filterMode.value === 'READ' && !item.read) {
      return false
    }
    if (roleFilterMode.value !== 'ALL_VIEW' && item.targetRole !== roleFilterMode.value) {
      return false
    }
    return true
  })
})

function isMarkingRead(notificationId: string) {
  return readingIds.value.includes(notificationId)
}

function getTypeLabel(type: string) {
  switch (type) {
    case 'INTERVIEW':
      return '面试进度'
    case 'ALERT':
      return '服务预警'
    case 'SYSTEM':
      return '系统通知'
    default:
      return type || '通知'
  }
}

function getTypeClass(type: string) {
  switch (type) {
    case 'INTERVIEW':
      return 'type-interview'
    case 'ALERT':
      return 'type-alert'
    default:
      return 'type-system'
  }
}

function getRoleLabel(role: RoleFilterMode) {
  switch (role) {
    case 'ALL_VIEW':
      return '全部角色'
    case 'ALL':
      return '全角色'
    case 'ROLE_JOBSEEKER':
      return '求职者'
    case 'ROLE_ENTERPRISE':
      return '企业'
    case 'ROLE_SERVICE_ORG':
      return '服务机构'
    case 'ROLE_ADMIN':
      return '管理端'
    default:
      return role
  }
}

function resetAnnouncementForm() {
  announcementForm.title = ''
  announcementForm.content = ''
  announcementForm.targetRole = 'ALL'
}

async function loadNotificationList() {
  if (!authStore.token) {
    return
  }

  isLoading.value = true
  loadError.value = ''

  try {
    notifications.value = await getNotifications(authStore.token)
  } catch (error) {
    loadError.value = error instanceof ApiError ? error.message : '通知列表加载失败'
  } finally {
    isLoading.value = false
  }
}

async function handleMarkRead(item: NotificationItem) {
  if (!authStore.token || item.read || isMarkingRead(item.id)) {
    return
  }

  readingIds.value = [...readingIds.value, item.id]
  actionError.value = ''
  actionSuccess.value = ''

  try {
    await markNotificationRead(authStore.token, item.id)
    notifications.value = notifications.value.map((notification) =>
      notification.id === item.id ? { ...notification, read: true } : notification,
    )
  } catch (error) {
    actionError.value = error instanceof ApiError ? error.message : '标记已读失败'
  } finally {
    readingIds.value = readingIds.value.filter((value) => value !== item.id)
  }
}

async function publishAnnouncement() {
  if (!authStore.token) {
    return
  }

  isPublishing.value = true
  actionError.value = ''
  actionSuccess.value = ''

  try {
    await publishSystemAnnouncement(authStore.token, {
      title: announcementForm.title.trim(),
      content: announcementForm.content.trim(),
      targetRole: announcementForm.targetRole,
    })
    resetAnnouncementForm()
    actionSuccess.value = '系统公告已发布。'
    await loadNotificationList()
  } catch (error) {
    actionError.value = error instanceof ApiError ? error.message : '系统公告发布失败'
  } finally {
    isPublishing.value = false
  }
}

onMounted(() => {
  loadNotificationList()
})
</script>

<template>
  <div class="page-head">
    <div>
      <p class="eyebrow">通知中心</p>
      <h2>通知中心</h2>
    </div>
    <div class="page-actions">
      <label class="inline-filter">
        <span>阅读状态</span>
        <select v-model="filterMode" class="inline-select">
          <option value="ALL">全部</option>
          <option value="UNREAD">未读</option>
          <option value="READ">已读</option>
        </select>
      </label>
      <label v-if="isAdmin" class="inline-filter">
        <span>目标角色</span>
        <select v-model="roleFilterMode" class="inline-select">
          <option value="ALL_VIEW">全部角色</option>
          <option value="ALL">全角色</option>
          <option value="ROLE_JOBSEEKER">求职者</option>
          <option value="ROLE_ENTERPRISE">企业</option>
          <option value="ROLE_SERVICE_ORG">服务机构</option>
          <option value="ROLE_ADMIN">管理端</option>
        </select>
      </label>
      <button type="button" class="primary-button" @click="loadNotificationList">刷新通知</button>
    </div>
  </div>

  <p v-if="actionSuccess" class="success-banner" aria-live="polite">{{ actionSuccess }}</p>
  <p v-if="actionError" class="status-error" role="alert">{{ actionError }}</p>

  <section class="metric-strip">
    <div class="metric-cell">
      <span>通知总数</span>
      <strong>{{ notifications.length }}</strong>
    </div>
    <div class="metric-cell">
      <span>未读通知</span>
      <strong>{{ unreadCount }}</strong>
    </div>
    <div class="metric-cell">
      <span>已读通知</span>
      <strong>{{ readCount }}</strong>
    </div>
    <div class="metric-cell">
      <span>最近一条</span>
      <strong>{{ latestNotification ? getTypeLabel(latestNotification.type) : '--' }}</strong>
      <p>{{ latestNotification?.createdAt || '暂无通知' }}</p>
    </div>
  </section>

  <section v-if="isAdmin" class="ledger-panel announce-panel">
    <div class="panel-headline">
      <div>
        <p class="eyebrow">系统公告</p>
        <h3>发布系统公告</h3>
      </div>
    </div>

    <div class="announce-grid">
      <label class="field">
        <span class="field-label">公告标题</span>
        <input
          v-model="announcementForm.title"
          class="field-control"
          maxlength="128"
          placeholder="例如：平台本周政策库更新"
        />
      </label>

      <label class="field">
        <span class="field-label">目标角色</span>
        <select v-model="announcementForm.targetRole" class="field-control">
          <option value="ALL">全角色</option>
          <option value="ROLE_JOBSEEKER">求职者</option>
          <option value="ROLE_ENTERPRISE">企业</option>
          <option value="ROLE_SERVICE_ORG">服务机构</option>
          <option value="ROLE_ADMIN">管理端</option>
        </select>
      </label>

      <label class="field announce-content">
        <span class="field-label">公告内容</span>
        <textarea
          v-model="announcementForm.content"
          class="field-control textarea-control"
          rows="4"
          maxlength="500"
          placeholder="输入要推送到通知中心的系统公告内容"
        />
      </label>
    </div>

    <div class="action-row">
      <button type="button" class="primary-button" :disabled="isPublishing" @click="publishAnnouncement">
        {{ isPublishing ? '发布中...' : '发布公告' }}
      </button>
    </div>
  </section>

  <article class="ledger-panel">
    <div class="panel-headline">
      <div>
        <p class="eyebrow">通知列表</p>
        <h3>{{ filterMode === 'ALL' ? '全部通知' : filterMode === 'UNREAD' ? '未读通知' : '已读通知' }}</h3>
      </div>
    </div>

    <p v-if="loadError" class="status-error" role="alert">{{ loadError }}</p>
    <p v-else-if="isLoading" class="status-muted">正在加载通知...</p>
    <div v-else-if="filteredNotifications.length" class="notification-list">
      <article
        v-for="item in filteredNotifications"
        :key="item.id"
        class="notification-card"
        :class="{ 'is-read': item.read }"
      >
        <div class="row-head">
          <div>
            <p class="eyebrow">#{{ item.id }}</p>
            <h4>{{ item.title }}</h4>
          </div>
          <div class="notification-tags">
            <span class="notification-type" :class="getTypeClass(item.type)">{{ getTypeLabel(item.type) }}</span>
            <span class="notification-state">{{ item.read ? '已读' : '未读' }}</span>
            <span v-if="isAdmin" class="notification-role">{{ getRoleLabel(item.targetRole) }}</span>
          </div>
        </div>
        <p class="detail-copy">{{ item.content }}</p>
        <div class="card-footer">
          <div class="inline-tags">
            <span>{{ item.createdAt }}</span>
            <span>{{ isAdmin ? `面向 ${getRoleLabel(item.targetRole)}` : item.read ? '已归档' : '待处理' }}</span>
          </div>
          <button
            v-if="!item.read"
            type="button"
            class="secondary-link"
            :disabled="isMarkingRead(item.id)"
            @click="handleMarkRead(item)"
          >
            {{ isMarkingRead(item.id) ? '处理中...' : '标记已读' }}
          </button>
        </div>
      </article>
    </div>
    <p v-else class="status-muted">当前筛选条件下没有通知。</p>
  </article>
</template>

<style scoped>
.announce-panel,
.announce-grid,
.notification-list {
  display: grid;
}

.announce-panel,
.notification-list {
  gap: 16px;
}

.announce-panel {
  margin-bottom: 20px;
}

.announce-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.announce-content {
  grid-column: 1 / -1;
}

.notification-card {
  display: grid;
  gap: 14px;
  padding: 18px;
  border: 1px solid var(--line);
  background: rgba(255, 255, 255, 0.74);
}

.notification-card.is-read {
  opacity: 0.82;
}

.notification-tags {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  align-items: start;
}

.notification-type,
.notification-state,
.notification-role {
  display: inline-flex;
  align-items: center;
  min-height: 32px;
  padding: 0 12px;
  border: 1px solid var(--line-strong);
  background: rgba(47, 111, 237, 0.08);
  color: var(--brand);
  font-size: 0.9rem;
  font-weight: 700;
}

.notification-role {
  background: rgba(29, 42, 68, 0.06);
  color: var(--heading);
}

.type-interview {
  background: rgba(47, 111, 237, 0.12);
  color: var(--brand);
}

.type-alert {
  background: rgba(183, 121, 31, 0.12);
  color: #8a5b10;
}

.type-system {
  background: rgba(36, 91, 74, 0.12);
  color: var(--success);
}

.inline-filter {
  display: inline-flex;
  gap: 10px;
  align-items: center;
}

.inline-select {
  min-height: 40px;
  padding: 0 12px;
  border: 1px solid var(--line-strong);
  background: #fff;
}

.detail-copy {
  margin: 0;
  line-height: 1.8;
  white-space: pre-line;
}

.action-row,
.card-footer {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  justify-content: space-between;
  align-items: center;
}

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

@media (max-width: 880px) {
  .announce-grid {
    grid-template-columns: 1fr;
  }

  .card-footer {
    align-items: start;
  }
}
</style>

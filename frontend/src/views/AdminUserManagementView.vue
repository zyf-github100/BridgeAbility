<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { RouterLink } from 'vue-router'

import { getAdminUsers, type AdminUserSummary } from '../api/admin'
import { ApiError } from '../lib/http'
import { useAuthStore } from '../stores/auth'

const authStore = useAuthStore()

const users = ref<AdminUserSummary[]>([])
const selectedUserId = ref<number | null>(null)
const keyword = ref('')
const roleFilter = ref('ALL')
const statusFilter = ref('ALL')
const isLoading = ref(false)
const loadError = ref('')

const roleOptions = [
  { value: 'ALL', label: '全部角色' },
  { value: 'ROLE_JOBSEEKER', label: '求职者' },
  { value: 'ROLE_ENTERPRISE', label: '企业' },
  { value: 'ROLE_SERVICE_ORG', label: '服务机构' },
  { value: 'ROLE_ADMIN', label: '管理端' },
]

const filteredUsers = computed(() => {
  const text = keyword.value.trim().toLowerCase()
  return users.value.filter((user) => {
    if (roleFilter.value !== 'ALL' && !user.roles.includes(roleFilter.value)) {
      return false
    }
    if (statusFilter.value !== 'ALL' && user.statusLabel !== statusFilter.value) {
      return false
    }
    if (!text) {
      return true
    }
    const haystack = [user.account, user.nickname, user.email, user.phone, ...user.roles]
      .filter(Boolean)
      .join(' ')
      .toLowerCase()
    return haystack.includes(text)
  })
})

const selectedUser = computed(
  () => filteredUsers.value.find((user) => user.userId === selectedUserId.value) ?? filteredUsers.value[0] ?? null,
)
const activeCount = computed(() => users.value.filter((item) => item.statusLabel === 'ACTIVE').length)
const adminCount = computed(() => users.value.filter((item) => item.roles.includes('ROLE_ADMIN')).length)
const enterpriseCount = computed(() => users.value.filter((item) => item.roles.includes('ROLE_ENTERPRISE')).length)
const incompleteContactCount = computed(
  () => users.value.filter((item) => !item.email && !item.phone).length,
)

function getRoleLabel(role: string) {
  switch (role) {
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

async function loadUsers() {
  if (!authStore.token) {
    return
  }

  isLoading.value = true
  loadError.value = ''
  try {
    users.value = await getAdminUsers(authStore.token)
    if (!selectedUserId.value && users.value.length) {
      selectedUserId.value = users.value[0].userId
    }
  } catch (error) {
    loadError.value = error instanceof ApiError ? error.message : '用户列表加载失败'
  } finally {
    isLoading.value = false
  }
}

watch(filteredUsers, (value) => {
  if (!value.length) {
    selectedUserId.value = null
    return
  }
  if (!value.some((item) => item.userId === selectedUserId.value)) {
    selectedUserId.value = value[0].userId
  }
})

onMounted(() => {
  void loadUsers()
})
</script>

<template>
  <div class="page-head">
    <div>
      <p class="eyebrow">管理端 / 用户管理</p>
      <h2>用户管理</h2>
    </div>
    <div class="page-actions">
      <RouterLink class="secondary-link" to="/admin">返回管理首页</RouterLink>
      <button type="button" class="primary-button" @click="loadUsers">刷新用户列表</button>
    </div>
  </div>

  <p v-if="loadError" class="status-error" role="alert">{{ loadError }}</p>

  <section class="metric-strip">
    <div class="metric-cell">
      <span>账号总数</span>
      <strong>{{ users.length }}</strong>
      <p>已纳入管理端的全部账号</p>
    </div>
    <div class="metric-cell">
      <span>活跃账号</span>
      <strong>{{ activeCount }}</strong>
      <p>状态正常，可继续登录使用</p>
    </div>
    <div class="metric-cell">
      <span>企业账号</span>
      <strong>{{ enterpriseCount }}</strong>
      <p>可进入企业首页与岗位管理</p>
    </div>
    <div class="metric-cell">
      <span>联系信息缺口</span>
      <strong>{{ incompleteContactCount }}</strong>
      <p>未填写邮箱和手机号的账号</p>
    </div>
  </section>

  <div class="filter-strip user-filter">
    <label class="inline-filter">
      <span>搜索</span>
      <input v-model="keyword" class="inline-input" type="text" placeholder="账号 / 昵称 / 邮箱 / 手机" />
    </label>
    <label class="inline-filter">
      <span>角色</span>
      <select v-model="roleFilter" class="inline-select">
        <option v-for="option in roleOptions" :key="option.value" :value="option.value">{{ option.label }}</option>
      </select>
    </label>
    <label class="inline-filter">
      <span>状态</span>
      <select v-model="statusFilter" class="inline-select">
        <option value="ALL">全部状态</option>
        <option value="ACTIVE">ACTIVE</option>
        <option value="DISABLED">DISABLED</option>
      </select>
    </label>
    <span>筛选结果：{{ filteredUsers.length }}</span>
    <span>管理员：{{ adminCount }}</span>
  </div>

  <section class="master-detail">
    <article class="list-pane">
      <div class="panel-headline">
        <div>
          <p class="eyebrow">账号列表</p>
          <h3>按创建时间倒序查看账号</h3>
        </div>
      </div>

      <p v-if="isLoading" class="status-muted">正在加载用户数据...</p>
      <p v-else-if="!filteredUsers.length" class="status-muted">当前筛选条件下没有匹配的账号。</p>

      <button
        v-for="user in filteredUsers"
        :key="user.userId"
        type="button"
        class="select-row user-row"
        :class="{ 'is-selected': selectedUser?.userId === user.userId }"
        @click="selectedUserId = user.userId"
      >
        <div class="select-score">
          <span>状态</span>
          <strong>{{ user.statusLabel }}</strong>
        </div>
        <div class="select-copy">
          <div class="row-head">
            <h4>{{ user.nickname || user.account }}</h4>
            <span>{{ user.account }}</span>
          </div>
          <div class="inline-tags">
            <span v-for="role in user.roles" :key="role">{{ getRoleLabel(role) }}</span>
          </div>
          <p>{{ user.email || '未填写邮箱' }} / {{ user.phone || '未填写手机' }}</p>
        </div>
      </button>
    </article>

    <article class="detail-pane user-detail">
      <template v-if="selectedUser">
        <div class="panel-headline">
          <div>
            <p class="eyebrow">账号详情</p>
            <h3>{{ selectedUser.nickname || selectedUser.account }}</h3>
          </div>
        </div>

        <div class="detail-block">
          <p class="eyebrow">基础信息</p>
          <ul class="detail-list">
            <li>账号：{{ selectedUser.account }}</li>
            <li>邮箱：{{ selectedUser.email || '未填写' }}</li>
            <li>手机号：{{ selectedUser.phone || '未填写' }}</li>
            <li>状态：{{ selectedUser.statusLabel }}</li>
            <li>创建时间：{{ selectedUser.createdAt }}</li>
            <li>最近登录：{{ selectedUser.lastLoginAt || '暂无记录' }}</li>
          </ul>
        </div>

        <div class="detail-block">
          <p class="eyebrow">角色权限</p>
          <div class="inline-tags">
            <span v-for="role in selectedUser.roles" :key="role">{{ getRoleLabel(role) }}</span>
          </div>
          <p class="body-copy">
            {{ selectedUser.roles.includes('ROLE_ADMIN') ? '该账号具备管理端访问权限。' : '该账号不具备管理端访问权限。' }}
          </p>
        </div>

        <div class="detail-block">
          <p class="eyebrow">管理动作</p>
          <div class="action-row">
            <RouterLink class="secondary-link" to="/admin/notifications">通知中心</RouterLink>
            <RouterLink class="secondary-link" to="/admin/issues">申诉纠错</RouterLink>
            <RouterLink class="secondary-link" to="/admin/risk-records">风险记录</RouterLink>
          </div>
        </div>

        <div class="detail-block">
          <p class="eyebrow">账号提示</p>
          <ul class="detail-list">
            <li v-if="!selectedUser.email && !selectedUser.phone">该账号缺少联系信息，后续找回密码和消息触达会受限。</li>
            <li v-if="selectedUser.roles.length > 1">该账号同时拥有多个角色，跨端权限治理时需要重点关注。</li>
            <li v-if="selectedUser.statusLabel !== 'ACTIVE'">该账号当前不是活跃状态，登录与业务访问可能受限。</li>
            <li v-if="selectedUser.email || selectedUser.phone">联系信息已具备基本完整度，可支持通知和账号恢复。</li>
          </ul>
        </div>
      </template>

      <p v-else class="status-muted">请选择一个账号查看详情。</p>
    </article>
  </section>
</template>

<style scoped>
.user-filter {
  align-items: center;
}

.inline-filter {
  display: inline-flex;
  gap: 10px;
  align-items: center;
}

.inline-input,
.inline-select {
  min-height: 40px;
  padding: 0 12px;
  border: 1px solid var(--line-strong);
  background: #fff;
}

.user-row,
.user-detail {
  display: grid;
}

.user-row {
  grid-template-columns: 112px minmax(0, 1fr);
  gap: 18px;
  align-items: start;
}

.user-row .select-score {
  display: grid;
  gap: 8px;
  align-content: start;
  min-width: 0;
}

.user-row .select-score strong {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 40px;
  padding: 0 10px;
  border: 1px solid var(--line-strong);
  background: var(--surface-strong);
  font-family: var(--mono);
  font-size: 0.84rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  line-height: 1.2;
  text-transform: uppercase;
}

.user-row .select-copy {
  display: grid;
  gap: 10px;
}

.user-row .select-copy p {
  margin: 0;
}

.user-detail {
  gap: 16px;
}

@media (max-width: 760px) {
  .user-row {
    grid-template-columns: 1fr;
  }

  .user-row .select-score strong {
    justify-content: flex-start;
  }
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
</style>

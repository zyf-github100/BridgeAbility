<script setup lang="ts">
import { computed, onMounted, watch } from 'vue'
import { RouterLink, RouterView, useRoute, useRouter } from 'vue-router'

import { useAccessibilityStore } from './stores/accessibility'
import { useAuthStore } from './stores/auth'

type SectionKey = 'home' | 'jobseeker' | 'enterprise' | 'service' | 'admin'
type RoleCode = 'ROLE_JOBSEEKER' | 'ROLE_ENTERPRISE' | 'ROLE_SERVICE_ORG' | 'ROLE_ADMIN'

interface NavItem {
  label: string
  to: string
  section: SectionKey
  matchPrefix?: boolean
}

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const accessibilityStore = useAccessibilityStore()

const isAuthLayout = computed(() => route.meta.layout === 'auth')

const publicTopNav: NavItem[] = [
  { label: '首页', to: '/overview', section: 'home' },
  { label: '帮助中心', to: '/help-center', section: 'home' },
  { label: '无障碍设置', to: '/accessibility', section: 'home' },
]

const roleEntryNav: NavItem[] = [
  { label: '求职入口', to: '/workspace', section: 'jobseeker' },
  { label: '企业入口', to: '/enterprise', section: 'enterprise' },
  { label: '服务入口', to: '/service', section: 'service' },
  { label: '管理入口', to: '/admin', section: 'admin' },
]

const sideMenus: Record<Exclude<SectionKey, 'home'>, NavItem[]> = {
  jobseeker: [
    { label: '首页', to: '/workspace', section: 'jobseeker' },
    { label: '基础档案', to: '/jobseeker/profile', section: 'jobseeker' },
    { label: '技能与经历', to: '/jobseeker/ability', section: 'jobseeker' },
    { label: '便利需求', to: '/jobseeker/support-needs', section: 'jobseeker' },
    { label: '简历预览', to: '/jobseeker/resume-preview', section: 'jobseeker' },
    { label: '推荐岗位', to: '/jobs', section: 'jobseeker', matchPrefix: true },
    { label: '投递记录', to: '/applications', section: 'jobseeker' },
    { label: '面试辅助', to: '/jobseeker/interview-assistance', section: 'jobseeker' },
    { label: '入职反馈', to: '/jobseeker/onboarding-feedback', section: 'jobseeker' },
    { label: '服务支持记录', to: '/jobseeker/service-records', section: 'jobseeker' },
    { label: '通知中心', to: '/jobseeker/notifications', section: 'jobseeker' },
  ],
  enterprise: [
    { label: '企业首页', to: '/enterprise', section: 'enterprise' },
    { label: '企业认证', to: '/enterprise/verification', section: 'enterprise' },
    { label: '岗位管理', to: '/enterprise/jobs', section: 'enterprise', matchPrefix: true },
    { label: '候选人列表', to: '/enterprise/candidates', section: 'enterprise', matchPrefix: true },
    { label: '面试管理', to: '/enterprise/interviews', section: 'enterprise', matchPrefix: true },
    { label: '招聘统计', to: '/enterprise/stats', section: 'enterprise' },
    { label: '通知中心', to: '/enterprise/notifications', section: 'enterprise' },
  ],
  service: [
    { label: '首页', to: '/service', section: 'service' },
    { label: '服务对象列表', to: '/service/cases', section: 'service', matchPrefix: true },
    { label: '预警处理', to: '/service/alerts', section: 'service' },
    { label: '通知中心', to: '/service/notifications', section: 'service' },
  ],
  admin: [
    { label: '管理首页', to: '/admin', section: 'admin' },
    { label: '企业审核', to: '/admin/reviews', section: 'admin', matchPrefix: true },
    { label: '用户管理', to: '/admin/users', section: 'admin' },
    { label: '匹配引擎', to: '/admin/matching-engine', section: 'admin' },
    { label: '标签字典', to: '/admin/tags', section: 'admin' },
    { label: '内容管理', to: '/admin/knowledge', section: 'admin' },
    { label: '日志管理', to: '/admin/logs', section: 'admin', matchPrefix: true },
    { label: '数据统计', to: '/admin/stats', section: 'admin', matchPrefix: true },
    { label: '风险记录', to: '/admin/risk-records', section: 'admin' },
    { label: '申诉纠错', to: '/admin/issues', section: 'admin' },
    { label: '通知中心', to: '/admin/notifications', section: 'admin' },
  ],
}

const sectionLabels: Record<SectionKey, string> = {
  home: '首页',
  jobseeker: '求职者',
  enterprise: '企业',
  service: '服务机构',
  admin: '管理端',
}

const sectionRoles: Record<SectionKey, RoleCode[]> = {
  home: [],
  jobseeker: ['ROLE_JOBSEEKER'],
  enterprise: ['ROLE_ENTERPRISE'],
  service: ['ROLE_SERVICE_ORG'],
  admin: ['ROLE_ADMIN'],
}

const currentSection = computed<SectionKey>(() => {
  const section = route.meta.section
  if (
    section === 'home' ||
    section === 'jobseeker' ||
    section === 'enterprise' ||
    section === 'service' ||
    section === 'admin'
  ) {
    return section
  }
  return 'home'
})

const currentTitle = computed(() => String(route.meta.title ?? sectionLabels[currentSection.value]))
const currentMenu = computed<NavItem[]>(() => {
  if (currentSection.value === 'home') {
    return []
  }
  return sideMenus[currentSection.value]
})

const headerNavItems = computed<NavItem[]>(() => {
  if (currentSection.value !== 'home') {
    return currentMenu.value
  }

  if (!authStore.isAuthenticated) {
    return publicTopNav
  }

  return [
    ...publicTopNav,
    ...roleEntryNav.filter((item) =>
      sectionRoles[item.section].some((role) => authStore.roles.includes(role)),
    ),
  ]
})

const breadcrumbs = computed(() => {
  if (currentSection.value === 'home') {
    if (route.path === '/overview') {
      return ['BridgeAbility', '首页']
    }
    return ['BridgeAbility', currentTitle.value]
  }
  return ['BridgeAbility', sectionLabels[currentSection.value], currentTitle.value]
})

const userSummary = computed(() => authStore.nickname || authStore.account || '未登录')

function isNavActive(item: NavItem) {
  if (route.path === item.to) {
    return true
  }
  if (item.matchPrefix && route.path.startsWith(`${item.to}/`)) {
    return true
  }
  return false
}

async function handleLogout() {
  await authStore.logoutCurrentUser()
  await router.push('/login')
}

onMounted(() => {
  authStore.hydrate()
  accessibilityStore.hydrate()
})

watch(
  () => route.fullPath,
  () => {
    document.title = `${currentTitle.value} | BridgeAbility`
  },
  { immediate: true },
)
</script>

<template>
  <div class="app-root" :class="{ 'auth-layout': isAuthLayout }">
    <a class="skip-link" href="#main-content">跳到主要内容</a>

    <header v-if="!isAuthLayout" class="app-header" aria-label="主导航">
      <div class="brand-lockup">
        <span class="brand-mark">RZQ</span>
        <div class="brand-copy">
          <p class="eyebrow">Accessible Employment Platform</p>
          <h1>融职桥</h1>
        </div>
      </div>

      <nav
        v-if="headerNavItems.length > 0"
        class="top-nav"
        :aria-label="currentSection === 'home' ? '模块导航' : '当前模块导航'"
      >
        <RouterLink
          v-for="link in headerNavItems"
          :key="link.to"
          :to="link.to"
          class="top-nav-link"
          :class="{ 'is-active': isNavActive(link) }"
        >
          {{ link.label }}
        </RouterLink>
      </nav>

      <div class="header-tools">
        <div class="mode-label">
          <span class="eyebrow">当前用户</span>
          <strong>{{ userSummary }}</strong>
        </div>
        <button v-if="authStore.isAuthenticated" type="button" class="toggle-button" @click="handleLogout">
          退出
        </button>
        <RouterLink v-else class="toggle-button" to="/login">登录</RouterLink>
      </div>
    </header>

    <main v-if="isAuthLayout" id="main-content" class="auth-pane">
      <RouterView />
    </main>

    <div v-else-if="currentSection !== 'home'" class="shell">
      <main id="main-content" class="content-pane">
        <div class="page-surface">
          <div class="breadcrumb" aria-label="面包屑">
            <span v-for="crumb in breadcrumbs" :key="crumb">{{ crumb }}</span>
          </div>
          <RouterView />
        </div>
      </main>
    </div>

    <main v-else id="main-content" class="home-pane">
      <RouterView />
    </main>
  </div>
</template>

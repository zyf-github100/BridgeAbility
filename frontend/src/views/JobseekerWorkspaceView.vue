<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'

import {
  getCurrentJobseekerProfile,
  getCurrentSupportNeeds,
  getCurrentUserApplications,
  getRecommendedJobs,
  type Job,
  type JobApplication,
  type JobseekerProfile,
  type SupportNeeds,
} from '../api/jobseeker'
import { ApiError } from '../lib/http'
import {
  formatDateTime,
  getApplicationStatusLabel,
  getSupportVisibilityLabel,
  getWorkModeLabel,
} from '../lib/jobseeker'
import { useAuthStore } from '../stores/auth'

interface JourneyStep {
  title: string
  description: string
  to: string
  done: boolean
}

const authStore = useAuthStore()

const isLoading = ref(false)
const loadError = ref('')
const profile = ref<JobseekerProfile | null>(null)
const supportNeeds = ref<SupportNeeds | null>(null)
const recommendedJobs = ref<Job[]>([])
const recommendedTotal = ref(0)
const applications = ref<JobApplication[]>([])

const profileReady = computed(() => Boolean(profile.value?.realName?.trim()))
const abilityReady = computed(
  () =>
    Boolean(profile.value?.abilityCards.length) ||
    Boolean(profile.value?.skillTags.length) ||
    Boolean(profile.value?.projectExperiences.length),
)
const supportReady = computed(() => Boolean(supportNeeds.value?.updatedAt?.trim()))
const resumeReady = computed(() => (profile.value?.profileCompletionRate ?? 0) >= 40)
const recommendationReady = computed(() => recommendedJobs.value.length > 0)
const applicationReady = computed(() => applications.value.length > 0)
const interviewReady = computed(() =>
  applications.value.some(
    (item) =>
      item.interviewRecords.length > 0 ||
      item.status === 'INTERVIEW' ||
      item.status === 'INTERVIEWING',
  ),
)
const onboardingReady = computed(() =>
  applications.value.some((item) => item.status === 'OFFERED' || item.status === 'HIRED'),
)
const supportSummaryPreview = computed(() => supportNeeds.value?.supportSummary ?? [])
const latestApplication = computed(() => applications.value[0] ?? null)
const recentApplications = computed(() => applications.value.slice(0, 3))
const nextAction = computed(() => {
  if (!profileReady.value) {
    return {
      label: '完善基础档案',
      to: '/jobseeker/profile',
      description: '完善真实姓名、求职意向和基础背景信息。',
    }
  }

  if (!abilityReady.value) {
    return {
      label: '完善技能与经历',
      to: '/jobseeker/ability',
      description: '继续补技能标签和项目经历，生成可复用的能力卡。',
    }
  }

  if (!supportReady.value) {
    return {
      label: '确认便利需求',
      to: '/jobseeker/support-needs',
      description: '补齐支持偏好，让推荐、投递和面试说明保持一致。',
    }
  }

  if (!applicationReady.value) {
    return {
      label: '查看推荐岗位',
      to: '/jobs',
      description: '前往推荐岗位列表，开始投递第一批岗位。',
    }
  }

  if (!interviewReady.value) {
    return {
      label: '打开面试辅助',
      to: '/jobseeker/interview-assistance',
      description: '集中管理面试记录、支持申请和沟通重点。',
    }
  }

  if (!onboardingReady.value) {
    return {
      label: '查看投递记录',
      to: '/applications',
      description: '继续跟进投递结果和企业反馈。',
    }
  }

  return {
    label: '维护入职反馈',
    to: '/jobseeker/onboarding-feedback',
    description: '录用后继续记录 7 天与 30 天适应情况。',
  }
})
const journeySteps = computed<JourneyStep[]>(() => [
  {
    title: '首页',
    description: '查看当前进度与下一步',
    to: '/workspace',
    done: true,
  },
  {
    title: '档案',
    description: '基础信息与求职意向',
    to: '/jobseeker/profile',
    done: profileReady.value,
  },
  {
    title: '能力',
    description: '技能标签、项目经历、能力卡',
    to: '/jobseeker/ability',
    done: abilityReady.value,
  },
  {
    title: '需求',
    description: '便利需求与沟通偏好',
    to: '/jobseeker/support-needs',
    done: supportReady.value,
  },
  {
    title: '简历',
    description: '预览导出简历成品',
    to: '/jobseeker/resume-preview',
    done: resumeReady.value,
  },
  {
    title: '推荐',
    description: '浏览系统推荐岗位',
    to: '/jobs',
    done: recommendationReady.value,
  },
  {
    title: '投递',
    description: '追踪投递结果与状态',
    to: '/applications',
    done: applicationReady.value,
  },
  {
    title: '面试',
    description: '面试支持申请与记录',
    to: '/jobseeker/interview-assistance',
    done: interviewReady.value,
  },
  {
    title: '入职',
    description: '录用后的适应反馈',
    to: '/jobseeker/onboarding-feedback',
    done: onboardingReady.value,
  },
])

async function loadWorkspace() {
  if (!authStore.token) {
    return
  }

  isLoading.value = true
  loadError.value = ''

  try {
    const [profileResponse, supportResponse, recommendedResponse, applicationResponse] = await Promise.all([
      getCurrentJobseekerProfile(authStore.token),
      getCurrentSupportNeeds(authStore.token),
      getRecommendedJobs(authStore.token, { pageSize: 4 }),
      getCurrentUserApplications(authStore.token),
    ])

    profile.value = profileResponse
    supportNeeds.value = supportResponse
    recommendedJobs.value = recommendedResponse.list
    recommendedTotal.value = recommendedResponse.total
    applications.value = applicationResponse
  } catch (error) {
    loadError.value = error instanceof ApiError ? error.message : '求职者首页加载失败'
    recommendedJobs.value = []
    recommendedTotal.value = 0
    applications.value = []
  } finally {
    isLoading.value = false
  }
}

onMounted(() => {
  void loadWorkspace()
})
</script>

<template>
  <div class="page-head">
    <div>
      <p class="eyebrow">求职者 / 首页</p>
      <h2>我的求职进展</h2>
    </div>
    <div class="page-actions">
      <RouterLink class="secondary-link" to="/jobseeker/resume-preview">查看简历</RouterLink>
      <button type="button" class="secondary-link" @click="loadWorkspace">刷新首页</button>
      <RouterLink class="primary-button" :to="nextAction.to">{{ nextAction.label }}</RouterLink>
    </div>
  </div>

  <p v-if="loadError" class="status-error" role="alert">{{ loadError }}</p>
  <p v-else-if="isLoading" class="status-muted">正在同步首页数据...</p>

  <section class="metric-strip">
    <div class="metric-cell">
      <span>档案完整度</span>
      <strong>{{ profile?.profileCompletionRate ?? 0 }}%</strong>
      <p>完善档案可提升整体完成度。</p>
    </div>
    <div class="metric-cell">
      <span>推荐岗位</span>
      <strong>{{ recommendedJobs.length }} / {{ recommendedTotal }}</strong>
      <p>根据当前资料生成的岗位结果。</p>
    </div>
    <div class="metric-cell">
      <span>投递记录</span>
      <strong>{{ applications.length }}</strong>
      <p>{{ latestApplication ? `最近更新：${latestApplication.jobTitle}` : '还没有投递记录' }}</p>
    </div>
    <div class="metric-cell">
      <span>便利需求</span>
      <strong>{{ supportNeeds ? getSupportVisibilityLabel(supportNeeds.supportVisibility) : '未设置' }}</strong>
      <p>{{ supportNeeds?.updatedAt ? formatDateTime(supportNeeds.updatedAt) : '尚未保存需求档案' }}</p>
    </div>
  </section>

  <section class="content-columns">
    <article class="hero-panel">
      <div class="panel-headline">
        <p class="eyebrow">下一步</p>
        <h3>{{ nextAction.label }}</h3>
      </div>
      <p class="body-copy">{{ nextAction.description }}</p>
      <div class="inline-tags overview-tags">
        <span>目标岗位：{{ profile?.expectedJob || '待补充' }}</span>
        <span>意向城市：{{ profile?.targetCity || '待补充' }}</span>
        <span>工作方式：{{ getWorkModeLabel(profile?.workModePreference) }}</span>
      </div>

      <div class="route-list journey-grid">
        <RouterLink
          v-for="step in journeySteps"
          :key="step.title"
          class="route-item journey-card"
          :class="{ 'is-done': step.done }"
          :to="step.to"
        >
          <span>{{ step.done ? '已就绪' : '待推进' }}</span>
          <strong>{{ step.title }}</strong>
          <p class="body-copy">{{ step.description }}</p>
        </RouterLink>
      </div>
    </article>

  </section>

  <section class="content-columns">
    <article class="ledger-panel">
      <div class="panel-headline">
        <p class="eyebrow">最近投递</p>
        <h3>{{ recentApplications.length ? '最新的投递与跟进记录' : '还没有投递记录' }}</h3>
      </div>

      <div v-if="recentApplications.length" class="preview-stack">
        <article v-for="application in recentApplications" :key="application.applicationId" class="preview-card">
          <div class="row-head">
            <h4>{{ application.jobTitle }}</h4>
            <span class="score-chip">{{ getApplicationStatusLabel(application.status) }}</span>
          </div>
          <p class="body-copy">{{ application.companyName }}</p>
          <p class="body-copy">{{ formatDateTime(application.submittedAt) }}</p>
        </article>
      </div>
      <p v-else class="status-muted">完善档案、技能和经历后，这里会为你推荐可投递的岗位。</p>
    </article>

    <article class="ledger-panel">
      <div class="panel-headline">
        <p class="eyebrow">推荐岗位</p>
        <h3>{{ recommendedJobs.length ? '当前最值得优先查看的岗位' : '推荐列表待刷新' }}</h3>
      </div>

      <div v-if="recommendedJobs.length" class="preview-stack">
        <RouterLink
          v-for="job in recommendedJobs"
          :key="job.id"
          class="preview-card"
          :to="`/jobs/${job.id}`"
        >
          <div class="row-head">
            <h4>{{ job.title }}</h4>
            <span class="score-chip">匹配 {{ job.matchScore }}</span>
          </div>
          <p class="body-copy">{{ job.company }} · {{ job.city }} · {{ getWorkModeLabel(job.workMode) }}</p>
          <p class="body-copy">{{ job.summary }}</p>
        </RouterLink>
      </div>
      <p v-else class="status-muted">完善基础档案、技能经历和便利需求后，这里会呈现更贴合的推荐结果。</p>
    </article>
  </section>

  <section class="content-columns">
    <article class="ledger-panel">
      <div class="panel-headline">
        <p class="eyebrow">便利需求摘要</p>
        <h3>{{ supportSummaryPreview.length ? '当前对外可复用的沟通重点' : '还没有形成需求摘要' }}</h3>
      </div>
      <ul v-if="supportSummaryPreview.length" class="detail-list">
        <li v-for="item in supportSummaryPreview" :key="item">{{ item }}</li>
      </ul>
      <p v-else class="status-muted">
        先完善便利需求，生成对外沟通重点。
      </p>
    </article>
  </section>
</template>

<style scoped>
.journey-grid,
.overview-note-grid,
.preview-stack {
  display: grid;
  gap: 14px;
}

.journey-grid,
.overview-note-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.journey-card.is-done {
  border-color: rgba(36, 91, 74, 0.28);
  background: linear-gradient(180deg, rgba(36, 91, 74, 0.06), rgba(252, 252, 249, 0.92));
}

.overview-note-card,
.preview-card {
  padding: 18px;
  border: 1px solid var(--line-strong);
  background: rgba(252, 252, 249, 0.9);
}

.overview-note-card span {
  display: block;
  margin-bottom: 8px;
  font-family: var(--mono);
  font-size: 0.78rem;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--muted);
}

.overview-note-card strong {
  display: block;
  margin-bottom: 8px;
  font-family: var(--serif);
  font-size: 1.5rem;
  color: var(--heading);
}

.overview-tags {
  margin: 18px 0;
}

.status-error {
  margin: 0 0 16px;
  color: var(--error);
  font-weight: 700;
}

.status-muted {
  margin: 0 0 16px;
  color: var(--muted);
}

@media (max-width: 1180px) {
  .journey-grid,
  .overview-note-grid {
    grid-template-columns: 1fr;
  }
}
</style>

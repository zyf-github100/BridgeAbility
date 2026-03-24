<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { RouterLink } from 'vue-router'

import ScoreBar from '../components/ScoreBar.vue'
import { getRecommendedJobs, type Job } from '../api/jobseeker'
import { ApiError } from '../lib/http'
import {
  getJobStageLabel,
  getScoreDimensionLabel,
  getWorkModeLabel,
} from '../lib/jobseeker'
import { useAuthStore } from '../stores/auth'

const authStore = useAuthStore()

const filters = reactive({
  city: '',
  workMode: '',
  keyword: '',
})

const jobs = ref<Job[]>([])
const total = ref(0)
const selectedJobId = ref('')
const isLoading = ref(false)
const loadError = ref('')

const selectedJob = computed(() => jobs.value.find((job) => job.id === selectedJobId.value) ?? jobs.value[0] ?? null)

watch(
  jobs,
  (list) => {
    if (list.length === 0) {
      selectedJobId.value = ''
      return
    }

    if (!list.some((job) => job.id === selectedJobId.value)) {
      selectedJobId.value = list[0].id
    }
  },
  { immediate: true },
)

async function loadJobs() {
  if (!authStore.token) {
    return
  }

  isLoading.value = true
  loadError.value = ''

  try {
    const page = await getRecommendedJobs(authStore.token, {
      page: 1,
      pageSize: 10,
      city: filters.city || undefined,
      workMode: filters.workMode || undefined,
      keyword: filters.keyword || undefined,
    })
    jobs.value = page.list
    total.value = page.total
  } catch (error) {
    loadError.value = error instanceof ApiError ? error.message : '岗位列表加载失败'
  } finally {
    isLoading.value = false
  }
}

function resetFilters() {
  filters.city = ''
  filters.workMode = ''
  filters.keyword = ''
  loadJobs()
}

onMounted(() => {
  loadJobs()
})
</script>

<template>
  <div class="page-head">
    <div>
      <p class="eyebrow">求职者 / 推荐岗位</p>
      <h2>推荐岗位</h2>
    </div>
    <div class="page-actions">
      <RouterLink class="secondary-link" to="/applications">投递记录</RouterLink>
      <button type="button" class="primary-button" :disabled="isLoading" @click="loadJobs">
        {{ isLoading ? '刷新中...' : '刷新推荐' }}
      </button>
      <span class="muted-note">当前共 {{ total }} 个岗位</span>
    </div>
  </div>

  <form class="filter-card" @submit.prevent="loadJobs">
    <div class="filter-grid">
      <label class="field-shell">
        <span>城市</span>
        <input v-model="filters.city" class="field-input" type="text" placeholder="例如：南京 / 远程" />
      </label>
      <label class="field-shell">
        <span>工作方式</span>
        <select v-model="filters.workMode" class="field-input">
          <option value="">全部</option>
          <option value="HYBRID">混合办公</option>
          <option value="REMOTE">远程</option>
          <option value="FULL_TIME">全职</option>
        </select>
      </label>
      <label class="field-shell">
        <span>关键词</span>
        <input
          v-model="filters.keyword"
          class="field-input"
          type="text"
          placeholder="岗位名 / 公司 / 描述关键词"
        />
      </label>
    </div>
    <div class="filter-actions">
      <button type="submit" class="primary-button" :disabled="isLoading">
        {{ isLoading ? '加载中...' : '更新推荐结果' }}
      </button>
      <button type="button" class="toggle-button" @click="resetFilters">重置</button>
      <span class="muted-note">支持城市、工作方式和关键词组合筛选</span>
    </div>
  </form>

  <section class="master-detail">
    <article class="list-pane">
      <div class="panel-headline">
        <p class="eyebrow">推荐结果</p>
        <h3>岗位列表</h3>
      </div>

      <p v-if="loadError" class="status-error" role="alert">{{ loadError }}</p>
      <p v-else-if="isLoading" class="status-muted">正在加载岗位列表...</p>
      <p v-else-if="jobs.length === 0" class="status-muted empty-state">当前筛选条件下没有岗位。</p>

      <button
        v-for="job in jobs"
        :key="job.id"
        type="button"
        class="select-row"
        :class="{ 'is-selected': selectedJobId === job.id }"
        @click="selectedJobId = job.id"
      >
        <div class="select-score">
          <span>匹配分</span>
          <strong>{{ job.matchScore }}</strong>
        </div>
        <div class="select-copy">
          <div class="row-head">
            <h4>{{ job.title }}</h4>
            <span>{{ job.company }}</span>
          </div>
          <p>{{ job.summary }}</p>
          <div class="inline-tags">
            <span>{{ job.city }}</span>
            <span>{{ getWorkModeLabel(job.workMode) }}</span>
            <span>{{ job.salaryRange }}</span>
            <span>{{ getJobStageLabel(job.stage) }}</span>
          </div>
        </div>
      </button>
    </article>

    <article class="detail-pane">
      <template v-if="selectedJob">
        <div class="panel-headline">
          <p class="eyebrow">岗位预览</p>
          <h3>{{ selectedJob.title }}</h3>
        </div>
        <p class="body-copy">
          {{ selectedJob.company }} · {{ selectedJob.city }} · {{ selectedJob.salaryRange }} ·
          {{ getWorkModeLabel(selectedJob.workMode) }}
        </p>
        <div class="score-stack">
          <ScoreBar
            v-for="score in selectedJob.dimensionScores"
            :key="score.label"
            :label="getScoreDimensionLabel(score.label)"
            :value="score.value"
          />
        </div>

        <div class="detail-block">
          <p class="eyebrow">推荐理由</p>
          <ul class="detail-list">
            <li v-for="reason in selectedJob.reasons" :key="reason">{{ reason }}</li>
          </ul>
        </div>

        <div class="detail-block">
          <p class="eyebrow">风险提示</p>
          <ul class="detail-list">
            <li v-for="risk in selectedJob.risks" :key="risk">{{ risk }}</li>
          </ul>
        </div>

        <div class="detail-block">
          <p class="eyebrow">可提供支持</p>
          <ul class="detail-list">
            <li v-for="support in selectedJob.supports" :key="support">{{ support }}</li>
          </ul>
        </div>

        <div class="detail-actions">
          <RouterLink class="secondary-link" :to="`/jobs/${selectedJob.id}`">查看详情并投递</RouterLink>
          <RouterLink class="toggle-button" to="/applications">查看投递记录</RouterLink>
        </div>
      </template>
      <p v-else class="status-muted empty-state">请先选择一个岗位。</p>
    </article>
  </section>
</template>

<style scoped>
.filter-card {
  display: grid;
  gap: 16px;
  margin-bottom: 20px;
  padding: 18px;
  border: 1px solid var(--line-strong);
  background: rgba(252, 252, 249, 0.92);
}

.filter-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.field-shell {
  display: grid;
  gap: 8px;
}

.field-shell span {
  font-weight: 700;
  color: var(--heading);
}

.field-input {
  width: 100%;
  min-height: 48px;
  padding: 0 14px;
  border: 1px solid var(--line-strong);
  border-radius: 12px;
  background: #ffffff;
}

.filter-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
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

.empty-state {
  padding-top: 12px;
}

@media (max-width: 960px) {
  .filter-grid {
    grid-template-columns: 1fr;
  }
}
</style>

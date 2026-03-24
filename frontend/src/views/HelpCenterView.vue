<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'

import { getPublishedKnowledgeArticles, type PublishedKnowledgeArticle } from '../api/knowledge'
import { ApiError } from '../lib/http'

const fallbackArticles: PublishedKnowledgeArticle[] = [
  {
    id: 'resume-accessibility',
    title: '如何整理适合无障碍岗位沟通的简历摘要',
    category: '求职准备',
    summary: '梳理项目经历、沟通偏好与辅助支持需求，便于企业和服务机构快速理解协作方式。',
    tags: ['简历', '无障碍', '沟通'],
    publishDate: '2026-03-20',
  },
  {
    id: 'enterprise-labeling',
    title: '企业如何补齐岗位无障碍标签',
    category: '企业发布',
    summary: '明确办公场地、面试方式和辅助软件支持，减少候选人试错成本。',
    tags: ['企业', '岗位标签', '发布'],
    publishDate: '2026-03-18',
  },
  {
    id: 'service-followup',
    title: '录用后 7 天和 30 天回访该记录什么',
    category: '入职跟进',
    summary: '围绕适应度、沟通问题、环境障碍和持续支持记录结构化反馈。',
    tags: ['回访', '入职反馈', '服务支持'],
    publishDate: '2026-03-22',
  },
]

const articles = ref<PublishedKnowledgeArticle[]>([])
const searchKeyword = ref('')
const categoryFilter = ref('ALL')
const isLoading = ref(false)
const loadError = ref('')

const sourceArticles = computed(() => (articles.value.length ? articles.value : fallbackArticles))
const categoryOptions = computed(() => ['ALL', ...new Set(sourceArticles.value.map((item) => item.category))])
const filteredArticles = computed(() => {
  const keyword = searchKeyword.value.trim().toLowerCase()
  return sourceArticles.value.filter((item) => {
    if (categoryFilter.value !== 'ALL' && item.category !== categoryFilter.value) {
      return false
    }
    if (!keyword) {
      return true
    }
    const haystack = [item.title, item.summary, item.category, ...item.tags].join(' ').toLowerCase()
    return haystack.includes(keyword)
  })
})

async function loadArticles() {
  isLoading.value = true
  loadError.value = ''
  try {
    articles.value = await getPublishedKnowledgeArticles()
  } catch (error) {
    loadError.value = error instanceof ApiError ? error.message : '帮助内容加载失败，已切换为本地指引。'
  } finally {
    isLoading.value = false
  }
}

onMounted(() => {
  void loadArticles()
})
</script>

<template>
  <section class="home-hero help-hero">
    <div class="home-copy">
      <p class="eyebrow">Public Help</p>
      <h2>帮助中心</h2>
      <p class="body-copy">
        提供求职准备、企业发布、服务支持和账号恢复等常见说明，进入页面即可查看。
      </p>
      <div class="home-actions">
        <RouterLink class="primary-link" to="/login">前往登录</RouterLink>
        <RouterLink class="secondary-link" to="/accessibility">无障碍设置</RouterLink>
      </div>
    </div>

    <div class="hero-board">
      <div class="board-header">
        <span>常用内容</span>
        <strong>这里汇集了求职过程中常用的说明与帮助</strong>
      </div>
      <div class="board-grid">
        <div>
          <p class="board-label">账号恢复</p>
          <strong>忘记密码与登录问题</strong>
          <p>支持在线发送验证码、重置密码，并提供人工协助方式。</p>
        </div>
        <div>
          <p class="board-label">岗位沟通</p>
          <strong>企业发布与候选人沟通</strong>
          <p>补齐无障碍标签、整理支持需求摘要、减少沟通折返。</p>
        </div>
        <div>
          <p class="board-label">入职反馈</p>
          <strong>7 天与 30 天跟踪记录</strong>
          <p>把录用后的适应情况、环境问题和支持落地情况结构化记录下来。</p>
        </div>
        <div>
          <p class="board-label">无障碍模式</p>
          <strong>高对比度、大字号与键盘聚焦</strong>
          <p>可在首页和登录页直接切换显示偏好。</p>
        </div>
      </div>
    </div>
  </section>

  <section class="metric-strip">
    <div class="metric-cell">
      <span>文章总数</span>
      <strong>{{ sourceArticles.length }}</strong>
      <p>当前可访问的帮助主题</p>
    </div>
    <div class="metric-cell">
      <span>分类数</span>
      <strong>{{ categoryOptions.length - 1 }}</strong>
      <p>覆盖求职、企业、服务支持和账号支持</p>
    </div>
    <div class="metric-cell">
      <span>筛选结果</span>
      <strong>{{ filteredArticles.length }}</strong>
      <p>根据关键词和分类实时过滤</p>
    </div>
    <div class="metric-cell">
      <span>数据来源</span>
      <strong>{{ articles.length ? '在线' : '本地' }}</strong>
      <p>{{ articles.length ? '正在使用在线知识库' : '接口异常时自动回退到本地指引' }}</p>
    </div>
  </section>

  <section class="content-columns">
    <article class="ledger-panel">
      <div class="page-actions toolbar">
        <label class="search-field">
          <span>关键词</span>
          <input v-model="searchKeyword" class="field-control" type="text" placeholder="搜索岗位、回访、账号等关键词" />
        </label>
        <label class="search-field">
          <span>分类</span>
          <select v-model="categoryFilter" class="field-control">
            <option v-for="option in categoryOptions" :key="option" :value="option">
              {{ option === 'ALL' ? '全部分类' : option }}
            </option>
          </select>
        </label>
        <button type="button" class="primary-button" @click="loadArticles">刷新内容</button>
      </div>

      <p v-if="loadError" class="status-note" role="status">{{ loadError }}</p>
      <p v-else-if="isLoading" class="status-note">正在加载帮助内容...</p>

      <div class="article-list">
        <article v-for="article in filteredArticles" :key="article.id" class="article-card">
          <div class="row-head">
            <div>
              <p class="eyebrow">{{ article.category }}</p>
              <h4>{{ article.title }}</h4>
            </div>
            <span class="article-date">{{ article.publishDate }}</span>
          </div>
          <p class="body-copy">{{ article.summary }}</p>
          <div class="inline-tags">
            <span v-for="tag in article.tags" :key="tag">{{ tag }}</span>
          </div>
        </article>
      </div>
    </article>

    <aside class="detail-sidebar help-sidebar">
      <div class="detail-block">
        <p class="eyebrow">快捷入口</p>
        <div class="quick-links">
          <RouterLink class="secondary-link" to="/forgot-password">忘记密码</RouterLink>
          <RouterLink class="secondary-link" to="/accessibility">无障碍模式设置</RouterLink>
          <RouterLink class="secondary-link" to="/overview">返回首页</RouterLink>
        </div>
      </div>

      <div class="detail-block">
        <p class="eyebrow">常见问题</p>
        <ul class="detail-list">
          <li>如何在投递时同步支持需求摘要？先维护“便利需求”，投递时会自动带入摘要。</li>
          <li>企业首页看什么？优先关注认证状态、草稿岗位和候选人推进节奏。</li>
          <li>入职反馈什么时候填？录用后 7 天和 30 天分别记录一次最有价值。</li>
        </ul>
      </div>

      <div class="detail-block">
        <p class="eyebrow">建议阅读顺序</p>
        <ul class="detail-list">
          <li>求职者：简历预览 → 岗位详情 → 投递记录 → 入职反馈</li>
          <li>企业：企业首页 → 企业认证 → 岗位管理 → 候选人列表</li>
          <li>管理员：管理首页 → 用户管理 → 风险记录 → 申诉纠错</li>
        </ul>
      </div>
    </aside>
  </section>
</template>

<style scoped>
.help-hero {
  margin-bottom: 24px;
}

.toolbar,
.article-list,
.help-sidebar,
.quick-links {
  display: grid;
  gap: 16px;
}

.toolbar {
  grid-template-columns: minmax(0, 1.2fr) 220px auto;
  margin-bottom: 20px;
}

.search-field {
  display: grid;
  gap: 8px;
}

.field-control {
  width: 100%;
  border: 1px solid var(--line-strong);
  padding: 12px 14px;
  background: #fff;
}

.article-card {
  display: grid;
  gap: 12px;
  padding: 18px;
  border: 1px solid var(--line);
  background: rgba(255, 255, 255, 0.76);
}

.article-date,
.status-note {
  color: var(--muted);
}

.status-note {
  margin: 0 0 16px;
}

@media (max-width: 960px) {
  .toolbar {
    grid-template-columns: 1fr;
  }
}
</style>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'

import {
  exportCurrentResume,
  getCurrentResumePreview,
  type ResumeExportFormat,
  type ResumePreview,
} from '../api/jobseeker'
import { ApiError } from '../lib/http'
import {
  getApplicationStatusLabel,
  getSupportVisibilityLabel,
  getWorkModeLabel,
} from '../lib/jobseeker'
import { useAuthStore } from '../stores/auth'

const authStore = useAuthStore()

const isLoading = ref(false)
const exportingFormat = ref<ResumeExportFormat | ''>('')
const loadError = ref('')
const exportError = ref('')
const exportSuccess = ref('')
const preview = ref<ResumePreview | null>(null)

const skillCount = computed(() => preview.value?.profile?.skillTags.length ?? 0)
const projectCount = computed(() => preview.value?.profile?.projectExperiences.length ?? 0)
const abilityCount = computed(() => preview.value?.profile?.abilityCards.length ?? 0)
const recentApplicationCount = computed(() => preview.value?.recentApplications.length ?? 0)

function getExportMimeType(format: ResumeExportFormat) {
  return format === 'pdf'
    ? 'application/pdf'
    : 'application/vnd.openxmlformats-officedocument.wordprocessingml.document'
}

function getExportExtension(format: ResumeExportFormat) {
  return format === 'pdf' ? '.pdf' : '.docx'
}

function ensureExportFileName(fileName: string, format: ResumeExportFormat) {
  const extension = getExportExtension(format)
  return fileName.toLowerCase().endsWith(extension) ? fileName : `${fileName}${extension}`
}

type SaveFileHandle = {
  createWritable: () => Promise<{
    write: (data: Blob) => Promise<void>
    close: () => Promise<void>
  }>
}

async function requestSaveLocation(fileName: string, format: ResumeExportFormat): Promise<SaveFileHandle | null | undefined> {
  const saveWindow = window as Window & {
    showSaveFilePicker?: (options?: unknown) => Promise<SaveFileHandle>
  }

  if (typeof saveWindow.showSaveFilePicker !== 'function') {
    return undefined
  }

  try {
    return await saveWindow.showSaveFilePicker({
      suggestedName: ensureExportFileName(fileName, format),
      types: [
        {
          description: format === 'pdf' ? 'PDF 文件' : 'Word 文档',
          accept: {
            [getExportMimeType(format)]: [getExportExtension(format)],
          },
        },
      ],
    })
  } catch (error) {
    if (error instanceof DOMException && error.name === 'AbortError') {
      return null
    }
    return undefined
  }
}

async function writeToSaveHandle(handle: SaveFileHandle, blob: Blob) {
  const writable = await handle.createWritable()
  await writable.write(blob)
  await writable.close()
}

function saveWithBrowserDownload(blob: Blob, fileName: string) {
  const url = window.URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = fileName
  link.style.display = 'none'
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  window.setTimeout(() => window.URL.revokeObjectURL(url), 1000)
}

async function loadResumePreview() {
  if (!authStore.token) {
    return
  }

  isLoading.value = true
  loadError.value = ''

  try {
    preview.value = await getCurrentResumePreview(authStore.token)
  } catch (error) {
    loadError.value = error instanceof ApiError ? error.message : '简历预览加载失败'
  } finally {
    isLoading.value = false
  }
}

async function handleExportResume(format: ResumeExportFormat) {
  if (!authStore.token) {
    return
  }

  exportingFormat.value = format
  exportError.value = ''
  exportSuccess.value = ''

  try {
    const suggestedFileName = ensureExportFileName(
      preview.value?.exportFileName ?? 'bridgeability-resume',
      format,
    )
    const saveHandle = await requestSaveLocation(suggestedFileName, format)

    if (saveHandle === null) {
      return
    }

    const { blob, fileName } = await exportCurrentResume(authStore.token, format)
    const finalFileName = ensureExportFileName(fileName || suggestedFileName, format)

    if (saveHandle) {
      await writeToSaveHandle(saveHandle, blob)
      exportSuccess.value = `已保存 ${finalFileName}。`
    } else {
      saveWithBrowserDownload(blob, finalFileName)
      exportSuccess.value = `已开始下载 ${finalFileName}，请到浏览器下载列表或系统“下载”文件夹查看。`
    }
  } catch (error) {
    exportError.value = error instanceof ApiError ? error.message : '简历导出失败'
  } finally {
    exportingFormat.value = ''
  }
}

onMounted(() => {
  loadResumePreview()
})
</script>

<template>
  <div class="page-head">
    <div>
      <p class="eyebrow">求职者端 / 简历预览</p>
      <h2>简历预览</h2>
    </div>
    <div class="page-actions">
      <button type="button" class="secondary-link" @click="loadResumePreview">刷新预览</button>
      <button
        type="button"
        class="secondary-link"
        :disabled="exportingFormat !== ''"
        @click="handleExportResume('docx')"
      >
        {{ exportingFormat === 'docx' ? '导出 Word 中...' : '导出 Word' }}
      </button>
      <button
        type="button"
        class="primary-button"
        :disabled="exportingFormat !== ''"
        @click="handleExportResume('pdf')"
      >
        {{ exportingFormat === 'pdf' ? '导出 PDF 中...' : '导出 PDF' }}
      </button>
    </div>
  </div>

  <p v-if="loadError" class="status-error" role="alert">{{ loadError }}</p>
  <p v-if="exportSuccess" class="success-banner" aria-live="polite">{{ exportSuccess }}</p>
  <p v-if="exportError" class="status-error" role="alert">{{ exportError }}</p>
  <p v-if="isLoading" class="status-muted">正在生成简历预览...</p>

  <template v-if="preview && !isLoading">
    <section class="metric-strip">
      <div class="metric-cell">
        <span>完整度</span>
        <strong>{{ preview.profileCompletionRate }}%</strong>
        <p>当前档案生成简历的完整程度。</p>
      </div>
      <div class="metric-cell">
        <span>技能标签</span>
        <strong>{{ skillCount }}</strong>
        <p>已写入简历的技能标签数量。</p>
      </div>
      <div class="metric-cell">
        <span>项目经历</span>
        <strong>{{ projectCount }}</strong>
        <p>可直接用于面试表达的项目经历。</p>
      </div>
      <div class="metric-cell">
        <span>最近投递</span>
        <strong>{{ recentApplicationCount }}</strong>
        <p>近期待回看的投递轨迹。</p>
      </div>
    </section>

    <section class="content-columns">
      <article class="ledger-panel">
        <div class="panel-headline">
          <div>
            <p class="eyebrow">简历抬头</p>
            <h3>{{ preview.displayName }}</h3>
          </div>
        </div>

        <div class="detail-block">
          <p class="eyebrow">一句话标题</p>
          <h3>{{ preview.headline }}</h3>
          <p class="detail-copy">{{ preview.summary }}</p>
          <div class="inline-tags">
            <span>{{ preview.generatedAt }}</span>
            <span>{{ preview.exportFileName }}.pdf / .docx</span>
          </div>
        </div>

        <div class="detail-block">
          <p class="eyebrow">基础信息</p>
          <ul class="detail-list">
            <li>目标岗位：{{ preview.profile?.expectedJob || '未补充' }}</li>
            <li>意向城市：{{ preview.profile?.targetCity || '未补充' }}</li>
            <li>工作方式：{{ getWorkModeLabel(preview.profile?.workModePreference) }}</li>
            <li>学校：{{ preview.profile?.schoolName || '未补充' }}</li>
            <li>专业：{{ preview.profile?.major || '未补充' }}</li>
            <li>个人简介：{{ preview.profile?.intro || '未补充' }}</li>
          </ul>
        </div>

        <div class="detail-block">
          <p class="eyebrow">核心技能</p>
          <div v-if="preview.profile?.skillTags.length" class="inline-tags">
            <span v-for="skill in preview.profile?.skillTags" :key="skill.skillCode">
              {{ skill.skillName }} / {{ skill.skillLevelLabel }}
            </span>
          </div>
          <p v-else class="status-muted">当前还没有技能标签。</p>
        </div>

        <div class="detail-block">
          <p class="eyebrow">项目经历</p>
          <div v-if="preview.profile?.projectExperiences.length" class="project-list">
            <article
              v-for="project in preview.profile?.projectExperiences"
              :key="`${project.projectName}-${project.periodLabel}`"
              class="project-card"
            >
              <div class="row-head">
                <h4>{{ project.projectName }}</h4>
                <span>{{ project.roleName }}</span>
              </div>
              <p class="project-period">{{ project.periodLabel }}</p>
              <p class="detail-copy">{{ project.description }}</p>
            </article>
          </div>
          <p v-else class="status-muted">当前还没有项目经历。</p>
        </div>

        <div class="detail-block">
          <p class="eyebrow">最近投递</p>
          <div v-if="preview.recentApplications.length" class="application-list">
            <article
              v-for="application in preview.recentApplications"
              :key="application.applicationId"
              class="application-card"
            >
              <div class="row-head">
                <h4>{{ application.jobTitle }}</h4>
                <span class="status-chip">{{ getApplicationStatusLabel(application.status) }}</span>
              </div>
              <p class="detail-copy">{{ application.companyName }}</p>
              <div class="inline-tags">
                <span>{{ application.submittedAt }}</span>
                <span>匹配分 {{ application.matchScoreSnapshot }}</span>
              </div>
            </article>
          </div>
          <p v-else class="status-muted">当前还没有投递记录。</p>
        </div>
      </article>

      <aside class="side-stack">
        <div class="detail-block">
          <p class="eyebrow">能力卡</p>
          <div v-if="preview.profile?.abilityCards.length" class="ability-list">
            <article v-for="card in preview.profile?.abilityCards" :key="card.code" class="ability-card">
              <p class="eyebrow">{{ card.code }}</p>
              <h3>{{ card.title }}</h3>
              <p class="detail-copy">{{ card.summary }}</p>
              <ul class="detail-list">
                <li v-for="highlight in card.highlights" :key="highlight">{{ highlight }}</li>
              </ul>
            </article>
          </div>
          <p v-else class="status-muted">保存档案后会自动生成能力卡。</p>
        </div>

        <div class="detail-block">
          <p class="eyebrow">沟通与便利需求</p>
          <h3>{{ getSupportVisibilityLabel(preview.supportNeeds.supportVisibility) }}</h3>
          <ul v-if="preview.supportNeeds.supportSummary.length" class="detail-list">
            <li v-for="item in preview.supportNeeds.supportSummary" :key="item">{{ item }}</li>
          </ul>
          <p v-else class="status-muted">当前未展示额外的便利需求摘要。</p>
        </div>

        <div class="detail-block">
          <p class="eyebrow">当前优势</p>
          <ul v-if="preview.strengths.length" class="detail-list">
            <li v-for="item in preview.strengths" :key="item">{{ item }}</li>
          </ul>
          <p v-else class="status-muted">当前还没有可提炼的优势摘要。</p>
        </div>

        <div class="detail-block">
          <p class="eyebrow">建议补充</p>
          <ul v-if="preview.suggestions.length" class="detail-list">
            <li v-for="item in preview.suggestions" :key="item">{{ item }}</li>
          </ul>
          <p v-else class="status-muted">当前内容已经足够导出。</p>
        </div>

        <div class="detail-block">
          <p class="eyebrow">导出说明</p>
          <p class="detail-copy">
            当前可直接导出 PDF 与 Word 成品简历，导出内容会保留结构化排版，并按授权范围决定是否展示便利需求摘要。
          </p>
          <div class="inline-tags">
            <span>能力卡 {{ abilityCount }}</span>
            <span>建议 {{ preview.suggestions.length }}</span>
          </div>
        </div>
      </aside>
    </section>
  </template>
</template>

<style scoped>
.side-stack,
.ability-list,
.project-list,
.application-list {
  display: grid;
  gap: 16px;
}

.ability-card,
.project-card,
.application-card {
  display: grid;
  gap: 10px;
  padding: 16px;
  border: 1px solid var(--line);
  background: rgba(255, 255, 255, 0.68);
}

.ability-card {
  background: linear-gradient(180deg, rgba(47, 111, 237, 0.06), rgba(255, 255, 255, 0.78));
}

.project-period {
  margin: 0;
  color: var(--muted);
}

.status-chip {
  display: inline-flex;
  align-items: center;
  min-height: 32px;
  padding: 0 12px;
  border: 1px solid var(--line-strong);
  background: rgba(47, 111, 237, 0.12);
  color: var(--brand);
  font-weight: 700;
}

.detail-copy {
  margin: 0;
  line-height: 1.8;
  white-space: pre-line;
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

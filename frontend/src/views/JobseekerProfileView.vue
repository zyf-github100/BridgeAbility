<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { RouterLink } from 'vue-router'

import ErrorSummary from '../components/ErrorSummary.vue'
import { useJobseekerProfileEditor } from '../composables/useJobseekerProfileEditor'
import { getWorkModeLabel } from '../lib/jobseeker'

const editor = reactive(useJobseekerProfileEditor())
const submitted = ref(false)

const workModeOptions = [
  { value: 'FULL_TIME', label: '全职' },
  { value: 'INTERNSHIP', label: '实习' },
  { value: 'PART_TIME', label: '兼职' },
  { value: 'REMOTE', label: '远程' },
  { value: 'HYBRID', label: '混合办公' },
]

async function submitBasicProfile() {
  submitted.value = true
  await editor.saveBasicProfile()
}

onMounted(() => {
  void editor.loadProfile()
})
</script>

<template>
  <div class="page-head">
    <div>
      <p class="eyebrow">求职者 / 基础档案</p>
      <h2>{{ editor.displayName }} 的基础档案</h2>
    </div>
    <div class="page-actions">
      <RouterLink class="secondary-link" to="/workspace">返回首页</RouterLink>
      <RouterLink class="secondary-link" to="/jobseeker/ability">技能与经历</RouterLink>
      <button
        type="submit"
        form="basic-profile-form"
        class="primary-button"
        :disabled="editor.isSaving || editor.isLoading"
      >
        {{ editor.isSaving ? '保存中...' : '保存基础档案' }}
      </button>
    </div>
  </div>

  <p v-if="editor.loadError" class="status-error" role="alert">{{ editor.loadError }}</p>
  <p v-if="editor.saveSuccess" class="success-banner" aria-live="polite">{{ editor.saveSuccess }}</p>
  <p v-if="editor.saveError" class="status-error" role="alert">{{ editor.saveError }}</p>

  <section class="metric-strip">
    <div class="metric-cell">
      <span>档案完整度</span>
      <strong>{{ editor.completionRate }}%</strong>
      <p>基础档案和技能与经历会写回同一份求职档案。</p>
    </div>
    <div class="metric-cell">
      <span>目标岗位</span>
      <strong>{{ editor.profileForm.expectedJob || '待填写' }}</strong>
      <p>会直接影响推荐岗位和简历标题。</p>
    </div>
    <div class="metric-cell">
      <span>意向城市</span>
      <strong>{{ editor.profileForm.targetCity || '待填写' }}</strong>
      <p>用于筛选更贴合的推荐岗位。</p>
    </div>
    <div class="metric-cell">
      <span>工作方式</span>
      <strong>{{ getWorkModeLabel(editor.profileForm.workModePreference) }}</strong>
      <p>会在岗位匹配、简历预览和投递时持续复用。</p>
    </div>
  </section>

  <section class="content-columns">
    <article class="ledger-panel">
      <p class="eyebrow">档案编辑</p>
      <h3>维护对外展示的基础信息</h3>
      <ErrorSummary :errors="submitted ? editor.basicProfileErrors : []" />

      <form id="basic-profile-form" class="form-shell" novalidate @submit.prevent="submitBasicProfile">
        <div class="field">
          <label class="field-label" for="profile-real-name">
            真实姓名
            <span class="required-mark" aria-hidden="true">*</span>
          </label>
          <input
            id="profile-real-name"
            v-model="editor.profileForm.realName"
            type="text"
            class="field-control"
            :aria-invalid="submitted && editor.basicProfileErrors.some((error) => error.id === 'profile-real-name')"
          />
        </div>

        <div class="profile-grid">
          <div class="field">
            <label class="field-label" for="profile-school-name">学校</label>
            <input id="profile-school-name" v-model="editor.profileForm.schoolName" type="text" class="field-control" />
          </div>
          <div class="field">
            <label class="field-label" for="profile-major">专业</label>
            <input id="profile-major" v-model="editor.profileForm.major" type="text" class="field-control" />
          </div>
          <div class="field">
            <label class="field-label" for="profile-target-city">意向城市</label>
            <input id="profile-target-city" v-model="editor.profileForm.targetCity" type="text" class="field-control" />
          </div>
          <div class="field">
            <label class="field-label" for="profile-expected-job">目标岗位</label>
            <input id="profile-expected-job" v-model="editor.profileForm.expectedJob" type="text" class="field-control" />
          </div>
        </div>

        <div class="field">
          <label class="field-label" for="profile-work-mode-preference">工作方式偏好</label>
          <select id="profile-work-mode-preference" v-model="editor.profileForm.workModePreference" class="field-control">
            <option value="">请选择</option>
            <option v-for="option in workModeOptions" :key="option.value" :value="option.value">
              {{ option.label }}
            </option>
          </select>
        </div>

        <div class="field">
          <label class="field-label" for="profile-intro">个人简介</label>
          <textarea
            id="profile-intro"
            v-model="editor.profileForm.intro"
            rows="5"
            class="field-control textarea-control"
          />
        </div>
      </form>
    </article>

  </section>
</template>

<style scoped>
.profile-grid,
.side-stack {
  display: grid;
  gap: 16px;
}

.profile-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.status-error {
  margin: 0 0 16px;
  color: var(--error);
  font-weight: 700;
}

@media (max-width: 960px) {
  .profile-grid {
    grid-template-columns: 1fr;
  }
}
</style>

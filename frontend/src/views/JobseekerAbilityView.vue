<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { RouterLink } from 'vue-router'

import ErrorSummary from '../components/ErrorSummary.vue'
import { useJobseekerProfileEditor } from '../composables/useJobseekerProfileEditor'

const editor = reactive(useJobseekerProfileEditor())
const submitted = ref(false)

const skillLevelOptions = [
  { value: 1, label: 'Lv.1 / 入门' },
  { value: 2, label: 'Lv.2 / 基础' },
  { value: 3, label: 'Lv.3 / 熟练' },
  { value: 4, label: 'Lv.4 / 进阶' },
  { value: 5, label: 'Lv.5 / 擅长' },
]

async function submitAbilityProfile() {
  submitted.value = true
  await editor.saveAbilityProfile()
}

onMounted(() => {
  void editor.loadProfile()
})
</script>

<template>
  <div class="page-head">
    <div>
      <p class="eyebrow">求职者 / 技能与经历</p>
      <h2>技能与经历</h2>
    </div>
    <div class="page-actions">
      <RouterLink class="secondary-link" to="/jobseeker/profile">基础档案</RouterLink>
      <RouterLink class="secondary-link" to="/jobseeker/resume-preview">简历预览</RouterLink>
      <button
        type="submit"
        form="ability-profile-form"
        class="primary-button"
        :disabled="editor.isSaving || editor.isLoading"
      >
        {{ editor.isSaving ? '保存中...' : '保存技能与经历' }}
      </button>
    </div>
  </div>

  <p v-if="editor.loadError" class="status-error" role="alert">{{ editor.loadError }}</p>
  <p v-if="editor.saveSuccess" class="success-banner" aria-live="polite">{{ editor.saveSuccess }}</p>
  <p v-if="editor.saveError" class="status-error" role="alert">{{ editor.saveError }}</p>

  <section class="metric-strip">
    <div class="metric-cell">
      <span>技能标签</span>
      <strong>{{ editor.profileForm.skillTags.filter((item) => item.skillName.trim()).length }}</strong>
      <p>建议至少补充 3 个技能标签，方便生成更稳定的能力卡。</p>
    </div>
    <div class="metric-cell">
      <span>项目经历</span>
      <strong>{{ editor.projectPreview.length }}</strong>
      <p>项目经历越完整，岗位匹配与简历摘要越准确。</p>
    </div>
    <div class="metric-cell">
      <span>能力卡</span>
      <strong>{{ editor.abilityCards.length }}</strong>
      <p>保存后会基于当前档案实时刷新。</p>
    </div>
    <div class="metric-cell">
      <span>完整度</span>
      <strong>{{ editor.completionRate }}%</strong>
      <p>技能与经历和基础档案共同决定完整度。</p>
    </div>
  </section>

  <section class="content-columns">
    <article class="ledger-panel">
      <p class="eyebrow">内容编辑</p>
      <h3>完善技能与经历</h3>
      <ErrorSummary :errors="submitted ? editor.abilityProfileErrors : []" />

      <form id="ability-profile-form" class="form-shell" novalidate @submit.prevent="submitAbilityProfile">
        <section class="editor-section">
          <div class="section-head">
            <div>
              <p class="eyebrow">技能标签</p>
              <h4>按熟练度维护核心技能</h4>
            </div>
            <button type="button" class="toggle-button" @click="editor.addSkillTag">新增技能标签</button>
          </div>

          <div class="editor-list">
            <div
              v-for="(skill, index) in editor.profileForm.skillTags"
              :key="`skill-${index}`"
              class="editor-card"
            >
              <div class="editor-grid">
                <div class="field">
                  <label class="field-label" :for="`skill-name-${index}`">技能名称</label>
                  <input :id="`skill-name-${index}`" v-model="skill.skillName" type="text" class="field-control" />
                </div>
                <div class="field">
                  <label class="field-label" :for="`skill-level-${index}`">熟练度</label>
                  <select :id="`skill-level-${index}`" v-model.number="skill.skillLevel" class="field-control">
                    <option v-for="option in skillLevelOptions" :key="option.value" :value="option.value">
                      {{ option.label }}
                    </option>
                  </select>
                </div>
              </div>
              <button
                type="button"
                class="secondary-link inline-action"
                :disabled="editor.profileForm.skillTags.length === 1"
                @click="editor.removeSkillTag(index)"
              >
                删除技能
              </button>
            </div>
          </div>
        </section>

        <section class="editor-section">
          <div class="section-head">
            <div>
              <p class="eyebrow">项目经历</p>
              <h4>记录能证明交付能力的项目片段</h4>
            </div>
            <button type="button" class="toggle-button" @click="editor.addProjectExperience">新增项目经历</button>
          </div>

          <div class="editor-list">
            <div
              v-for="(project, index) in editor.profileForm.projectExperiences"
              :key="`project-${index}`"
              class="editor-card"
            >
              <div class="field">
                <label class="field-label" :for="`project-name-${index}`">项目名称</label>
                <input :id="`project-name-${index}`" v-model="project.projectName" type="text" class="field-control" />
              </div>

              <div class="editor-grid editor-grid-wide">
                <div class="field">
                  <label class="field-label" :for="`project-role-${index}`">角色</label>
                  <input :id="`project-role-${index}`" v-model="project.roleName" type="text" class="field-control" />
                </div>
                <div class="field">
                  <label class="field-label" :for="`project-start-date-${index}`">开始日期</label>
                  <input :id="`project-start-date-${index}`" v-model="project.startDate" type="date" class="field-control" />
                </div>
                <div class="field">
                  <label class="field-label" :for="`project-end-date-${index}`">结束日期</label>
                  <input :id="`project-end-date-${index}`" v-model="project.endDate" type="date" class="field-control" />
                </div>
              </div>

              <div class="field">
                <label class="field-label" :for="`project-description-${index}`">项目描述</label>
                <textarea
                  :id="`project-description-${index}`"
                  v-model="project.description"
                  rows="4"
                  class="field-control textarea-control"
                />
              </div>

              <button
                type="button"
                class="secondary-link inline-action"
                :disabled="editor.profileForm.projectExperiences.length === 1"
                @click="editor.removeProjectExperience(index)"
              >
                删除项目
              </button>
            </div>
          </div>
        </section>
      </form>
    </article>

    <aside class="side-stack">
      <div class="detail-block">
        <p class="eyebrow">能力卡</p>
        <h3>{{ editor.abilityCards.length ? '已生成的对外表达卡片' : '保存后生成能力卡' }}</h3>
        <div v-if="editor.abilityCards.length" class="ability-card-list">
          <article v-for="card in editor.abilityCards" :key="card.code" class="ability-card">
            <p class="eyebrow">{{ card.code }}</p>
            <h4>{{ card.title }}</h4>
            <p class="body-copy">{{ card.summary }}</p>
            <ul class="detail-list compact-list">
              <li v-for="highlight in card.highlights" :key="highlight">{{ highlight }}</li>
            </ul>
          </article>
        </div>
        <p v-else class="status-muted">完善技能标签和项目经历后，即可生成能力卡。</p>
      </div>
    </aside>
  </section>
</template>

<style scoped>
.side-stack,
.editor-list,
.ability-card-list {
  display: grid;
  gap: 16px;
}

.editor-section {
  display: grid;
  gap: 16px;
}

.section-head,
.editor-grid {
  display: grid;
  gap: 16px;
}

.section-head {
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
}

.section-head h4,
.ability-card h4 {
  margin: 0;
  font-family: var(--serif);
  color: var(--heading);
}

.editor-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.editor-grid-wide {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.editor-card,
.ability-card {
  display: grid;
  gap: 12px;
  padding: 16px;
  border: 1px solid var(--line);
  background: rgba(252, 252, 249, 0.92);
}

.ability-card {
  border-color: rgba(47, 111, 237, 0.18);
  background: linear-gradient(180deg, rgba(47, 111, 237, 0.06), rgba(252, 252, 249, 0.94));
}

.compact-list {
  margin: 0;
}

.inline-action {
  justify-self: start;
}

.status-error {
  margin: 0 0 16px;
  color: var(--error);
  font-weight: 700;
}

.status-muted {
  color: var(--muted);
}

@media (max-width: 960px) {
  .section-head,
  .editor-grid,
  .editor-grid-wide {
    grid-template-columns: 1fr;
  }
}
</style>

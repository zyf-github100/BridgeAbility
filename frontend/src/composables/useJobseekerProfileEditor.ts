import { computed, reactive, ref } from 'vue'

import {
  getCurrentJobseekerProfile,
  saveCurrentJobseekerProfile,
  type JobseekerAbilityCard,
  type JobseekerProfile,
  type JobseekerProjectExperience,
  type JobseekerSkillTag,
} from '../api/jobseeker'
import { ApiError } from '../lib/http'
import { useAuthStore } from '../stores/auth'

export interface SkillTagForm {
  skillName: string
  skillLevel: number
}

export interface ProjectExperienceForm {
  projectName: string
  roleName: string
  description: string
  startDate: string
  endDate: string
}

export interface ProfileForm {
  realName: string
  schoolName: string
  major: string
  targetCity: string
  expectedJob: string
  workModePreference: string
  intro: string
  skillTags: SkillTagForm[]
  projectExperiences: ProjectExperienceForm[]
}

export function createEmptySkillTag(): SkillTagForm {
  return {
    skillName: '',
    skillLevel: 3,
  }
}

export function createEmptyProjectExperience(): ProjectExperienceForm {
  return {
    projectName: '',
    roleName: '',
    description: '',
    startDate: '',
    endDate: '',
  }
}

export function isProjectExperienceEmpty(project: ProjectExperienceForm) {
  return (
    !project.projectName.trim() &&
    !project.roleName.trim() &&
    !project.description.trim() &&
    !project.startDate &&
    !project.endDate
  )
}

function mapSkillTags(skillTags: JobseekerSkillTag[] = []): SkillTagForm[] {
  const mapped = skillTags.map((skill) => ({
    skillName: skill.skillName ?? '',
    skillLevel: skill.skillLevel ?? 3,
  }))
  return mapped.length ? mapped : [createEmptySkillTag()]
}

function mapProjectExperiences(projectExperiences: JobseekerProjectExperience[] = []): ProjectExperienceForm[] {
  const mapped = projectExperiences.map((project) => ({
    projectName: project.projectName ?? '',
    roleName: project.roleName ?? '',
    description: project.description ?? '',
    startDate: project.startDate ?? '',
    endDate: project.endDate ?? '',
  }))
  return mapped.length ? mapped : [createEmptyProjectExperience()]
}

function normalizeOptional(value: string) {
  const trimmed = value.trim()
  return trimmed || undefined
}

export function useJobseekerProfileEditor() {
  const authStore = useAuthStore()

  const isLoading = ref(false)
  const isSaving = ref(false)
  const loadError = ref('')
  const saveError = ref('')
  const saveSuccess = ref('')
  const completionRate = ref(0)
  const abilityCards = ref<JobseekerAbilityCard[]>([])
  const loadedProfile = ref<JobseekerProfile | null>(null)

  const profileForm = reactive<ProfileForm>({
    realName: '',
    schoolName: '',
    major: '',
    targetCity: '',
    expectedJob: '',
    workModePreference: '',
    intro: '',
    skillTags: [createEmptySkillTag()],
    projectExperiences: [createEmptyProjectExperience()],
  })

  const displayName = computed(
    () => profileForm.realName || authStore.nickname || authStore.account || '未命名用户',
  )
  const skillTagPreview = computed(() =>
    profileForm.skillTags
      .filter((skill) => skill.skillName.trim())
      .map((skill) => `${skill.skillName.trim()} / Lv.${skill.skillLevel}`),
  )
  const projectPreview = computed(() =>
    profileForm.projectExperiences.filter((project) => !isProjectExperienceEmpty(project)),
  )
  const basicProfileErrors = computed(() => {
    const nextErrors: Array<{ id: string; label: string; message: string }> = []

    if (!profileForm.realName.trim()) {
      nextErrors.push({
        id: 'profile-real-name',
        label: '真实姓名',
        message: '请先填写真实姓名，再保存基础档案。',
      })
    }

    return nextErrors
  })
  const abilityProfileErrors = computed(() => {
    const nextErrors = [...basicProfileErrors.value]
    const skillNames = new Set<string>()

    profileForm.skillTags.forEach((skill, index) => {
      const skillName = skill.skillName.trim()
      if (!skillName) {
        return
      }

      const normalized = skillName.toLowerCase()
      if (skillNames.has(normalized)) {
        nextErrors.push({
          id: `skill-name-${index}`,
          label: `技能标签 ${index + 1}`,
          message: '技能标签不要重复，避免生成重复能力卡。',
        })
        return
      }

      skillNames.add(normalized)
    })

    if (profileForm.skillTags.filter((skill) => skill.skillName.trim()).length > 20) {
      nextErrors.push({
        id: 'skill-tags',
        label: '技能标签',
        message: '技能标签最多保留 20 条。',
      })
    }

    if (projectPreview.value.length > 10) {
      nextErrors.push({
        id: 'project-experiences',
        label: '项目经历',
        message: '项目经历最多保留 10 条。',
      })
    }

    profileForm.projectExperiences.forEach((project, index) => {
      if (isProjectExperienceEmpty(project)) {
        return
      }

      if (!project.projectName.trim()) {
        nextErrors.push({
          id: `project-name-${index}`,
          label: `项目经历 ${index + 1}`,
          message: '已填写角色、描述或日期时，项目名称不能为空。',
        })
      }

      if (project.startDate && project.endDate && project.endDate < project.startDate) {
        nextErrors.push({
          id: `project-end-date-${index}`,
          label: `项目经历 ${index + 1}`,
          message: '项目结束日期不能早于开始日期。',
        })
      }
    })

    return nextErrors
  })

  function clearFeedback() {
    saveError.value = ''
    saveSuccess.value = ''
  }

  function addSkillTag() {
    if (profileForm.skillTags.length >= 20) {
      return
    }
    profileForm.skillTags.push(createEmptySkillTag())
  }

  function removeSkillTag(index: number) {
    profileForm.skillTags.splice(index, 1)
    if (!profileForm.skillTags.length) {
      profileForm.skillTags.push(createEmptySkillTag())
    }
  }

  function addProjectExperience() {
    if (profileForm.projectExperiences.length >= 10) {
      return
    }
    profileForm.projectExperiences.push(createEmptyProjectExperience())
  }

  function removeProjectExperience(index: number) {
    profileForm.projectExperiences.splice(index, 1)
    if (!profileForm.projectExperiences.length) {
      profileForm.projectExperiences.push(createEmptyProjectExperience())
    }
  }

  function applyEmptyProfile() {
    loadedProfile.value = null
    profileForm.realName = ''
    profileForm.schoolName = ''
    profileForm.major = ''
    profileForm.targetCity = ''
    profileForm.expectedJob = ''
    profileForm.workModePreference = ''
    profileForm.intro = ''
    profileForm.skillTags = [createEmptySkillTag()]
    profileForm.projectExperiences = [createEmptyProjectExperience()]
    abilityCards.value = []
    completionRate.value = 0
  }

  function applyProfileForm(profile: JobseekerProfile) {
    loadedProfile.value = profile
    profileForm.realName = profile.realName ?? ''
    profileForm.schoolName = profile.schoolName ?? ''
    profileForm.major = profile.major ?? ''
    profileForm.targetCity = profile.targetCity ?? ''
    profileForm.expectedJob = profile.expectedJob ?? ''
    profileForm.workModePreference = profile.workModePreference ?? ''
    profileForm.intro = profile.intro ?? ''
    profileForm.skillTags = mapSkillTags(profile.skillTags)
    profileForm.projectExperiences = mapProjectExperiences(profile.projectExperiences)
    abilityCards.value = profile.abilityCards ?? []
    completionRate.value = profile.profileCompletionRate ?? 0
  }

  async function loadProfile() {
    if (!authStore.token) {
      return
    }

    isLoading.value = true
    loadError.value = ''

    try {
      const profile = await getCurrentJobseekerProfile(authStore.token)
      if (!profile) {
        applyEmptyProfile()
        return
      }
      applyProfileForm(profile)
    } catch (error) {
      loadError.value = error instanceof ApiError ? error.message : '求职档案加载失败'
    } finally {
      isLoading.value = false
    }
  }

  function buildPayload() {
    return {
      realName: profileForm.realName.trim(),
      gender: loadedProfile.value?.gender,
      birthYear: loadedProfile.value?.birthYear,
      schoolName: normalizeOptional(profileForm.schoolName),
      major: normalizeOptional(profileForm.major),
      degree: loadedProfile.value?.degree,
      graduationYear: loadedProfile.value?.graduationYear,
      currentCity: loadedProfile.value?.currentCity,
      targetCity: normalizeOptional(profileForm.targetCity),
      expectedJob: normalizeOptional(profileForm.expectedJob),
      expectedSalaryMin: loadedProfile.value?.expectedSalaryMin,
      expectedSalaryMax: loadedProfile.value?.expectedSalaryMax,
      workModePreference: normalizeOptional(profileForm.workModePreference),
      intro: normalizeOptional(profileForm.intro),
      skillTags: profileForm.skillTags
        .map((skill) => ({
          skillName: skill.skillName.trim(),
          skillLevel: skill.skillLevel,
        }))
        .filter((skill) => skill.skillName),
      projectExperiences: profileForm.projectExperiences
        .filter((project) => !isProjectExperienceEmpty(project))
        .map((project) => ({
          projectName: project.projectName.trim(),
          roleName: normalizeOptional(project.roleName),
          description: normalizeOptional(project.description),
          startDate: project.startDate || undefined,
          endDate: project.endDate || undefined,
        })),
    }
  }

  async function saveBasicProfile() {
    clearFeedback()
    if (!authStore.token || basicProfileErrors.value.length > 0) {
      return
    }

    isSaving.value = true

    try {
      const savedProfile = await saveCurrentJobseekerProfile(authStore.token, buildPayload())
      applyProfileForm(savedProfile)
      saveSuccess.value = '基础档案已保存，求职意向和档案完整度已同步更新。'
    } catch (error) {
      saveError.value = error instanceof ApiError ? error.message : '基础档案保存失败'
    } finally {
      isSaving.value = false
    }
  }

  async function saveAbilityProfile() {
    clearFeedback()
    if (!authStore.token || abilityProfileErrors.value.length > 0) {
      return
    }

    isSaving.value = true

    try {
      const savedProfile = await saveCurrentJobseekerProfile(authStore.token, buildPayload())
      applyProfileForm(savedProfile)
      saveSuccess.value = '技能与经历已保存，技能标签、项目经历和能力卡已同步刷新。'
    } catch (error) {
      saveError.value = error instanceof ApiError ? error.message : '技能与经历保存失败'
    } finally {
      isSaving.value = false
    }
  }

  return {
    isLoading,
    isSaving,
    loadError,
    saveError,
    saveSuccess,
    completionRate,
    abilityCards,
    loadedProfile,
    profileForm,
    displayName,
    skillTagPreview,
    projectPreview,
    basicProfileErrors,
    abilityProfileErrors,
    clearFeedback,
    addSkillTag,
    removeSkillTag,
    addProjectExperience,
    removeProjectExperience,
    loadProfile,
    saveBasicProfile,
    saveAbilityProfile,
  }
}

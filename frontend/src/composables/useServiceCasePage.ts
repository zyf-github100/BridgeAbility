import { computed, onMounted, reactive, watch } from 'vue'
import { useRoute } from 'vue-router'

import { useServiceWorkbench } from './useServiceWorkbench'

function getRouteCaseId(value: string | string[] | undefined) {
  if (Array.isArray(value)) {
    return value[0] ?? ''
  }
  return value ?? ''
}

export function useServiceCasePage() {
  const route = useRoute()
  const workbench = reactive(useServiceWorkbench())

  const caseId = computed(() => getRouteCaseId(route.params.caseId as string | string[] | undefined))
  const caseBasePath = computed(() => (caseId.value ? `/service/cases/${caseId.value}` : '/service/cases'))

  async function syncSelectedCase() {
    await workbench.initializeWorkbench()

    if (!caseId.value || caseId.value === workbench.selectedCaseId) {
      return
    }

    if (!workbench.serviceCases.some((item) => item.id === caseId.value)) {
      return
    }

    await workbench.selectCase(caseId.value)
  }

  async function refreshCasePage() {
    await workbench.refreshWorkbench()

    if (!caseId.value || caseId.value === workbench.selectedCaseId) {
      return
    }

    if (!workbench.serviceCases.some((item) => item.id === caseId.value)) {
      return
    }

    await workbench.selectCase(caseId.value)
  }

  watch(
    () => caseId.value,
    async (nextCaseId) => {
      if (!nextCaseId || nextCaseId === workbench.selectedCaseId) {
        return
      }

      if (!workbench.serviceCases.length) {
        return
      }

      if (!workbench.serviceCases.some((item) => item.id === nextCaseId)) {
        return
      }

      await workbench.selectCase(nextCaseId)
    },
  )

  onMounted(() => {
    void syncSelectedCase()
  })

  return {
    workbench,
    caseId,
    caseBasePath,
    refreshCasePage,
  }
}

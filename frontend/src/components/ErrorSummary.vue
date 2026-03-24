<script setup lang="ts">
import { nextTick, ref, watch } from 'vue'

interface FieldError {
  id: string
  label: string
  message: string
}

const props = withDefaults(
  defineProps<{
    title?: string
    errors: FieldError[]
  }>(),
  {
    title: '请先修正以下问题',
  },
)

const summaryRef = ref<HTMLElement | null>(null)

watch(
  () => props.errors.map((error) => `${error.id}:${error.message}`).join('|'),
  async (value) => {
    if (!value) {
      return
    }

    await nextTick()
    summaryRef.value?.focus()
  },
)
</script>

<template>
  <section
    v-if="errors.length > 0"
    ref="summaryRef"
    class="error-summary"
    role="alert"
    tabindex="-1"
    aria-labelledby="error-summary-title"
  >
    <p class="eyebrow">错误摘要</p>
    <h2 id="error-summary-title">{{ title }}</h2>
    <ul>
      <li v-for="error in errors" :key="error.id">
        <a :href="`#${error.id}`">{{ error.label }}：{{ error.message }}</a>
      </li>
    </ul>
  </section>
</template>

<script setup lang="ts">
defineProps<{
  label: string
  value: number
}>()
</script>

<template>
  <div class="score-pair" role="meter" :aria-valuenow="value" aria-valuemin="0" aria-valuemax="100">
    <div class="score-labels">
      <span>{{ label }}</span>
      <strong>{{ value }}</strong>
    </div>
    <div class="score-track" aria-hidden="true">
      <span class="score-fill" :style="{ width: `${value}%` }"></span>
    </div>
  </div>
</template>

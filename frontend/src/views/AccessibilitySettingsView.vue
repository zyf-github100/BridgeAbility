<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { RouterLink } from 'vue-router'

import type { AccessibilityPreset } from '../stores/accessibility'
import { useAccessibilityStore } from '../stores/accessibility'

const accessibilityStore = useAccessibilityStore()

const presetOptions: Array<{
  value: AccessibilityPreset
  label: string
  summary: string
}> = [
  { value: 'balanced', label: '平衡模式', summary: '保持默认视觉层级，仅保留清晰焦点。' },
  { value: 'comfort', label: '舒适模式', summary: '增大字号、强化链接识别，适合长时间阅读。' },
  { value: 'focus', label: '专注模式', summary: '开启高对比度、减少动效，强调键盘导航。' },
]

const enabledFeatures = computed<string[]>(() =>
  [
    accessibilityStore.preferences.highContrast ? '高对比度' : null,
    accessibilityStore.preferences.largeText ? '大字号' : null,
    accessibilityStore.preferences.reduceMotion ? '减少动效' : null,
    accessibilityStore.preferences.keyboardFocus ? '键盘聚焦' : null,
    accessibilityStore.preferences.underlineLinks ? '链接下划线' : null,
  ].filter((item): item is string => Boolean(item)),
)

function toggleFeature(
  key: 'highContrast' | 'largeText' | 'reduceMotion' | 'keyboardFocus' | 'underlineLinks',
) {
  accessibilityStore.updatePreferences({
    [key]: !accessibilityStore.preferences[key],
  })
}

onMounted(() => {
  accessibilityStore.hydrate()
})
</script>

<template>
  <section class="page-surface settings-shell">
    <div class="page-head">
      <div>
        <p class="eyebrow">Accessibility</p>
        <h2>无障碍模式设置</h2>
        <p class="body-copy">调整后的显示偏好会在常用页面中持续生效，让浏览更轻松。</p>
      </div>
      <div class="page-actions">
        <RouterLink class="secondary-link" to="/help-center">帮助中心</RouterLink>
        <button type="button" class="primary-button" @click="accessibilityStore.resetPreferences">恢复默认</button>
      </div>
    </div>

    <section class="metric-strip">
      <div class="metric-cell">
        <span>当前预设</span>
        <strong>{{ presetOptions.find((item) => item.value === accessibilityStore.preferences.preset)?.label }}</strong>
      </div>
      <div class="metric-cell">
        <span>已启用特性</span>
        <strong>{{ accessibilityStore.enabledCount }}</strong>
        <p>实时写入本地浏览器配置</p>
      </div>
      <div class="metric-cell">
        <span>链接识别</span>
        <strong>{{ accessibilityStore.preferences.underlineLinks ? '开启' : '关闭' }}</strong>
        <p>增强正文中的链接辨识度</p>
      </div>
      <div class="metric-cell">
        <span>动效策略</span>
        <strong>{{ accessibilityStore.preferences.reduceMotion ? '减弱' : '默认' }}</strong>
        <p>适合对动画敏感的访问场景</p>
      </div>
    </section>

    <section class="content-columns">
      <article class="ledger-panel settings-main">
        <div class="panel-headline">
          <div>
            <p class="eyebrow">预设模式</p>
            <h3>一键切换常用组合</h3>
          </div>
        </div>

        <div class="preset-grid">
          <button
            v-for="preset in presetOptions"
            :key="preset.value"
            type="button"
            class="preset-card"
            :class="{ 'is-active': preset.value === accessibilityStore.preferences.preset }"
            @click="accessibilityStore.applyPreset(preset.value)"
          >
            <div class="row-head">
              <h4>{{ preset.label }}</h4>
              <span>{{ preset.value === accessibilityStore.preferences.preset ? '当前' : '切换' }}</span>
            </div>
            <p>{{ preset.summary }}</p>
          </button>
        </div>

        <div class="panel-headline">
          <div>
            <p class="eyebrow">单项能力</p>
            <h3>按需细调显示偏好</h3>
          </div>
        </div>

        <div class="feature-grid">
          <article class="feature-card">
            <div class="row-head">
              <h4>高对比度</h4>
              <button type="button" class="toggle-button" @click="toggleFeature('highContrast')">
                {{ accessibilityStore.preferences.highContrast ? '已开启' : '开启' }}
              </button>
            </div>
            <p>增强前景和背景反差，适合低对比环境或视觉疲劳时使用。</p>
          </article>

          <article class="feature-card">
            <div class="row-head">
              <h4>大字号</h4>
              <button type="button" class="toggle-button" @click="toggleFeature('largeText')">
                {{ accessibilityStore.preferences.largeText ? '已开启' : '开启' }}
              </button>
            </div>
            <p>放大整体字号和阅读节奏，适合长文档和密集表单。</p>
          </article>

          <article class="feature-card">
            <div class="row-head">
              <h4>减少动效</h4>
              <button type="button" class="toggle-button" @click="toggleFeature('reduceMotion')">
                {{ accessibilityStore.preferences.reduceMotion ? '已开启' : '开启' }}
              </button>
            </div>
            <p>降低过渡与动画带来的刺激，减少滚动和交互时的不适感。</p>
          </article>

          <article class="feature-card">
            <div class="row-head">
              <h4>键盘聚焦</h4>
              <button type="button" class="toggle-button" @click="toggleFeature('keyboardFocus')">
                {{ accessibilityStore.preferences.keyboardFocus ? '已开启' : '开启' }}
              </button>
            </div>
            <p>强化焦点描边与跳转提示，方便纯键盘导航。</p>
          </article>

          <article class="feature-card">
            <div class="row-head">
              <h4>链接下划线</h4>
              <button type="button" class="toggle-button" @click="toggleFeature('underlineLinks')">
                {{ accessibilityStore.preferences.underlineLinks ? '已开启' : '开启' }}
              </button>
            </div>
            <p>让文本中的链接拥有更明确的可识别样式。</p>
          </article>
        </div>
      </article>

      <aside class="detail-sidebar settings-sidebar">
        <div class="detail-block">
          <p class="eyebrow">状态区</p>
          <h3>当前已生效</h3>
          <ul class="detail-list">
            <li v-for="item in enabledFeatures" :key="item">{{ item }}</li>
            <li v-if="enabledFeatures.length === 0">当前使用默认显示。</li>
          </ul>
        </div>

        <div class="detail-block">
          <p class="eyebrow">预览说明</p>
          <ul class="detail-list">
            <li>设置会实时应用到当前页面，并在后续访问时继续生效。</li>
            <li>偏好会保存在当前浏览器，下次访问时仍会保留。</li>
            <li>若需要恢复平台默认风格，可随时点击“恢复默认”。</li>
          </ul>
        </div>

        <div class="detail-block">
          <p class="eyebrow">快捷访问</p>
          <div class="settings-links">
            <RouterLink class="secondary-link" to="/overview">首页</RouterLink>
            <RouterLink class="secondary-link" to="/help-center">帮助中心</RouterLink>
            <RouterLink class="secondary-link" to="/login">登录页</RouterLink>
          </div>
        </div>
      </aside>
    </section>
  </section>
</template>

<style scoped>
.settings-shell,
.settings-main,
.settings-sidebar,
.settings-links {
  display: grid;
  gap: 18px;
}

.preset-grid,
.feature-grid {
  display: grid;
  gap: 14px;
}

.preset-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.preset-card,
.feature-card {
  display: grid;
  gap: 10px;
  padding: 18px;
  border: 1px solid var(--line);
  background: rgba(255, 255, 255, 0.78);
  text-align: left;
}

.preset-card.is-active {
  border-color: var(--brand);
  background: rgba(37, 99, 235, 0.08);
}

.preset-card p,
.feature-card p {
  margin: 0;
  line-height: 1.75;
}

@media (max-width: 980px) {
  .preset-grid {
    grid-template-columns: 1fr;
  }
}
</style>

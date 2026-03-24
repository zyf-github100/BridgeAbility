import { defineStore } from 'pinia'
import { computed, ref } from 'vue'

export type AccessibilityPreset = 'balanced' | 'comfort' | 'focus'

export interface AccessibilityPreferences {
  preset: AccessibilityPreset
  highContrast: boolean
  largeText: boolean
  reduceMotion: boolean
  keyboardFocus: boolean
  underlineLinks: boolean
}

const STORAGE_KEY = 'bridge-ability-accessibility'

const defaultPreferences: AccessibilityPreferences = {
  preset: 'balanced',
  highContrast: false,
  largeText: false,
  reduceMotion: false,
  keyboardFocus: true,
  underlineLinks: false,
}

export const useAccessibilityStore = defineStore('accessibility', () => {
  const preferences = ref<AccessibilityPreferences>({ ...defaultPreferences })
  const hydrated = ref(false)

  const enabledCount = computed(
    () =>
      [
        preferences.value.highContrast,
        preferences.value.largeText,
        preferences.value.reduceMotion,
        preferences.value.keyboardFocus,
        preferences.value.underlineLinks,
      ].filter(Boolean).length,
  )

  function hydrate() {
    if (hydrated.value || typeof window === 'undefined') {
      applyDocumentSettings()
      return
    }

    const raw = window.localStorage.getItem(STORAGE_KEY)
    if (raw) {
      try {
        preferences.value = {
          ...defaultPreferences,
          ...(JSON.parse(raw) as Partial<AccessibilityPreferences>),
        }
      } catch {
        window.localStorage.removeItem(STORAGE_KEY)
      }
    }
    hydrated.value = true
    applyDocumentSettings()
  }

  function updatePreferences(nextPreferences: Partial<AccessibilityPreferences>) {
    preferences.value = {
      ...preferences.value,
      ...nextPreferences,
    }
    persist()
  }

  function applyPreset(preset: AccessibilityPreset) {
    const presetOverrides: Record<AccessibilityPreset, Partial<AccessibilityPreferences>> = {
      balanced: {
        preset,
        highContrast: false,
        largeText: false,
        reduceMotion: false,
        keyboardFocus: true,
        underlineLinks: false,
      },
      comfort: {
        preset,
        highContrast: false,
        largeText: true,
        reduceMotion: false,
        keyboardFocus: true,
        underlineLinks: true,
      },
      focus: {
        preset,
        highContrast: true,
        largeText: true,
        reduceMotion: true,
        keyboardFocus: true,
        underlineLinks: true,
      },
    }

    preferences.value = {
      ...preferences.value,
      ...presetOverrides[preset],
    }
    persist()
  }

  function resetPreferences() {
    preferences.value = { ...defaultPreferences }
    persist()
  }

  function persist() {
    applyDocumentSettings()
    if (typeof window === 'undefined') {
      return
    }
    window.localStorage.setItem(STORAGE_KEY, JSON.stringify(preferences.value))
  }

  function applyDocumentSettings() {
    if (typeof document === 'undefined') {
      return
    }

    const root = document.documentElement
    root.dataset.accessibilityPreset = preferences.value.preset
    root.classList.toggle('a11y-high-contrast', preferences.value.highContrast)
    root.classList.toggle('a11y-large-text', preferences.value.largeText)
    root.classList.toggle('a11y-reduce-motion', preferences.value.reduceMotion)
    root.classList.toggle('a11y-keyboard-focus', preferences.value.keyboardFocus)
    root.classList.toggle('a11y-underline-links', preferences.value.underlineLinks)
  }

  return {
    preferences,
    enabledCount,
    hydrate,
    updatePreferences,
    applyPreset,
    resetPreferences,
  }
})

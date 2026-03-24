import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

import type { AuthSession, LoginPayload, RegisterPayload } from '../api/auth'
import { login, logout, register } from '../api/auth'

const STORAGE_KEY = 'bridge-ability-auth'

export const useAuthStore = defineStore('auth', () => {
  const session = ref<AuthSession | null>(null)
  const hydrated = ref(false)

  const isAuthenticated = computed(() => Boolean(session.value?.accessToken))
  const token = computed(() => session.value?.accessToken ?? '')
  const roles = computed(() => session.value?.roles ?? [])
  const account = computed(() => session.value?.account ?? '')
  const nickname = computed(() => session.value?.nickname ?? '')
  const defaultRoute = computed(() => {
    if (roles.value.includes('ROLE_ADMIN')) {
      return '/admin'
    }
    if (roles.value.includes('ROLE_ENTERPRISE')) {
      return '/enterprise'
    }
    if (roles.value.includes('ROLE_SERVICE_ORG')) {
      return '/service'
    }
    return '/workspace'
  })

  function hydrate() {
    if (hydrated.value || typeof window === 'undefined') {
      return
    }

    const raw = window.localStorage.getItem(STORAGE_KEY)
    if (raw) {
      try {
        session.value = JSON.parse(raw) as AuthSession
      } catch {
        window.localStorage.removeItem(STORAGE_KEY)
      }
    }
    hydrated.value = true
  }

  function persist(nextSession: AuthSession | null) {
    session.value = nextSession

    if (typeof window === 'undefined') {
      return
    }

    if (!nextSession) {
      window.localStorage.removeItem(STORAGE_KEY)
      return
    }

    window.localStorage.setItem(STORAGE_KEY, JSON.stringify(nextSession))
  }

  async function loginWithPassword(payload: LoginPayload) {
    const nextSession = await login(payload)
    persist(nextSession)
    return nextSession
  }

  async function registerAccount(payload: RegisterPayload) {
    const nextSession = await register(payload)
    persist(nextSession)
    return nextSession
  }

  async function logoutCurrentUser() {
    try {
      if (token.value) {
        await logout(token.value)
      }
    } finally {
      persist(null)
    }
  }

  return {
    session,
    isAuthenticated,
    token,
    roles,
    account,
    nickname,
    defaultRoute,
    hydrate,
    loginWithPassword,
    registerAccount,
    logoutCurrentUser,
  }
})

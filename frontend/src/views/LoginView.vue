<script setup lang="ts">
import { computed, onUnmounted, reactive, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'

import { sendRegisterEmailCode as requestRegisterEmailCode } from '../api/auth'
import ErrorSummary from '../components/ErrorSummary.vue'
import { ApiError } from '../lib/http'
import { useAuthStore } from '../stores/auth'

type AuthMode = 'login' | 'register'
type RoleCode = 'JOBSEEKER' | 'ENTERPRISE' | 'SERVICE_ORG'

interface LoginForm {
  account: string
  password: string
  remember: boolean
}

interface RegisterForm {
  account: string
  password: string
  confirmPassword: string
  phone: string
  email: string
  emailVerificationCode: string
  roleCode: RoleCode
}

interface FieldError {
  id: string
  label: string
  message: string
}

interface AuthCardCopy {
  eyebrow: string
  title: string
}

interface AuthShowcaseMetric {
  label: string
  value: string
}

interface AuthShowcaseCopy {
  kicker: string
  status: string
  metrics: AuthShowcaseMetric[]
}

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const authMode = ref<AuthMode>('login')
const hasSubmitted = ref(false)
const isSubmitting = ref(false)
const submitError = ref('')
const emailCodeNotice = ref('')
const isSendingEmailCode = ref(false)
const emailCodeCooldown = ref(0)
const emailCodeTarget = ref('')
let emailCodeTimer: number | null = null

const loginForm = reactive<LoginForm>({
  account: '',
  password: '',
  remember: true,
})

const registerForm = reactive<RegisterForm>({
  account: '',
  password: '',
  confirmPassword: '',
  phone: '',
  email: '',
  emailVerificationCode: '',
  roleCode: 'JOBSEEKER',
})

const loginTouched = reactive({
  account: false,
  password: false,
})

const registerTouched = reactive({
  account: false,
  password: false,
  confirmPassword: false,
  phone: false,
  email: false,
  emailVerificationCode: false,
})

const roleOptions: Array<{ code: RoleCode; label: string; hint: string }> = [
  { code: 'JOBSEEKER', label: '求职者', hint: '管理个人档案、岗位投递与支持需求' },
  { code: 'ENTERPRISE', label: '企业', hint: '发布岗位、查看候选人与招聘进度' },
  { code: 'SERVICE_ORG', label: '服务机构', hint: '跟进个案、记录服务动作与预警' },
]

const accountPattern = /^[A-Za-z0-9_]{4,32}$/
const passwordPattern = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).{8,20}$/
const phonePattern = /^1\d{10}$/
const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
const emailCodePattern = /^\d{6}$/

function normalizeEmail(email: string) {
  return email.trim().toLowerCase()
}

function clearEmailCodeTimer() {
  if (emailCodeTimer === null || typeof window === 'undefined') {
    return
  }
  window.clearInterval(emailCodeTimer)
  emailCodeTimer = null
}

function resetEmailCodeState() {
  clearEmailCodeTimer()
  emailCodeNotice.value = ''
  isSendingEmailCode.value = false
  emailCodeCooldown.value = 0
  emailCodeTarget.value = ''
  registerForm.emailVerificationCode = ''
  registerTouched.emailVerificationCode = false
}

function startEmailCodeCooldown(targetEmail: string) {
  clearEmailCodeTimer()
  emailCodeTarget.value = targetEmail
  emailCodeCooldown.value = 60
  if (typeof window === 'undefined') {
    return
  }
  emailCodeTimer = window.setInterval(() => {
    if (emailCodeCooldown.value <= 1) {
      clearEmailCodeTimer()
      emailCodeCooldown.value = 0
      return
    }
    emailCodeCooldown.value -= 1
  }, 1000)
}

function resetTouchedState() {
  loginTouched.account = false
  loginTouched.password = false
  registerTouched.account = false
  registerTouched.password = false
  registerTouched.confirmPassword = false
  registerTouched.phone = false
  registerTouched.email = false
  registerTouched.emailVerificationCode = false
}

watch(
  () => route.meta.authMode,
  (mode) => {
    authMode.value = mode === 'register' ? 'register' : 'login'
    hasSubmitted.value = false
    isSubmitting.value = false
    submitError.value = ''
    resetTouchedState()
    resetEmailCodeState()
  },
  { immediate: true },
)

onUnmounted(() => {
  clearEmailCodeTimer()
})

const loginErrors = computed<FieldError[]>(() => {
  const errors: FieldError[] = []

  if (!loginForm.account.trim()) {
    errors.push({
      id: 'login-account',
      label: '登录账号',
      message: '请输入登录账号',
    })
  }

  if (!loginForm.password.trim()) {
    errors.push({
      id: 'login-password',
      label: '密码',
      message: '请输入密码',
    })
  } else if (loginForm.password.trim().length < 8) {
    errors.push({
      id: 'login-password',
      label: '密码',
      message: '密码至少需要 8 个字符',
    })
  }

  return errors
})

const registerErrors = computed<FieldError[]>(() => {
  const errors: FieldError[] = []
  const account = registerForm.account.trim()
  const password = registerForm.password.trim()
  const phone = registerForm.phone.trim()
  const email = registerForm.email.trim()
  const emailVerificationCode = registerForm.emailVerificationCode.trim()

  if (!account) {
    errors.push({
      id: 'register-account',
      label: '登录账号',
      message: '请输入登录账号',
    })
  } else if (!accountPattern.test(account)) {
    errors.push({
      id: 'register-account',
      label: '登录账号',
      message: '账号需为 4-32 位字母、数字或下划线',
    })
  }

  if (!password) {
    errors.push({
      id: 'register-password',
      label: '密码',
      message: '请输入密码',
    })
  } else if (!passwordPattern.test(password)) {
    errors.push({
      id: 'register-password',
      label: '密码',
      message: '密码需为 8-20 位，且包含大小写字母和数字',
    })
  }

  if (!registerForm.confirmPassword.trim()) {
    errors.push({
      id: 'register-confirm-password',
      label: '确认密码',
      message: '请再次输入密码',
    })
  } else if (registerForm.confirmPassword !== registerForm.password) {
    errors.push({
      id: 'register-confirm-password',
      label: '确认密码',
      message: '两次输入的密码不一致',
    })
  }

  if (phone && !phonePattern.test(phone)) {
    errors.push({
      id: 'register-phone',
      label: '手机号',
      message: '请输入 11 位手机号',
    })
  }

  if (email && !emailPattern.test(email)) {
    errors.push({
      id: 'register-email',
      label: '邮箱',
      message: '请输入有效的邮箱格式',
    })
  }

  if (!email) {
    errors.push({
      id: 'register-email',
      label: '邮箱',
      message: '请输入邮箱',
    })
  }

  if (!emailVerificationCode) {
    errors.push({
      id: 'register-email-code',
      label: '邮箱验证码',
      message: '请输入邮箱验证码',
    })
  } else if (!emailCodePattern.test(emailVerificationCode)) {
    errors.push({
      id: 'register-email-code',
      label: '邮箱验证码',
      message: '请输入 6 位验证码',
    })
  }

  return errors
})

const activeErrors = computed(() =>
  authMode.value === 'login' ? loginErrors.value : registerErrors.value,
)

function fieldError(id: string, touched: boolean) {
  if (!hasSubmitted.value && !touched) {
    return ''
  }

  return activeErrors.value.find((error) => error.id === id)?.message ?? ''
}

const loginAccountError = computed(() => fieldError('login-account', loginTouched.account))
const loginPasswordError = computed(() => fieldError('login-password', loginTouched.password))
const registerAccountError = computed(() =>
  fieldError('register-account', registerTouched.account),
)
const registerPasswordError = computed(() =>
  fieldError('register-password', registerTouched.password),
)
const registerConfirmPasswordError = computed(() =>
  fieldError('register-confirm-password', registerTouched.confirmPassword),
)
const registerPhoneError = computed(() => fieldError('register-phone', registerTouched.phone))
const registerEmailError = computed(() => fieldError('register-email', registerTouched.email))
const registerEmailCodeError = computed(() =>
  fieldError('register-email-code', registerTouched.emailVerificationCode),
)
const isEmailCodeCoolingDown = computed(
  () => emailCodeCooldown.value > 0 && emailCodeTarget.value === normalizeEmail(registerForm.email),
)
const emailCodeButtonText = computed(() => {
  if (isSendingEmailCode.value) {
    return '发送中...'
  }
  if (isEmailCodeCoolingDown.value) {
    return `${emailCodeCooldown.value}s 后重发`
  }
  return '发送验证码'
})

const authTitleId = computed(() => (authMode.value === 'login' ? 'login-title' : 'register-title'))

const authCardCopy = computed<AuthCardCopy>(() =>
  authMode.value === 'login'
    ? {
        eyebrow: 'Welcome back',
        title: '登录你的账户',
      }
    : {
        eyebrow: 'Create workspace',
        title: '创建你的账户',
      },
)

const authShowcaseCopy = computed<AuthShowcaseCopy>(() =>
  authMode.value === 'login'
    ? {
        kicker: '无障碍就业平台',
        status: '求职与招聘',
        metrics: [
          { label: '岗位沟通', value: '已准备' },
          { label: '档案同步', value: '已同步' },
          { label: '服务响应', value: '在线' },
        ],
      }
    : {
        kicker: '账号注册',
        status: '选择身份',
        metrics: [
          { label: '身份选择', value: '3 类' },
          { label: '资料建档', value: '准备中' },
          { label: '注册准备', value: '就绪' },
        ],
      },
)

const orbitLabels = computed(() => roleOptions.map((role) => role.label))

const showcaseCoreLabel = computed(() =>
  authMode.value === 'login' ? '欢迎登录' : '创建账号',
)

const showcaseAccent = computed(() =>
  authMode.value === 'login' ? '已准备' : '可注册',
)

const showcasePillLabels = computed(() => [
  authMode.value === 'login' ? '岗位沟通' : '资料建档',
  authMode.value === 'login' ? '档案完善' : '身份选择',
  authMode.value === 'login' ? '支持跟进' : '服务接入',
])

function switchMode(mode: AuthMode) {
  submitError.value = ''
  hasSubmitted.value = false
  isSubmitting.value = false
  router.replace(mode === 'login' ? '/login' : '/register')
}

function resolveNextRoute() {
  const redirect = route.query.redirect
  if (typeof redirect === 'string' && redirect.startsWith('/')) {
    return redirect
  }
  return authStore.defaultRoute
}

function updateShowcaseMotion(event: PointerEvent) {
  const currentTarget = event.currentTarget
  if (!(currentTarget instanceof HTMLElement)) {
    return
  }

  const rect = currentTarget.getBoundingClientRect()
  const pointerX = event.clientX - rect.left
  const pointerY = event.clientY - rect.top
  const ratioX = pointerX / rect.width
  const ratioY = pointerY / rect.height
  const tiltX = (0.5 - ratioY) * 5.5
  const tiltY = (ratioX - 0.5) * 6.5

  currentTarget.style.setProperty('--spotlight-x', `${pointerX}px`)
  currentTarget.style.setProperty('--spotlight-y', `${pointerY}px`)
  currentTarget.style.setProperty('--tilt-x', `${tiltX.toFixed(2)}deg`)
  currentTarget.style.setProperty('--tilt-y', `${tiltY.toFixed(2)}deg`)
}

function resetShowcaseMotion(event: PointerEvent) {
  const currentTarget = event.currentTarget
  if (!(currentTarget instanceof HTMLElement)) {
    return
  }

  currentTarget.style.setProperty('--spotlight-x', '50%')
  currentTarget.style.setProperty('--spotlight-y', '38%')
  currentTarget.style.setProperty('--tilt-x', '0deg')
  currentTarget.style.setProperty('--tilt-y', '0deg')
}

async function submitLogin() {
  submitError.value = ''
  hasSubmitted.value = true
  loginTouched.account = true
  loginTouched.password = true

  if (loginErrors.value.length > 0) {
    return
  }

  isSubmitting.value = true
  try {
    await authStore.loginWithPassword({
      account: loginForm.account.trim(),
      password: loginForm.password,
    })
    await router.push(resolveNextRoute())
  } catch (error) {
    submitError.value = error instanceof ApiError ? error.message : '登录失败，请稍后重试'
  } finally {
    isSubmitting.value = false
  }
}

async function sendEmailCode() {
  submitError.value = ''
  emailCodeNotice.value = ''
  registerTouched.email = true

  const email = registerForm.email.trim()
  if (!email) {
    hasSubmitted.value = true
    return
  }

  if (!emailPattern.test(email)) {
    hasSubmitted.value = true
    return
  }

  isSendingEmailCode.value = true
  try {
    await requestRegisterEmailCode({
      email,
    })
    emailCodeNotice.value = '验证码已发送，请查收邮箱。'
    startEmailCodeCooldown(normalizeEmail(email))
  } catch (error) {
    submitError.value = error instanceof ApiError ? error.message : '发送验证码失败，请稍后重试'
  } finally {
    isSendingEmailCode.value = false
  }
}

async function submitRegister() {
  submitError.value = ''
  hasSubmitted.value = true
  registerTouched.account = true
  registerTouched.password = true
  registerTouched.confirmPassword = true
  registerTouched.phone = true
  registerTouched.email = true
  registerTouched.emailVerificationCode = true

  if (registerErrors.value.length > 0) {
    return
  }

  isSubmitting.value = true
  try {
    await authStore.registerAccount({
      account: registerForm.account.trim(),
      password: registerForm.password,
      nickname: registerForm.account.trim(),
      phone: registerForm.phone.trim() || undefined,
      email: registerForm.email.trim(),
      emailVerificationCode: registerForm.emailVerificationCode.trim(),
      roles: [registerForm.roleCode],
    })
    await router.push(resolveNextRoute())
  } catch (error) {
    submitError.value = error instanceof ApiError ? error.message : '注册失败，请稍后重试'
  } finally {
    isSubmitting.value = false
  }
}
</script>

<template>
  <section class="auth-shell">
    <div class="auth-backdrop" aria-hidden="true"></div>

    <header class="auth-header">
      <RouterLink class="auth-brand" to="/login">
        <span class="brand-mark">RZQ</span>
        <div>
          <strong>融职桥</strong>
          <small>BridgeAbility</small>
        </div>
      </RouterLink>
    </header>

    <main class="auth-stage">
      <section class="auth-layout">
        <section class="auth-showcase" aria-label="登录视觉效果">
          <div
            class="showcase-panel"
            @pointermove="updateShowcaseMotion"
            @pointerleave="resetShowcaseMotion"
          >
            <div class="showcase-grid" aria-hidden="true"></div>
            <div class="showcase-noise" aria-hidden="true"></div>

            <span class="showcase-kicker">{{ authShowcaseCopy.kicker }}</span>
            <span class="showcase-accent">{{ showcaseAccent }}</span>

            <div class="showcase-stage">
              <span class="aurora aurora-a" aria-hidden="true"></span>
              <span class="aurora aurora-b" aria-hidden="true"></span>
              <span class="aurora aurora-c" aria-hidden="true"></span>
              <span class="orbit orbit-a" aria-hidden="true"></span>
              <span class="orbit orbit-b" aria-hidden="true"></span>
              <span class="orbit orbit-c" aria-hidden="true"></span>

              <div class="showcase-core">
                <small>{{ authShowcaseCopy.status }}</small>
                <strong>{{ showcaseCoreLabel }}</strong>
              </div>

              <span
                v-for="(label, index) in orbitLabels"
                :key="label"
                class="orbit-pill"
                :class="`orbit-pill-${index + 1}`"
              >
                {{ label }}
              </span>

              <article
                v-for="(metric, index) in authShowcaseCopy.metrics"
                :key="metric.label"
                class="signal-card"
                :class="`signal-card-${index + 1}`"
              >
                <span>{{ metric.label }}</span>
                <strong>{{ metric.value }}</strong>
              </article>

              <span
                v-for="(label, index) in showcasePillLabels"
                :key="label"
                class="floating-pill"
                :class="`floating-pill-${index + 1}`"
              >
                {{ label }}
              </span>
            </div>

          </div>
        </section>

        <section class="auth-card" :aria-labelledby="authTitleId">
          <div class="mode-switch" aria-label="认证模式">
            <button
              type="button"
              class="mode-switch-button"
              :class="{ 'is-active': authMode === 'login' }"
              :aria-pressed="authMode === 'login'"
              @click="switchMode('login')"
            >
              登录
            </button>
            <button
              type="button"
              class="mode-switch-button"
              :class="{ 'is-active': authMode === 'register' }"
              :aria-pressed="authMode === 'register'"
              @click="switchMode('register')"
            >
              注册
            </button>
          </div>

          <div class="card-head">
            <p class="card-eyebrow">{{ authCardCopy.eyebrow }}</p>
            <h2 v-if="authMode === 'login'" id="login-title">{{ authCardCopy.title }}</h2>
            <h2 v-else id="register-title">{{ authCardCopy.title }}</h2>
          </div>

          <ErrorSummary
            v-if="hasSubmitted && activeErrors.length > 0"
            :errors="activeErrors"
            title="请先修正以下问题"
          />

          <form v-if="authMode === 'login'" class="auth-form" novalidate @submit.prevent="submitLogin">
            <label class="form-field" for="login-account">
              <span>登录账号</span>
              <input
                id="login-account"
                v-model="loginForm.account"
                class="field-input"
                :class="{ 'is-error': loginAccountError }"
                type="text"
                autocomplete="username"
                placeholder="请输入登录账号"
                @blur="loginTouched.account = true"
              />
              <strong v-if="loginAccountError" class="field-error">{{ loginAccountError }}</strong>
            </label>

            <label class="form-field" for="login-password">
              <span>密码</span>
              <input
                id="login-password"
                v-model="loginForm.password"
                class="field-input"
                :class="{ 'is-error': loginPasswordError }"
                type="password"
                autocomplete="current-password"
                placeholder="请输入密码"
                @blur="loginTouched.password = true"
              />
              <strong v-if="loginPasswordError" class="field-error">{{ loginPasswordError }}</strong>
            </label>

            <label class="remember-row">
              <span class="remember-choice">
                <input v-model="loginForm.remember" type="checkbox" />
                <span>记住当前登录状态</span>
              </span>
              <RouterLink class="inline-text-link" to="/forgot-password">忘记密码？</RouterLink>
            </label>

            <p v-if="submitError" class="submit-error" role="alert">{{ submitError }}</p>

            <button class="submit-button" type="submit" :disabled="isSubmitting">
              {{ isSubmitting ? '登录中...' : '登录' }}
            </button>
          </form>

          <form v-else class="auth-form" novalidate @submit.prevent="submitRegister">
            <label class="form-field" for="register-account">
              <span>登录账号</span>
              <input
                id="register-account"
                v-model="registerForm.account"
                class="field-input"
                :class="{ 'is-error': registerAccountError }"
                type="text"
                autocomplete="username"
                placeholder="4-32 位字母、数字或下划线"
                @blur="registerTouched.account = true"
              />
              <strong v-if="registerAccountError" class="field-error">{{ registerAccountError }}</strong>
            </label>

            <div class="split-fields">
              <label class="form-field" for="register-password">
                <span>密码</span>
                <input
                  id="register-password"
                  v-model="registerForm.password"
                  class="field-input"
                  :class="{ 'is-error': registerPasswordError }"
                  type="password"
                  autocomplete="new-password"
                  placeholder="8-20 位，包含大小写字母和数字"
                  @blur="registerTouched.password = true"
                />
                <strong v-if="registerPasswordError" class="field-error">{{ registerPasswordError }}</strong>
              </label>

              <label class="form-field" for="register-confirm-password">
                <span>确认密码</span>
                <input
                  id="register-confirm-password"
                  v-model="registerForm.confirmPassword"
                  class="field-input"
                  :class="{ 'is-error': registerConfirmPasswordError }"
                  type="password"
                  autocomplete="new-password"
                  placeholder="再次输入密码"
                  @blur="registerTouched.confirmPassword = true"
                />
                <strong v-if="registerConfirmPasswordError" class="field-error">
                  {{ registerConfirmPasswordError }}
                </strong>
              </label>
            </div>

            <div class="split-fields">
              <label class="form-field" for="register-phone">
                <span>手机号</span>
                <input
                  id="register-phone"
                  v-model="registerForm.phone"
                  class="field-input"
                  :class="{ 'is-error': registerPhoneError }"
                  type="tel"
                  autocomplete="tel"
                  placeholder="可选"
                  @blur="registerTouched.phone = true"
                />
                <strong v-if="registerPhoneError" class="field-error">{{ registerPhoneError }}</strong>
              </label>

              <label class="form-field" for="register-email">
                <span>邮箱</span>
                <input
                  id="register-email"
                  v-model="registerForm.email"
                  class="field-input"
                  :class="{ 'is-error': registerEmailError }"
                  type="email"
                  autocomplete="email"
                  placeholder="可选"
                  @blur="registerTouched.email = true"
                />
                <strong v-if="registerEmailError" class="field-error">{{ registerEmailError }}</strong>
              </label>
            </div>

            <label class="form-field" for="register-email-code">
              <span>邮箱验证码</span>
              <div class="verification-row">
                <input
                  id="register-email-code"
                  v-model="registerForm.emailVerificationCode"
                  class="field-input"
                  :class="{ 'is-error': registerEmailCodeError }"
                  type="text"
                  inputmode="numeric"
                  maxlength="6"
                  autocomplete="one-time-code"
                  placeholder="请输入 6 位验证码"
                  @blur="registerTouched.emailVerificationCode = true"
                />
                <button
                  type="button"
                  class="verification-button"
                  :disabled="isSendingEmailCode || isEmailCodeCoolingDown"
                  @click="sendEmailCode"
                >
                  {{ emailCodeButtonText }}
                </button>
              </div>
              <small v-if="emailCodeNotice" class="field-note">{{ emailCodeNotice }}</small>
              <strong v-if="registerEmailCodeError" class="field-error">{{ registerEmailCodeError }}</strong>
            </label>

            <fieldset class="role-fieldset">
              <legend class="role-legend">注册身份</legend>
              <div class="role-grid">
                <label
                  v-for="role in roleOptions"
                  :key="role.code"
                  class="role-option"
                  :class="{ 'is-selected': registerForm.roleCode === role.code }"
                >
                  <input v-model="registerForm.roleCode" type="radio" :value="role.code" />
                  <span class="role-copy">
                    <strong>{{ role.label }}</strong>
                    <small>{{ role.hint }}</small>
                  </span>
                </label>
              </div>
            </fieldset>

            <p v-if="submitError" class="submit-error" role="alert">{{ submitError }}</p>

            <button class="submit-button" type="submit" :disabled="isSubmitting">
              {{ isSubmitting ? '注册中...' : '创建账号' }}
            </button>
          </form>

          <footer class="auth-footer">
            <p v-if="authMode === 'login'">
              还没有账号？
              <button type="button" class="footer-link" @click="switchMode('register')">去注册</button>
            </p>
            <p v-else>
              已有账号？
              <button type="button" class="footer-link" @click="switchMode('login')">返回登录</button>
            </p>
            <div class="auth-utility-links">
              <RouterLink class="inline-text-link" to="/help-center">帮助中心</RouterLink>
              <RouterLink class="inline-text-link" to="/accessibility">无障碍设置</RouterLink>
              <RouterLink class="inline-text-link" to="/forgot-password">找回密码</RouterLink>
            </div>
          </footer>
        </section>
      </section>
    </main>
  </section>
</template>

<style scoped>
.auth-shell {
  --auth-ink: #24364b;
  --auth-text: #5f7489;
  --auth-muted: #90a3b4;
  --auth-line-rgb: 187, 202, 217;
  --auth-accent: #6d93c2;
  --auth-accent-strong: #5b81ac;
  --auth-accent-rgb: 109, 147, 194;
  --auth-mint: #84cb7b;
  --auth-mint-rgb: 132, 203, 123;
  --auth-peach-rgb: 223, 239, 206;
  --spotlight-x: 50%;
  --spotlight-y: 38%;
  --tilt-x: 0deg;
  --tilt-y: 0deg;
  min-height: calc(100vh - 56px);
  position: relative;
  overflow: visible;
  padding: clamp(24px, 3vw, 40px);
  border: 0;
  border-radius: 0;
  background: transparent;
  box-shadow: none;
}

.auth-backdrop {
  position: absolute;
  inset: -6% -4% 0;
  pointer-events: none;
  background:
    linear-gradient(180deg, rgba(248, 251, 252, 0.76) 0%, rgba(248, 251, 252, 0.42) 18%, rgba(248, 251, 252, 0.12) 38%, transparent 58%),
    radial-gradient(circle at 14% 18%, rgba(var(--auth-accent-rgb), 0.12), transparent 24%),
    radial-gradient(circle at 82% 16%, rgba(var(--auth-mint-rgb), 0.12), transparent 22%),
    radial-gradient(circle at 52% 36%, rgba(255, 255, 255, 0.8), rgba(255, 255, 255, 0) 44%),
    radial-gradient(circle at 48% 78%, rgba(var(--auth-peach-rgb), 0.24), rgba(255, 255, 255, 0) 34%);
  -webkit-mask-image: radial-gradient(
    circle at 50% 38%,
    #000 0%,
    #000 60%,
    rgba(0, 0, 0, 0.78) 78%,
    transparent 100%
  );
  mask-image: radial-gradient(
    circle at 50% 38%,
    #000 0%,
    #000 60%,
    rgba(0, 0, 0, 0.78) 78%,
    transparent 100%
  );
}

.auth-backdrop::after {
  content: '';
  position: absolute;
  width: min(58vw, 820px);
  aspect-ratio: 1;
  right: -16%;
  top: -26%;
  border-radius: 50%;
  background:
    radial-gradient(circle at center, rgba(255, 255, 255, 0.88) 0%, rgba(255, 255, 255, 0) 56%),
    conic-gradient(
      from 210deg,
      rgba(var(--auth-accent-rgb), 0),
      rgba(var(--auth-accent-rgb), 0.14),
      rgba(var(--auth-mint-rgb), 0.16),
      rgba(var(--auth-accent-rgb), 0)
    );
  filter: blur(36px);
  opacity: 0.58;
}

.auth-header,
.auth-stage {
  position: relative;
  z-index: 1;
}

.auth-header {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  gap: 20px;
  max-width: 1220px;
  margin: 0 auto;
}

.auth-brand {
  display: inline-flex;
  align-items: center;
  gap: 14px;
  color: var(--auth-ink);
}

.brand-mark {
  width: 50px;
  height: 50px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 1px solid rgba(var(--auth-line-rgb), 0.42);
  border-radius: 17px;
  background: linear-gradient(
    180deg,
    rgba(255, 255, 255, 0.96),
    rgba(243, 249, 250, 0.92)
  );
  font-family: var(--mono);
  font-weight: 600;
  letter-spacing: 0.16em;
  box-shadow: 0 12px 24px rgba(var(--auth-accent-rgb), 0.08);
}

.auth-brand strong,
.auth-brand small {
  display: block;
}

.auth-brand small {
  color: var(--auth-text);
  margin-top: 2px;
}

.auth-stage {
  margin-top: clamp(22px, 3vw, 30px);
  max-width: 1220px;
  margin-left: auto;
  margin-right: auto;
}

.auth-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(420px, 0.78fr);
  gap: 30px;
  align-items: center;
}

.auth-showcase,
.auth-card {
  min-width: 0;
}

.auth-showcase {
  display: grid;
  gap: 16px;
}

.showcase-panel {
  position: relative;
  overflow: visible;
  padding: 12px 12px 0 0;
  background: transparent;
  border: 0;
  border-radius: 0;
  box-shadow: none;
  transform: perspective(1600px) rotateX(var(--tilt-x)) rotateY(var(--tilt-y));
  transition:
    transform 160ms ease,
    box-shadow 160ms ease,
    border-color 160ms ease;
}

.showcase-panel::after {
  content: '';
  position: absolute;
  inset: -8%;
  background: radial-gradient(
    circle at var(--spotlight-x) var(--spotlight-y),
    rgba(var(--auth-accent-rgb), 0.16),
    rgba(var(--auth-mint-rgb), 0.1) 22%,
    rgba(255, 255, 255, 0) 46%
  );
  filter: blur(18px);
  pointer-events: none;
}

.showcase-grid,
.showcase-noise {
  position: absolute;
  inset: 0 0 44px 0;
  pointer-events: none;
  -webkit-mask-image: radial-gradient(
    circle at 48% 42%,
    #000 0%,
    #000 50%,
    rgba(0, 0, 0, 0.54) 74%,
    transparent 96%
  );
  mask-image: radial-gradient(
    circle at 48% 42%,
    #000 0%,
    #000 50%,
    rgba(0, 0, 0, 0.54) 74%,
    transparent 96%
  );
}

.showcase-grid {
  background:
    linear-gradient(to right, rgba(var(--auth-line-rgb), 0.24) 1px, transparent 1px),
    linear-gradient(to bottom, rgba(var(--auth-line-rgb), 0.24) 1px, transparent 1px);
  background-size: 28px 28px;
  opacity: 0.86;
}

.showcase-noise {
  background:
    radial-gradient(circle at top left, rgba(255, 255, 255, 0.42), transparent 28%),
    radial-gradient(circle at 62% 44%, rgba(var(--auth-mint-rgb), 0.12), transparent 38%);
  opacity: 0.5;
}

.showcase-kicker,
.showcase-accent,
.card-eyebrow,
.signal-card span,
.floating-pill,
.orbit-pill {
  font-family: var(--mono);
  font-size: 0.74rem;
  letter-spacing: 0.1em;
  text-transform: uppercase;
}

.showcase-kicker,
.showcase-accent {
  position: relative;
  z-index: 1;
  display: inline-flex;
  align-items: center;
  padding: 0 0 8px;
  border: 0;
  border-radius: 999px;
  background: transparent;
  color: var(--auth-muted);
  box-shadow: none;
}

.showcase-kicker::after,
.showcase-accent::after {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  height: 1px;
  background: linear-gradient(90deg, rgba(var(--auth-line-rgb), 0.72), rgba(var(--auth-line-rgb), 0));
}

.showcase-accent {
  position: absolute;
  right: 12px;
  top: 12px;
}

.showcase-stage {
  position: relative;
  min-height: 348px;
  margin-top: 16px;
}

.showcase-stage::before {
  content: '';
  position: absolute;
  inset: 0;
  border-radius: 0;
  background: radial-gradient(
    circle at 50% 50%,
    rgba(var(--auth-accent-rgb), 0.18),
    rgba(var(--auth-accent-rgb), 0) 56%
  );
}

.aurora,
.orbit {
  position: absolute;
  border-radius: 50%;
  pointer-events: none;
}

.aurora {
  filter: blur(20px);
  opacity: 0.75;
}

.aurora-a {
  inset: 72px 90px 84px 88px;
  background: radial-gradient(
    circle,
    rgba(var(--auth-accent-rgb), 0.28),
    rgba(var(--auth-accent-rgb), 0) 70%
  );
}

.aurora-b {
  inset: 120px 110px 120px 130px;
  background: radial-gradient(
    circle,
    rgba(var(--auth-mint-rgb), 0.24),
    rgba(var(--auth-mint-rgb), 0) 68%
  );
}

.aurora-c {
  inset: 160px 180px 70px 160px;
  background: radial-gradient(
    circle,
    rgba(var(--auth-peach-rgb), 0.22),
    rgba(var(--auth-peach-rgb), 0) 70%
  );
}

.orbit {
  border: 1px solid rgba(var(--auth-line-rgb), 0.34);
}

.orbit-a {
  inset: 54px 118px 104px 118px;
}

.orbit-b {
  inset: 94px 158px 144px 158px;
}

.orbit-c {
  inset: 128px 194px 178px 194px;
}

.showcase-core {
  position: absolute;
  left: 50%;
  top: 53%;
  width: 226px;
  min-height: 176px;
  display: grid;
  align-content: center;
  gap: 8px;
  padding: 22px;
  transform: translate(-50%, -50%);
  border-radius: 0;
  border: 0;
  background: transparent;
  box-shadow: none;
  text-align: center;
  color: var(--auth-text);
}

.showcase-core::before {
  content: '';
  position: absolute;
  inset: -18px;
  border-radius: 50%;
  background: radial-gradient(
    circle,
    rgba(var(--auth-accent-rgb), 0.24),
    rgba(var(--auth-accent-rgb), 0) 72%
  );
  z-index: -1;
}

.showcase-core small {
  font-family: var(--mono);
  font-size: 0.72rem;
  letter-spacing: 0.1em;
  text-transform: uppercase;
}

.showcase-core strong {
  font-family: var(--serif);
  font-size: 1.82rem;
  line-height: 1.15;
  color: var(--auth-ink);
}

.showcase-core p {
  margin: 0;
  max-width: 16ch;
  line-height: 1.5;
  text-wrap: balance;
}

.orbit-pill,
.floating-pill {
  position: absolute;
  z-index: 1;
  padding: 0 0 6px;
  border: 0;
  border-radius: 0;
  background: transparent;
  color: var(--auth-text);
  box-shadow: none;
}

.orbit-pill::after,
.floating-pill::after {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  height: 1px;
  background: linear-gradient(90deg, rgba(var(--auth-line-rgb), 0.78), rgba(var(--auth-line-rgb), 0));
}

.orbit-pill-1 {
  left: 38px;
  top: 98px;
}

.orbit-pill-2 {
  right: 42px;
  top: 88px;
}

.orbit-pill-3 {
  left: 204px;
  bottom: 54px;
}

.signal-card {
  position: absolute;
  display: grid;
  gap: 6px;
  min-width: 96px;
  padding: 8px 0 0 14px;
  border: 0;
  border-left: 1px solid rgba(var(--auth-line-rgb), 0.8);
  border-radius: 0;
  background: transparent;
  box-shadow: none;
}

.signal-card strong {
  font-size: 1.05rem;
  color: var(--auth-ink);
}

.signal-card-1 {
  left: 18px;
  bottom: 104px;
}

.signal-card-2 {
  right: 34px;
  top: 142px;
}

.signal-card-3 {
  right: 28px;
  bottom: 74px;
}

.floating-pill-1 {
  left: 152px;
  top: 72px;
}

.floating-pill-2 {
  left: 364px;
  top: 256px;
}

.floating-pill-3 {
  left: 74px;
  bottom: 66px;
}

.auth-card {
  width: 100%;
  max-width: 520px;
  justify-self: end;
  position: relative;
  padding: 8px 0 8px 38px;
  border: 0;
  border-radius: 0;
  background: transparent;
  box-shadow: none;
}

.auth-card::before {
  content: '';
  position: absolute;
  left: 0;
  top: 10px;
  bottom: 10px;
  width: 1px;
  background: linear-gradient(
    180deg,
    rgba(var(--auth-line-rgb), 0),
    rgba(var(--auth-line-rgb), 0.92) 12%,
    rgba(var(--auth-line-rgb), 0.92) 88%,
    rgba(var(--auth-line-rgb), 0)
  );
}

.mode-switch {
  display: flex;
  gap: 8px;
  padding: 0 0 14px;
  border: 0;
  border-bottom: 1px solid rgba(var(--auth-line-rgb), 0.9);
  border-radius: 0;
  background: transparent;
}

.mode-switch-button {
  flex: 1;
  border: 0;
  padding: 10px 0 14px;
  border-radius: 0;
  background: transparent;
  color: var(--auth-text);
  font-weight: 700;
  transition:
    background-color 160ms ease,
    color 160ms ease,
    box-shadow 160ms ease;
}

.mode-switch-button.is-active {
  color: var(--auth-ink);
  box-shadow: inset 0 -2px 0 var(--auth-accent);
}

.card-head {
  margin: 22px 0 18px;
}

.card-eyebrow {
  margin: 0 0 10px;
  color: var(--auth-muted);
}

.card-head h2 {
  margin: 0;
  font-family: 'STKaiti', 'KaiTi', 'Kaiti SC', 'DFKai-SB', 'Songti SC', serif;
  font-size: clamp(2.1rem, 4vw, 2.8rem);
  line-height: 1.1;
  letter-spacing: 0.02em;
  color: var(--auth-ink);
}

.auth-form {
  display: grid;
  gap: 16px;
}

.split-fields {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.form-field {
  display: grid;
  gap: 8px;
}

.form-field > span,
.role-legend {
  font-weight: 700;
  color: var(--auth-ink);
}

.field-input {
  width: 100%;
  border: 1px solid rgba(var(--auth-line-rgb), 0.76);
  border-radius: 18px;
  padding: 14px 16px;
  background: rgba(255, 255, 255, 0.84);
  color: var(--auth-ink);
  transition:
    border-color 160ms ease,
    box-shadow 160ms ease,
    background-color 160ms ease;
}

.field-input::placeholder {
  color: #b2c0cd;
}

.field-input:hover {
  border-color: rgba(var(--auth-accent-rgb), 0.42);
  box-shadow: none;
  background: rgba(255, 255, 255, 0.9);
}

.field-input:focus,
.field-input:focus-visible {
  outline: none;
  border-color: rgba(var(--auth-accent-rgb), 0.68);
  box-shadow: 0 0 0 2px rgba(var(--auth-accent-rgb), 0.14);
  background: rgba(255, 255, 255, 0.94);
}

.field-input.is-error {
  border-color: #fda4af;
  box-shadow: 0 0 0 2px rgba(244, 63, 94, 0.1);
}

.field-input.is-error:focus,
.field-input.is-error:focus-visible {
  outline: none;
  border-color: #fb7185;
  box-shadow: 0 0 0 2px rgba(244, 63, 94, 0.14);
}

.field-error {
  color: #b42318;
}

.field-note {
  color: var(--auth-text);
  line-height: 1.5;
}

.verification-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 12px;
  align-items: center;
}

.verification-button {
  border: 1px solid rgba(var(--auth-line-rgb), 0.76);
  border-radius: 18px;
  padding: 14px 18px;
  background: rgba(255, 255, 255, 0.92);
  color: var(--auth-accent-strong);
  font-weight: 700;
  white-space: nowrap;
}

.verification-button:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.remember-row,
.remember-choice {
  display: flex;
  align-items: center;
}

.remember-row {
  justify-content: space-between;
  gap: 12px;
  color: var(--auth-text);
}

.remember-choice {
  gap: 10px;
}

.remember-choice input,
.role-option input {
  accent-color: var(--auth-accent);
}

.role-fieldset {
  margin: 0;
  padding: 0;
  border: 0;
}

.role-grid {
  display: grid;
  gap: 10px;
  margin-top: 12px;
}

.role-option {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 10px;
  padding: 14px 16px;
  border: 1px solid rgba(var(--auth-line-rgb), 0.76);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.82);
  transition:
    border-color 160ms ease,
    box-shadow 160ms ease;
}

.role-option.is-selected {
  border-color: rgba(var(--auth-mint-rgb), 0.66);
  box-shadow: 0 0 0 4px rgba(var(--auth-mint-rgb), 0.12);
}

.role-copy strong,
.role-copy small {
  display: block;
}

.role-copy small {
  margin-top: 4px;
  color: var(--auth-text);
  line-height: 1.65;
}

.submit-error {
  margin: 0;
  padding: 12px 14px;
  border-radius: 16px;
  border: 1px solid rgba(244, 63, 94, 0.2);
  background: rgba(254, 226, 226, 0.68);
  color: #991b1b;
}

.submit-button {
  border: 0;
  border-radius: 18px;
  padding: 16px 18px;
  background: linear-gradient(135deg, var(--auth-accent), var(--auth-mint));
  color: #ffffff;
  font-weight: 700;
  box-shadow: 0 18px 34px rgba(var(--auth-accent-rgb), 0.2);
}

.submit-button:disabled {
  opacity: 0.7;
}

.auth-footer {
  margin-top: 22px;
  padding-top: 16px;
  border-top: 1px solid rgba(var(--auth-line-rgb), 0.56);
}

.auth-footer p {
  margin: 0;
  color: var(--auth-text);
}

.footer-link {
  border: 0;
  padding: 0;
  background: transparent;
  color: var(--auth-accent-strong);
  font-weight: 700;
}

.inline-text-link {
  color: var(--auth-accent-strong);
  font-weight: 700;
}

.auth-utility-links {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  margin-top: 16px;
}

@media (max-width: 1180px) {
  .auth-layout {
    grid-template-columns: 1fr;
  }

  .auth-card {
    order: 1;
    max-width: none;
  }

  .auth-showcase {
    order: 2;
  }
}

@media (max-width: 780px) {
  .auth-shell {
    padding: 18px;
    border-radius: 0;
  }

  .auth-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .auth-card,
  .showcase-panel {
    border-radius: 0;
  }

  .showcase-stage {
    min-height: 280px;
  }

  .signal-card,
  .floating-pill,
  .orbit-pill {
    display: none;
  }

  .showcase-core {
    width: min(100%, 220px);
  }

  .auth-card {
    padding-left: 0;
    padding-top: 22px;
  }

  .auth-card::before {
    top: 0;
    right: 0;
    bottom: auto;
    width: auto;
    height: 1px;
  }

  .split-fields {
    grid-template-columns: 1fr;
  }

  .verification-row {
    grid-template-columns: 1fr;
  }

  .remember-row {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>

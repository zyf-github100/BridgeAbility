<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { RouterLink } from 'vue-router'

import { resetPassword, sendPasswordResetEmailCode } from '../api/auth'
import { ApiError } from '../lib/http'

const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

const form = reactive({
  account: '',
  email: '',
  emailVerificationCode: '',
  password: '',
  confirmPassword: '',
})

const isSendingCode = ref(false)
const isSubmitting = ref(false)
const codeNotice = ref('')
const actionError = ref('')
const successMessage = ref('')

const passwordStrength = computed(() => {
  const password = form.password.trim()
  if (password.length >= 12) {
    return '较强'
  }
  if (password.length >= 8) {
    return '可用'
  }
  return '待完善'
})

const completedSteps = computed(() => {
  let count = 0
  if (form.account.trim() && emailPattern.test(form.email.trim())) {
    count += 1
  }
  if (form.emailVerificationCode.trim().length === 6) {
    count += 1
  }
  if (form.password.trim().length >= 8 && form.password === form.confirmPassword) {
    count += 1
  }
  return count
})

function validateAccountStep() {
  if (!form.account.trim()) {
    return '请输入登录账号。'
  }
  if (!emailPattern.test(form.email.trim())) {
    return '请输入与账号绑定的有效邮箱。'
  }
  return ''
}

function validateResetForm() {
  const accountStepError = validateAccountStep()
  if (accountStepError) {
    return accountStepError
  }
  if (!form.emailVerificationCode.trim()) {
    return '请输入邮箱验证码。'
  }
  if (form.password.trim().length < 8) {
    return '新密码至少需要 8 个字符。'
  }
  if (form.password !== form.confirmPassword) {
    return '两次输入的新密码不一致。'
  }
  return ''
}

async function handleSendCode() {
  actionError.value = validateAccountStep()
  successMessage.value = ''
  codeNotice.value = ''

  if (actionError.value) {
    return
  }

  isSendingCode.value = true
  try {
    await sendPasswordResetEmailCode({
      account: form.account.trim(),
      email: form.email.trim(),
    })
    codeNotice.value = '验证码已发送到绑定邮箱，请在 10 分钟内完成重置。'
  } catch (error) {
    actionError.value = error instanceof ApiError ? error.message : '验证码发送失败，请稍后重试。'
  } finally {
    isSendingCode.value = false
  }
}

async function handleResetPassword() {
  actionError.value = validateResetForm()
  successMessage.value = ''

  if (actionError.value) {
    return
  }

  isSubmitting.value = true
  try {
    await resetPassword({
      account: form.account.trim(),
      email: form.email.trim(),
      emailVerificationCode: form.emailVerificationCode.trim(),
      password: form.password,
    })
    successMessage.value = '密码已重置成功。你现在可以使用新密码重新登录。'
    form.emailVerificationCode = ''
    form.password = ''
    form.confirmPassword = ''
  } catch (error) {
    actionError.value = error instanceof ApiError ? error.message : '密码重置失败，请稍后重试。'
  } finally {
    isSubmitting.value = false
  }
}
</script>

<template>
  <section class="page-surface recovery-shell">
    <div class="page-head">
      <div>
        <p class="eyebrow">账号恢复</p>
        <h2>忘记密码</h2>
        <p class="body-copy">
          通过账号与绑定邮箱完成校验后，可直接重置登录密码。未绑定邮箱的账号需要联系平台支持人工处理。
        </p>
      </div>
      <div class="page-actions">
        <RouterLink class="secondary-link" to="/login">返回登录</RouterLink>
        <RouterLink class="secondary-link" to="/help-center">查看帮助中心</RouterLink>
      </div>
    </div>

    <section class="metric-strip">
      <div class="metric-cell">
        <span>恢复步骤</span>
        <strong>3</strong>
        <p>账号校验、邮箱验证、设置新密码</p>
      </div>
      <div class="metric-cell">
        <span>已完成</span>
        <strong>{{ completedSteps }}/3</strong>
        <p>当前表单进度</p>
      </div>
      <div class="metric-cell">
        <span>密码强度</span>
        <strong>{{ passwordStrength }}</strong>
        <p>建议使用 12 位以上且含大小写字母与数字</p>
      </div>
      <div class="metric-cell">
        <span>验证码有效期</span>
        <strong>10 分钟</strong>
        <p>发送后请尽快完成重置</p>
      </div>
    </section>

    <section class="content-columns">
      <article class="ledger-panel">
        <div class="panel-headline">
          <div>
            <p class="eyebrow">在线重置</p>
            <h3>校验账号并更新密码</h3>
          </div>
        </div>

        <p v-if="actionError" class="status-error" role="alert">{{ actionError }}</p>
        <p v-if="successMessage" class="success-banner" aria-live="polite">{{ successMessage }}</p>
        <p v-if="codeNotice" class="status-note">{{ codeNotice }}</p>

        <form class="recovery-form" @submit.prevent="handleResetPassword">
          <label class="field-block">
            <span>登录账号</span>
            <input v-model="form.account" class="field-input" type="text" placeholder="请输入登录账号" />
          </label>

          <label class="field-block">
            <span>绑定邮箱</span>
            <input v-model="form.email" class="field-input" type="email" placeholder="请输入绑定邮箱" />
          </label>

          <div class="verification-row">
            <label class="field-block">
              <span>邮箱验证码</span>
              <input
                v-model="form.emailVerificationCode"
                class="field-input"
                type="text"
                maxlength="6"
                placeholder="请输入 6 位验证码"
              />
            </label>
            <button type="button" class="secondary-link code-button" :disabled="isSendingCode" @click="handleSendCode">
              {{ isSendingCode ? '发送中...' : '发送验证码' }}
            </button>
          </div>

          <div class="split-fields">
            <label class="field-block">
              <span>新密码</span>
              <input v-model="form.password" class="field-input" type="password" placeholder="至少 8 位" />
            </label>

            <label class="field-block">
              <span>确认新密码</span>
              <input v-model="form.confirmPassword" class="field-input" type="password" placeholder="再次输入新密码" />
            </label>
          </div>

          <div class="action-row">
            <button type="submit" class="primary-button" :disabled="isSubmitting">
              {{ isSubmitting ? '重置中...' : '确认重置密码' }}
            </button>
          </div>
        </form>
      </article>

      <aside class="detail-sidebar recovery-sidebar">
        <div class="detail-block">
          <p class="eyebrow">状态区</p>
          <h3>恢复进度</h3>
          <ul class="detail-list">
            <li>{{ form.account.trim() ? '账号已填写' : '等待填写账号' }}</li>
            <li>{{ emailPattern.test(form.email.trim()) ? '邮箱格式已通过校验' : '等待填写有效邮箱' }}</li>
            <li>{{ codeNotice ? '验证码已发送' : '尚未发送验证码' }}</li>
            <li>{{ successMessage ? '密码已完成更新' : '尚未提交密码重置' }}</li>
          </ul>
        </div>

        <div class="detail-block">
          <p class="eyebrow">补充说明</p>
          <ul class="detail-list">
            <li>仅支持已绑定邮箱且状态正常的账号在线找回。</li>
            <li>重置成功后，旧密码立即失效。</li>
            <li>若连续输错验证码，请重新发送。</li>
          </ul>
        </div>

        <div class="detail-block">
          <p class="eyebrow">其他入口</p>
          <div class="sidebar-actions">
            <RouterLink class="secondary-link" to="/help-center">查看常见问题</RouterLink>
            <RouterLink class="secondary-link" to="/accessibility">无障碍设置</RouterLink>
            <RouterLink class="secondary-link" to="/login">返回登录页</RouterLink>
          </div>
        </div>
      </aside>
    </section>
  </section>
</template>

<style scoped>
.recovery-shell,
.recovery-form,
.recovery-sidebar,
.sidebar-actions {
  display: grid;
  gap: 18px;
}

.field-block {
  display: grid;
  gap: 8px;
}

.field-input {
  width: 100%;
  border: 1px solid var(--line-strong);
  padding: 12px 14px;
  background: #fff;
}

.verification-row,
.split-fields,
.action-row {
  display: grid;
  gap: 14px;
}

.verification-row,
.split-fields {
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: end;
}

.split-fields {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.code-button {
  min-height: 48px;
}

.status-error,
.status-note {
  margin: 0;
}

.status-error {
  color: var(--error);
  font-weight: 700;
}

.status-note {
  color: var(--brand);
}

@media (max-width: 880px) {
  .verification-row,
  .split-fields {
    grid-template-columns: 1fr;
  }
}
</style>

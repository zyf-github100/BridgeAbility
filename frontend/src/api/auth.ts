import { apiRequest } from '../lib/http'

export interface AuthSession {
  accessToken: string
  tokenType: string
  expiresInSeconds: number
  userId: number
  account: string
  nickname: string
  roles: string[]
}

export interface LoginPayload {
  account: string
  password: string
}

export interface RegisterPayload {
  account: string
  password: string
  nickname: string
  phone?: string
  email?: string
  emailVerificationCode?: string
  roles: string[]
}

export interface SendRegisterEmailCodePayload {
  email: string
}

export interface SendPasswordResetCodePayload {
  account: string
  email: string
}

export interface ResetPasswordPayload {
  account: string
  email: string
  emailVerificationCode: string
  password: string
}

export function login(payload: LoginPayload) {
  return apiRequest<AuthSession>('/api/auth/login', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export function register(payload: RegisterPayload) {
  return apiRequest<AuthSession>('/api/auth/register', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export function sendRegisterEmailCode(payload: SendRegisterEmailCodePayload) {
  return apiRequest<null>('/api/auth/register/email-code', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export function sendPasswordResetEmailCode(payload: SendPasswordResetCodePayload) {
  return apiRequest<null>('/api/auth/password-reset/email-code', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export function resetPassword(payload: ResetPasswordPayload) {
  return apiRequest<null>('/api/auth/password-reset', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export function logout(token?: string) {
  return apiRequest<null>('/api/auth/logout', {
    method: 'POST',
    token,
  })
}

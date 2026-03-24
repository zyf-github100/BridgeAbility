export interface ApiEnvelope<T> {
  code: number
  message: string
  data: T
  traceId?: string
}

export class ApiError extends Error {
  code: number
  traceId?: string

  constructor(message: string, code = 5000, traceId?: string) {
    super(message)
    this.name = 'ApiError'
    this.code = code
    this.traceId = traceId
  }
}

interface ApiRequestOptions extends RequestInit {
  token?: string
}

export async function apiRequest<T>(url: string, options: ApiRequestOptions = {}) {
  const headers = new Headers(options.headers)
  const hasBody = options.body !== undefined && options.body !== null

  if (hasBody && !(options.body instanceof FormData) && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json')
  }

  if (options.token) {
    headers.set('Authorization', `Bearer ${options.token}`)
  }

  const response = await fetch(url, {
    ...options,
    headers,
  })

  let payload: ApiEnvelope<T> | null = null

  try {
    payload = (await response.json()) as ApiEnvelope<T>
  } catch {
    if (!response.ok) {
      throw new ApiError(`请求失败（HTTP ${response.status}）`, response.status)
    }
  }

  if (!payload) {
    throw new ApiError('服务器返回为空')
  }

  if (!response.ok) {
    throw new ApiError(payload.message || `请求失败（HTTP ${response.status}）`, response.status, payload.traceId)
  }

  if (payload.code !== 0) {
    throw new ApiError(payload.message || '请求失败，请稍后重试', payload.code, payload.traceId)
  }

  return payload.data
}

const API_BASE_URL = import.meta.env.VITE_API_URL ?? '/design-api/api'

interface ApiEnvelope<T> {
  code: number
  message?: string
  result: T
}

export class ApiError extends Error {
  readonly status: number

  constructor(message: string, status: number) {
    super(message)
    this.status = status
  }
}

export async function apiRequest<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...init,
    headers: {
      ...(init?.body ? { 'Content-Type': 'application/json' } : {}),
      ...init?.headers,
    },
  })

  if (!response.ok) {
    let message = `Yêu cầu thất bại (${response.status})`
    try {
      const body = await response.json() as { message?: string }
      if (body.message) message = body.message
    } catch { /* response không phải JSON */ }
    throw new ApiError(message, response.status)
  }

  const body = await response.json() as ApiEnvelope<T>
  return body.result
}

export async function apiText(path: string): Promise<string> {
  const response = await fetch(`${API_BASE_URL}${path}`)
  if (!response.ok) throw new ApiError(`Không thể tải báo cáo (${response.status})`, response.status)
  return response.text()
}

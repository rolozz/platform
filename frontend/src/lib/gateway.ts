import { GATEWAY_BASE_URL } from '../config'

export class HttpError extends Error {
  status: number
  bodyText: string

  constructor(status: number, message: string, bodyText: string) {
    super(message)
    this.status = status
    this.bodyText = bodyText
  }
}

export type JsonPrimitive = string | number | boolean | null
export type JsonValue = JsonPrimitive | JsonValue[] | { [k: string]: JsonValue }

export type RequestOptions = {
  method?: string
  headers?: Record<string, string>
  body?: BodyInit | null
  json?: unknown
}

export async function gatewayRequest<T = unknown>(path: string, options: RequestOptions = {}, accessToken?: string): Promise<T> {
  const url = new URL(path, GATEWAY_BASE_URL)

  const headers: Record<string, string> = {
    ...(options.headers ?? {}),
  }

  if (accessToken) {
    headers.Authorization = `Bearer ${accessToken}`
  }

  let body: BodyInit | null | undefined = options.body

  if (options.json !== undefined) {
    headers['Content-Type'] = headers['Content-Type'] ?? 'application/json'
    body = JSON.stringify(options.json)
  }

  const res = await fetch(url.toString(), {
    method: options.method ?? 'GET',
    headers,
    body,
  })

  const contentType = res.headers.get('content-type') ?? ''
  const text = await res.text()

  if (!res.ok) {
    throw new HttpError(res.status, `HTTP ${res.status} ${res.statusText}`, text)
  }

  if (!text) return undefined as T

  if (contentType.includes('application/json')) {
    return JSON.parse(text) as T
  }

  return text as T
}

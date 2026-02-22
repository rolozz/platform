export function asString(value: unknown): string {
  if (typeof value === 'string') return value
  if (value === null || value === undefined) return ''
  return String(value)
}

export function safeJson(value: unknown): string {
  try {
    const s = JSON.stringify(value, null, 2)
    return s ?? ''
  } catch {
    return ''
  }
}

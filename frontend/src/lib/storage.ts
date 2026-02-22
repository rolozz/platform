export type StoredTokens = {
  accessToken: string
  refreshToken: string
  tokenType: string
  scope?: string
  accessExpiresAtMs: number
  refreshExpiresAtMs?: number
  idToken?: string
}

const STORAGE_KEY = 'platform.tokens'

export function loadTokens(): StoredTokens | null {
  const raw = localStorage.getItem(STORAGE_KEY)
  if (!raw) return null

  try {
    const parsed = JSON.parse(raw) as StoredTokens
    if (!parsed.accessToken || !parsed.refreshToken || !parsed.tokenType || !parsed.accessExpiresAtMs) {
      return null
    }
    return parsed
  } catch {
    return null
  }
}

export function saveTokens(tokens: StoredTokens): void {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(tokens))
}

export function clearTokens(): void {
  localStorage.removeItem(STORAGE_KEY)
}

export type JwtPayload = {
  exp?: number
  preferred_username?: string
  username?: string
  sub?: string
  roles?: string[]
  [key: string]: unknown
}

function base64UrlDecode(input: string): string {
  const base64 = input.replace(/-/g, '+').replace(/_/g, '/')
  const padded = base64.padEnd(Math.ceil(base64.length / 4) * 4, '=')
  const binary = atob(padded)
  const bytes = Uint8Array.from(binary, (c) => c.charCodeAt(0))
  const decoded = new TextDecoder().decode(bytes)
  return decoded
}

export function decodeJwtPayload(token: string): JwtPayload | null {
  const parts = token.split('.')
  if (parts.length < 2) return null

  try {
    const json = base64UrlDecode(parts[1] ?? '')
    return JSON.parse(json) as JwtPayload
  } catch {
    return null
  }
}

export function getJwtExpiryMs(token: string): number | null {
  const payload = decodeJwtPayload(token)
  const exp = payload?.exp
  if (!exp || typeof exp !== 'number') return null
  return exp * 1000
}

export function isJwtExpiringSoon(token: string, skewSeconds = 30): boolean {
  const expMs = getJwtExpiryMs(token)
  if (!expMs) return false
  return Date.now() + skewSeconds * 1000 >= expMs
}

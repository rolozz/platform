import { KEYCLOAK_BASE_URL, KEYCLOAK_CLIENT_ID, KEYCLOAK_REALM } from '../config'
import type { StoredTokens } from './storage'

export type KeycloakTokenResponse = {
  access_token: string
  refresh_token: string
  expires_in: number
  refresh_expires_in?: number
  token_type: string
  scope?: string
  id_token?: string
}

async function postForm<T>(url: string, form: URLSearchParams): Promise<T> {
  const res = await fetch(url, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded',
    },
    body: form,
  })

  if (!res.ok) {
    const text = await res.text().catch(() => '')
    throw new Error(`HTTP ${res.status} ${res.statusText}${text ? `: ${text}` : ''}`)
  }

  return (await res.json()) as T
}

function tokenEndpoint(): string {
  return `${KEYCLOAK_BASE_URL}/realms/${KEYCLOAK_REALM}/protocol/openid-connect/token`
}

function logoutEndpoint(): string {
  return `${KEYCLOAK_BASE_URL}/realms/${KEYCLOAK_REALM}/protocol/openid-connect/logout`
}

export async function keycloakLogin(username: string, password: string): Promise<StoredTokens> {
  const body = new URLSearchParams()
  body.set('grant_type', 'password')
  body.set('client_id', KEYCLOAK_CLIENT_ID)
  body.set('username', username)
  body.set('password', password)

  const tr = await postForm<KeycloakTokenResponse>(tokenEndpoint(), body)
  return {
    accessToken: tr.access_token,
    refreshToken: tr.refresh_token,
    tokenType: tr.token_type,
    scope: tr.scope,
    accessExpiresAtMs: Date.now() + tr.expires_in * 1000,
    refreshExpiresAtMs: tr.refresh_expires_in ? Date.now() + tr.refresh_expires_in * 1000 : undefined,
    idToken: tr.id_token,
  }
}

export async function keycloakRefresh(refreshToken: string): Promise<StoredTokens> {
  const body = new URLSearchParams()
  body.set('grant_type', 'refresh_token')
  body.set('client_id', KEYCLOAK_CLIENT_ID)
  body.set('refresh_token', refreshToken)

  const tr = await postForm<KeycloakTokenResponse>(tokenEndpoint(), body)
  return {
    accessToken: tr.access_token,
    refreshToken: tr.refresh_token,
    tokenType: tr.token_type,
    scope: tr.scope,
    accessExpiresAtMs: Date.now() + tr.expires_in * 1000,
    refreshExpiresAtMs: tr.refresh_expires_in ? Date.now() + tr.refresh_expires_in * 1000 : undefined,
    idToken: tr.id_token,
  }
}

export async function keycloakLogout(refreshToken: string): Promise<void> {
  const body = new URLSearchParams()
  body.set('client_id', KEYCLOAK_CLIENT_ID)
  body.set('refresh_token', refreshToken)

  const res = await fetch(logoutEndpoint(), {
    method: 'POST',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded',
    },
    body,
  })

  if (!res.ok) {
    const text = await res.text().catch(() => '')
    throw new Error(`HTTP ${res.status} ${res.statusText}${text ? `: ${text}` : ''}`)
  }
}

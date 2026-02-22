import { useCallback, useMemo, useState } from 'react'
import './App.css'
import { KEYCLOAK_BASE_URL, KEYCLOAK_CLIENT_ID, KEYCLOAK_REALM, GATEWAY_BASE_URL } from './config'
import { decodeJwtPayload, isJwtExpiringSoon } from './lib/jwt'
import { keycloakLogin, keycloakLogout, keycloakRefresh } from './lib/keycloak'
import { gatewayRequest, HttpError } from './lib/gateway'
import { clearTokens, loadTokens, saveTokens } from './lib/storage'
import type { StoredTokens } from './lib/storage'
import { asString, safeJson } from './lib/strings'

type UserProfileDto = {
  username: string
  email: string
  firstName: string
  lastName: string
  createdAt?: string
  updatedAt?: string
}

type SpringPage<T> = {
  content: T[]
  totalElements?: number
  totalPages?: number
  number?: number
  size?: number
}

type UserRole = 'USER' | 'ADMIN' | 'OWNER'

function formatMs(ms: number | undefined): string {
  if (!ms) return ''
  const d = new Date(ms)
  if (Number.isNaN(d.getTime())) return ''
  return d.toLocaleString()
}

export default function App() {
  const [tokens, setTokens] = useState<StoredTokens | null>(() => loadTokens())
  const accessToken = tokens?.accessToken ?? null

  const tokenPayload = useMemo(() => {
    if (!accessToken) return null
    return decodeJwtPayload(accessToken)
  }, [accessToken])

  const [authForm, setAuthForm] = useState({ username: '', password: '' })

  const [registerForm, setRegisterForm] = useState({
    username: '',
    email: '',
    firstName: '',
    lastName: '',
    password: '',
    confirmPassword: '',
  })

  const [updateForm, setUpdateForm] = useState({
    username: '',
    email: '',
    firstName: '',
    lastName: '',
    password: '',
    confirmPassword: '',
  })

  const [allQuery, setAllQuery] = useState({ page: '1', size: '20', sort: 'createdAt', direction: 'DESC' })
  const [adminForm, setAdminForm] = useState({
    username: '',
    newRole: 'USER' as UserRole,
  })

  const [result, setResult] = useState<{ ok: boolean; title: string; body: unknown } | null>(null)

  const setAndPersistTokens = useCallback((t: StoredTokens | null) => {
    if (!t) {
      clearTokens()
      setTokens(null)
      return
    }
    saveTokens(t)
    setTokens(t)
  }, [])

  const ensureFreshAccessToken = useCallback(async (): Promise<string> => {
    if (!tokens) throw new Error('Not authenticated')

    if (!isJwtExpiringSoon(tokens.accessToken, 30)) {
      return tokens.accessToken
    }

    const refreshed = await keycloakRefresh(tokens.refreshToken)
    setAndPersistTokens(refreshed)
    return refreshed.accessToken
  }, [setAndPersistTokens, tokens])

  const run = useCallback(
    async <T,>(title: string, fn: () => Promise<T>) => {
      try {
        const data = await fn()
        setResult({ ok: true, title, body: data })
      } catch (e) {
        if (e instanceof HttpError) {
          setResult({ ok: false, title, body: { status: e.status, message: e.message, bodyText: e.bodyText } })
          return
        }
        setResult({ ok: false, title, body: { message: asString(e) } })
      }
    },
    [],
  )

  const onLogin = useCallback(async () => {
    await run('Keycloak login', async () => {
      const next = await keycloakLogin(authForm.username.trim(), authForm.password)
      setAndPersistTokens(next)
      return { accessExpiresAt: formatMs(next.accessExpiresAtMs), refreshExpiresAt: formatMs(next.refreshExpiresAtMs) }
    })
  }, [authForm.password, authForm.username, run, setAndPersistTokens])

  const onRefresh = useCallback(async () => {
    await run('Keycloak refresh', async () => {
      if (!tokens) throw new Error('Not authenticated')
      const next = await keycloakRefresh(tokens.refreshToken)
      setAndPersistTokens(next)
      return { accessExpiresAt: formatMs(next.accessExpiresAtMs), refreshExpiresAt: formatMs(next.refreshExpiresAtMs) }
    })
  }, [run, setAndPersistTokens, tokens])

  const onLogout = useCallback(async () => {
    await run('Keycloak logout', async () => {
      if (tokens) {
        await keycloakLogout(tokens.refreshToken).catch(() => undefined)
      }
      setAndPersistTokens(null)
      return { ok: true }
    })
  }, [run, setAndPersistTokens, tokens])

  const onRegister = useCallback(async () => {
    await run('POST /api/v1/user-profiles/create (via gateway)', async () => {
      return gatewayRequest<UserProfileDto>('/api/v1/user-profiles/create', { method: 'POST', json: registerForm })
    })
  }, [registerForm, run])

  const onGetMe = useCallback(async () => {
    await run('GET /api/v1/user-profiles/get', async () => {
      const token = await ensureFreshAccessToken()
      return gatewayRequest<UserProfileDto>('/api/v1/user-profiles/get', {}, token)
    })
  }, [ensureFreshAccessToken, run])

  const onGetAll = useCallback(async () => {
    await run('GET /api/v1/user-profiles/all', async () => {
      const token = await ensureFreshAccessToken()
      const qs = new URLSearchParams()
      qs.set('page', allQuery.page)
      qs.set('size', allQuery.size)
      qs.set('sort', allQuery.sort)
      qs.set('direction', allQuery.direction)

      return gatewayRequest<SpringPage<UserProfileDto>>(`/api/v1/user-profiles/all?${qs.toString()}`, {}, token)
    })
  }, [allQuery.direction, allQuery.page, allQuery.size, allQuery.sort, ensureFreshAccessToken, run])

  const onUpdateMe = useCallback(async () => {
    await run('PUT /api/v1/user-profiles/update', async () => {
      const token = await ensureFreshAccessToken()
      return gatewayRequest<UserProfileDto>('/api/v1/user-profiles/update', { method: 'PUT', json: updateForm }, token)
    })
  }, [ensureFreshAccessToken, run, updateForm])

  const onDeleteMe = useCallback(async () => {
    await run('DELETE /api/v1/user-profiles/delete', async () => {
      const token = await ensureFreshAccessToken()
      return gatewayRequest<void>('/api/v1/user-profiles/delete', { method: 'DELETE' }, token)
    })
  }, [ensureFreshAccessToken, run])

  const onAdminGetRole = useCallback(async () => {
    await run('GET /api/admin/users/{username}/role', async () => {
      const token = await ensureFreshAccessToken()
      const username = adminForm.username.trim()
      return gatewayRequest<UserRole>(`/api/admin/users/${encodeURIComponent(username)}/role`, {}, token)
    })
  }, [adminForm.username, ensureFreshAccessToken, run])

  const onAdminDeleteUser = useCallback(async () => {
    await run('DELETE /api/admin/users/{username}', async () => {
      const token = await ensureFreshAccessToken()
      const username = adminForm.username.trim()
      return gatewayRequest<void>(`/api/admin/users/${encodeURIComponent(username)}`, { method: 'DELETE' }, token)
    })
  }, [adminForm.username, ensureFreshAccessToken, run])

  const onAdminChangeRole = useCallback(async (mode: 'promote' | 'demote' | 'role') => {
    await run(`PUT /api/admin/users/{username}/${mode}`, async () => {
      const token = await ensureFreshAccessToken()
      const username = adminForm.username.trim()
      const qs = new URLSearchParams({
        newRole: adminForm.newRole,
      })

      return gatewayRequest<UserProfileDto>(
        `/api/admin/users/${encodeURIComponent(username)}/${mode}?${qs.toString()}`,
        { method: 'PUT' },
        token,
      )
    })
  }, [adminForm.newRole, adminForm.username, ensureFreshAccessToken, run])

  return (
    <div className="layout">
      <header className="header">
        <div className="brand">platform / frontend</div>
        <div className="meta">
          <div className="metaRow">
            <span className="metaKey">Gateway</span>
            <span className="metaVal">{GATEWAY_BASE_URL}</span>
          </div>
          <div className="metaRow">
            <span className="metaKey">Keycloak</span>
            <span className="metaVal">{KEYCLOAK_BASE_URL} / realms/{KEYCLOAK_REALM} / {KEYCLOAK_CLIENT_ID}</span>
          </div>
        </div>
      </header>

      <main className="grid">
        <section className="panel">
          <h2>Auth</h2>

          <div className="form">
            <label className="field">
              <span className="label">username</span>
              <input
                value={authForm.username}
                onChange={(e) => setAuthForm((s) => ({ ...s, username: e.target.value }))}
                placeholder="admin"
                autoComplete="username"
              />
            </label>

            <label className="field">
              <span className="label">password</span>
              <input
                value={authForm.password}
                onChange={(e) => setAuthForm((s) => ({ ...s, password: e.target.value }))}
                placeholder="admin"
                type="password"
                autoComplete="current-password"
              />
            </label>

            <div className="row">
              <button className="btn" onClick={onLogin}>
                login
              </button>
              <button className="btn" onClick={onRefresh} disabled={!tokens}>
                refresh
              </button>
              <button className="btn danger" onClick={onLogout}>
                logout
              </button>
            </div>
          </div>

          <div className="kv">
            <div className="kvRow">
              <div className="kvKey">authenticated</div>
              <div className="kvVal">{tokens ? 'yes' : 'no'}</div>
            </div>
            <div className="kvRow">
              <div className="kvKey">access exp</div>
              <div className="kvVal">{formatMs(tokens?.accessExpiresAtMs)}</div>
            </div>
            <div className="kvRow">
              <div className="kvKey">refresh exp</div>
              <div className="kvVal">{formatMs(tokens?.refreshExpiresAtMs)}</div>
            </div>
            <div className="kvRow">
              <div className="kvKey">preferred_username</div>
              <div className="kvVal">{asString(tokenPayload?.preferred_username)}</div>
            </div>
            <div className="kvRow">
              <div className="kvKey">roles</div>
              <div className="kvVal">{Array.isArray(tokenPayload?.roles) ? tokenPayload?.roles?.join(', ') : ''}</div>
            </div>
          </div>
        </section>

        <section className="panel">
          <h2>User Profile</h2>

          <details open>
            <summary>Register (public)</summary>
            <div className="form">
              <div className="twoCols">
                <label className="field">
                  <span className="label">username</span>
                  <input value={registerForm.username} onChange={(e) => setRegisterForm((s) => ({ ...s, username: e.target.value }))} />
                </label>
                <label className="field">
                  <span className="label">email</span>
                  <input value={registerForm.email} onChange={(e) => setRegisterForm((s) => ({ ...s, email: e.target.value }))} />
                </label>
                <label className="field">
                  <span className="label">firstName</span>
                  <input value={registerForm.firstName} onChange={(e) => setRegisterForm((s) => ({ ...s, firstName: e.target.value }))} />
                </label>
                <label className="field">
                  <span className="label">lastName</span>
                  <input value={registerForm.lastName} onChange={(e) => setRegisterForm((s) => ({ ...s, lastName: e.target.value }))} />
                </label>
                <label className="field">
                  <span className="label">password</span>
                  <input type="password" value={registerForm.password} onChange={(e) => setRegisterForm((s) => ({ ...s, password: e.target.value }))} />
                </label>
                <label className="field">
                  <span className="label">confirmPassword</span>
                  <input
                    type="password"
                    value={registerForm.confirmPassword}
                    onChange={(e) => setRegisterForm((s) => ({ ...s, confirmPassword: e.target.value }))}
                  />
                </label>
              </div>
              <div className="row">
                <button className="btn" onClick={onRegister}>
                  POST /create
                </button>
              </div>
            </div>
          </details>

          <details open>
            <summary>Me (needs token)</summary>
            <div className="row">
              <button className="btn" onClick={onGetMe} disabled={!tokens}>
                GET /get
              </button>
              <button className="btn" onClick={onDeleteMe} disabled={!tokens}>
                DELETE /delete
              </button>
            </div>
          </details>

          <details>
            <summary>Update (needs token)</summary>
            <div className="form">
              <div className="twoCols">
                <label className="field">
                  <span className="label">username</span>
                  <input value={updateForm.username} onChange={(e) => setUpdateForm((s) => ({ ...s, username: e.target.value }))} />
                </label>
                <label className="field">
                  <span className="label">email</span>
                  <input value={updateForm.email} onChange={(e) => setUpdateForm((s) => ({ ...s, email: e.target.value }))} />
                </label>
                <label className="field">
                  <span className="label">firstName</span>
                  <input value={updateForm.firstName} onChange={(e) => setUpdateForm((s) => ({ ...s, firstName: e.target.value }))} />
                </label>
                <label className="field">
                  <span className="label">lastName</span>
                  <input value={updateForm.lastName} onChange={(e) => setUpdateForm((s) => ({ ...s, lastName: e.target.value }))} />
                </label>
                <label className="field">
                  <span className="label">password</span>
                  <input type="password" value={updateForm.password} onChange={(e) => setUpdateForm((s) => ({ ...s, password: e.target.value }))} />
                </label>
                <label className="field">
                  <span className="label">confirmPassword</span>
                  <input type="password" value={updateForm.confirmPassword} onChange={(e) => setUpdateForm((s) => ({ ...s, confirmPassword: e.target.value }))} />
                </label>
              </div>
              <div className="row">
                <button className="btn" onClick={onUpdateMe} disabled={!tokens}>
                  PUT /update
                </button>
              </div>
            </div>
          </details>

          <details>
            <summary>All (needs token)</summary>
            <div className="form">
              <div className="twoCols">
                <label className="field">
                  <span className="label">page</span>
                  <input value={allQuery.page} onChange={(e) => setAllQuery((s) => ({ ...s, page: e.target.value }))} />
                </label>
                <label className="field">
                  <span className="label">size</span>
                  <input value={allQuery.size} onChange={(e) => setAllQuery((s) => ({ ...s, size: e.target.value }))} />
                </label>
                <label className="field">
                  <span className="label">sort</span>
                  <input value={allQuery.sort} onChange={(e) => setAllQuery((s) => ({ ...s, sort: e.target.value }))} />
                </label>
                <label className="field">
                  <span className="label">direction</span>
                  <select value={allQuery.direction} onChange={(e) => setAllQuery((s) => ({ ...s, direction: e.target.value }))}>
                    <option value="DESC">DESC</option>
                    <option value="ASC">ASC</option>
                  </select>
                </label>
              </div>
              <div className="row">
                <button className="btn" onClick={onGetAll} disabled={!tokens}>
                  GET /all
                </button>
              </div>
            </div>
          </details>
        </section>

        <section className="panel">
          <h2>Admin</h2>

          <div className="form">
            <div className="twoCols">
              <label className="field">
                <span className="label">username</span>
                <input value={adminForm.username} onChange={(e) => setAdminForm((s) => ({ ...s, username: e.target.value }))} />
              </label>
              <label className="field">
                <span className="label">newRole</span>
                <select value={adminForm.newRole} onChange={(e) => setAdminForm((s) => ({ ...s, newRole: e.target.value as UserRole }))}>
                  <option value="USER">USER</option>
                  <option value="ADMIN">ADMIN</option>
                  <option value="OWNER">OWNER</option>
                </select>
              </label>
            </div>

            <div className="row">
              <button className="btn" onClick={onAdminGetRole} disabled={!tokens}>
                GET role
              </button>
              <button className="btn" onClick={() => void onAdminChangeRole('role')} disabled={!tokens}>
                PUT set role
              </button>
              <button className="btn" onClick={() => void onAdminChangeRole('promote')} disabled={!tokens}>
                PUT promote
              </button>
              <button className="btn" onClick={() => void onAdminChangeRole('demote')} disabled={!tokens}>
                PUT demote
              </button>
              <button className="btn danger" onClick={onAdminDeleteUser} disabled={!tokens}>
                DELETE user
              </button>
            </div>
          </div>
        </section>

        <section className="panel wide">
          <h2>Result</h2>
          {!result ? (
            <div className="muted">No requests yet</div>
          ) : (
            <div className={result.ok ? 'result ok' : 'result err'}>
              <div className="resultTitle">{result.title}</div>
              <pre className="resultBody">{typeof result.body === 'string' ? result.body : safeJson(result.body)}</pre>
            </div>
          )}
        </section>
      </main>
    </div>
  )
}

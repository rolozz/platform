import { useState, useEffect, useMemo } from 'react'
import './App.css'
import './components.css'
import { decodeJwtPayload, isJwtExpiringSoon } from './lib/jwt'
import { keycloakRefresh } from './lib/keycloak'
import { saveTokens } from './lib/storage'
import type { StoredTokens } from './lib/storage'

import AuthPage from './components/AuthPage'
import ProfilePage from './components/ProfilePage'
import AdminPage from './components/AdminPage'

type Page = 'auth' | 'profile' | 'admin'

export default function App() {
  const [tokens, setTokens] = useState<StoredTokens | null>(() => {
    const saved = localStorage.getItem('auth_tokens')
    return saved ? JSON.parse(saved) : null
  })
  
  const [currentPage, setCurrentPage] = useState<Page>(() => {
    return tokens ? 'profile' : 'auth'
  })

  const accessToken = tokens?.accessToken ?? null

  const tokenPayload = useMemo(() => {
    if (!accessToken) return null
    return decodeJwtPayload(accessToken)
  }, [accessToken])

  const userRoles = useMemo(() => {
    if (!tokenPayload?.roles) return []
    const roles = Array.isArray(tokenPayload.roles) ? tokenPayload.roles : [tokenPayload.roles]
    console.log('User roles from token:', roles)
    return roles
  }, [tokenPayload])

  const isAdmin = useMemo(() => {
    const adminStatus = userRoles.includes('ADMIN') || userRoles.includes('OWNER')
    console.log('Is admin:', adminStatus, 'Roles:', userRoles)
    return adminStatus
  }, [userRoles])

  // Auto-refresh token
  useEffect(() => {
    if (!tokens) return

    const checkAndRefresh = async () => {
      if (isJwtExpiringSoon(tokens.accessToken)) {
        try {
          const newTokens = await keycloakRefresh(tokens.refreshToken)
          setTokens(newTokens)
          saveTokens(newTokens)
        } catch (error) {
          console.error('Token refresh failed:', error)
          handleLogout()
        }
      }
    }

    const interval = setInterval(checkAndRefresh, 60000) // Check every minute
    checkAndRefresh() // Check immediately

    return () => clearInterval(interval)
  }, [tokens])

  const handleLogin = (newTokens: StoredTokens) => {
    setTokens(newTokens)
    saveTokens(newTokens)
    setCurrentPage('profile')
  }

  const handleLogout = () => {
    setTokens(null)
    localStorage.removeItem('auth_tokens')
    setCurrentPage('auth')
  }

  const navigateToProfile = () => setCurrentPage('profile')
  const navigateToAdmin = () => setCurrentPage('admin')

  // Redirect to profile if logged in but on auth page
  useEffect(() => {
    if (tokens && currentPage === 'auth') {
      setCurrentPage('profile')
    }
  }, [tokens, currentPage])

  // Redirect to auth if not logged in but not on auth page
  useEffect(() => {
    if (!tokens && currentPage !== 'auth') {
      setCurrentPage('auth')
    }
  }, [tokens, currentPage])

  return (
    <div className="app">
      {currentPage === 'auth' && (
        <AuthPage onLogin={handleLogin} />
      )}
      
      {currentPage === 'profile' && tokens && (
        <ProfilePage
          accessToken={tokens.accessToken}
          tokens={{ accessToken: tokens.accessToken, refreshToken: tokens.refreshToken }}
          onLogout={handleLogout}
          isAdmin={isAdmin}
          onNavigateToAdmin={navigateToAdmin}
        />
      )}
      
      {currentPage === 'admin' && tokens && (
        <AdminPage
          accessToken={tokens.accessToken}
          onNavigateToProfile={navigateToProfile}
        />
      )}
    </div>
  )
}

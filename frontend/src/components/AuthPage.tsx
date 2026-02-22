import { useState } from 'react'
import { keycloakLogin } from '../lib/keycloak'
import { gatewayRequest } from '../lib/gateway'
import { HttpError } from '../lib/gateway'
import type { StoredTokens } from '../lib/storage'

type AuthPageProps = {
  onLogin: (tokens: StoredTokens) => void
}

type RegisterForm = {
  username: string
  email: string
  firstName: string
  lastName: string
  password: string
  confirmPassword: string
}

type LoginForm = {
  username: string
  password: string
}

export default function AuthPage({ onLogin }: AuthPageProps) {
  const [isLogin, setIsLogin] = useState(true)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState<string | null>(null)

  const [loginForm, setLoginForm] = useState<LoginForm>({ username: '', password: '' })
  const [registerForm, setRegisterForm] = useState<RegisterForm>({
    username: '',
    email: '',
    firstName: '',
    lastName: '',
    password: '',
    confirmPassword: ''
  })

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault()
    setLoading(true)
    setError(null)
    setSuccess(null)

    try {
      const tokens = await keycloakLogin(loginForm.username, loginForm.password)
      onLogin(tokens)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Login failed')
    } finally {
      setLoading(false)
    }
  }

  const handleRegister = async (e: React.FormEvent) => {
    e.preventDefault()
    setLoading(true)
    setError(null)
    setSuccess(null)

    // Валидация паролей
    if (registerForm.password !== registerForm.confirmPassword) {
      setError('Passwords do not match')
      setLoading(false)
      return
    }

    try {
      await gatewayRequest('/api/v1/user-profiles/create', {
        method: 'POST',
        json: {
          username: registerForm.username,
          email: registerForm.email,
          firstName: registerForm.firstName,
          lastName: registerForm.lastName,
          password: registerForm.password,
          confirmPassword: registerForm.confirmPassword
        }
      })
      
      // После успешной регистрации переключаемся на логин
      setSuccess('Registration successful! Please login.')
      setIsLogin(true)
      // Очищаем форму логина и устанавливаем username
      setLoginForm({ username: registerForm.username, password: '' })
      // Очищаем форму регистрации
      setRegisterForm({
        username: '',
        email: '',
        firstName: '',
        lastName: '',
        password: '',
        confirmPassword: ''
      })
    } catch (err) {
      if (err instanceof HttpError) {
        setError(`Registration failed: ${err.bodyText || err.message}`)
      } else {
        setError(err instanceof Error ? err.message : 'Registration failed')
      }
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="auth-container">
      <div className="auth-card">
        <h1>{isLogin ? 'Login' : 'Register'}</h1>
        
        {error && (
          <div className="error-message">
            {error}
          </div>
        )}

        {success && (
          <div className="success-message">
            {success}
          </div>
        )}

        <form onSubmit={isLogin ? handleLogin : handleRegister} className="auth-form">
          {isLogin ? (
            <>
              <div className="form-group">
                <label>Username</label>
                <input
                  type="text"
                  value={loginForm.username}
                  onChange={(e) => setLoginForm(prev => ({ ...prev, username: e.target.value }))}
                  required
                />
              </div>
              <div className="form-group">
                <label>Password</label>
                <input
                  type="password"
                  value={loginForm.password}
                  onChange={(e) => setLoginForm(prev => ({ ...prev, password: e.target.value }))}
                  required
                />
              </div>
            </>
          ) : (
            <>
              <div className="form-group">
                <label>Username</label>
                <input
                  type="text"
                  value={registerForm.username}
                  onChange={(e) => setRegisterForm(prev => ({ ...prev, username: e.target.value }))}
                  required
                />
              </div>
              <div className="form-group">
                <label>Email</label>
                <input
                  type="email"
                  value={registerForm.email}
                  onChange={(e) => setRegisterForm(prev => ({ ...prev, email: e.target.value }))}
                  required
                />
              </div>
              <div className="form-group">
                <label>First Name</label>
                <input
                  type="text"
                  value={registerForm.firstName}
                  onChange={(e) => setRegisterForm(prev => ({ ...prev, firstName: e.target.value }))}
                  required
                />
              </div>
              <div className="form-group">
                <label>Last Name</label>
                <input
                  type="text"
                  value={registerForm.lastName}
                  onChange={(e) => setRegisterForm(prev => ({ ...prev, lastName: e.target.value }))}
                  required
                />
              </div>
              <div className="form-group">
                <label>Password</label>
                <input
                  type="password"
                  value={registerForm.password}
                  onChange={(e) => setRegisterForm(prev => ({ ...prev, password: e.target.value }))}
                  required
                />
              </div>
              <div className="form-group">
                <label>Confirm Password</label>
                <input
                  type="password"
                  value={registerForm.confirmPassword}
                  onChange={(e) => setRegisterForm(prev => ({ ...prev, confirmPassword: e.target.value }))}
                  required
                />
              </div>
            </>
          )}

          <button type="submit" disabled={loading} className="auth-button">
            {loading ? 'Loading...' : (isLogin ? 'Login' : 'Register')}
          </button>
        </form>

        <div className="auth-switch">
          <span>
            {isLogin ? "Don't have an account?" : "Already have an account?"}
            <button 
              type="button" 
              onClick={() => {
                setIsLogin(!isLogin)
                setError(null)
                setSuccess(null)
              }}
              className="switch-button"
            >
              {isLogin ? 'Register' : 'Login'}
            </button>
          </span>
        </div>
      </div>
    </div>
  )
}

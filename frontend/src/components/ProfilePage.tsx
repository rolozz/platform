import { useState, useEffect } from 'react'
import { gatewayRequest } from '../lib/gateway'
import { keycloakLogout } from '../lib/keycloak'
import { clearTokens } from '../lib/storage'

type UserProfileDto = {
  username: string
  email: string
  firstName: string
  lastName: string
  createdAt?: string
  updatedAt?: string
}

type ProfilePageProps = {
  accessToken: string
  tokens: { accessToken: string; refreshToken: string }
  onLogout: () => void
  isAdmin: boolean
  onNavigateToAdmin: () => void
}

export default function ProfilePage({ accessToken, tokens, onLogout, isAdmin, onNavigateToAdmin }: ProfilePageProps) {
  const [profile, setProfile] = useState<UserProfileDto | null>(null)
  const [loading, setLoading] = useState(true)
  const [editing, setEditing] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState<string | null>(null)

  const [editForm, setEditForm] = useState({
    username: '',
    email: '',
    firstName: '',
    lastName: '',
    password: '',
    confirmPassword: ''
  })

  useEffect(() => {
    loadProfile()
  }, [accessToken])

  const loadProfile = async () => {
    try {
      setLoading(true)
      const data = await gatewayRequest<UserProfileDto>('/api/v1/user-profiles/get', {}, accessToken)
      setProfile(data)
      setEditForm({
        username: data.username,
        email: data.email,
        firstName: data.firstName,
        lastName: data.lastName,
        password: '',
        confirmPassword: ''
      })
    } catch (err) {
      setError('Failed to load profile')
    } finally {
      setLoading(false)
    }
  }

  const handleUpdate = async (e: React.FormEvent) => {
    e.preventDefault()
    
    // Валидация паролей
    if (!editForm.password || !editForm.confirmPassword) {
      setError('Password and confirm password are required')
      return
    }
    if (editForm.password !== editForm.confirmPassword) {
      setError('Passwords do not match')
      return
    }
    if (editForm.password.length < 8) {
      setError('Password must be at least 8 characters long')
      return
    }

    try {
      setError(null)
      setSuccess(null)
      await gatewayRequest('/api/v1/user-profiles/update', {
        method: 'PUT',
        json: {
          username: editForm.username,
          email: editForm.email,
          firstName: editForm.firstName,
          lastName: editForm.lastName,
          password: editForm.password,
          confirmPassword: editForm.confirmPassword
        }
      }, accessToken)
      
      await loadProfile()
      setEditing(false)
      setSuccess('Profile updated successfully')
    } catch (err) {
      setError('Failed to update profile')
    }
  }

  const handleDelete = async () => {
    if (!confirm('Are you sure you want to delete your account? This action cannot be undone.')) {
      return
    }

    try {
      setError(null)
      await gatewayRequest('/api/v1/user-profiles/delete', {
        method: 'DELETE'
      }, accessToken)
      
      await keycloakLogout(tokens.accessToken)
      clearTokens()
      onLogout()
    } catch (err) {
      setError('Failed to delete profile')
    }
  }

  if (loading) {
    return <div className="loading">Loading profile...</div>
  }

  if (!profile) {
    return (
      <div className="profile-container">
        <div className="error-message">Failed to load profile</div>
      </div>
    )
  }

  return (
    <div className="profile-container">
      <div className="profile-header">
        <h1>Profile</h1>
        <div className="profile-actions">
          {isAdmin && (
            <button onClick={onNavigateToAdmin} className="admin-button">
              Admin Panel
            </button>
          )}
          <button onClick={onLogout} className="logout-button">
            Logout
          </button>
        </div>
      </div>

      {error && <div className="error-message">{error}</div>}
      {success && <div className="success-message">{success}</div>}

      <div className="profile-card">
        {editing ? (
          <form onSubmit={handleUpdate} className="profile-form">
            <div className="form-group">
              <label>Username</label>
              <input
                type="text"
                value={editForm.username}
                onChange={(e) => setEditForm(prev => ({ ...prev, username: e.target.value }))}
                required
              />
            </div>
            <div className="form-group">
              <label>Email</label>
              <input
                type="email"
                value={editForm.email}
                onChange={(e) => setEditForm(prev => ({ ...prev, email: e.target.value }))}
                required
              />
            </div>
            <div className="form-group">
              <label>First Name</label>
              <input
                type="text"
                value={editForm.firstName}
                onChange={(e) => setEditForm(prev => ({ ...prev, firstName: e.target.value }))}
                required
              />
            </div>
            <div className="form-group">
              <label>Last Name</label>
              <input
                type="text"
                value={editForm.lastName}
                onChange={(e) => setEditForm(prev => ({ ...prev, lastName: e.target.value }))}
                required
              />
            </div>
            <div className="form-group">
              <label>New Password</label>
              <input
                type="password"
                value={editForm.password}
                onChange={(e) => setEditForm(prev => ({ ...prev, password: e.target.value }))}
                required
                placeholder="Enter new password"
              />
            </div>
            <div className="form-group">
              <label>Confirm New Password</label>
              <input
                type="password"
                value={editForm.confirmPassword}
                onChange={(e) => setEditForm(prev => ({ ...prev, confirmPassword: e.target.value }))}
                required
                placeholder="Confirm new password"
              />
            </div>
            <div className="form-actions">
              <button type="submit" className="save-button">Save</button>
              <button type="button" onClick={() => setEditing(false)} className="cancel-button">Cancel</button>
            </div>
          </form>
        ) : (
          <div className="profile-view">
            <div className="profile-field">
              <label>Username:</label>
              <span>{profile.username}</span>
            </div>
            <div className="profile-field">
              <label>Email:</label>
              <span>{profile.email}</span>
            </div>
            <div className="profile-field">
              <label>First Name:</label>
              <span>{profile.firstName}</span>
            </div>
            <div className="profile-field">
              <label>Last Name:</label>
              <span>{profile.lastName}</span>
            </div>
            {profile.createdAt && (
              <div className="profile-field">
                <label>Created At:</label>
                <span>{new Date(profile.createdAt).toLocaleString()}</span>
              </div>
            )}
            {profile.updatedAt && (
              <div className="profile-field">
                <label>Updated At:</label>
                <span>{new Date(profile.updatedAt).toLocaleString()}</span>
              </div>
            )}
            <div className="profile-actions">
              <button onClick={() => setEditing(true)} className="edit-button">
                Edit Profile
              </button>
              <button onClick={handleDelete} className="delete-button">
                Delete Account
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  )
}

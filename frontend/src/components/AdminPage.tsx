import { useState, useEffect } from 'react'
import { gatewayRequest } from '../lib/gateway'
import { decodeJwtPayload } from '../lib/jwt'

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

type AdminPageProps = {
  accessToken: string
  onNavigateToProfile: () => void
}

export default function AdminPage({ accessToken, onNavigateToProfile }: AdminPageProps) {
  const [users, setUsers] = useState<UserProfileDto[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState<string | null>(null)
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [currentUsername, setCurrentUsername] = useState<string | null>(null)

  useEffect(() => {
    if (accessToken) {
      const payload = decodeJwtPayload(accessToken)
      if (payload) {
        const username = payload.preferred_username || payload.username || payload.sub
        setCurrentUsername(username || null)
      }
    }
  }, [accessToken])

  useEffect(() => {
    loadUsers()
  }, [accessToken, page])

  const loadUsers = async () => {
    try {
      setLoading(true)
      setError(null)
      
      const data = await gatewayRequest<SpringPage<UserProfileDto>>(
        `/api/v1/user-profiles/all?page=${page}&size=20&sort=createdAt,desc`,
        {},
        accessToken
      )
      
      const filteredUsers = currentUsername 
        ? data.content.filter(user => user.username !== currentUsername)
        : data.content
      setUsers(filteredUsers)
      setTotalPages(data.totalPages || 0)
    } catch (error) {
      console.error('Error loading users:', error)
      setError(`Failed to load users: ${error instanceof Error ? error.message : 'Unknown error'}`)
    } finally {
      setLoading(false)
    }
  }

  const handleDeleteUser = async (username: string) => {
    if (username === currentUsername) {
      setError('You cannot delete your own account')
      return
    }

    if (!confirm(`Are you sure you want to delete user "${username}"?`)) {
      return
    }

    try {
      setError(null)
      setSuccess(null)
      await gatewayRequest(`/api/admin/users/${encodeURIComponent(username)}`, {
        method: 'DELETE'
      }, accessToken)
      
      setSuccess(`User "${username}" deleted successfully`)
      await loadUsers()
    } catch {
      setError(`Failed to delete user "${username}"`)
    }
  }

  const handleRoleChange = async (username: string, newRole: UserRole) => {
    if (username === currentUsername) {
      setError('You cannot change your own role')
      return
    }

    try {
      setError(null)
      setSuccess(null)
      await gatewayRequest(`/api/admin/users/${encodeURIComponent(username)}/role?newRole=${newRole}`, {
        method: 'PUT'
      }, accessToken)
      
      setSuccess(`Role changed to "${newRole}" for user "${username}"`)
      await loadUsers()
    } catch {
      setError(`Failed to change role for user "${username}"`)
    }
  }

  if (loading) {
    return <div className="loading">Loading users...</div>
  }

  return (
    <div className="admin-container">
      <div className="admin-header">
        <h1>Admin Panel</h1>
        <div className="admin-actions">
          <button onClick={onNavigateToProfile} className="back-button">
            Back to Profile
          </button>
        </div>
      </div>

      {error && <div className="error-message">{error}</div>}
      {success && <div className="success-message">{success}</div>}

      <div className="users-table-container">
        <h2>Users Management</h2>
        
        {users.length === 0 ? (
          <div className="no-users">No users found</div>
        ) : (
          <table className="users-table">
            <thead>
              <tr>
                <th>Username</th>
                <th>Email</th>
                <th>First Name</th>
                <th>Last Name</th>
                <th>Created</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {users.map((user) => (
                <tr key={user.username}>
                  <td>{user.username}</td>
                  <td>{user.email}</td>
                  <td>{user.firstName}</td>
                  <td>{user.lastName}</td>
                  <td>{user.createdAt ? new Date(user.createdAt).toLocaleDateString() : 'N/A'}</td>
                  <td>
                    <div className="action-buttons">
                      <select 
                        className="role-select"
                        onChange={(e) => {
                          const newRole = e.target.value as UserRole | 'change'
                          if (newRole !== 'change') {
                            handleRoleChange(user.username, newRole as UserRole)
                            e.target.value = 'change'
                          }
                        }}
                        defaultValue="change"
                      >
                        <option value="change" disabled>Change Role</option>
                        <option value="USER">USER</option>
                        <option value="ADMIN">ADMIN</option>
                        <option value="OWNER">OWNER</option>
                      </select>
                      
                      <button 
                        onClick={() => handleDeleteUser(user.username)}
                        className="delete-user-button"
                        title="Delete user"
                      >
                        Delete
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}

        {totalPages > 1 && (
          <div className="pagination">
            <button 
              onClick={() => setPage(prev => Math.max(0, prev - 1))}
              disabled={page === 0}
              className="pagination-button"
            >
              Previous
            </button>
            <span className="pagination-info">
              Page {page + 1} of {totalPages}
            </span>
            <button 
              onClick={() => setPage(prev => Math.min(totalPages - 1, prev + 1))}
              disabled={page >= totalPages - 1}
              className="pagination-button"
            >
              Next
            </button>
          </div>
        )}
      </div>
    </div>
  )
}

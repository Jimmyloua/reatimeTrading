import { Routes, Route, Link, Navigate, useNavigate } from 'react-router-dom'
import { useEffect, useRef } from 'react'
import { useAuthStore } from './stores/authStore'
import { ProtectedRoute } from './components/ProtectedRoute'
import { Toaster } from './components/ui/sonner'
import { Button } from './components/ui/button'
import { Avatar, AvatarImage, AvatarFallback } from './components/ui/avatar'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from './components/ui/dropdown-menu'
import LoginPage from './pages/LoginPage'
import RegisterPage from './pages/RegisterPage'
import ProfilePage from './pages/ProfilePage'
import UserProfilePage from './pages/UserProfilePage'
import CreateListingPage from './pages/CreateListingPage'
import EditListingPage from './pages/EditListingPage'
import BrowseListingsPage from './pages/BrowseListingsPage'
import ListingDetailPage from './pages/ListingDetailPage'
import MessagesPage from './pages/MessagesPage'
import NotificationsPage from './pages/NotificationsPage'
import HomePage from './pages/HomePage'
import { TransactionsPage } from './pages/TransactionsPage'
import { TransactionDetailPage } from './pages/TransactionDetailPage'
import { RatingPage } from './pages/RatingPage'
import { NotificationBell } from './components/notifications/NotificationBell'
import { getInitials, getAvatarColor } from './pages/ProfilePage'
import { authApi } from './api/authApi'
import { useWebSocket } from './hooks/useWebSocket'

function App() {
  const navigate = useNavigate()
  const didBootstrap = useRef(false)
  const {
    accessToken,
    refreshToken,
    isAuthenticated,
    hasHydrated,
    user,
    logout,
    setBootstrapping,
    setTokens,
    setUser,
  } = useAuthStore()

  useEffect(() => {
    if (!hasHydrated || didBootstrap.current) {
      return
    }

    if (!refreshToken || accessToken) {
      didBootstrap.current = true
      return
    }

    didBootstrap.current = true
    setBootstrapping(true)

    void authApi
      .refresh(refreshToken)
      .then(async (response) => {
        setTokens(response.accessToken, response.refreshToken)
        const profile = await authApi.getProfile()
        setUser(profile)
      })
      .catch(() => {
        logout()
      })
      .finally(() => {
        setBootstrapping(false)
      })
  }, [accessToken, hasHydrated, logout, refreshToken, setBootstrapping, setTokens, setUser])

  const handleLogout = async () => {
    try {
      if (refreshToken) {
        await authApi.logout(refreshToken)
      }
    } catch (error) {
      console.error('Failed to notify backend about logout:', error)
    } finally {
      logout()
      navigate('/login', { replace: true })
    }
  }

  return (
    <div className="relative min-h-screen overflow-hidden bg-background">
      {isAuthenticated ? <SessionConnectionManager /> : null}
      <div className="pointer-events-none absolute inset-0 bg-[radial-gradient(circle_at_top_left,rgba(35,92,148,0.18),transparent_28%),radial-gradient(circle_at_top_right,rgba(251,191,36,0.14),transparent_24%),linear-gradient(180deg,#f8fbff_0%,#eef5fb_46%,#ffffff_100%)]" />
      {/* Navigation */}
      <nav className="sticky top-0 z-20 border-b border-white/40 bg-white/72 px-4 py-3 backdrop-blur-xl">
        <div className="mx-auto flex max-w-6xl items-center justify-between">
          <div className="flex items-center gap-6">
            <Link to="/" className="text-xl font-semibold tracking-tight text-slate-900">
              Trading Platform
            </Link>
            <Link to="/listings" className="text-sm text-slate-600 transition hover:text-slate-900">
              Browse
            </Link>
            {isAuthenticated && (
              <Link to="/transactions" className="text-sm text-slate-600 transition hover:text-slate-900">
                Transactions
              </Link>
            )}
          </div>
          <div className="flex items-center gap-4">
            {isAuthenticated && user ? (
              <>
                <Link to="/listings/create">
                  <Button size="sm">Sell</Button>
                </Link>
                <NotificationBell />
                <DropdownMenu>
                  <DropdownMenuTrigger className="flex items-center gap-2 rounded-full p-1 hover:bg-accent outline-none">
                    {user.avatarUrl ? (
                      <Avatar size="sm">
                        <AvatarImage src={user.avatarUrl} alt={user.displayName || 'User'} />
                        <AvatarFallback>
                          {getInitials(user.displayName, user.email)}
                        </AvatarFallback>
                      </Avatar>
                    ) : (
                      <div
                        className="flex h-6 w-6 items-center justify-center rounded-full text-xs font-semibold text-white"
                        style={{ backgroundColor: getAvatarColor(user.id) }}
                      >
                        {getInitials(user.displayName, user.email)}
                      </div>
                    )}
                  </DropdownMenuTrigger>
                  <DropdownMenuContent align="end">
                    <div className="px-1.5 py-1 text-sm font-medium">
                      {user.displayName || 'New User'}
                    </div>
                    <DropdownMenuSeparator />
                    <DropdownMenuItem>
                      <Link to="/profile">Profile</Link>
                    </DropdownMenuItem>
                    <DropdownMenuSeparator />
                    <DropdownMenuItem onClick={handleLogout}>
                      Sign out
                    </DropdownMenuItem>
                  </DropdownMenuContent>
                </DropdownMenu>
              </>
            ) : (
              <>
                <Link to="/login">
                  <Button variant="ghost">Sign in</Button>
                </Link>
                <Link to="/register">
                  <Button>Create account</Button>
                </Link>
              </>
            )}
          </div>
        </div>
      </nav>

      {/* Main content */}
      <main className="relative mx-auto max-w-6xl px-4 py-8">
        <Routes>
          <Route path="/" element={<HomePage />} />
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />
          <Route
            path="/profile"
            element={
              <ProtectedRoute>
                <ProfilePage />
              </ProtectedRoute>
            }
          />
          <Route path="/users/:id" element={<UserProfilePage />} />
          <Route path="/listings" element={<BrowseListingsPage />} />
          <Route path="/listings/:id" element={<ListingDetailPage />} />
          <Route
            path="/listings/create"
            element={
              <ProtectedRoute>
                <CreateListingPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/listings/:id/edit"
            element={
              <ProtectedRoute>
                <EditListingPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/messages"
            element={
              <ProtectedRoute>
                <MessagesPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/notifications"
            element={
              <ProtectedRoute>
                <NotificationsPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/transactions"
            element={
              <ProtectedRoute>
                <TransactionsPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/transactions/:id"
            element={
              <ProtectedRoute>
                <TransactionDetailPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/transactions/:transactionId/rate"
            element={
              <ProtectedRoute>
                <RatingPage />
              </ProtectedRoute>
            }
          />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </main>

      {/* Toast notifications */}
      <Toaster />
    </div>
  )
}

function SessionConnectionManager() {
  useWebSocket()
  return null
}

export default App

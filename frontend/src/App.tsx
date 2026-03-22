import { Routes, Route, Link, Navigate } from 'react-router-dom'
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
import { TransactionsPage } from './pages/TransactionsPage'
import { TransactionDetailPage } from './pages/TransactionDetailPage'
import { RatingPage } from './pages/RatingPage'
import { NotificationBell } from './components/notifications/NotificationBell'
import { getInitials, getAvatarColor } from './pages/ProfilePage'

const HOME_HERO_BG =
  'https://images.unsplash.com/photo-1756705533779-105bf34e0722?auto=format&fit=crop&q=80&w=2000'
const DASHBOARD_HERO_BG =
  'https://images.unsplash.com/photo-1555664424-778a1e5e1b48?auto=format&fit=crop&q=80&w=2000'

function App() {
  const { isAuthenticated, user, logout } = useAuthStore()

  const handleLogout = () => {
    logout()
  }

  return (
    <div className="relative min-h-screen overflow-hidden bg-background">
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

function HomePage() {
  const { isAuthenticated } = useAuthStore()

  if (!isAuthenticated) {
    return (
      <section
        className="relative overflow-hidden rounded-[2.25rem] border border-white/40 px-8 py-14 text-white shadow-[0_30px_90px_rgba(15,23,42,0.22)]"
        style={{
          backgroundImage: `linear-gradient(110deg, rgba(5, 15, 28, 0.88), rgba(16, 55, 92, 0.68) 52%, rgba(255, 199, 79, 0.18)), url(${HOME_HERO_BG})`,
          backgroundSize: 'cover',
          backgroundPosition: 'center',
        }}
      >
        <div className="absolute inset-0 bg-[radial-gradient(circle_at_top_right,rgba(255,255,255,0.22),transparent_28%),linear-gradient(180deg,transparent,rgba(2,6,23,0.12))]" />
        <div className="relative grid gap-10 lg:grid-cols-[1.2fr_0.8fr] lg:items-center">
          <div className="max-w-2xl">
            <p className="text-sm font-medium uppercase tracking-[0.32em] text-amber-200/80">
              Inspired by modern resale marketplaces
            </p>
            <h1 className="mt-5 text-4xl font-semibold leading-tight text-white md:text-6xl">
              Buy and sell digital devices in a cleaner, faster marketplace.
            </h1>
            <p className="mt-5 max-w-xl text-base leading-7 text-slate-200 md:text-lg">
              Discover verified listings, start instant conversations, and manage every transaction
              from one polished trading hub.
            </p>
            <div className="mt-8 flex flex-wrap gap-3">
              <div className="rounded-full border border-white/20 bg-white/10 px-4 py-2 text-sm text-white/90 backdrop-blur-sm">
                Live chat
              </div>
              <div className="rounded-full border border-white/20 bg-white/10 px-4 py-2 text-sm text-white/90 backdrop-blur-sm">
                Smart search
              </div>
              <div className="rounded-full border border-white/20 bg-white/10 px-4 py-2 text-sm text-white/90 backdrop-blur-sm">
                Secure transactions
              </div>
            </div>
            <div className="mt-10 flex flex-wrap gap-4">
              <Link to="/register">
                <Button className="h-11 bg-amber-300 px-6 text-slate-950 hover:bg-amber-200">
                  Create account
                </Button>
              </Link>
              <Link to="/listings">
                <Button variant="outline" className="h-11 border-white/40 bg-white/10 px-6 text-white hover:bg-white/15">
                  Browse listings
                </Button>
              </Link>
            </div>
          </div>

          <div className="grid gap-4">
            <div className="rounded-[1.75rem] border border-white/20 bg-white/12 p-5 shadow-xl backdrop-blur-md">
              <p className="text-sm uppercase tracking-[0.24em] text-white/65">Trending today</p>
              <div className="mt-4 grid gap-3">
                <div className="rounded-2xl bg-white/92 p-4 text-slate-900 shadow-lg">
                  <div className="text-sm text-slate-500">Mirrorless Camera</div>
                  <div className="mt-1 text-xl font-semibold">$799</div>
                </div>
                <div className="rounded-2xl bg-slate-950/70 p-4 text-white shadow-lg">
                  <div className="text-sm text-slate-300">Drone Bundle</div>
                  <div className="mt-1 text-xl font-semibold">$459</div>
                </div>
              </div>
            </div>
            <div className="rounded-[1.75rem] border border-white/15 bg-slate-950/35 p-5 shadow-xl backdrop-blur-md">
              <p className="text-sm text-slate-200">Marketplace pulse</p>
              <div className="mt-4 grid grid-cols-3 gap-3">
                <div className="rounded-2xl bg-white/10 p-4">
                  <div className="text-2xl font-semibold text-white">24k+</div>
                  <div className="mt-1 text-xs uppercase tracking-[0.18em] text-slate-300">Listings</div>
                </div>
                <div className="rounded-2xl bg-white/10 p-4">
                  <div className="text-2xl font-semibold text-white">8m</div>
                  <div className="mt-1 text-xs uppercase tracking-[0.18em] text-slate-300">Chats</div>
                </div>
                <div className="rounded-2xl bg-white/10 p-4">
                  <div className="text-2xl font-semibold text-white">98%</div>
                  <div className="mt-1 text-xs uppercase tracking-[0.18em] text-slate-300">Trust</div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>
    )
  }

  return (
    <section
      className="relative overflow-hidden rounded-[2rem] border border-white/40 px-8 py-12 shadow-[0_24px_80px_rgba(15,23,42,0.16)]"
      style={{
        backgroundImage: `linear-gradient(110deg, rgba(255,255,255,0.82), rgba(240,247,255,0.68) 48%, rgba(207,229,248,0.72)), url(${DASHBOARD_HERO_BG})`,
        backgroundSize: 'cover',
        backgroundPosition: 'center',
      }}
    >
      <div className="absolute inset-0 bg-[radial-gradient(circle_at_top_left,rgba(255,255,255,0.5),transparent_30%)]" />
      <div className="relative max-w-3xl">
        <p className="text-sm font-medium uppercase tracking-[0.26em] text-sky-700">Dashboard</p>
        <h1 className="mt-4 text-4xl font-semibold text-slate-900">Welcome back to your trading desk.</h1>
        <p className="mt-4 max-w-2xl text-base leading-7 text-slate-700">
          Track conversations, explore fresh listings, and move quickly on the devices your buyers
          and sellers care about most.
        </p>
        <div className="mt-8 flex flex-wrap gap-3">
          <Link to="/listings">
            <Button className="h-11 px-6">Browse listings</Button>
          </Link>
          <Link to="/listings/create">
            <Button variant="outline" className="h-11 bg-white/70 px-6">
              Create listing
            </Button>
          </Link>
        </div>
      </div>
    </section>
  )
}

export default App

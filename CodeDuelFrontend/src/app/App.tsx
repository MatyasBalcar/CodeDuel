import { useMutation, useQueryClient } from '@tanstack/react-query'
import {
  ArrowUpRight,
  BookOpen,
  Braces,
  CircleDot,
  Flag,
  Home,
  LogOut,
  Swords,
  Trophy,
  UserRound,
  Settings,
  ShieldCheck as ShieldIcon,
} from 'lucide-react'
import { Link, NavLink, Route, Routes, useLocation, useNavigate } from 'react-router-dom'
import { useSession, useSystem } from '../api/hooks'
import { write } from '../api/client'
import { Avatar, ErrorNotice } from '../components/Feedback'
import { AuthPage } from '../features/AuthPage'
import { LobbyPage } from '../features/LobbyPage'
import { ArenaPage } from '../features/ArenaPage'
import { ProblemsPage, ProblemPage } from '../features/ProblemsPage'
import { LeaderboardPage, ProfilePage, PublicProfilePage } from '../features/CommunityPages'
import { InvitePage } from '../features/InvitePage'
import { AccountSettingsPage, PrivacyPage } from '../features/AccountSettingsPage'
import { RecoveryPage } from '../features/RecoveryPage'
import { AdminPage } from '../features/AdminPage'
import { PracticePage } from '../features/PracticePage'
import { clearDrafts } from '../features/drafts'

const navigation = [
  { to: '/', label: 'Lobby', icon: Home, end: true },
  { to: '/problems', label: 'Challenges', icon: BookOpen },
  { to: '/leaderboard', label: 'Leaderboard', icon: Trophy },
  { to: '/profile', label: 'Your profile', icon: UserRound },
  { to: '/settings', label: 'Account settings', icon: Settings },
]

export function App() {
  const { data: session } = useSession()
  const system = useSystem()
  const queries = useQueryClient()
  const navigate = useNavigate()
  const location = useLocation()
  const logout = useMutation({
    mutationFn: () => write<void>('/auth/logout'),
    onSuccess: () => {
      if (session) clearDrafts(session.id)
      queries.clear()
      navigate('/')
    },
  })
  return (
    <div className="app-shell">
      <a className="skip-link" href="#main-content">
        Skip to content
      </a>
      <aside className="sidebar">
        <Link className="brand" to="/" aria-label="CodeDuel home">
          <span className="brand-mark">
            <Braces size={23} strokeWidth={2.5} />
          </span>
          CodeDuel<span className="brand-dot">.</span>
        </Link>
        <nav aria-label="Main navigation">
          {navigation.map(({ to, label, icon: Icon, end }) => (
            <NavLink
              key={to}
              to={to}
              end={end}
              className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}
            >
              <Icon size={19} />
              {label}
            </NavLink>
          ))}
          {session?.role === 'ADMIN' && (
            <NavLink className="nav-link" to="/admin">
              <ShieldIcon />
              Administration
            </NavLink>
          )}
        </nav>
        <div className="sidebar-note">
          <Swords size={23} />
          <h3>Practice Java</h3>
          <p>Work through challenges at your own pace.</p>
          <Link to="/problems">
            Browse challenges <ArrowUpRight size={14} />
          </Link>
        </div>
        <div className="sidebar-bottom">
          <span className="alpha-mark">
            <Flag size={14} /> ALPHA / 0.1
          </span>
        </div>
      </aside>
      <div className="workspace">
        <header className="topbar">
          <div className="breadcrumb">
            <span>Workspace</span>
            <span>/</span>
            <strong>{pageTitle(location.pathname)}</strong>
          </div>
          <div className="topbar-right">
            <span className={`connection ${system.isSuccess ? 'online' : ''}`}>
              <CircleDot size={13} />
              {system.isSuccess ? 'API connected' : 'Connecting'}
            </span>
            {session ? (
              <>
                <Link className="account-chip" to="/profile">
                  <Avatar name={session.username} small />
                  <span>{session.username}</span>
                </Link>
                <button
                  className="icon-button"
                  onClick={() => logout.mutate()}
                  disabled={logout.isPending}
                  aria-label="Sign out"
                >
                  <LogOut size={17} />
                </button>
              </>
            ) : (
              <Link className="button small secondary" to="/login">
                Sign in <ArrowUpRight size={14} />
              </Link>
            )}
          </div>
        </header>
        <main
          id="main-content"
          className={
            location.pathname.startsWith('/arena') ? 'main-content arena-main' : 'main-content'
          }
        >
          <ErrorNotice error={logout.error} />
          {system.data?.judgeMode === 'FIXTURE' && (
            <div className="demo-banner">
              <span className="demo-badge">DEMO</span>
              <span>
                Demo mode. Choose a sample program to try a submission.
              </span>
            </div>
          )}
          {system.data?.judgeMode === 'DISABLED' && (
            <div className="demo-banner">
              <span className="demo-badge">SETUP</span>
              <span>
                Submissions are unavailable while the judge is offline.
              </span>
            </div>
          )}
          <Routes>
            <Route path="/" element={<LobbyPage />} />
            <Route path="/login" element={<AuthPage />} />
            <Route path="/forgot-password" element={<RecoveryPage key="forgot" mode="forgot" />} />
            <Route path="/reset-password" element={<RecoveryPage key="reset" mode="reset" />} />
            <Route path="/verify-email" element={<RecoveryPage key="verify" mode="verify" />} />
            <Route path="/settings" element={<AccountSettingsPage />} />
            <Route path="/privacy" element={<PrivacyPage />} />
            <Route path="/register" element={<AuthPage register />} />
            <Route path="/invite/:code" element={<InvitePage />} />
            <Route path="/arena/:id" element={<ArenaPage />} />
            <Route path="/practice/:slug" element={<PracticePage />} />
            <Route path="/problems" element={<ProblemsPage />} />
            <Route path="/problems/:slug" element={<ProblemPage />} />
            <Route path="/leaderboard" element={<LeaderboardPage />} />
            <Route path="/players/:username" element={<PublicProfilePage />} />
            <Route path="/admin" element={<AdminPage />} />
            <Route path="/profile" element={<ProfilePage />} />
            <Route
              path="*"
              element={
                <section className="empty-state panel">
                  <h1>Page not found</h1>
                  <Link to="/" className="button primary">
                    Back to lobby
                  </Link>
                </section>
              }
            />
          </Routes>
        </main>
        <footer className="page-footer">
          <span>
            <Braces size={13} /> CodeDuel
          </span>
          <Link to="/privacy">Privacy</Link>
          <span>
            Java 21 <span className="tiny-dot" /> v0.1.0
          </span>
        </footer>
      </div>
    </div>
  )
}

function pageTitle(path: string) {
  if (path.startsWith('/arena')) return 'Arena'
  if (path.startsWith('/practice')) return 'Practice'
  if (path.startsWith('/problems')) return 'Challenges'
  if (path.startsWith('/leaderboard')) return 'Leaderboard'
  if (path.startsWith('/profile') || path.startsWith('/players')) return 'Player profile'
  if (path.startsWith('/settings')) return 'Account settings'
  if (path.startsWith('/admin')) return 'Administration'
  if (path.startsWith('/privacy')) return 'Privacy'
  if (path.startsWith('/invite')) return 'Private duel'
  if (path === '/') return 'Lobby'
  return 'Account'
}

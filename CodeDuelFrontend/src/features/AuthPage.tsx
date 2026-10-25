import { useMutation, useQueryClient } from '@tanstack/react-query'
import { ArrowRight, LockKeyhole } from 'lucide-react'
import { Link, Navigate, useNavigate, useSearchParams } from 'react-router-dom'
import { write } from '../api/client'
import { sessionKey, useSession, useSystem } from '../api/hooks'
import type { Profile } from '../api/types'
import { ErrorNotice, Loading } from '../components/Feedback'

export function AuthPage({ register = false }: { register?: boolean }) {
  const session = useSession()
  const system = useSystem()
  const queries = useQueryClient()
  const navigate = useNavigate()
  const [params] = useSearchParams()
  const requestedPage = params.get('next') ?? ''
  const next = requestedPage.startsWith('/invite/') ? requestedPage : '/'
  const login = useMutation({
    mutationFn: async ({
      username,
      email,
      password,
    }: {
      username: string
      email?: string
      password: string
    }) => {
      if (register) await write<void>('/auth/register', 'POST', { username, email, password })
      return write<Profile>('/auth/login', 'POST', { username, password })
    },
    onSuccess: (profile) => {
      queries.setQueryData(sessionKey, profile)
      navigate(next, { replace: true })
    },
  })
  if (session.isPending) return <Loading />
  if (session.data) return <Navigate to={next} replace />
  return (
    <div className="auth-layout">
      <div className="auth-story">
        <h1>Code with an opponent.</h1>
        <p>Solve Java challenges in a timed duel, or practice on your own.</p>
      </div>
      <section className="panel auth-card">
        <div className="section-icon">
          <LockKeyhole size={22} />
        </div>
        <h2>{register ? 'Create an account' : 'Sign in'}</h2>
        <p className="muted">
          {register ? 'Choose a username to get started.' : 'Use your CodeDuel account.'}
        </p>
        <ErrorNotice error={login.error} />
        <form
          onSubmit={(event) => {
            event.preventDefault()
            const form = new FormData(event.currentTarget)
            login.mutate({
              username: String(form.get('username')),
              email: String(form.get('email') ?? ''),
              password: String(form.get('password')),
            })
          }}
        >
          <label>
            Username
            <input
              name="username"
              autoComplete="username"
              required
              minLength={3}
              maxLength={24}
              pattern={register ? '[a-zA-Z][a-zA-Z0-9_]{2,23}' : undefined}
              placeholder="Your player name"
            />
          </label>
          {register && (
            <label>
              Email
              <input
                name="email"
                type="email"
                autoComplete="email"
                maxLength={254}
                required
                placeholder="you@example.com"
              />
            </label>
          )}
          <label>
            Password
            <input
              name="password"
              type="password"
              autoComplete={register ? 'new-password' : 'current-password'}
              minLength={register ? 12 : undefined}
              maxLength={72}
              required
              placeholder={register ? 'At least 12 characters' : 'Your password'}
            />
          </label>
          <button className="button primary full" disabled={login.isPending}>
            {login.isPending ? 'Signing in…' : register ? 'Create account' : 'Sign in'}
            <ArrowRight size={17} />
          </button>
        </form>
        {!register && (
          <Link className="text-link" to="/forgot-password">
            Forgot your password?
          </Link>
        )}
        <p className="auth-switch">
          {register ? 'Already have an account?' : 'Need an account?'}{' '}
          <Link
            to={`${register ? '/login' : '/register'}${next !== '/' ? `?next=${encodeURIComponent(next)}` : ''}`}
          >
            {register ? 'Sign in' : 'Create account'}
          </Link>
        </p>
        {system.data?.judgeMode === 'FIXTURE' && (
          <div className="demo-accounts">
            <h3>Demo accounts</h3>
            <p>
              <code>nova</code> or <code>atlas</code>
              <br />
              Password: <code>CodeDuel-demo-2026!</code>
            </p>
            <p>Use separate browser profiles to play both sides.</p>
          </div>
        )}
      </section>
    </div>
  )
}

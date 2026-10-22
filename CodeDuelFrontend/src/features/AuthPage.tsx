import { useMutation, useQueryClient } from '@tanstack/react-query'
import { ArrowRight, Braces, LockKeyhole } from 'lucide-react'
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
  const next = params.get('next')?.startsWith('/invite/') ? params.get('next')! : '/'
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
        <span className="eyebrow">
          <Braces size={16} /> THE NEXT LEVEL IS YOURS
        </span>
        <h1>
          Great code.
          <br />
          Better rivals.
        </h1>
        <p>
          A shared problem, a ticking clock, and someone who makes you a little better. Welcome to
          CodeDuel.
        </p>
        <div className="auth-principles">
          <span>
            01 <strong>Equal challenge</strong>
          </span>
          <span>
            02 <strong>Fair competition</strong>
          </span>
          <span>
            03 <strong>Visible progress</strong>
          </span>
        </div>
      </div>
      <section className="panel auth-card">
        <div className="section-icon">
          <LockKeyhole size={22} />
        </div>
        <h2>{register ? 'Join the arena' : 'Welcome back'}</h2>
        <p className="muted">
          {register ? 'Your first good rivalry starts here.' : 'Your next challenge is waiting.'}
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
            {login.isPending ? 'Opening the arena…' : register ? 'Create account' : 'Sign in'}
            <ArrowRight size={17} />
          </button>
        </form>
        {!register && (
          <Link className="text-link" to="/forgot-password">
            Forgot your password?
          </Link>
        )}
        <p className="auth-switch">
          {register ? 'Already have an account?' : 'New around here?'}{' '}
          <Link
            to={`${register ? '/login' : '/register'}${next !== '/' ? `?next=${encodeURIComponent(next)}` : ''}`}
          >
            {register ? 'Sign in' : 'Create account'}
          </Link>
        </p>
        {system.data?.judgeMode === 'FIXTURE' && (
          <div className="demo-accounts">
            <span className="eyebrow">LOCAL DEMO ACCOUNTS</span>
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

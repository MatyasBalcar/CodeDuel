import { useEffect, useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { write } from '../api/client'
import { ErrorNotice } from '../components/Feedback'

export function RecoveryPage({ mode }: { mode: 'forgot' | 'reset' | 'verify' }) {
  const queries = useQueryClient()
  const [token] = useState(
    () => new URLSearchParams(window.location.hash.slice(1)).get('token') ?? '',
  )
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [confirm, setConfirm] = useState('')
  useEffect(() => {
    if (window.location.hash)
      window.history.replaceState(null, '', window.location.pathname + window.location.search)
  }, [])
  const action = useMutation({
    mutationFn: async () => {
      if (mode === 'forgot')
        await write<{ message: string }>('/auth/forgot-password', 'POST', { email })
      else if (mode === 'reset')
        await write<void>('/auth/reset-password', 'POST', { token, password })
      else await write<void>('/auth/verify-email', 'POST', { token })
    },
    onSuccess: () => {
      setPassword('')
      setConfirm('')
      void queries.invalidateQueries({ queryKey: ['session'] })
      void queries.invalidateQueries({ queryKey: ['account'] })
    },
  })
  const title =
    mode === 'forgot'
      ? 'Find your way back.'
      : mode === 'reset'
        ? 'A fresh start.'
        : 'Make it official.'
  return (
    <section className="panel account-panel">
      <span className="eyebrow">YOUR ACCOUNT</span>
      <h1>{title}</h1>
      <ErrorNotice error={action.error} />
      {action.isSuccess ? (
        <div role="status">
          <p>
            {mode === 'forgot'
              ? 'If this address belongs to an account, a reset link will arrive shortly.'
              : mode === 'reset'
                ? 'Password updated. Sign in with your new password; previous sessions have been revoked.'
                : 'Email verified. You can now join ranked games.'}
          </p>
          <Link className="button primary" to={mode === 'verify' ? '/settings' : '/login'}>
            {mode === 'verify' ? 'Account settings' : 'Back to sign in'}
          </Link>
        </div>
      ) : mode !== 'forgot' && !token ? (
        <>
          <p>This page needs the one-time link from your email.</p>
          <Link to="/forgot-password" className="text-link">
            Request a new reset link
          </Link>
        </>
      ) : (
        <form
          className="account-form"
          onSubmit={(event) => {
            event.preventDefault()
            action.mutate()
          }}
        >
          {mode === 'forgot' && (
            <label>
              Email address
              <input
                required
                type="email"
                autoComplete="email"
                value={email}
                onChange={(event) => setEmail(event.target.value)}
              />
            </label>
          )}
          {mode === 'reset' && (
            <>
              <label>
                New password
                <input
                  required
                  type="password"
                  autoComplete="new-password"
                  minLength={12}
                  maxLength={72}
                  value={password}
                  onChange={(event) => setPassword(event.target.value)}
                />
              </label>
              <label>
                Confirm new password
                <input
                  required
                  type="password"
                  autoComplete="new-password"
                  value={confirm}
                  onChange={(event) => setConfirm(event.target.value)}
                />
              </label>
              {confirm && confirm !== password && (
                <p className="danger" role="status">
                  The passwords need to match.
                </p>
              )}
            </>
          )}
          {mode === 'verify' && <p>Confirm this one-time link to verify your email address.</p>}
          <button
            className="button primary"
            disabled={action.isPending || (mode === 'reset' && (!password || password !== confirm))}
          >
            {action.isPending
              ? 'Working…'
              : mode === 'forgot'
                ? 'Email a reset link'
                : mode === 'reset'
                  ? 'Update password'
                  : 'Verify email'}
          </button>
        </form>
      )}
    </section>
  )
}

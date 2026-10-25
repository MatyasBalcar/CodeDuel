import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link, useNavigate } from 'react-router-dom'
import { read, write } from '../api/client'
import { useSession } from '../api/hooks'
import { ErrorNotice, Loading } from '../components/Feedback'
import { clearDrafts } from './drafts'

interface Account {
  email: string
  emailVerified: boolean
  language: string
  sourceRetentionDays: number
}
export function AccountSettingsPage() {
  const session = useSession()
  const navigate = useNavigate()
  const queries = useQueryClient()
  const account = useQuery({
    queryKey: ['account', session.data?.id],
    queryFn: () => read<Account>('/auth/account'),
    enabled: !!session.data,
  })
  const [password, setPassword] = useState('')
  const [confirmed, setConfirmed] = useState(false)
  const resend = useMutation({ mutationFn: () => write<void>('/auth/verification-email') })
  const deletion = useMutation({
    mutationFn: () => write<void>('/auth/delete-account', 'POST', { password }),
    onSuccess: () => {
      if (session.data) clearDrafts(session.data.id)
      queries.clear()
      navigate('/')
    },
  })
  if (session.isPending) return <Loading />
  if (!session.data)
    return (
      <section className="panel empty-state">
        <h1>Account settings</h1>
        <Link className="button primary" to="/login">
          Sign in
        </Link>
      </section>
    )
  return (
    <>
      <div className="page-heading">
        <div>
          <h1>Account settings</h1>
          <p>Manage your email, password, and saved source code.</p>
        </div>
      </div>
      <section className="panel section-panel">
        <h2>Email & language</h2>
        <ErrorNotice error={account.error || resend.error} />
        {account.data ? (
          <>
            <p>
              {account.data.email} ·{' '}
              <strong>{account.data.emailVerified ? 'Verified' : 'Verification pending'}</strong>
            </p>
            <p>Java 21 is the supported language.</p>
            {!account.data.emailVerified && (
              <button
                className="button secondary"
                disabled={resend.isPending}
                onClick={() => resend.mutate()}
              >
                Email me a verification link
              </button>
            )}
            {resend.isSuccess && (
              <p role="status">Verification email queued. Use the most recent link.</p>
            )}
            <p>
              <Link to="/forgot-password" className="text-link">
                Reset your password
              </Link>
            </p>
          </>
        ) : (
          <Loading />
        )}
      </section>
      <section className="panel section-panel account-retention">
        <h2>Saved source code</h2>
        <p>
          Submitted programs and detailed feedback are visible to you. Source is removed after{' '}
          {account.data?.sourceRetentionDays ?? 90} days. Match outcomes and rating history remain
          available.
        </p>
        <p>
          Drafts stay in this browser for seven days and are cleared for this account on sign-out.
        </p>
        <Link to="/privacy" className="text-link">
          Read the data policy
        </Link>
      </section>
      <section className="panel section-panel">
        <details>
          <summary className="danger">Delete this account</summary>
          <p>
            This removes your email, credentials, private source and feedback, and signs you out
            everywhere. Historical results retain an anonymous player entry. Finish or forfeit any
            active battle first.
          </p>
          <ErrorNotice error={deletion.error} />
          <form
            className="account-form"
            onSubmit={(event) => {
              event.preventDefault()
              deletion.mutate()
            }}
          >
            <label>
              Current password
              <input
                required
                type="password"
                autoComplete="current-password"
                value={password}
                onChange={(event) => setPassword(event.target.value)}
              />
            </label>
            <label className="checkbox-label">
              <input
                required
                type="checkbox"
                checked={confirmed}
                onChange={(event) => setConfirmed(event.target.checked)}
              />
              I understand this permanently removes my account data.
            </label>
            <button
              className="button secondary danger"
              disabled={!confirmed || !password || deletion.isPending}
            >
              {deletion.isPending ? 'Deleting…' : 'Permanently delete account'}
            </button>
          </form>
        </details>
      </section>
    </>
  )
}

export function PrivacyPage() {
  return (
    <section className="panel section-panel account-policy">
      <h1>Privacy</h1>
      <h2>Accounts and source</h2>
      <p>
        We use your username, email and password hash to operate your account. Your submitted source
        and detailed feedback are private to you. Opponents receive only minimal progress status.
      </p>
      <p>
        Source is retained for the configured period shown in account settings, normally 90 days.
        Browser drafts expire after seven days and clear on sign-out. Isolated compiled artifacts
        expire within five minutes. Reset links expire after 15 minutes and verification links after
        24 hours.
      </p>
      <h2>Deleting your account</h2>
      <p>
        Deletion revokes sessions, removes account contact details and private submission content,
        and replaces your public identity with an anonymous player entry. Completed match results,
        rating events and administrative audit records remain to preserve fair competition.
        Published challenge content remains part of the library.
      </p>
      <h2>Backups and operators</h2>
      <p>
        Database operators can access stored data for maintenance. Backups must be encrypted and
        restricted, with a maximum 30-day retention in a hosted installation. Deleted content can
        remain in those backups until they expire. A restored backup must have current deletion
        requests reapplied before serving users.
      </p>
      <h2>Fair play</h2>
      <p>
        Ranked games use the earliest server-received solution that passes every case.
        Infrastructure failures do not award a loss. Use your own work in ranked play; this platform
        does not claim to reliably detect AI assistance or plagiarism.
      </p>
    </section>
  )
}

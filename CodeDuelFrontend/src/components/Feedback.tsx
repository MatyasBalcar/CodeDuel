import { AlertCircle, LoaderCircle } from 'lucide-react'
import { Link } from 'react-router-dom'
import { ApiError } from '../api/client'

export function ErrorNotice({ error }: { error: unknown }) {
  if (!error) return null
  const message = error instanceof Error ? error.message : 'Something went wrong. Please try again.'
  return (
    <div className="error-notice" role="alert">
      <AlertCircle size={18} />
      <span>{message}</span>
    </div>
  )
}

export function Loading({ label = 'Loading…' }: { label?: string }) {
  return (
    <div className="loading" role="status">
      <LoaderCircle className="spin" size={21} />
      <span>{label}</span>
    </div>
  )
}

export function Unavailable({ error }: { error: unknown }) {
  return (
    <section className="panel empty-state">
      <AlertCircle size={28} />
      <h1>
        {error instanceof ApiError && error.status === 404
          ? 'This page is unavailable'
          : 'We couldn’t load this page'}
      </h1>
      <ErrorNotice error={error} />
      <Link className="button primary" to="/">
        Back to lobby
      </Link>
    </section>
  )
}

export function Avatar({ name, small = false }: { name: string; small?: boolean }) {
  return (
    <span className={`avatar ${small ? 'small' : ''}`} aria-hidden="true">
      {name.slice(0, 2).toUpperCase()}
    </span>
  )
}

export function Difficulty({ value }: { value: string }) {
  return <span className={`difficulty ${value.toLowerCase()}`}>{value.toLowerCase()}</span>
}

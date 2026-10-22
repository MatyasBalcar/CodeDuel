import { useQuery } from '@tanstack/react-query'
import { api } from '../api/client'
import { displayRating } from '../api/ratings'
import { ErrorNotice, Loading } from './Feedback'

export function RatingHistory({ username }: { username: string }) {
  const history = useQuery({
    queryKey: ['rating-history', username],
    queryFn: () => api.ratingHistory(username),
  })
  const events = history.data ?? []
  const values = events.length
    ? [events[0]!.oldRating, ...events.map((point) => point.newRating)]
    : []
  const low = Math.min(...values) - 15
  const high = Math.max(...values) + 15
  const points = values
    .map(
      (rating, i) =>
        `${32 + (i * 696) / Math.max(1, values.length - 1)},${178 - ((rating - low) * 144) / (high - low)}`,
    )
    .join(' ')
  return (
    <section className="panel section-panel rating-history">
      <div className="section-heading">
        <h2>Rating over time</h2>
        <span className="muted">Last 100 ranked games</span>
      </div>
      <ErrorNotice error={history.error} />
      {history.isPending ? (
        <Loading />
      ) : values.length ? (
        <>
          <svg
            viewBox="0 0 760 215"
            role="img"
            aria-label={`Rating history for ${username}, from ${displayRating(values[0]!)} to ${displayRating(values.at(-1)!)} Elo`}
          >
            <title>{username} rating history</title>
            <line x1="32" y1="178" x2="728" y2="178" stroke="currentColor" opacity=".15" />
            <polyline
              points={points}
              fill="none"
              stroke="#237a5c"
              strokeWidth="3"
              strokeLinejoin="round"
            />
            <text x="32" y="205">
              {displayRating(values[0]!)} Elo
            </text>
            <text x="728" y="205" textAnchor="end">
              {displayRating(values.at(-1)!)} Elo
            </text>
          </svg>
          <details>
            <summary>View rating changes</summary>
            <div className="rating-events">
              {events.map((event) => (
                <p key={event.battleId}>
                  <time>{new Date(event.occurredAt).toLocaleString()}</time>
                  <span className="mono">
                    {displayRating(event.oldRating)} → {displayRating(event.newRating)} (
                    {event.delta >= 0 ? '+' : ''}
                    {displayRating(event.delta)})
                  </span>
                </p>
              ))}
            </div>
          </details>
        </>
      ) : (
        <p className="muted">A rating history appears after the first ranked game.</p>
      )}
    </section>
  )
}

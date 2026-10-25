import { displayRating } from '../api/ratings'
import { ChevronRight, Swords, Trophy } from 'lucide-react'
import { Link } from 'react-router-dom'
import type { MatchHistory } from '../api/types'

export function HistoryList({ matches, userId }: { matches: MatchHistory[]; userId: string }) {
  return (
    <div className="history-list">
      {matches.map((match) => {
        const outcome = matchOutcome(match, userId)
        return (
          <Link to={`/arena/${match.id}`} className="history-row" key={match.id}>
            <span className={`outcome-mark ${outcome.toLowerCase()}`}>
              {outcome === 'Win' ? <Trophy size={17} /> : <Swords size={17} />}
            </span>
            <span className="history-name">
              <strong>{match.title}</strong>
              <small>
                vs {match.opponent ?? 'waiting for opponent'} · {match.mode.toLowerCase()}
              </small>
            </span>
            <span className="history-outcome">
              {outcome}
              <small>
                {new Date(match.createdAt).toLocaleDateString(undefined, {
                  month: 'short',
                  day: 'numeric',
                })}
              </small>
            </span>
            <span className={`rating-delta ${match.delta && match.delta > 0 ? 'positive' : ''}`}>
              {match.delta == null
                ? '—'
                : `${match.delta >= 0 ? '+' : ''}${displayRating(match.delta)}`}
            </span>
            <ChevronRight size={15} />
          </Link>
        )
      })}
    </div>
  )
}

function matchOutcome(match: MatchHistory, userId: string) {
  if (match.status === 'CANCELLED') return 'Cancelled'
  if (match.status !== 'FINISHED') return 'Active'
  if (!match.winnerId) return 'Draw'
  return match.winnerId === userId ? 'Win' : 'Loss'
}

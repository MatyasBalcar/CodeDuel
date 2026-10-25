import { displayRating } from '../api/ratings'
import { useQuery } from '@tanstack/react-query'
import { ArrowRight, ShieldCheck, Swords, Trophy } from 'lucide-react'
import { Link, useParams } from 'react-router-dom'
import { api } from '../api/client'
import { useSession } from '../api/hooks'
import { Avatar, ErrorNotice, Loading } from '../components/Feedback'
import { RatingHistory } from '../components/RatingHistory'
import { HistoryList } from '../components/HistoryList'

export function LeaderboardPage() {
  const players = useQuery({
    queryKey: ['leaderboard'],
    queryFn: api.leaderboard,
    refetchInterval: 15_000,
  })
  const { data: session } = useSession()
  return (
    <>
      <div className="page-heading">
        <div>
          <h1>Leaderboard</h1>
          <p>Players ranked by Elo rating.</p>
        </div>
        <span className="outline-chip">
          <Trophy size={14} /> TOP 100
        </span>
      </div>
      <section className="panel leaderboard-panel">
        <div className="leaderboard-table-head">
          <span>RANK</span>
          <span>PLAYER</span>
          <span>W / L / D</span>
          <span>GAMES</span>
          <span>ELO</span>
        </div>
        <ErrorNotice error={players.error} />
        {players.isPending ? (
          <Loading />
        ) : (
          players.data?.map((player, i) => (
            <div
              className={`leaderboard-table-row ${player.id === session?.id ? 'is-you' : ''}`}
              key={player.id}
            >
              <span className={`rank-number rank-${i}`}>{String(i + 1).padStart(2, '0')}</span>
              <div className="leaderboard-player">
                <Avatar name={player.username} />
                <span>
                  <strong>
                    <Link to={`/players/${player.username}`}>{player.username}</Link>
                    {player.id === session?.id && <small className="you-label">YOU</small>}
                  </strong>
                  <small>
                    {player.provisional
                      ? `Provisional · ${player.rankedGames}/10 games`
                      : 'Established'}
                  </small>
                </span>
              </div>
              <span className="mono record-cell">
                {player.wins} / {player.losses} / {player.draws}
              </span>
              <span className="mono">{player.rankedGames}</span>
              <strong className="mono leaderboard-rating">{displayRating(player.rating)}</strong>
            </div>
          ))
        )}
        {players.data?.length === 0 && (
          <div className="empty-state">
            <h2>No players yet</h2>
            <Link className="text-link" to="/register">
              Be the first player <ArrowRight size={15} />
            </Link>
          </div>
        )}
      </section>
      <div className="content-note">
        <ShieldCheck size={17} />
        <span>
          Players begin at 1200 Elo. The first 10 ranked games are provisional. Private games never
          affect ratings. Public values are rounded; calculations retain six decimal places.
        </span>
      </div>
    </>
  )
}

export function ProfilePage() {
  const session = useSession()
  const history = useQuery({
    queryKey: ['history', session.data?.id],
    queryFn: api.history,
    enabled: !!session.data,
  })
  if (session.isPending) return <Loading />
  if (!session.data)
    return (
      <section className="panel empty-state">
        <Trophy size={31} />
        <h1>Your profile</h1>
        <p>Sign in to track your progress and revisit your battles.</p>
        <Link className="button primary" to="/login">
          Sign in <ArrowRight size={16} />
        </Link>
      </section>
    )
  const player = session.data
  return (
    <>
      <div className="page-heading">
        <div>
          <h1>Your profile</h1>
          <p>Your rating, results, and recent battles.</p>
        </div>
      </div>
      <section className="panel profile-card">
        <Avatar name={player.username} />
        <div>
          <h2>{player.username}</h2>
          <p>
            {player.provisional
              ? `Provisional · ${10 - player.rankedGames} games until established`
              : 'Established player'}{' '}
            · Java 21
          </p>
        </div>
        <div className="profile-rating">
          <strong>{displayRating(player.rating)}</strong>
          <span>ELO RATING</span>
        </div>
      </section>
      <div className="profile-stats">
        {[
          { label: 'Ranked games', value: player.rankedGames },
          { label: 'Wins', value: player.wins },
          { label: 'Losses', value: player.losses },
          { label: 'Draws', value: player.draws },
        ].map((item) => (
          <section className="panel" key={item.label}>
            <strong>{item.value}</strong>
            <span>{item.label}</span>
          </section>
        ))}
      </div>
      <RatingHistory username={player.username} />
      <section className="panel section-panel">
        <div className="section-heading">
          <div>
            <h2>Your battle history</h2>
          </div>
          <Swords size={23} />
        </div>
        <ErrorNotice error={history.error} />
        {history.isPending ? (
          <Loading />
        ) : history.data?.length ? (
          <HistoryList matches={history.data} userId={player.id} />
        ) : (
          <div className="empty-state">
            <h3>No battles yet.</h3>
            <Link className="button primary" to="/">
              Find an opponent <ArrowRight size={16} />
            </Link>
          </div>
        )}
      </section>
    </>
  )
}

export function PublicProfilePage() {
  const { username = '' } = useParams()
  const profile = useQuery({
    queryKey: ['profile', username],
    queryFn: () => api.profile(username),
  })
  if (profile.isPending) return <Loading />
  if (!profile.data) return <ErrorNotice error={profile.error} />
  const player = profile.data
  return (
    <>
      <div className="page-heading">
        <div>
          <h1>{player.username}</h1>
          <p>{player.provisional ? 'Provisional' : 'Established'} · Java 21</p>
        </div>
      </div>
      <section className="panel profile-card">
        <Avatar name={player.username} />
        <div>
          <h2>{displayRating(player.rating)} Elo</h2>
          <p>
            {player.wins} wins · {player.losses} losses · {player.draws} draws
          </p>
        </div>
      </section>
      <RatingHistory username={username} />
    </>
  )
}

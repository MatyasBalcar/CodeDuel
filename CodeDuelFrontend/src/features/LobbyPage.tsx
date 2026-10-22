import { displayRating } from '../api/ratings'
import { useEffect, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  ArrowRight,
  ArrowUpRight,
  Check,
  Clock3,
  Code2,
  Flag,
  Radar,
  ShieldCheck,
  Swords,
  Target,
  Trophy,
  Users,
  X,
} from 'lucide-react'
import { Link, useNavigate } from 'react-router-dom'
import { api } from '../api/client'
import { HistoryList } from '../components/HistoryList'
import { useSession } from '../api/hooks'
import { Avatar, Difficulty, ErrorNotice, Loading } from '../components/Feedback'

export function LobbyPage() {
  const { data: session, isPending } = useSession()
  const navigate = useNavigate()
  const queries = useQueryClient()
  const [invite, setInvite] = useState('')
  const queue = useQuery({
    queryKey: ['queue', session?.id],
    queryFn: api.queue,
    enabled: !!session,
    refetchInterval: 2000,
  })
  const history = useQuery({
    queryKey: ['history', session?.id],
    queryFn: api.history,
    enabled: !!session,
  })
  const problems = useQuery({ queryKey: ['problems'], queryFn: api.problems })
  const leaderboard = useQuery({ queryKey: ['leaderboard'], queryFn: api.leaderboard })
  const enter = useMutation({
    mutationFn: api.enterQueue,
    onSuccess: (state) => queries.setQueryData(['queue', session?.id], state),
  })
  const cancel = useMutation({
    mutationFn: api.cancelQueue,
    onSuccess: (state) => queries.setQueryData(['queue', session?.id], state),
  })
  const room = useMutation({
    mutationFn: api.privateRoom,
    onSuccess: ({ battleId, inviteCode }) => {
      sessionStorage.setItem(`invite:${battleId}`, inviteCode)
      navigate(`/arena/${battleId}`)
    },
  })
  const queued = queue.data?.status === 'QUEUED'
  const currentBattle = queue.data?.battleId
  const heartbeat = enter.mutate

  useEffect(() => {
    if (!queued) return
    const timer = setInterval(() => heartbeat(), 20_000)
    return () => clearInterval(timer)
  }, [queued, heartbeat])

  if (isPending) return <Loading />
  const startRanked = () => (session ? enter.mutate() : navigate('/login'))
  return (
    <>
      <div className="page-heading">
        <div>
          <span className="eyebrow">YOUR DAILY DOSE OF A GOOD CHALLENGE</span>
          <h1>
            {session ? `Good to see you, ${session.username}.` : 'Welcome to your next level.'}
          </h1>
          <p>A little competition. A lot of progress.</p>
        </div>
        <span className="outline-chip">
          <Flag size={14} /> EARLY ACCESS
        </span>
      </div>
      <ErrorNotice error={enter.error || cancel.error || room.error || queue.error} />
      {currentBattle && (
        <Link className="active-battle-banner" to={`/arena/${currentBattle}`}>
          <Swords size={20} />
          <div>
            <strong>Your battle is ready</strong>
            <span>Rejoin your active match. Your server clock keeps running.</span>
          </div>
          <span className="button small primary">
            Enter arena <ArrowRight size={15} />
          </span>
        </Link>
      )}
      <section className="lobby-hero">
        <div className="hero-copy">
          <span className="hero-label">
            <span className="pulse-dot" /> THE HEAD-TO-HEAD CODING ARENA
          </span>
          <h2>
            Same problem.
            <br />
            Your <em>next move.</em>
          </h2>
          <p>
            Meet your match. Find your flow. Turn the problems you love solving into a game worth
            playing.
          </p>
          <div className="hero-actions">
            <button
              className="button lime"
              onClick={startRanked}
              disabled={enter.isPending || queued || !!currentBattle}
            >
              <Swords size={18} />
              {queued ? 'Finding your rival…' : 'Play ranked'}
              {!queued && <ArrowUpRight size={17} />}
            </button>
            <button
              className="button hero-secondary"
              onClick={() => (session ? room.mutate() : navigate('/login'))}
              disabled={room.isPending || queued || !!currentBattle}
            >
              <Users size={17} />
              Challenge a friend
            </button>
          </div>
          <div className="hero-facts">
            <span>
              <Check size={13} /> 1v1 duels
            </span>
            <span>
              <Check size={13} /> Java 21
            </span>
            <span>
              <Check size={13} /> Your pace, your progress
            </span>
          </div>
        </div>
        <div className="duel-graphic" aria-hidden="true">
          <div className="graphic-orbit orbit-one" />
          <div className="graphic-orbit orbit-two" />
          <div className="code-tile tile-left">
            <span className="tile-label">YOUR APPROACH</span>
            <Code2 size={44} strokeWidth={1.3} />
            <div className="code-lines">
              <i />
              <i />
              <i />
            </div>
            <span className="tile-status">
              <span /> READY TO SOLVE
            </span>
          </div>
          <div className="versus">
            vs<span>THE SAME CLOCK</span>
          </div>
          <div className="code-tile tile-right">
            <span className="tile-label">A FRESH PERSPECTIVE</span>
            <Brackets />
            <div className="code-lines">
              <i />
              <i />
              <i />
            </div>
            <span className="tile-status">
              <span /> YOUR NEXT RIVAL
            </span>
          </div>
          <span className="graphic-coordinate">01 / THE ARENA</span>
        </div>
      </section>
      {queued && (
        <section className="queue-panel panel" aria-live="polite">
          <Radar className="spin-slow" size={31} />
          <div>
            <h3>Finding a worthy rival…</h3>
            <p>
              Searching within ±{queue.data?.ratingWindow} Elo. The window expands as you wait. Keep
              this page open.
            </p>
          </div>
          <button
            className="button secondary small"
            onClick={() => cancel.mutate()}
            disabled={cancel.isPending}
          >
            <X size={15} />
            Cancel search
          </button>
        </section>
      )}
      <div className="stat-grid">
        <Stat
          icon={<Trophy size={20} />}
          label="YOUR RATING"
          value={session ? String(displayRating(session.rating)) : '1,200'}
          detail={
            session
              ? session.provisional
                ? `${session.rankedGames}/10 provisional games`
                : 'Established rating'
              : 'Your starting Elo'
          }
        />
        <Stat
          icon={<Swords size={20} />}
          label="RANKED BATTLES"
          value={String(session?.rankedGames ?? 0)}
          detail="Every game is a new lesson"
        />
        <Stat
          icon={<Target size={20} />}
          label="WIN RATE"
          value={
            session?.rankedGames
              ? `${Math.round((session.wins / session.rankedGames) * 100)}%`
              : '—'
          }
          detail={
            session
              ? `${session.wins} wins · ${session.draws} draws · ${session.losses} losses`
              : 'Your story starts with one game'
          }
        />
        <Stat
          icon={<ShieldCheck size={20} />}
          label="FAIR BY DESIGN"
          value="One clock"
          detail="First accepted by server receipt"
        />
      </div>
      <div className="lobby-columns">
        <div>
          <section className="panel section-panel">
            <div className="section-heading">
              <div>
                <span className="eyebrow">KEEP THE MOMENTUM</span>
                <h2>Recent battles</h2>
              </div>
              {session && (
                <Link className="text-link" to="/profile">
                  Your profile <ArrowUpRight size={14} />
                </Link>
              )}
            </div>
            {!session ? (
              <div className="empty-battles">
                <Swords size={29} />
                <h3>Your first rivalry is ahead of you.</h3>
                <p>Sign in, find an opponent, and make your first move.</p>
                <Link className="text-link" to="/register">
                  Create your account <ArrowRight size={14} />
                </Link>
              </div>
            ) : history.isPending ? (
              <Loading />
            ) : history.error ? (
              <ErrorNotice error={history.error} />
            ) : history.data?.length ? (
              <HistoryList matches={history.data.slice(0, 5)} userId={session.id} />
            ) : (
              <div className="empty-battles">
                <Swords size={29} />
                <h3>A fresh page. A fair fight.</h3>
                <p>Your battle history will appear here after your first duel.</p>
              </div>
            )}
          </section>
          <section className="panel invite-panel">
            <div className="section-icon">
              <Users size={21} />
            </div>
            <div>
              <h3>Got a worthy rival?</h3>
              <p>Join a private room with a friend’s invite code.</p>
            </div>
            <form
              onSubmit={(event) => {
                event.preventDefault()
                let code = invite.trim()
                try {
                  code = new URL(code).pathname.split('/').filter(Boolean).at(-1) ?? code
                } catch {
                  /* A raw code is also accepted. */
                }
                navigate(`/invite/${encodeURIComponent(code)}`)
              }}
            >
              <label className="sr-only" htmlFor="invite-code">
                Invite code or link
              </label>
              <input
                id="invite-code"
                required
                value={invite}
                onChange={(event) => setInvite(event.target.value)}
                placeholder="Paste invite code or link"
              />
              <button className="icon-button" aria-label="Join private room">
                <ArrowRight size={19} />
              </button>
            </form>
          </section>
        </div>
        <div>
          <section className="panel section-panel">
            <div className="section-heading">
              <div>
                <span className="eyebrow">THE CLIMB</span>
                <h2>Top players</h2>
              </div>
              <Link to="/leaderboard" className="icon-button" aria-label="View full leaderboard">
                <ArrowUpRight size={18} />
              </Link>
            </div>
            {leaderboard.isPending ? (
              <Loading />
            ) : leaderboard.error ? (
              <ErrorNotice error={leaderboard.error} />
            ) : (
              leaderboard.data?.slice(0, 4).map((player, index) => (
                <div className="leaderboard-mini" key={player.id}>
                  <span className={`rank-number rank-${index}`}>
                    {String(index + 1).padStart(2, '0')}
                  </span>
                  <Avatar name={player.username} small />
                  <span className="player-mini">
                    {player.username}
                    <small>{player.provisional ? 'Provisional' : 'Established'}</small>
                  </span>
                  <strong className="mono">{displayRating(player.rating)}</strong>
                </div>
              ))
            )}
            {!leaderboard.data?.length && !leaderboard.isPending && (
              <p className="muted">The first place is waiting for its first player.</p>
            )}
          </section>
          <section className="fair-play-note">
            <ShieldCheck size={19} />
            <div>
              <h3>A better kind of competition.</h3>
              <p>
                The same challenge and deadline. Private solutions. Ratings applied once. Your code
                deserves a fair game.
              </p>
            </div>
          </section>
        </div>
      </div>
      <section className="challenge-section">
        <div className="section-heading">
          <div>
            <span className="eyebrow">GET YOUR MIND MOVING</span>
            <h2>Meet the challenges</h2>
          </div>
          <Link to="/problems" className="text-link">
            Browse library <ArrowRight size={14} />
          </Link>
        </div>
        <ErrorNotice error={problems.error} />
        <div className="challenge-grid">
          {problems.data?.map((problem, index) => (
            <Link
              className="problem-card panel"
              to={`/problems/${problem.slug}`}
              key={problem.slug}
            >
              <div className="problem-card-top">
                <span className="problem-number">0{index + 1}</span>
                <Difficulty value={problem.difficulty} />
              </div>
              <h3>{problem.title}</h3>
              <div className="tag-row">
                {problem.tags.map((tag) => (
                  <span key={tag}>{tag}</span>
                ))}
              </div>
              <div className="problem-card-footer">
                <span>
                  <Clock3 size={13} />
                  {problem.durationSeconds / 60} min duel
                </span>
                <ArrowUpRight size={18} />
              </div>
            </Link>
          ))}
        </div>
      </section>
    </>
  )
}

function Brackets() {
  return <span className="brackets-art">{'{ }'}</span>
}
function Stat({
  icon,
  label,
  value,
  detail,
}: {
  icon: React.ReactNode
  label: string
  value: string
  detail: string
}) {
  return (
    <section className="stat panel">
      <div className="stat-label">
        {label}
        {icon}
      </div>
      <strong>{value}</strong>
      <span>{detail}</span>
    </section>
  )
}

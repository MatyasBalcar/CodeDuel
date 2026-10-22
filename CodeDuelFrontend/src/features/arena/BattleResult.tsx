import { displayRating } from '../../api/ratings'
import { useMutation, useQuery } from '@tanstack/react-query'
import { ArrowRight, Clock3, Flag, Swords, Trophy, Users } from 'lucide-react'
import { Link, useNavigate } from 'react-router-dom'
import { api } from '../../api/client'
import type { Battle, Profile } from '../../api/types'
import { Avatar, ErrorNotice, Loading } from '../../components/Feedback'
import { VerdictLabel } from './VerdictLabel'

export function BattleResult({ battle, user }: { battle: Battle; user: Profile }) {
  const navigate = useNavigate()
  const timeline = useQuery({
    queryKey: ['timeline', battle.id],
    queryFn: () => api.timeline(battle.id),
  })
  const room = useMutation({
    mutationFn: () => api.rematch(battle.id),
    onSuccess: ({ battleId, inviteCode }) => {
      if (inviteCode) sessionStorage.setItem(`invite:${battleId}`, inviteCode)
      navigate(`/arena/${battleId}`)
    },
  })
  const proposal = useQuery({
    queryKey: ['rematch', battle.id],
    queryFn: () => api.rematchState(battle.id),
    refetchInterval: 2000,
  })
  const cancelled = battle.status === 'CANCELLED'
  const won = battle.winnerId === user.id
  const draw = !battle.winnerId && !cancelled
  const self = battle.players.find((p) => p.userId === user.id)
  const reasons: Record<string, string> = {
    FIRST_ACCEPTED: 'The earliest server-received, fully accepted solution wins.',
    DEADLINE_DRAW: 'The deadline passed with no accepted solution. Both players draw.',
    FORFEIT: 'The battle ended by an explicit forfeit.',
    JUDGE_UNAVAILABLE: 'The judge could not provide a reliable result. No rating was changed.',
    READY_TIMEOUT: 'The ready check expired. No rating was changed.',
    PLAYER_LEFT: 'A player left before the shared start. No rating was changed.',
  }
  const eventNames: Record<string, string> = {
    ROOM_CREATED: 'Private room opened',
    MATCH_FOUND: 'Opponents matched',
    PLAYER_READY: 'Ready confirmed',
    BATTLE_STARTED: 'The shared clock started',
    SUBMISSION_RECEIVED: 'Official solution received',
    SUBMISSION_JUDGED: 'Submission judged',
    JUDGING_STARTED: 'Waiting for relevant verdicts',
    PLAYER_FORFEITED: 'Battle forfeited',
    BATTLE_FINISHED: 'Final result recorded',
  }
  return (
    <>
      <section className={`panel result-card ${won ? 'won' : ''}`}>
        <div className="result-icon">
          {cancelled ? <Flag size={35} /> : draw ? <Users size={35} /> : <Trophy size={35} />}
        </div>
        <span className="eyebrow">{cancelled ? 'BATTLE CANCELLED' : 'FINAL RESULT'}</span>
        <h1>
          {cancelled
            ? 'Another time, another duel.'
            : draw
              ? 'A worthy draw.'
              : won
                ? 'That’s your win.'
                : 'A lesson for the next one.'}
        </h1>
        <p>{reasons[battle.finalizationReason ?? ''] ?? 'This battle has ended.'}</p>
        <div className="result-players">
          {battle.players.map((player) => (
            <div className={battle.winnerId === player.userId ? 'winner' : ''} key={player.userId}>
              <Avatar name={player.username} />
              <strong>
                {player.username}
                {battle.winnerId === player.userId && <Trophy size={15} />}
              </strong>
              <span className="mono">
                {displayRating(player.oldRating ?? player.rating)}
                {player.newRating !== null && (
                  <>
                    {' '}
                    <ArrowRight size={12} /> {displayRating(player.newRating)}
                  </>
                )}
              </span>
              {player.delta !== null && (
                <span className={`result-delta ${player.delta >= 0 ? 'positive' : ''}`}>
                  {player.delta >= 0 ? '+' : ''}
                  {displayRating(player.delta)} Elo
                </span>
              )}
            </div>
          ))}
        </div>
        {battle.mode === 'PRIVATE' && (
          <span className="outline-chip">UNRANKED · NO RATING CHANGE</span>
        )}
        {self?.delta != null && (
          <p className="result-rating-note">Rating history saved. This result is safe to reload.</p>
        )}
        <ErrorNotice error={room.error} />
        <div className="result-actions">
          <Link className="button primary" to="/">
            Back to lobby <ArrowRight size={16} />
          </Link>
          <button
            className="button secondary"
            onClick={() => room.mutate()}
            disabled={room.isPending}
          >
            <Swords size={16} />
            {proposal.data?.proposedBy && proposal.data.proposedBy !== user.id
              ? 'Accept rematch'
              : 'Private rematch'}
          </button>
        </div>
      </section>
      <section className="panel section-panel replay-panel">
        <div className="section-heading">
          <div>
            <span className="eyebrow">HOW IT PLAYED OUT</span>
            <h2>Battle replay</h2>
          </div>
          <span className="outline-chip">
            <Clock3 size={13} /> SERVER TIMELINE
          </span>
        </div>
        <ErrorNotice error={timeline.error} />
        {timeline.isPending ? (
          <Loading />
        ) : (
          <ol className="timeline">
            {timeline.data?.map((event) => {
              const player = battle.players.find((p) => p.userId === event.payload.userId)
              return (
                <li key={event.sequence}>
                  <span className="timeline-node" />
                  <time className="mono">{new Date(event.occurredAt).toLocaleTimeString()}</time>
                  <span>
                    <strong>{eventNames[event.type] ?? event.type}</strong>
                    {player && (
                      <small>
                        {player.username}
                        {event.payload.status
                          ? ` · ${event.payload.status.toLowerCase().replaceAll('_', ' ')}`
                          : ''}
                      </small>
                    )}
                  </span>
                  <span className="timeline-sequence mono">#{event.sequence}</span>
                </li>
              )
            })}
          </ol>
        )}
      </section>
      {battle.submissions.length > 0 && (
        <section className="panel section-panel">
          <div className="section-heading">
            <h2>Your submissions</h2>
            <span className="muted">Private to you</span>
          </div>
          <div className="submission-list">
            {battle.submissions.map((submission) => (
              <div className="submission-row" key={submission.id}>
                <span className="mono">#{submission.sequence}</span>
                <span>{submission.official ? 'Official' : 'Examples'}</span>
                <VerdictLabel verdict={submission.verdict} />
                <time>{new Date(submission.receivedAt).toLocaleTimeString()}</time>
              </div>
            ))}
          </div>
        </section>
      )}
    </>
  )
}

import { displayRating } from '../../api/ratings'
import { useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { Check, CheckCheck, Clock3, Copy, ShieldCheck, Swords, Users } from 'lucide-react'
import { api } from '../../api/client'
import type { Battle, Profile } from '../../api/types'
import { Avatar, ErrorNotice } from '../../components/Feedback'
import { formatClock, remainingSeconds } from '../clock'

export function ReadyRoom({ battle, user, now }: { battle: Battle; user: Profile; now: number }) {
  const queries = useQueryClient()
  const [copied, setCopied] = useState(false)
  const ready = useMutation({
    mutationFn: () => api.ready(battle.id),
    onSuccess: () => queries.invalidateQueries({ queryKey: ['battle', battle.id] }),
  })
  const leave = useMutation({
    mutationFn: () => api.forfeit(battle.id),
    onSuccess: () => queries.invalidateQueries({ queryKey: ['battle', battle.id] }),
  })
  const self = battle.players.find((p) => p.userId === user.id)
  const starting = battle.startsAt !== null
  const seconds = remainingSeconds(starting ? battle.startsAt : battle.readyDeadline, now)
  const inviteCode = sessionStorage.getItem(`invite:${battle.id}`)
  const inviteUrl = inviteCode ? `${window.location.origin}/invite/${inviteCode}` : ''
  return (
    <section className="panel ready-room">
      <h1>
        {battle.status === 'WAITING'
          ? 'Waiting for an opponent'
          : starting
            ? 'Starting soon'
            : 'Ready to start?'}
      </h1>
      <p>
        {battle.status === 'WAITING'
          ? 'Share your one-use invite. This room expires in five minutes.'
          : starting
            ? 'The challenge opens for both players at the shared server start.'
            : 'Both players must confirm before the server deadline.'}
      </p>
      <div className="ready-players">
        {[1, 2].map((slot) => {
          const player = battle.players.find((p) => p.slot === slot)
          return (
            <div className="ready-player" key={slot}>
              {player ? (
                <>
                  <Avatar name={player.username} />
                  <strong>
                    {player.username}
                    {player.userId === user.id && <small>YOU</small>}
                  </strong>
                  <span className="mono">{displayRating(player.rating)} Elo</span>
                  <span className={`player-ready ${player.ready ? 'yes' : ''}`}>
                    {player.ready ? <CheckCheck size={15} /> : <Clock3 size={15} />}
                    {player.ready ? 'Ready' : 'Getting ready'}
                  </span>
                </>
              ) : (
                <>
                  <span className="avatar waiting-avatar">
                    <Users size={28} />
                  </span>
                  <strong>Opponent</strong>
                  <span>Waiting to join…</span>
                </>
              )}
            </div>
          )
        })}
        <span className="ready-versus">VS</span>
      </div>
      {inviteUrl && battle.status === 'WAITING' && (
        <div className="invite-share">
          <label htmlFor="share-invite">PRIVATE INVITE LINK</label>
          <div>
            <input
              id="share-invite"
              readOnly
              value={inviteUrl}
              onFocus={(event) => event.target.select()}
            />
            <button
              className="button secondary small"
              onClick={() => {
                void navigator.clipboard
                  .writeText(inviteUrl)
                  .then(() => setCopied(true))
                  .catch(() => setCopied(false))
              }}
            >
              {copied ? <Check size={15} /> : <Copy size={15} />}
              {copied ? 'Copied' : 'Copy link'}
            </button>
          </div>
        </div>
      )}
      <div className="ready-countdown mono" role="timer">
        {formatClock(seconds)}
      </div>
      <ErrorNotice error={ready.error || leave.error} />
      <div className="ready-actions">
        {battle.status === 'READY_CHECK' && (
          <button
            className="button primary"
            onClick={() => ready.mutate()}
            disabled={ready.isPending || self?.ready || seconds === 0}
          >
            {self?.ready ? (
              <>
                <Check size={17} />
                Waiting for opponent
              </>
            ) : (
              <>
                <Swords size={17} />
                I’m ready
              </>
            )}
          </button>
        )}
        {!starting && (
          <button
            className="button secondary"
            onClick={() => leave.mutate()}
            disabled={leave.isPending}
          >
            Leave room
          </button>
        )}
      </div>
      <p className="ready-fairness">
        <ShieldCheck size={15} />
        {battle.mode === 'PRIVATE'
          ? 'Private games are unranked.'
          : 'Ratings change only after the final result.'}{' '}
        Your solution stays private.
      </p>
    </section>
  )
}

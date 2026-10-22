import { useEffect, useState } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { ArrowLeft, Radio, Swords } from 'lucide-react'
import { Link, Navigate, useParams } from 'react-router-dom'
import { api } from '../api/client'
import { sessionKey, useSession } from '../api/hooks'
import type { Battle } from '../api/types'
import { Loading, Unavailable } from '../components/Feedback'
import { ReadyRoom } from './arena/ReadyRoom'
import { SolutionWorkspace } from './arena/SolutionWorkspace'
import { BattleResult } from './arena/BattleResult'

const terminal = (battle?: Battle) =>
  battle?.status === 'FINISHED' || battle?.status === 'CANCELLED'

function useServerNow(serverTime: string | undefined) {
  const [now, setNow] = useState(() => (serverTime ? Date.parse(serverTime) : 0))
  useEffect(() => {
    const server = serverTime ? Date.parse(serverTime) : 0
    const local = performance.now()
    const interval = setInterval(() => setNow(server + performance.now() - local), 100)
    return () => clearInterval(interval)
  }, [serverTime])
  return now
}

export function ArenaPage() {
  const { id = '' } = useParams()
  const session = useSession()
  const queries = useQueryClient()
  const [live, setLive] = useState(false)
  const battle = useQuery({
    queryKey: ['battle', id],
    queryFn: () => api.battle(id),
    enabled: !!session.data,
    refetchInterval: (query) =>
      terminal(query.state.data) &&
      !query.state.data?.submissions.some((s) => s.verdict === 'PENDING')
        ? false
        : 2000,
  })
  const now = useServerNow(battle.data?.serverTime)
  const finished = terminal(battle.data)
  const userId = session.data?.id
  useEffect(() => {
    if (!userId || !id || finished) return
    const source = new EventSource(`/api/v1/battles/${id}/events`)
    source.onopen = () => setLive(true)
    source.onerror = () => setLive(false)
    source.addEventListener('battle', () => {
      void queries.invalidateQueries({ queryKey: ['battle', id] })
    })
    return () => source.close()
  }, [id, userId, finished, queries])
  useEffect(() => {
    if (!finished) return
    for (const queryKey of [sessionKey, ['queue'], ['history'], ['leaderboard']])
      void queries.invalidateQueries({ queryKey })
  }, [finished, queries])

  if (session.isPending) return <Loading />
  if (!session.data) return <Navigate to="/login" replace />
  if (battle.isPending) return <Loading label="Rejoining the arena…" />
  if (battle.error || !battle.data) return <Unavailable error={battle.error} />
  const snapshot = battle.data
  const beforeStart = snapshot.startsAt !== null && now < Date.parse(snapshot.startsAt)
  return (
    <>
      <div className="arena-heading">
        <Link to="/" className="text-link">
          <ArrowLeft size={15} />
          Lobby
        </Link>
        <span className="arena-mode">
          <Swords size={16} />
          {snapshot.mode === 'RANKED' ? 'Ranked speed duel' : 'Private duel'}
          <span className="outline-chip">JAVA 21</span>
        </span>
        <span className={`connection ${live && !finished ? 'online' : ''}`}>
          <Radio size={13} />
          {finished ? 'Match complete' : live ? 'Live events' : 'Syncing snapshots'}
        </span>
      </div>
      {finished ? (
        <BattleResult battle={snapshot} user={session.data} />
      ) : !snapshot.problem || beforeStart ? (
        <ReadyRoom battle={snapshot} user={session.data} now={now} />
      ) : (
        <SolutionWorkspace
          key={`${snapshot.id}:${snapshot.problem.versionId}`}
          battle={snapshot}
          problem={snapshot.problem}
          user={session.data}
          now={now}
        />
      )}
    </>
  )
}

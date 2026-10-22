import { useMutation, useQuery } from '@tanstack/react-query'
import { Link, Navigate, useNavigate, useParams } from 'react-router-dom'
import { Swords } from 'lucide-react'
import { api } from '../api/client'
import { useSession } from '../api/hooks'
import { ErrorNotice, Loading } from '../components/Feedback'

export function InvitePage() {
  const { code = '' } = useParams()
  const session = useSession()
  const navigate = useNavigate()
  const active = useQuery({
    queryKey: ['queue', session.data?.id],
    queryFn: api.queue,
    enabled: !!session.data,
  })
  const join = useMutation({
    mutationFn: () => api.join(code),
    onSuccess: ({ battleId }) => navigate(`/arena/${battleId}`, { replace: true }),
  })
  if (session.isPending) return <Loading />
  if (!session.data)
    return <Navigate to={`/login?next=${encodeURIComponent(`/invite/${code}`)}`} replace />
  return (
    <section className="panel join-card">
      <div className="section-icon">
        <Swords size={26} />
      </div>
      <span className="eyebrow">A FRIENDLY RIVALRY</span>
      <h1>You’ve been challenged.</h1>
      <p>
        Same problem. Same clock. A private, unranked duel.
        <br />
        Your rating stays yours.
      </p>
      <ErrorNotice error={join.error} />
      <button className="button primary" onClick={() => join.mutate()} disabled={join.isPending}>
        {join.isPending ? 'Joining…' : 'Join private duel'}
      </button>
      {active.data?.battleId && (
        <Link className="text-link" to={`/arena/${active.data.battleId}`}>
          Resume your active battle
        </Link>
      )}
    </section>
  )
}

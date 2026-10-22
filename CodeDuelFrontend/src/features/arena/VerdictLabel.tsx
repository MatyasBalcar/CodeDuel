import { CheckCheck, Clock3 } from 'lucide-react'
import type { Verdict } from '../../api/types'

export function VerdictLabel({ verdict }: { verdict: Verdict }) {
  return (
    <span className={`verdict ${verdict.toLowerCase()}`}>
      {verdict === 'PENDING' ? (
        <Clock3 size={13} />
      ) : verdict === 'ACCEPTED' ? (
        <CheckCheck size={14} />
      ) : null}
      {verdict.toLowerCase().replaceAll('_', ' ')}
    </span>
  )
}

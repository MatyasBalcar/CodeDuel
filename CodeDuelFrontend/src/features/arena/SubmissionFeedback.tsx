import { useQuery } from '@tanstack/react-query'
import { api } from '../../api/client'
import { ErrorNotice, Loading } from '../../components/Feedback'
import { VerdictLabel } from './VerdictLabel'

export function SubmissionFeedback({
  id,
  functionBased = false,
}: {
  id: string
  functionBased?: boolean
}) {
  const detail = useQuery({
    queryKey: ['submission', id],
    queryFn: () => api.submission(id),
    refetchInterval: (query) => (query.state.data?.verdict === 'PENDING' ? 1000 : false),
  })
  if (detail.isPending) return <Loading label="Loading judge feedback…" />
  if (!detail.data) return <ErrorNotice error={detail.error} />
  const result = detail.data
  return (
    <div className="submission-feedback">
      <VerdictLabel verdict={result.verdict} />
      {result.compilerDiagnostic && (
        <>
          <h4>Compiler feedback</h4>
          <pre>{result.compilerDiagnostic}</pre>
        </>
      )}
      {result.publicResults.map((example, index) => (
        <div key={example.ordinal}>
          <strong>Public example {index + 1}</strong>{' '}
          {example.verdict ? <VerdictLabel verdict={example.verdict} /> : <span>Waiting</span>}
          {example.stdout && (
            <>
              <span>{functionBased ? 'Returned value' : 'Output'}</span>
              <pre
                aria-label={`Example ${index + 1} ${functionBased ? 'returned value' : 'output'}`}
              >
                {example.stdout}
              </pre>
            </>
          )}
        </div>
      ))}
      <p className="muted">Hidden inputs, expected answers and execution output are private.</p>
    </div>
  )
}

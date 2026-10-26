import { lazy, Suspense, useEffect, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link, useParams } from 'react-router-dom'
import { Play, Send } from 'lucide-react'
import { api } from '../api/client'
import { useSession, useSystem } from '../api/hooks'
import type { Problem, Profile } from '../api/types'
import { ErrorNotice, Loading, Unavailable } from '../components/Feedback'
import { ProblemStatement } from './ProblemsPage'
import { draftKey, loadDraft, saveDraft } from './drafts'
import { SubmissionFeedback } from './arena/SubmissionFeedback'
import { VerdictLabel } from './arena/VerdictLabel'
const CodeEditor = lazy(() => import('./CodeEditor'))

export function PracticePage() {
  const { slug = '' } = useParams()
  const session = useSession()
  const problem = useQuery({ queryKey: ['problem', slug], queryFn: () => api.problem(slug) })
  if (session.isPending || problem.isPending) return <Loading />
  if (!problem.data) return <Unavailable error={problem.error} />
  if (!session.data)
    return (
      <section className="panel empty-state">
        <h1>Sign in to practice</h1>
        <p>Sign in to run examples and submit a Java solution.</p>
        <Link to="/login" className="button primary">
          Sign in
        </Link>
      </section>
    )
  return (
    <PracticeWorkspace
      key={`${session.data.id}:${problem.data.versionId}`}
      problem={problem.data}
      user={session.data}
    />
  )
}

function PracticeWorkspace({ problem, user }: { problem: Problem; user: Profile }) {
  const system = useSystem()
  const fixtures = useQuery({
    queryKey: ['practice-fixtures', problem.slug],
    queryFn: () => api.practiceFixtures(problem.slug),
    enabled: system.data?.judgeMode === 'FIXTURE',
  })
  const queries = useQueryClient()
  const key = draftKey(user.id, `practice:${problem.slug}`, problem.versionId)
  const [code, setCode] = useState(() => loadDraft(key, problem.starterCode))
  const [saved, setSaved] = useState(true)
  useEffect(() => {
    const timer = setTimeout(() => setSaved(saveDraft(key, code)), 400)
    return () => clearTimeout(timer)
  }, [key, code])
  const history = useQuery({
    queryKey: ['practice', problem.versionId, user.id],
    queryFn: () => api.practiceHistory(problem.slug),
    refetchInterval: (query) =>
      query.state.data?.some((entry) => entry.verdict === 'PENDING') ? 1500 : false,
  })
  const submit = useMutation({
    mutationFn: (examples: boolean) => api.practice(problem.slug, code, examples),
    onSuccess: () =>
      queries.invalidateQueries({ queryKey: ['practice', problem.versionId, user.id] }),
  })
  const send = (examples: boolean) => {
    if (code.trim() && !submit.isPending) submit.mutate(examples)
  }
  return (
    <>
      <div className="page-heading">
        <div>
          <h1>Practice</h1>
          <p>No time limit or rating change.</p>
        </div>
        <Link to="/problems" className="text-link">
          All challenges
        </Link>
      </div>
      <div className="practice-grid">
        <section className="panel">
          <ProblemStatement problem={problem} />
        </section>
        <section className="panel practice-editor">
          <div className="editor-toolbar">
            <strong>{problem.function ? 'Solution.java' : 'Main.java'}</strong>
            <span className="editor-language">Java 21</span>
          </div>
          {fixtures.data && (
            <div className="fixture-toolbar">
              <label htmlFor="practice-sample">Demo program</label>
              <select
                id="practice-sample"
                defaultValue=""
                onChange={(event) => {
                  const programs = fixtures.data
                  if (!programs) return
                  if (event.target.value === 'accepted') setCode(programs.acceptedSource)
                  else if (event.target.value === 'wrong') setCode(programs.wrongSource)
                  else setCode(problem.starterCode)
                }}
              >
                <option value="">Starter template</option>
                <option value="wrong">Wrong-answer sample</option>
                <option value="accepted">Accepted sample</option>
              </select>
              <span>Exact fixtures only</span>
            </div>
          )}
          <div className="editor-container">
            <Suspense fallback={<Loading label="Opening the editor…" />}>
              <CodeEditor
                readOnly={false}
                code={code}
                onChange={setCode}
                onRun={() => send(true)}
                onSubmit={() => send(false)}
              />
            </Suspense>
          </div>
          <div className="editor-status">
            <span>{saved ? 'Draft saved locally · 7 days' : 'Draft storage unavailable'}</span>
          </div>
          <div className="practice-actions">
            <button
              className="button secondary small"
              onClick={() => send(true)}
              disabled={submit.isPending}
            >
              <Play size={14} />
              Run examples
            </button>
            <button
              className="button primary small"
              onClick={() => send(false)}
              disabled={submit.isPending}
            >
              <Send size={14} />
              Submit solution
            </button>
          </div>
          <ErrorNotice error={submit.error} />
        </section>
      </div>
      <section className="panel section-panel">
        <h2>Your practice attempts</h2>
        <ErrorNotice error={history.error} />
        {history.data?.length ? (
          history.data.map((entry) => (
            <details key={entry.id}>
              <summary className="submission-row">
                <span>
                  {entry.purpose === 'PUBLIC_EXAMPLES' ? 'Public examples' : 'Full submission'}
                </span>
                <VerdictLabel verdict={entry.verdict} />
                <time>{new Date(entry.receivedAt).toLocaleString()}</time>
              </summary>
              <SubmissionFeedback id={entry.id} functionBased={!!problem.function} />
            </details>
          ))
        ) : (
          <p className="muted">Your submissions will appear here. Hidden cases stay private.</p>
        )}
      </section>
    </>
  )
}

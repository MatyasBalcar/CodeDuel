import { displayRating } from '../../api/ratings'
import { lazy, Suspense, useEffect, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Clock3, FileCode2, Flag, Play, RotateCcw, Send } from 'lucide-react'
import { api } from '../../api/client'
import { useSystem } from '../../api/hooks'
import type { Battle, Problem, Profile } from '../../api/types'
import { Avatar, ErrorNotice, Loading } from '../../components/Feedback'
import { ProblemExample, ProblemStatement } from '../ProblemsPage'
import { formatClock, remainingSeconds } from '../clock'
import { draftKey, loadDraft, saveDraft } from '../drafts'
import { ForfeitDialog } from './ForfeitDialog'
import { VerdictLabel } from './VerdictLabel'
import { SubmissionFeedback } from './SubmissionFeedback'
const CodeEditor = lazy(() => import('../CodeEditor'))

export function SolutionWorkspace({
  battle,
  problem,
  user,
  now,
}: {
  battle: Battle
  problem: Problem
  user: Profile
  now: number
}) {
  const system = useSystem()
  const queries = useQueryClient()
  const key = draftKey(user.id, battle.id, problem.versionId)
  const [code, setCode] = useState(() => loadDraft(key, problem.starterCode))
  const [saved, setSaved] = useState(true)
  const [pane, setPane] = useState(43)
  const [tab, setTab] = useState<'examples' | 'submissions'>('examples')
  const [sample, setSample] = useState('')
  const [forfeitOpen, setForfeitOpen] = useState(false)
  const seconds = remainingSeconds(battle.endsAt, now)
  const expired = seconds === 0
  const fixtures = useQuery({
    queryKey: ['fixtures', battle.id],
    queryFn: () => api.fixtures(battle.id),
    enabled: system.data?.judgeMode === 'FIXTURE',
    staleTime: Infinity,
  })
  const submit = useMutation({
    mutationFn: ({ source, official }: { source: string; official: boolean }) =>
      api.submit(battle.id, source, official),
    onSuccess: () => {
      setTab('submissions')
      void queries.invalidateQueries({ queryKey: ['battle', battle.id] })
    },
  })
  const forfeit = useMutation({
    mutationFn: () => api.forfeit(battle.id),
    onSuccess: () => queries.invalidateQueries({ queryKey: ['battle', battle.id] }),
  })
  useEffect(() => {
    const timeout = setTimeout(() => setSaved(saveDraft(key, code)), 400)
    return () => clearTimeout(timeout)
  }, [key, code])
  const send = (official: boolean) => {
    if (!expired && !submit.isPending && code.trim()) submit.mutate({ source: code, official })
  }
  const opponent = battle.players.find((p) => p.userId !== user.id)

  return (
    <>
      <div className="arena-scoreboard">
        <div className="scoreboard-player">
          <Avatar name={user.username} small />
          <span>
            <strong>
              {user.username}
              <small>YOU</small>
            </strong>
            <span className="mono">{displayRating(user.rating)} Elo</span>
          </span>
        </div>
        <div className={`battle-clock ${seconds < 60 ? 'urgent' : ''}`}>
          <span>{battle.status === 'JUDGING' ? 'AWAITING FINAL VERDICTS' : 'TIME REMAINING'}</span>
          <strong className="mono" role="timer">
            {formatClock(seconds)}
          </strong>
        </div>
        <div className="scoreboard-player opponent">
          <span>
            <strong>{opponent?.username}</strong>
            <span className="mono">{displayRating(opponent?.rating ?? 1200)} Elo</span>
          </span>
          <Avatar name={opponent?.username ?? '?'} small />
        </div>
      </div>
      {battle.status === 'JUDGING' && (
        <div className="judging-notice" role="status">
          <Clock3 size={18} />
          {expired
            ? 'Time is up. Every in-time submission can finish judging.'
            : 'An accepted submission is being reviewed against earlier receipts.'}{' '}
          The result is final only when the server confirms it.
        </div>
      )}
      <div className="pane-control">
        <span>
          <FileCode2 size={14} /> PROBLEM & WORKSPACE
        </span>
        <label>
          Split
          <input
            type="range"
            min="30"
            max="62"
            value={pane}
            onChange={(event) => setPane(Number(event.target.value))}
            aria-label="Problem pane width"
          />
        </label>
        <button className="text-link danger" onClick={() => setForfeitOpen(true)}>
          <Flag size={13} />
          Forfeit
        </button>
      </div>
      <div className="arena-workspace" style={{ gridTemplateColumns: `${pane}% 1fr` }}>
        <div className="arena-problem">
          <div className="pane-header">
            THE CHALLENGE <span>v{problem.version} · immutable</span>
          </div>
          <ProblemStatement problem={problem} />
        </div>
        <div className="arena-code">
          <div className="editor-toolbar">
            <span>
              <FileCode2 size={15} />
              {problem.function ? 'Solution.java' : 'Main.java'}
            </span>
            <span className="editor-language">Java 21</span>
            <button
              className="icon-button"
              onClick={() => setCode(problem.starterCode)}
              aria-label="Reset to starter code"
            >
              <RotateCcw size={14} />
            </button>
          </div>
          {system.data?.judgeMode === 'FIXTURE' && (
            <div className="fixture-toolbar">
              <label htmlFor="demo-program">Demo program</label>
              <select
                id="demo-program"
                value={sample}
                onChange={(event) => {
                  const value = event.target.value
                  setSample(value)
                  const programs = fixtures.data
                  if (!programs) return
                  if (value === 'accepted') setCode(programs.acceptedSource)
                  else if (value === 'wrong') setCode(programs.wrongSource)
                  else setCode(problem.starterCode)
                }}
                disabled={!fixtures.data || expired}
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
                code={code}
                onChange={setCode}
                readOnly={expired}
                onSubmit={() => send(true)}
                onRun={() => send(false)}
              />
            </Suspense>
          </div>
          <div className="editor-status">
            <span className="mono">UTF-8 · Java 21</span>
            <span>{saved ? 'Draft saved locally · 7 days' : 'Draft storage unavailable'}</span>
          </div>
        </div>
      </div>
      <div className="arena-bottom panel">
        <div className="arena-bottom-bar">
          <div className="arena-tabs" role="tablist" aria-label="Judge feedback">
            <button
              id="examples-tab"
              role="tab"
              aria-selected={tab === 'examples'}
              aria-controls="judge-feedback"
              className={tab === 'examples' ? 'selected' : ''}
              onClick={() => setTab('examples')}
            >
              Public examples <span>{problem.examples.length}</span>
            </button>
            <button
              id="submissions-tab"
              role="tab"
              aria-selected={tab === 'submissions'}
              aria-controls="judge-feedback"
              className={tab === 'submissions' ? 'selected' : ''}
              onClick={() => setTab('submissions')}
            >
              Your submissions <span>{battle.submissions.length}</span>
            </button>
          </div>
          <div className="submission-actions">
            <button
              className="button secondary small"
              onClick={() => send(false)}
              disabled={expired || submit.isPending}
            >
              <Play size={14} />
              Run examples
            </button>
            <button
              className="button primary small"
              onClick={() => send(true)}
              disabled={expired || submit.isPending}
            >
              <Send size={14} />
              {submit.isPending ? 'Sending…' : 'Submit solution'}
            </button>
          </div>
        </div>
        <ErrorNotice error={submit.error || fixtures.error} />
        <div
          id="judge-feedback"
          role="tabpanel"
          aria-labelledby={tab === 'examples' ? 'examples-tab' : 'submissions-tab'}
          className="feedback-panel"
        >
          {tab === 'examples' ? (
            <div className="example-tabs-content">
              {problem.examples.map((example, i) => (
                <div key={i}>
                  <strong>Example {i + 1}</strong>
                  <ProblemExample example={example} functionBased={!!problem.function} />
                </div>
              ))}
            </div>
          ) : battle.submissions.length ? (
            <div className="submission-list">
              {battle.submissions.map((submission) => (
                <details key={submission.id}>
                  <summary className="submission-row">
                    <span className="mono">#{submission.sequence}</span>
                    <span>{submission.official ? 'Official submission' : 'Public examples'}</span>
                    <VerdictLabel verdict={submission.verdict} />
                    <time>{new Date(submission.receivedAt).toLocaleTimeString()}</time>
                  </summary>
                  <SubmissionFeedback id={submission.id} functionBased={!!problem.function} />
                </details>
              ))}
            </div>
          ) : (
            <p className="muted">
              Run the public examples or submit your solution to the full test suite.
            </p>
          )}
        </div>
        <div className="shortcut-note">
          ⌘ / Ctrl + Enter to submit <span>·</span> ⌘ / Ctrl + Shift + Enter to run examples{' '}
          <span>·</span> Hidden tests stay on the server.
        </div>
      </div>
      {forfeitOpen && (
        <ForfeitDialog
          onClose={() => setForfeitOpen(false)}
          onConfirm={() => forfeit.mutate()}
          pending={forfeit.isPending}
          error={forfeit.error}
        />
      )}
    </>
  )
}

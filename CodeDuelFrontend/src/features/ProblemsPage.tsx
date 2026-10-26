import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { ArrowLeft, ArrowRight, ArrowUpRight, Clock3, Search } from 'lucide-react'
import { Link, useParams } from 'react-router-dom'
import { api } from '../api/client'
import type { Problem } from '../api/types'
import { Difficulty, ErrorNotice, Loading, Unavailable } from '../components/Feedback'

export function ProblemsPage() {
  const [search, setSearch] = useState('')
  const problems = useQuery({ queryKey: ['problems'], queryFn: api.problems })
  const filtered = problems.data?.filter((p) =>
    `${p.title} ${p.tags.join(' ')}`.toLowerCase().includes(search.toLowerCase()),
  )
  return (
    <>
      <div className="page-heading">
        <div>
          <h1>Challenges</h1>
          <p>Find a Java problem to practice.</p>
        </div>
        <span className="outline-chip">{problems.data?.length ?? '—'} challenges</span>
      </div>
      <div className="library-toolbar">
        <label className="search-field">
          <Search size={17} />
          <input
            aria-label="Search challenges"
            placeholder="Search titles or topics…"
            value={search}
            onChange={(event) => setSearch(event.target.value)}
          />
        </label>
        <span className="muted">Java 21 · function arguments and return values</span>
      </div>
      <ErrorNotice error={problems.error} />
      {problems.isPending ? (
        <Loading />
      ) : (
        <div className="challenge-grid">
          {filtered?.map((problem, i) => (
            <Link className="problem-card panel" to={`/problems/${problem.slug}`} key={problem.id}>
              <div className="problem-card-top">
                <span className="problem-number">{String(i + 1).padStart(2, '0')}</span>
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
                  {problem.durationSeconds / 60} minute duel
                </span>
                <ArrowUpRight size={18} />
              </div>
            </Link>
          ))}
        </div>
      )}
      {!problems.isPending && filtered?.length === 0 && (
        <div className="panel empty-state">
          <h2>No matching challenges</h2>
          <p>Try a title or a topic such as “strings”.</p>
        </div>
      )}
      <div className="content-note">
        Every challenge includes examples and hidden tests. Choose a familiar problem or try
        something new.
      </div>
    </>
  )
}

export function ProblemPage() {
  const { slug = '' } = useParams()
  const problem = useQuery({ queryKey: ['problem', slug], queryFn: () => api.problem(slug) })
  if (problem.isPending) return <Loading label="Opening challenge…" />
  if (problem.error || !problem.data) return <Unavailable error={problem.error} />
  return (
    <div className="problem-detail-page">
      <Link className="text-link" to="/problems">
        <ArrowLeft size={15} />
        All challenges
      </Link>
      <section className="panel problem-detail">
        <ProblemStatement problem={problem.data} />
        <div className="problem-detail-action">
          <Link className="button primary" to={`/practice/${slug}`}>
            Practice this challenge
          </Link>
          <p>You can also solve a challenge against another player.</p>
          <Link className="button primary" to="/">
            Find a duel <ArrowRight size={16} />
          </Link>
        </div>
      </section>
    </div>
  )
}

export function ProblemStatement({ problem }: { problem: Problem }) {
  return (
    <article className="problem-statement">
      <h1>{problem.title}</h1>
      <div className="problem-meta">
        <Difficulty value={problem.difficulty} />
        <span>Version {problem.version}</span>
        <span>{problem.timeLimitMs / 1000}s CPU</span>
        <span>{problem.memoryLimitMb} MB</span>
      </div>
      <div className="tag-row">
        {problem.tags.map((tag) => (
          <span key={tag}>{tag}</span>
        ))}
      </div>
      <p className="statement-body">{problem.statement}</p>
      {problem.function && (
        <>
          <h3>Function</h3>
          <pre>{`${problem.function.returnType} solve(${problem.function.parameters.map((parameter) => `${parameter.type} ${parameter.name}`).join(', ')})`}</pre>
        </>
      )}
      <h3>{problem.function ? 'Arguments' : 'Input'}</h3>
      <p>{problem.inputFormat}</p>
      <h3>{problem.function ? 'Return value' : 'Output'}</h3>
      <p>{problem.outputFormat}</p>
      <h3>Examples</h3>
      {problem.examples.map((example, i) => (
        <div className="example" key={i}>
          <div className="example-title">Example {i + 1}</div>
          <ProblemExample example={example} functionBased={!!problem.function} />
        </div>
      ))}
      <h3>Constraints</h3>
      <p>{problem.constraints}</p>
      <div className="contract-note">
        {problem.function ? (
          <>
            Implement <code>solve</code> in <code>public class Solution</code>. The judge passes the
            arguments and checks your return value. Array order and whitespace inside strings
            matter.
          </>
        ) : (
          <>
            Write a <code>public class Main</code> with a <code>main</code> method. Read standard
            input and print your answer. Whitespace between output tokens is ignored.
          </>
        )}
      </div>
    </article>
  )
}

export function ProblemExample({
  example,
  functionBased,
}: {
  example: Problem['examples'][number]
  functionBased: boolean
}) {
  const input = example.input
  const argumentsText =
    functionBased && input && typeof input === 'object' && !Array.isArray(input)
      ? Object.entries(input)
          .map(([name, value]) => `${name} = ${JSON.stringify(value)}`)
          .join('\n')
      : String(input ?? '')
  return (
    <>
      <span>{functionBased ? 'Arguments' : 'Input'}</span>
      <pre>{argumentsText || (functionBased ? '(no arguments)' : '(empty line)')}</pre>
      <span>{functionBased ? 'Returns' : 'Output'}</span>
      <pre>{functionBased ? JSON.stringify(example.output) : String(example.output ?? '')}</pre>
    </>
  )
}

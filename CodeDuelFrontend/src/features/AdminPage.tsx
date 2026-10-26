import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { read, write } from '../api/client'
import { useSession } from '../api/hooks'
import { ErrorNotice, Loading } from '../components/Feedback'
import type { JavaFunction } from '../api/types'

interface TestCase {
  input: string
  output: string
  public: boolean
}
interface Draft {
  slug: string
  title: string
  difficulty: string
  tags: string[]
  statement: string
  constraints: string
  inputFormat: string
  outputFormat: string
  starterCode: string
  referenceSource: string
  timeLimitMs: number
  memoryLimitMb: number
  durationSeconds: number
  tests: TestCase[]
  function?: JavaFunction | null
}
interface Version {
  problemId: string
  versionId: string
  title: string
  slug: string
  version: number
  status: string
  active: boolean
  verification: string | null
}
interface Detail extends Omit<Version, 'title' | 'slug'> {
  content: Draft
}
const empty: Draft = {
  slug: '',
  title: '',
  difficulty: 'EASY',
  tags: ['Arrays'],
  statement: '',
  constraints: '',
  inputFormat: '',
  outputFormat: '',
  starterCode: 'public class Solution {\n  public int solve(int[] nums) {\n    return 0;\n  }\n}\n',
  referenceSource: '',
  timeLimitMs: 2000,
  memoryLimitMb: 256,
  durationSeconds: 900,
  tests: [
    { input: '{"nums":[1,2,3]}', output: '0', public: true },
    { input: '{"nums":[]}', output: '0', public: false },
  ],
  function: { returnType: 'int', parameters: [{ name: 'nums', type: 'int[]' }] },
}

export function AdminPage() {
  const session = useSession()
  const allowed = session.data?.role === 'ADMIN'
  const queries = useQueryClient()
  const [selected, setSelected] = useState<string | null>(null)
  const [creating, setCreating] = useState(false)
  const versions = useQuery({
    queryKey: ['admin-problems'],
    queryFn: () => read<Version[]>('/admin/problems'),
    enabled: allowed,
  })
  const detail = useQuery({
    queryKey: ['admin-version', selected],
    queryFn: () => read<Detail>(`/admin/problems/versions/${selected}`),
    enabled: allowed && !!selected,
    refetchInterval: (query) => (query.state.data?.verification === 'PENDING' ? 1500 : false),
  })
  const action = useMutation({
    mutationFn: ({ path, body, method }: { path: string; body?: unknown; method?: string }) =>
      write<{ versionId?: string } | undefined>(path, method ?? 'POST', body),
    onSuccess: (data) => {
      if (data?.versionId) {
        setSelected(data.versionId)
        setCreating(false)
      }
      void queries.invalidateQueries({ queryKey: ['admin-problems'] })
      void queries.invalidateQueries({ queryKey: ['admin-version'] })
      void queries.invalidateQueries({ queryKey: ['problems'] })
    },
  })
  if (session.isPending) return <Loading />
  if (!allowed)
    return (
      <section className="panel empty-state">
        <h1>Administrator access required</h1>
        <p>Sign in with a provisioned administrator account.</p>
      </section>
    )
  return (
    <>
      <div className="page-heading">
        <div>
          <h1>Administration</h1>
          <p>Create, test, and publish challenges.</p>
        </div>
        <button
          className="button primary"
          onClick={() => {
            setCreating(true)
            setSelected(null)
          }}
        >
          New problem
        </button>
      </div>
      <ErrorNotice error={versions.error || detail.error || action.error} />
      <div className="admin-grid">
        <aside className="panel admin-version-list">
          <h2>Problem versions</h2>
          {versions.data?.map((version) => (
            <button
              key={version.versionId}
              onClick={() => {
                setSelected(version.versionId)
                setCreating(false)
              }}
              className={selected === version.versionId ? 'selected' : ''}
            >
              <strong>{version.title}</strong>
              <span>
                v{version.version} · {version.status.toLowerCase()}
                {!version.active ? ' · retired' : ''}
              </span>
            </button>
          ))}
        </aside>
        <section className="panel section-panel">
          {creating ? (
            <DraftEditor
              key="new"
              initial={empty}
              pending={action.isPending}
              onSave={(body) => action.mutate({ path: '/admin/problems', body })}
            />
          ) : detail.data ? (
            <>
              <div className="section-heading">
                <h2>
                  {detail.data.content.title} · v{detail.data.version}
                </h2>
                <span>{detail.data.status}</span>
              </div>
              <p className="muted">
                Reference check: {detail.data.verification ?? 'Not queued'}. Reviewed versions are
                frozen. Clone a draft to revise them.
              </p>
              <div className="admin-actions">
                <button
                  className="button secondary small"
                  disabled={action.isPending}
                  onClick={() =>
                    action.mutate({ path: `/admin/problems/versions/${selected}/clone` })
                  }
                >
                  Clone new draft
                </button>
                {detail.data.status === 'DRAFT' && (
                  <button
                    className="button primary small"
                    disabled={action.isPending}
                    onClick={() =>
                      action.mutate({ path: `/admin/problems/versions/${selected}/review` })
                    }
                  >
                    Freeze & verify saved draft
                  </button>
                )}
                {detail.data.status === 'REVIEW' && (
                  <button
                    className="button primary small"
                    disabled={action.isPending || detail.data.verification !== 'ACCEPTED'}
                    onClick={() =>
                      action.mutate({ path: `/admin/problems/versions/${selected}/publish` })
                    }
                  >
                    Publish verified version
                  </button>
                )}
                <button
                  className="button secondary small"
                  disabled={action.isPending}
                  onClick={() =>
                    action.mutate({
                      path: `/admin/problems/${detail.data!.problemId}/${detail.data!.active ? 'retire' : 'activate'}`,
                    })
                  }
                >
                  {detail.data.active ? 'Retire problem' : 'Activate problem'}
                </button>
              </div>
              <DraftEditor
                key={`${selected}:${detail.data.status}`}
                initial={detail.data.content}
                frozen={detail.data.status !== 'DRAFT'}
                pending={action.isPending}
                onSave={(body) =>
                  action.mutate({
                    path: `/admin/problems/versions/${selected}`,
                    method: 'PUT',
                    body,
                  })
                }
              />
            </>
          ) : (
            <p>Select a version or create a challenge.</p>
          )}
        </section>
      </div>
      <JudgeOperations />
    </>
  )
}

function DraftEditor({
  initial,
  frozen = false,
  pending,
  onSave,
}: {
  initial: Draft
  frozen?: boolean
  pending: boolean
  onSave: (value: Draft) => void
}) {
  const [draft, setDraft] = useState(initial)
  const [error, setError] = useState<Error | null>(null)
  const types = [
    'int',
    'long',
    'double',
    'boolean',
    'String',
    'int[]',
    'long[]',
    'double[]',
    'boolean[]',
    'String[]',
    'int[][]',
    'long[][]',
  ]
  const textFields = [
    ['slug', 'URL slug'],
    ['title', 'Title'],
    ['statement', 'Statement'],
    ['constraints', 'Constraints'],
    ['inputFormat', draft.function ? 'Arguments' : 'Input format'],
    ['outputFormat', draft.function ? 'Return value' : 'Output format'],
    ['starterCode', 'Java starter'],
    ['referenceSource', 'Private reference solution'],
  ] as const
  return (
    <form
      className="admin-form"
      onSubmit={(event) => {
        event.preventDefault()
        try {
          if (draft.function)
            for (const test of draft.tests) {
              JSON.parse(test.input)
              JSON.parse(test.output)
            }
          setError(null)
          onSave(draft)
        } catch {
          setError(
            new Error('Use valid JSON for every set of arguments and expected return value.'),
          )
        }
      }}
    >
      <ErrorNotice error={error} />
      <fieldset disabled={frozen || pending}>
        <label>
          Solution format
          <select
            value={draft.function ? 'function' : 'stdio'}
            onChange={(event) =>
              setDraft({
                ...draft,
                function: event.target.value === 'function' ? empty.function : null,
              })
            }
          >
            <option value="function">Function arguments and return value</option>
            <option value="stdio">Legacy standard input / output</option>
          </select>
        </label>
        {draft.function && (
          <>
            <label>
              Return type
              <select
                value={draft.function.returnType}
                onChange={(event) =>
                  setDraft({
                    ...draft,
                    function: { ...draft.function!, returnType: event.target.value },
                  })
                }
              >
                {types.map((type) => (
                  <option key={type}>{type}</option>
                ))}
              </select>
            </label>
            <h3>Function parameters</h3>
            {draft.function.parameters.map((parameter, index) => (
              <div key={index}>
                <label>
                  Parameter name
                  <input
                    required
                    pattern="[a-zA-Z][a-zA-Z0-9_]{0,39}"
                    value={parameter.name}
                    onChange={(event) =>
                      setDraft({
                        ...draft,
                        function: {
                          ...draft.function!,
                          parameters: draft.function!.parameters.map((value, i) =>
                            i === index ? { ...value, name: event.target.value } : value,
                          ),
                        },
                      })
                    }
                  />
                </label>
                <label>
                  Parameter type
                  <select
                    value={parameter.type}
                    onChange={(event) =>
                      setDraft({
                        ...draft,
                        function: {
                          ...draft.function!,
                          parameters: draft.function!.parameters.map((value, i) =>
                            i === index ? { ...value, type: event.target.value } : value,
                          ),
                        },
                      })
                    }
                  >
                    {types.map((type) => (
                      <option key={type}>{type}</option>
                    ))}
                  </select>
                </label>
                <button
                  type="button"
                  className="text-link danger"
                  onClick={() =>
                    setDraft({
                      ...draft,
                      function: {
                        ...draft.function!,
                        parameters: draft.function!.parameters.filter((_, i) => i !== index),
                      },
                    })
                  }
                >
                  Remove parameter
                </button>
              </div>
            ))}
            <button
              type="button"
              className="button secondary small"
              disabled={draft.function.parameters.length >= 8}
              onClick={() =>
                setDraft({
                  ...draft,
                  function: {
                    ...draft.function!,
                    parameters: [...draft.function!.parameters, { name: '', type: 'int' }],
                  },
                })
              }
            >
              Add parameter
            </button>
            <p className="muted">
              Use a public solve method in class Solution with these parameters and return type.
              Cases contain named arguments as JSON and an expected return value.
            </p>
          </>
        )}
        {textFields.map(([field, label]) => (
          <label key={field}>
            {label}
            <textarea
              required
              rows={field.endsWith('Code') || field === 'referenceSource' ? 8 : 3}
              value={draft[field]}
              onChange={(event) => setDraft({ ...draft, [field]: event.target.value })}
            />
          </label>
        ))}
        <label>
          Difficulty
          <select
            value={draft.difficulty}
            onChange={(event) => setDraft({ ...draft, difficulty: event.target.value })}
          >
            {['EASY', 'MEDIUM', 'HARD'].map((level) => (
              <option key={level}>{level}</option>
            ))}
          </select>
        </label>
        <label>
          Tags, separated by commas
          <input
            value={draft.tags.join(', ')}
            onChange={(event) =>
              setDraft({ ...draft, tags: event.target.value.split(',').map((tag) => tag.trim()) })
            }
          />
        </label>
        <div className="admin-number-fields">
          {(
            [
              ['timeLimitMs', 'CPU limit (ms)'],
              ['memoryLimitMb', 'Memory (MiB)'],
              ['durationSeconds', 'Battle time (seconds)'],
            ] as const
          ).map(([field, label]) => (
            <label key={field}>
              {label}
              <input
                required
                type="number"
                value={draft[field]}
                onChange={(event) => setDraft({ ...draft, [field]: Number(event.target.value) })}
              />
            </label>
          ))}
        </div>
        <h3>Public and private cases</h3>
        <p className="muted">
          Keep at least one of each. Output uses case-sensitive whitespace-separated tokens.
        </p>
        {draft.tests.map((item, index) => (
          <div className="admin-test-case" key={index}>
            <strong>Case {index + 1}</strong>
            <label className="checkbox-label">
              <input
                type="checkbox"
                checked={item.public}
                onChange={(event) =>
                  setDraft({
                    ...draft,
                    tests: draft.tests.map((value, i) =>
                      i === index ? { ...value, public: event.target.checked } : value,
                    ),
                  })
                }
              />
              Public example
            </label>
            {(['input', 'output'] as const).map((field) => (
              <label key={field}>
                {field === 'input'
                  ? draft.function
                    ? 'Arguments (JSON object)'
                    : 'Input'
                  : draft.function
                    ? 'Expected return value (JSON)'
                    : 'Expected output'}
                <textarea
                  rows={3}
                  value={item[field]}
                  onChange={(event) =>
                    setDraft({
                      ...draft,
                      tests: draft.tests.map((value, i) =>
                        i === index ? { ...value, [field]: event.target.value } : value,
                      ),
                    })
                  }
                />
              </label>
            ))}
            <button
              type="button"
              className="text-link danger"
              disabled={draft.tests.length <= 2}
              onClick={() =>
                setDraft({ ...draft, tests: draft.tests.filter((_, i) => i !== index) })
              }
            >
              Remove case
            </button>
          </div>
        ))}
        <button
          type="button"
          className="button secondary small"
          onClick={() =>
            setDraft({
              ...draft,
              tests: [
                ...draft.tests,
                {
                  input: draft.function ? '{}' : '',
                  output: draft.function ? '0' : '',
                  public: false,
                },
              ],
            })
          }
        >
          Add private case
        </button>
        <button className="button primary small" type="submit">
          {pending ? 'Saving…' : 'Save draft'}
        </button>
      </fieldset>
    </form>
  )
}

interface Job {
  id: string
  status: string
  submissionId: string
  failures: number
  lastError: string | null
  leaseUntil: string | null
  leaseExpired: boolean
}
function JudgeOperations() {
  const queries = useQueryClient()
  const jobs = useQuery({
    queryKey: ['admin-jobs'],
    queryFn: () => read<Job[]>('/admin/judge-jobs'),
    refetchInterval: 5000,
  })
  const release = useMutation({
    mutationFn: (id: string) => write<void>(`/admin/judge-jobs/${id}/release-expired-lease`),
    onSuccess: () => queries.invalidateQueries({ queryKey: ['admin-jobs'] }),
  })
  return (
    <section className="panel section-panel">
      <h2>Judge operations</h2>
      <p className="muted">
        Terminal results stay immutable. Expired leases can be released safely; active work keeps
        its lease.
      </p>
      <ErrorNotice error={jobs.error || release.error} />
      {jobs.data?.length ? (
        jobs.data.map((job) => (
          <div className="operation-row" key={job.id}>
            <code>{job.id.slice(0, 8)}</code>
            <strong>{job.status}</strong>
            <span>
              {job.failures} failures · {job.lastError ?? 'No infrastructure error'}
            </span>
            {job.status === 'RUNNING' && job.leaseExpired && (
              <button
                className="button secondary small"
                disabled={release.isPending}
                onClick={() => release.mutate(job.id)}
              >
                Release expired lease
              </button>
            )}
          </div>
        ))
      ) : (
        <p>No pending or dead-letter jobs.</p>
      )}
    </section>
  )
}

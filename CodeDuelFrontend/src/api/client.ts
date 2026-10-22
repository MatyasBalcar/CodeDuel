import type {
  Battle,
  BattleEvent,
  MatchHistory,
  Problem,
  ProblemSummary,
  Profile,
  QueueState,
  SystemInfo,
  SubmissionDetail,
  PracticeEntry,
  RatingPoint,
} from './types'

export class ApiError extends Error {
  readonly status: number
  readonly code: string
  constructor(status: number, code: string, message: string) {
    super(message)
    this.status = status
    this.code = code
  }
}

async function response<T>(result: Response): Promise<T> {
  const text = await result.text()
  let data: unknown
  try {
    data = text ? JSON.parse(text) : undefined
  } catch {
    throw new ApiError(
      result.status || 502,
      'SERVICE_UNAVAILABLE',
      'The service is temporarily unavailable. Please try again shortly.',
    )
  }
  if (!result.ok) {
    const problem = data as { code?: string; detail?: string } | undefined
    throw new ApiError(
      result.status,
      problem?.code ?? 'REQUEST_FAILED',
      problem?.detail ?? 'The request could not be completed.',
    )
  }
  return data as T
}

export async function read<T>(path: string, signal?: AbortSignal): Promise<T> {
  return response<T>(await fetch(`/api/v1${path}`, { credentials: 'same-origin', signal }))
}

export async function write<T>(path: string, method = 'POST', body?: unknown): Promise<T> {
  const csrf = await read<{ token: string; headerName: string }>('/auth/csrf')
  return response<T>(
    await fetch(`/api/v1${path}`, {
      method,
      credentials: 'same-origin',
      headers: { 'Content-Type': 'application/json', [csrf.headerName]: csrf.token },
      body: body === undefined ? undefined : JSON.stringify(body),
    }),
  )
}

export const api = {
  me: async () => {
    try {
      return await read<Profile>('/auth/me')
    } catch (error) {
      if (error instanceof ApiError && error.status === 401) return null
      throw error
    }
  },
  system: () => read<SystemInfo>('/system'),
  practice: (slug: string, sourceCode: string, examples: boolean) =>
    write<{ submissionId: string }>(
      `/practice/${encodeURIComponent(slug)}/${examples ? 'run' : 'submissions'}`,
      'POST',
      { language: 'JAVA_21', sourceCode },
    ),
  practiceHistory: (slug: string) =>
    read<PracticeEntry[]>(`/practice/${encodeURIComponent(slug)}/submissions`),
  submission: (id: string) => read<SubmissionDetail>(`/submissions/${id}`),
  problems: () => read<ProblemSummary[]>('/problems'),
  problem: (slug: string) => read<Problem>(`/problems/${encodeURIComponent(slug)}`),
  profile: (name: string) => read<Profile>(`/users/${encodeURIComponent(name)}`),
  ratingHistory: (name: string) =>
    read<RatingPoint[]>(`/users/${encodeURIComponent(name)}/rating-history`),
  leaderboard: () => read<Profile[]>('/leaderboard'),
  history: () => read<MatchHistory[]>('/users/me/matches'),
  queue: () => read<QueueState>('/matchmaking'),
  enterQueue: () => write<QueueState>('/matchmaking/queue'),
  cancelQueue: () => write<QueueState>('/matchmaking/queue', 'DELETE'),
  privateRoom: () => write<{ battleId: string; inviteCode: string }>('/battles/private'),
  join: (code: string) =>
    write<{ battleId: string }>(`/battles/private/${encodeURIComponent(code)}/join`),
  rematch: (id: string) =>
    write<{ battleId: string; inviteCode: string | null }>(`/battles/${id}/rematch`),
  rematchState: (id: string) =>
    read<{ battleId?: string; proposedBy?: string }>(`/battles/${id}/rematch`),
  battle: (id: string) => read<Battle>(`/battles/${id}`),
  ready: (id: string) => write<void>(`/battles/${id}/ready`),
  forfeit: (id: string) => write<void>(`/battles/${id}/forfeit`),
  submit: (id: string, sourceCode: string, official: boolean) =>
    write<{ submissionId: string }>(`/battles/${id}/${official ? 'submissions' : 'run'}`, 'POST', {
      language: 'JAVA_21',
      sourceCode,
    }),
  practiceFixtures: (slug: string) =>
    read<{ acceptedSource: string; wrongSource: string }>(
      `/demo/practice/${encodeURIComponent(slug)}/fixtures`,
    ),
  fixtures: (id: string) =>
    read<{ acceptedSource: string; wrongSource: string }>(`/demo/battles/${id}/fixtures`),
  timeline: async (id: string) => {
    const events: BattleEvent[] = []
    let after = 0
    for (;;) {
      const page = await read<BattleEvent[]>(`/battles/${id}/timeline?after=${after}`)
      events.push(...page)
      if (page.length < 200) return events
      after = page[page.length - 1]!.sequence
    }
  },
}

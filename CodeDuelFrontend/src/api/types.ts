export interface Profile {
  id: string
  username: string
  rating: number
  rankedGames: number
  wins: number
  losses: number
  draws: number
  provisional: boolean
  role: 'USER' | 'ADMIN'
  emailVerified: boolean
}

export interface SystemInfo {
  judgeMode: 'FIXTURE' | 'JUDGE0' | 'GO_JUDGE' | 'DISABLED'
  language: string
  version: string
}

export interface ProblemSummary {
  id: string
  slug: string
  title: string
  difficulty: 'EASY' | 'MEDIUM' | 'HARD'
  tags: string[]
  durationSeconds: number
}

export type JsonValue =
  string | number | boolean | null | JsonValue[] | { [key: string]: JsonValue }

export interface JavaFunction {
  returnType: string
  parameters: { name: string; type: string }[]
}

export interface Problem extends Omit<ProblemSummary, 'id'> {
  versionId: string
  version: number
  statement: string
  constraints: string
  inputFormat: string
  outputFormat: string
  starterCode: string
  timeLimitMs: number
  memoryLimitMb: number
  function?: JavaFunction | null
  examples: { input: JsonValue; output: JsonValue }[]
}

export interface QueueState {
  status: 'IDLE' | 'QUEUED' | 'MATCHED'
  battleId: string | null
  queuedAt: string | null
  serverTime: string
  ratingWindow: number
}

export type Verdict =
  | 'PENDING'
  | 'ACCEPTED'
  | 'WRONG_ANSWER'
  | 'TIME_LIMIT_EXCEEDED'
  | 'MEMORY_LIMIT_EXCEEDED'
  | 'RUNTIME_ERROR'
  | 'COMPILATION_ERROR'
  | 'JUDGE_ERROR'

export interface Submission {
  id: string
  verdict: Verdict
  receivedAt: string
  sequence: number
  official: boolean
}

export interface SubmissionDetail {
  id: string
  battleId: string | null
  sourceCode: string
  verdict: Verdict
  compilerDiagnostic: string
  publicResults: {
    ordinal: number
    verdict: Verdict | null
    stdout: string
    runtimeMs: number | null
  }[]
}

export interface Player {
  userId: string
  username: string
  slot: number
  rating: number
  provisional: boolean
  ready: boolean
  forfeitedAt: string | null
  oldRating: number | null
  delta: number | null
  newRating: number | null
}

export interface Battle {
  id: string
  mode: 'RANKED' | 'PRIVATE'
  status: 'WAITING' | 'READY_CHECK' | 'IN_PROGRESS' | 'JUDGING' | 'FINISHED' | 'CANCELLED'
  serverTime: string
  readyDeadline: string
  startsAt: string | null
  endsAt: string | null
  finishedAt: string | null
  winnerId: string | null
  finalizationReason: string | null
  sequence: number
  players: Player[]
  problem: Problem | null
  submissions: Submission[]
}

export interface MatchHistory {
  id: string
  mode: string
  status: Battle['status']
  winnerId: string | null
  title: string
  opponent: string | null
  delta: number | null
  createdAt: string
  finishedAt: string | null
}

export interface BattleEvent {
  battleId: string
  sequence: number
  type: string
  occurredAt: string
  payload: Record<string, string | null>
}

export interface PracticeEntry {
  id: string
  verdict: Verdict
  receivedAt: string
  purpose: string
}

export interface RatingPoint {
  battleId: string
  oldRating: number
  newRating: number
  delta: number
  occurredAt: string
}

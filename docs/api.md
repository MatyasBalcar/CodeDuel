# API contract

Base: `/api/v1`. JSON names are camelCase. UUID IDs, UTC ISO timestamps. Unknown JSON properties are rejected. Errors use `application/problem+json` with `status`, `detail` and a stable `code` where supplied by the application.

## Authentication

`GET /auth/csrf` returns `{token, headerName}` and establishes the CSRF session. Include that header/token with every unsafe write, including login/register/logout. Login rotates the session ID and CSRF token; obtain a new token after authentication. Browser code fetches a fresh token before each write.

| Method | Route               | Behavior                                                  |
| ------ | ------------------- | --------------------------------------------------------- |
| POST   | `/auth/register`    | `{username,email,password}` → 201; then log in separately |
| POST   | `/auth/login`       | `{username,password}` → profile + HttpOnly session cookie |
| POST   | `/auth/logout`      | CSRF-protected session invalidation → 204                 |
| GET    | `/auth/me`          | Current profile, or 401                                   |
| GET    | `/system`           | `{judgeMode,language,version}`; no secrets                |
| GET    | `/leaderboard`      | Top 100 profiles by rating, ties by username              |
| GET    | `/users/{username}` | Public profile; no email/password/source                  |
| GET    | `/users/me/matches` | Authenticated user’s most recent 30 matches               |

Usernames are 3–24 ASCII letters/digits/underscores, starting with a letter, unique case-insensitively. Passwords are 12–72 characters and at most 72 UTF-8 bytes at registration; BCrypt cost 12. Profiles contain `id,username,rating,rankedGames,wins,losses,draws,provisional`.

## Problems

`GET /problems` lists summaries. `GET /problems/{slug}` returns the latest active published version with statement, stdin/stdout contract, starter `public class Main`, limits and **public examples only**. Published version IDs are immutable. Standalone practice and administrator version publication are described below.

## Queue and rooms

| Method | Route                          | Behavior                                                                        |
| ------ | ------------------------------ | ------------------------------------------------------------------------------- |
| GET    | `/matchmaking`                 | `IDLE`, `QUEUED` or `MATCHED`; battle ID, queued/server time and rating window  |
| POST   | `/matchmaking/queue`           | Idempotent queue/heartbeat; existing queued entry retains original wait time    |
| DELETE | `/matchmaking/queue`           | Cancel waiting entry; if pairing already won the race, return the active battle |
| POST   | `/battles/private`             | `{battleId,inviteCode}`; raw invite returned only on creation                   |
| POST   | `/battles/private/{code}/join` | One opponent; returns `{battleId}`; participant retries are idempotent          |
| POST   | `/battles/{id}/ready`          | Ready check; both confirmations schedule shared start/deadline                  |
| POST   | `/battles/{id}/forfeit`        | In-time forfeit; before start, cancel/leave room                                |

Search begins at ±150 Elo, grows by 50 every ten seconds, capped at 600, with both players’ windows required to accept the gap. Finished ranked opponents are excluded for ten minutes. Entries expire after 90 seconds without a heartbeat. Private empty rooms expire after five minutes; ready checks last 30 seconds.

## Battle and judging

`GET /battles/{id}` requires membership and returns authoritative clocks/state, players, source-free public problem details after the shared start, own submission summaries, final result/rating history and latest event sequence. No private case details or opponent code are included. Another player’s resource is indistinguishable from a missing one (404).

```json
POST /battles/{id}/submissions
{
  "language": "JAVA_21",
  "sourceCode": "public class Main { public static void main(String[] args) {} }"
}
```

Official `/submissions` and public-only `/run` return **202**:

```json
{
  "submissionId": "uuid",
  "verdict": "PENDING",
  "receivedAt": "2026-10-20T20:00:00Z",
  "sequence": 1
}
```

Source is limited to 64 KiB UTF-8, the complete HTTP body to 96 KiB. The API accepts no client time, problem ID, resource limits, callback URL or sandbox flags. `GET /submissions/{id}` is author-only and returns own immutable source and aggregate verdict, with bounded compiler diagnostics and public example output, without hidden test results or runtime stderr. Example runs do not enter scoring.

Only explicit demo mode exposes `GET /demo/battles/{id}/fixtures`, requiring membership and an already released problem. It returns exact known sample sources. Unknown edited source receives 422 `FIXTURE_ONLY`. Normal mode has no fixture endpoint. Disabled judging returns 503 `JUDGE_DISABLED` before enqueueing.

## Events and replay

`GET /battles/{id}/events?after=0` is authenticated SSE. Each `battle` event has SSE `id=sequence` and JSON `{battleId,sequence,type,occurredAt,payload}`. `Last-Event-ID` resumes delivery. Heartbeat comments keep the stream open; streams expire/reconnect after five minutes. Limits are three streams per user and 200 per API instance.

State events include `ROOM_CREATED`, `MATCH_FOUND`, `PLAYER_READY`, `BATTLE_STARTED`, `SUBMISSION_RECEIVED`, `SUBMISSION_JUDGED`, `JUDGING_STARTED`, `PLAYER_FORFEITED`, `BATTLE_FINISHED`. Public judged payloads expose only `ACCEPTED`, `ATTEMPTED` or `JUDGE_UNAVAILABLE` status, not detailed opponent verdicts/source/cases. Example runs produce no opponent events.

`GET /battles/{id}/timeline?after=0` requires membership and a terminal match. Pages contain at most 200 ordered events; advance `after` to the last sequence until a shorter page is returned. On any stream gap/error, fetch a fresh battle snapshot; events are notification/replay, never the outcome authority.

## Current throttles

Fixed windows persist in PostgreSQL and include rejected requests: registration 8/hour/IP, login 40/15 minutes/IP, queue writes 30/minute/account, room creation 10/minute/account, invite joins 20/minute/account, official submissions 10/minute/account, public runs 15/minute/account. At most four pending programs per account. UI polling is bounded but does not consume these write quotas. Production trusts only the address header overwritten by the private Caddy edge. Local mode ignores forwarded-address headers.

## Recovery and account data

| Method | Route                              | Behavior                                                                           |
| ------ | ---------------------------------- | ---------------------------------------------------------------------------------- |
| POST   | `/auth/forgot-password`            | `{email}`; same generic response for known/unknown address                         |
| POST   | `/auth/reset-password`             | `{token,password}`; one use, 15-minute expiry, revoke all sessions                 |
| POST   | `/auth/verify-email`               | `{token}`; one use, 24-hour expiry                                                 |
| POST   | `/auth/verification-email`         | Authenticated resend; invalidates older links                                      |
| GET    | `/auth/account`                    | Private email, verification flag, Java language and source retention days          |
| POST   | `/auth/delete-account`             | `{password}`; anonymize, revoke sessions and redact source; active battle rejected |
| GET    | `/users/{username}/rating-history` | Public chronological rating points, latest 100                                     |

Every unsafe call still needs CSRF. Reset/verification tokens travel in email URL fragments and are consumed by explicit POST, never by GET. Resend/reset operations have independent IP/account/address-hash limits. Production requires verified email for ranked matchmaking. Profiles also carry `role` and `emailVerified`; email itself is private to `/auth/account`.

## Practice and rematch

`POST /practice/{slug}/run` and `/practice/{slug}/submissions` accept the same Java source body and return 202 receipts. The former uses public examples; the latter all immutable cases. Both use the account's shared pending/source quotas and never affect Elo. `GET /practice/{slug}/submissions` lists the author's latest 20 practice attempts. Only active published problems may be practiced.

`GET /battles/{id}/rematch` returns the active private proposal for a terminal match's members. `POST /battles/{id}/rematch` creates the proposal or explicitly joins the other player's existing proposal. Repeated/concurrent calls are idempotent. One player cannot enroll the opponent silently.

## Administration

All `/admin/**` routes require ADMIN; ordinary authenticated users receive 403. `GET/POST /admin/problems` lists versions/creates a draft. `GET/PUT /admin/problems/versions/{id}` reads private content/saves a draft. POST suffixes `/clone`, `/review`, `/publish` respectively create a new version, freeze and queue real reference judging, and publish after an accepted real-provenance pass. `POST /admin/problems/{problemId}/retire` or `/activate` changes catalogue eligibility without mutating old versions. Admin JSON writes may be up to 4 MiB; normal source writes remain 96 KiB.

`GET /admin/judge-jobs` lists pending/running/dead jobs with safe metadata and server-computed lease expiry. `POST /admin/judge-jobs/{id}/release-expired-lease` releases only expired nonterminal work and appends an audit event. It cannot rewrite completed verdicts/results.

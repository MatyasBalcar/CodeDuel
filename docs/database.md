# Database and concurrency

Flyway migrations V1–V9 create UUID-based tables with UTC `timestamptz` clocks, numeric ratings and explicit constraints. All code-acceptance and battle-deadline decisions use PostgreSQL time after the relevant aggregate lock is acquired.

## Critical constraints

- `active_sessions.user_id` is unique across queue, private rooms and ranked games.
- Queue membership references the active-session claim; pairing moves both claims to the new battle atomically.
- Participants have unique `(battle_id, user_id)` and `(battle_id, slot)`; the winner must be a participant.
- Battle submissions have a composite foreign key to `(battle_id, problem_version_id)` and an author membership foreign key.
- Receipt sequences are unique per battle. A database trigger prevents changing source, language, author, problem version, receipt, sequence or official/practice designation.
- Terminal submission verdicts cannot be rewritten. Published problem versions and cases cannot be edited/deleted, including by the application role through ordinary writes.
- Case edits lock their parent version, serializing with publication. Seed checksums detect accidental edits to a previously published catalog version.
- Rating events have unique `(battle_id, user_id)` and `new = old + delta`; event/history mutation triggers enforce append-only ordinary writes.
- A job has one submission, one current lease token and a lease expiry. Terminal/dead states have no live lease.

The application still validates game transitions and the exact two-player start condition. Constraints complement those rules; they do not attempt to encode the entire state machine as triggers. A database owner/superuser can bypass ordinary protections, so production role restrictions still matter.

## Lock ordering

1. Session-changing requests serialize the user with **`FOR NO KEY UPDATE`**. This is deliberate: a full `FOR UPDATE` conflicts with the `KEY SHARE` locks that participant foreign-key inserts require, creating a user→queue / queue→user deadlock with cancellation.
2. Queue pairing holds the global transaction advisory lock, then locks queue rows in oldest-first order. It creates the battle and participants, moves active claims, deletes queue entries and commits together.
3. Submission receipt first locks the active user quota, then the **battle row**. Readiness, forfeit and finalization lock the battle directly; they never subsequently acquire a user-row lock. Source insertion/receipt sequence assignment cannot race past finalization.
4. Verdict completion takes **battle, then judge job**. Short token/case checkpoints take only the job row; they never acquire a battle lock afterwards in that transaction.
5. Ranked finalization locks **both rating profiles ordered by UUID in SQL**, computes changes from the same pre-match snapshot, inserts both events, marks the battle terminal, releases active claims and appends the finish event in one transaction.

SSE and the frontend never mutate authoritative outcomes. Snapshot queries use a repeatable-read transaction so players, source-free problem details and sequence belong to a consistent view.

## Judge delivery

A short claim transaction uses `SKIP LOCKED` and either takes a due pending job or reclaims an expired running lease. Each claim gets a new random fencing token and a 60-second lease. A worker handles up to four cases per step; remote tokens and finished results are checkpointed. Polling steps reschedule after 500 ms without treating “still running” as an infrastructure retry.

Every write rechecks token **and expiry under the job lock**. A crashed worker may duplicate an external execution if it dies between sending a request and persisting the remote token. At-least-once external execution is tolerated; stale writes and duplicate terminal/rating effects are prevented. Exactly-once external execution is not claimed.

Executor failures back off, with three failures or five minutes stuck leading to dead letter / `JUDGE_ERROR`. Relevant infrastructure failures cancel the battle without rating. Audited operator tools inspect jobs and release expired nonterminal leases. Immutable terminal player verdicts must not be patched in place; diagnosis and a new unranked attempt preserve historical truth.

## Tests

Real PostgreSQL tests cover concurrent duplicate queue calls, simultaneous matchmakers, repeated cancellation/pairing races, the exact foreign-key/session lock interaction, reverse judge completion order, concurrent result finalization, expired leases, stale token rejection, immutability, authorization and delayed on-time submissions. No H2 substitute is used for these tests.

## Accounts, practice and publication

V4 persists Spring Session and append-only audit events. V5 adds battle-independent practice/reference submissions; their direct author/version foreign keys remain valid with a null battle, and official submissions still require a battle. Review freezes all content/tests before asynchronous reference verification. V8 records executor provenance, so an exact fixture pass cannot authorize publication. V6 records a consent-based private rematch proposal per origin match.

V7 stores only recovery-token hashes, a fenced SMTP outbox and deletion/retention metadata. The sole source immutability exception is a fixed one-way redaction of a terminal program. It cannot change its verdict, receipt, owner or version, and cannot be undone. Deletion cancels pending standalone jobs under the user's lock before redaction and retains pseudonymous rating/match history.

V9 adds a JSONB function contract to problem versions and JSONB named arguments and
expected values to cases. The 100 function problems are new v2 rows. Existing v1 rows,
source checksums, submissions, and battle references stay intact. The existing freeze
triggers also protect the new columns. A case holds either the legacy text pair or
the structured argument/result pair.

Production migrations and grants run with a dedicated schema-owner role. API business writes and worker finalization privileges are separate; the worker cannot read account, session, mail or token tables and cannot update source. See `deploy/db/grant-runtime.sql`.

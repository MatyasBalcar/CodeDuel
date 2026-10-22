# Architecture

The product is a Spring Boot 3.5 / Java 21 modular monolith, plus a separate trusted orchestration process and a separate Linux execution VM. Controllers validate HTTP shape and call explicit domain services. Spring JDBC keeps SQL locks/transactions visible. Public DTOs are deliberate projections, rather than serialized database entities.

`CodeDuelBackend` is the IntelliJ/Maven project; `CodeDuelFrontend` is the WebStorm/React/TypeScript project. Both share the parent Git repository. Shared orchestration, content generation, worker provisioning and documentation stay outside the IDE source roots. Frontend builds do not require Java; backend builds do not require Node.

| Package | Responsibility |
| --- | --- |
| `auth` | BCrypt, JDBC sessions, CSRF, one-time recovery, SMTP outbox and account redaction |
| `users` | Public profiles, own history, rating graph and leaderboard projections |
| `problems` | Original seeded content, version/checksum discipline and public-only statements |
| `administration` | Audited draft/review/publish/retire and judge operations |
| `matchmaking` | Rating windows, queue claims, heartbeat expiry and pairing |
| `battles` | Active-session guard, readiness, authoritative clocks, rematches and finalization |
| `submissions` | Immutable source, battle/practice eligibility, quotas and owner-only feedback |
| `judge` | Pluggable adapters, leased durable jobs, compiled checkpoints and result aggregation |
| `ratings` | Pure Elo calculation and append-only transactional history |
| `events` | Ordered persisted projections and authenticated SSE replay |
| `common` | DB clock, errors, bounded bodies, rate limits, production policy and metrics |

```mermaid
sequenceDiagram
  participant C as Browser
  participant A as API
  participant D as PostgreSQL
  participant W as Orchestrator
  participant J as Execution VM
  C->>A: Submit immutable Java + CSRF
  A->>D: Lock user quota and battle; assign receipt sequence
  A->>D: Source + case rows + durable job + event
  A-->>C: 202 receipt
  W->>D: Short SKIP LOCKED lease claim
  W->>J: Authenticated bounded compile/run outside DB transaction
  W->>D: Fenced program/case checkpoints
  W->>D: Battle then job lock; terminal verdict
  W->>D: Pending-receipt barrier; Elo and final event in one transaction
  C->>A: SSE or reconnect snapshot
  A-->>C: Authoritative clocks and own-only feedback
```

The API runs HTTP, matchmaking/deadline reconciliation, email delivery and maintenance. Profile `worker` disables HTTP and runs only judging orchestration. It cannot read users/sessions/mail in the production DB role. The execution VM holds neither DB credentials nor project/home mounts; compilation/execution never occurs in either application JVM.

`GO_JUDGE` uses pinned go-judge v1.13.3 with Java 21 and validated sandbox limits. The alternative Judge0 adapter remains available. `demo` permits a visibly labelled exact-fixture orchestrator; unknown programs are rejected before enqueueing. Default non-demo startup has judging disabled unless explicitly configured. Fixture mode is refused without the demo profile and forbidden by production policy.

Snapshots are authoritative. Events have a transactionally allocated per-battle sequence. SSE replays after `Last-Event-ID`; the frontend periodically refreshes snapshots and reconciles gaps. PostgreSQL event persistence avoids a process-local broker. Multiple API instances can share JDBC sessions and DB locking, but scaling beyond the verified single API/worker topology still needs load/capacity validation.

Separate `migrate`, API and worker database identities constrain privileges. Caddy terminates HTTPS, overwrites the trusted client-address header and forwards to frontend Nginx; Nginx dynamically resolves the API and preserves SSE streaming. Production runtime cannot run Flyway or seed content, and its startup policy requires secure sessions/real judging. SMTP validation applies to the web API; the worker receives no SMTP secret.

See [database locks](database.md), [scoring](rating-and-scoring.md), [threat model](judge-threat-model.md), [API](api.md) and [ADRs](adr/001-core-decisions.md).

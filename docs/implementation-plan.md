# Implementation plan

Quality takes priority over feature count. The current release targets Java 21 only. Python and JavaScript are deferred. Deployment scope is local verification and deployable files, as requested; no cloud account or public launch is assumed.

## Baseline: first playable system

Implemented: Spring Boot/PostgreSQL/React foundation, sessions and CSRF, immutable problem versions, private rooms, ranked queue, shared clocks, durable judge jobs, receipt-order outcomes, transactional Elo, reconnect snapshots and replay. The fixture demonstration remains available as an explicitly labelled mode.

## 1. Real Java judging

- Provision a separate Linux execution VM, with no home/project mounts or application credentials.
- Use pinned go-judge v1.13.3 for native ARM64/x86-64 sandbox support and Java 21; retain the optional Judge0 adapter.
- Compile with independent limits; persist remote checkpoints; run immutable programs against public/hidden cases.
- Return bounded compiler feedback and public example output to the owner only.
- Verify the full two-account journey using real source execution.

## 2. Execution safety and recovery

- Exercise loops, high memory, output floods, process/thread limits, blocked network/metadata/database access and filesystem isolation.
- Prove lease fencing, worker/executor restart recovery and cancellation without rating on infrastructure failure.
- Document the precise sandbox boundary and measured limitations; local tests are not a penetration audit.

## 3. Hosting readiness, verified locally

- Persistent database sessions, distinct migration/API/worker roles, production validation and secret configuration.
- TLS reverse proxy configuration, secure cookies and private service networks.
- Backup and restore drill, metrics/alerts, queue health and dead-letter inspection/recovery.
- Deployment smoke test without creating public hosting resources.

## 4. Complete the product

- At least fifteen original, oracle-tested Java challenges.
- Audited administrator draft/edit/publish/retire workflow; preserve used versions.
- Email verification/password reset, local mail delivery testing and production SMTP configuration.
- Standalone practice judging, rematch, profile rating history.
- Account deletion/source retention policies and automation.
- Extend PostgreSQL/browser tests and update the separate local-run and deployment guides.

## Current completion

The four implementation stages above are implemented. Backend, API, sandbox, release and six browser checks passed locally; one final real-Java browser harness rerun is deferred at the user's request. See the verification report. The repository now has two separately configured IDE roots under the shared parent: `CodeDuelBackend` and `CodeDuelFrontend`. Java-only remains the language scope. The separate local-run/deployment guides cover both layouts.

Local evidence includes unit/PostgreSQL/sandbox checks, all 362 trusted reference cases, an HTTPS real ranked duel with SMTP/recovery/admin/practice/deletion, session/worker restarts, and browser journeys. The verification report records commands and exact counts.

Remaining launch activities depend on an actual hosting environment: public DNS/certificates, provider firewall/VPN/SMTP credentials, encrypted scheduled backups, external monitoring and deployment-specific sandbox review. They are not unimplemented local game features. Seasons, tournaments, spectators, optimization mode and additional languages remain deferred product work.

## Release evidence

Record actual passing commands, problem counts, sandbox checks and limitations in the README and verification report. A configured CI workflow is not a claimed hosted CI run. Deployment files are not a claimed public deployment. The optional fixture judge never executes arbitrary source code.

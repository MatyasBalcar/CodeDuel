# Local verification evidence

Verification took place on an ARM64 Mac with JDK 21, Node 24, Docker/Colima, a separate mount-free Lima execution VM, PostgreSQL 17.11 and go-judge v1.13.3 running Java 21.0.12.1. The IDE project split is included in the verified layout. This report describes local runs; no hosted CI run, public deployment, load target or external security audit is claimed.

## Automated backend and content

```sh
python3 worker/sandbox/verify-local.py
python3 problems/verify_catalog.py
```

Passed **77 backend checks**: 31 unit tests, 34 real PostgreSQL integration checks and 12 real sandbox checks. PostgreSQL coverage includes duplicate pairing/queue entry, cancellation races, FK/session lock compatibility, receipt-order finalization, deadline acceptance, concurrent Elo idempotency, lease fencing/retries, immutable payload/version protection, authorization, practice, admin fixture-publication rejection, recovery-token expiry/single use, session revocation, deletion, retention and mail fencing. No H2 substitute was used.

Sandbox checks execute actual Java in the isolated VM: correctness, wrong output, compiler feedback, hidden-stderr privacy, infinite loop, high memory, output flood, blocked external/database/metadata networking, inaccessible host/secrets, clean writable state and bounded thread exhaustion with healthy subsequent work. One test also executes the largest input of every seeded problem at its configured CPU/memory limit. A database-worker test judges real hidden cases and applies Elo once.

All **362 trusted reference cases** passed independent Python-oracle comparisons. These are fifteen original problems; trusted reference verification executes only repository-owned code on the host. User submissions execute exclusively inside the separate sandbox.

## Frontend

Node 24: format check, ESLint, TypeScript/Vite production build, all **7 unit tests**, and `npm audit --audit-level=moderate` passed with **zero reported vulnerabilities**. Monaco is lazy-loaded; its sizeable editor bundle is an acknowledged desktop IDE payload, not a claimed load-performance benchmark.

**Six browser checks passed:** private duel with draft recovery/replay/rematch, ranked duel with durable Elo/profile/leaderboard, email verification/practice/reset/deletion, mobile library/lobby, automated lobby/sign-in WCAG checks, and administrator real-reference publication/retirement. These ran against the appropriate fixture or HTTPS real-judge stack.

**One browser check remains unfinished:** the real-Java arena journey. It reached actual compilation feedback, but the automated editor replacement used the host keyboard modifier rather than Monaco's emulated browser-platform modifier, so it submitted combined source and correctly received a compilation error. The harness now targets the accessible editor and derives Select All from browser user-agent; that final adjustment has passed lint but has not been rerun. Testing stopped at the user's request to wrap up. The separately verified real API/worker/sandbox and HTTPS ranked journey below passed.

Resume only this check, with real local mode running:

```sh
cd CodeDuelFrontend
REAL_JUDGE=true E2E_BASE_URL=http://127.0.0.1:5173 npx playwright test real-judge.spec.ts
```

## Release rehearsal

```sh
python3 deploy/verify-local.py
deploy/backup-restore-drill.sh
```

Passed in the **separate local HTTPS release stack**:

- Trusted local CA validation, Secure/HttpOnly session cookie and authentication surviving API restart.
- SMTP capture/delivery, one-time email verification, password reset and old-session revocation.
- Two verified players in ranked play, identical version/start/deadline, bounded compiler feedback, actual Java accepted on hidden cases, earliest receipt winner, persisted Elo and replay.
- Worker stopped around persisted queued programs, restarted, then both programs judged without duplicate result/rating events.
- Standalone practice without Elo, explicit-consent private rematch, audited draft/save/freeze/real reference verification/publish/retire.
- Account anonymization/source erasure with rating ledger preserved; worker denied user/session/mail reads and source updates, API denied schema creation.
- Non-destructive dump/restore into a disposable database: 8 users, 4 battles, 6 submissions, 2 rating events, 18 battle events and 17 problem versions matched. Restored rating arithmetic and migration-process startup passed. Live data was retained and application processes resumed.

Both Docker application images build from the renamed directories. Ordinary and deployment Compose configurations validate. Nginx and Caddy configuration validation passed. Both IDE project XML configurations and local Markdown links passed the final handoff check.

## Problem library expansion (2026-10-26)

The library now has 45 problems: 16 easy, 22 medium, and 7 hard. All **930 reference cases**
passed on Windows with JBR 25.0.4, compiling every trusted reference with `--release 21`.
The 30 additions include 568 cases generated from independent Python answers and explicit
large-input expectations.

Regenerating the catalog produced the same file. The original 15 problem entries match
the saved catalog exactly, including reference source and test cases. The new entries
also passed checks for unique slugs, two public examples per problem, matching Java
sources, and the existing case input/output size limits. The updated library page passed
its Prettier check.

This run checked content and reference solutions. The earlier application and sandbox
evidence above covers the original library; those checks were not rerun for this expansion.

## Limits

The tested topology is one API, one trusted worker and one execution VM. Public-host routing/firewalls, encrypted provider SMTP, DNS/certificates, scheduled encrypted backups, deployed monitoring, abuse response and sandbox patch review remain operator-specific launch work. Adversarial examples are evidence for controls, not proof against all JVM/kernel escapes. Exactly-once external execution is not claimed; fencing and database constraints enforce idempotent terminal/rating effects. The fixture mode does not execute edited programs or establish sandbox safety.

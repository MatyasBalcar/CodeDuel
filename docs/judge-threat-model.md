# Judge threat model

## Trust zones

Source, stdin-derived output, stderr and diagnostics are hostile. API, PostgreSQL and orchestration are trusted business services. Execution runs in a dedicated Linux VM, behind an authenticated private boundary, with no host/home/project shares, DB credentials, Docker daemon or forwarded SSH agent. The orchestrator may reach PostgreSQL; submitted programs may not.

No application path invokes `Runtime.exec`, `ProcessBuilder`, a host compiler or shell to execute submitted code. The repository's Python oracle verifier executes only fixed, trusted reference programs. The go-judge adapter sends only server-owned command/flag/mount/limit policy; source is a copied input file, not a shell command.

## Implemented controls

| Threat                           | Control                                                                                                                      |
| -------------------------------- | ---------------------------------------------------------------------------------------------------------------------------- |
| Battle/source access             | Membership checks on snapshots, writes, SSE and replay; author-only detail endpoint                                          |
| Hidden-case leakage              | Explicit public-only DTOs; no private stdout/stderr persistence or public private-case traces                                |
| Mutable judged programs          | DB-protected source/version/receipt; one-way terminal retention tombstone only                                               |
| Source/request flooding          | 64 KiB source, 96 KiB normal JSON; 4 MiB admin content writes with two-reader admission; persistent rate limits/backpressure |
| Shared-machine execution         | Dedicated VM, per-execution namespaces/cgroups/seccomp and clean tmpfs state                                                 |
| Filesystem escape/data access    | Read-only minimal runtime mounts, unprivileged workload UID, no application/secret/host paths                                |
| Network/metadata/DB access       | Per-process network namespace plus executor-host isolation requirement                                                       |
| Compile abuse                    | Annotation processing disabled; compile CPU/wall/memory/process/output/file limits independent of run                        |
| Run abuse                        | Problem CPU/memory plus wall deadline, bounded JVM, process/thread count and output/filesystems                              |
| Remote body/redirect abuse       | No redirects; HTTP timeouts and streamed body caps; bounded persisted feedback                                               |
| Token/source logging             | Silent executor; application logs stable categories/IDs, not remote bodies or credentials                                    |
| Worker races/restarts            | 60-second expiring fencing token, case/program checkpoints, immutable terminal transition                                    |
| Infrastructure deciding a winner | Retry/dead-letter path; relevant system failure cancels without Elo                                                          |
| Unverified admin publication     | Frozen review content and accepted verification with real-executor provenance                                                |

The supervisor is privileged inside its dedicated VM to create namespaces/cgroups. Submitted processes run without those privileges. Ordinary Docker is not used as the asserted public hostile-code isolation boundary. See [sandbox configuration](../worker/sandbox/README.md) for concrete commands, pinned binary digests and limits.

## Measured local evidence

The real sandbox suite exercises correct/incorrect Java, bounded compilation feedback, hidden stderr, infinite loops, memory pressure, huge stdout, blocked network/metadata/database connections, inaccessible host/secret paths, writable-state cleanup, thread exhaustion and subsequent healthy execution. It also runs the largest input of every original problem under that problem's configured limits. Database tests cover stale-worker fencing, retries, duplicate finalization and reverse completion order. The release verifier restarts API/worker around persistent sessions/jobs.

These are functional/adversarial checks of this local setup, not proof against all JVM/kernel exploits or a penetration audit. A public host still needs patched/reproducible VM images, provider firewall/egress verification, monitored capacity, abuse response and deployment-specific review. Host Java packages are recorded exactly at install; update and retest them through a controlled release.

The 250-job admission threshold is soft under concurrent requests; strict source/user-session invariants remain database-enforced. A duplicated remote execution can occur between network completion and checkpoint commit. Fencing prevents stale verdict/Elo effects; exactly-once external execution is not claimed.

Output is compared as case-sensitive tokens, without numeric tolerances. Runtime is diagnostic and does not rank players. Only a recognized memory failure is reported as memory-limit exceeded; generic runtime signals remain runtime errors.

## Privacy and operations

Account verification/recovery, JDBC session revocation, audited publication, restricted DB roles, source redaction, local TLS and restore drills are implemented. See [retention policy](data-retention.md) and [deployment guide](deployment-guide.md). Source remains sensitive in backups until expiry. The configured monitoring examples require an operator-owned scrape/alert service. Public hosting and an external security audit are outside the requested local deployment scope.

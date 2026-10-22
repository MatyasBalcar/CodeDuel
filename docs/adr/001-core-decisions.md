# Initial architecture decisions

Status: accepted for the first playable milestone, 20 October 2026.

1. **Modular Spring core, separate execution service.** Business state stays in one transactional application. Isolation, not organizational complexity, motivates a separate judge boundary.
2. **First accepted by server acceptance sequence.** Judge completion speed cannot decide a match. A locked, monotonically assigned sequence breaks timestamp ties; relevant pending receipts form a finalization barrier.
3. **Elo v1.** Pure math, six-decimal history and provisional K are explainable and testable. Stable locks and unique history keys provide idempotency. Private games never change rating.
4. **PostgreSQL durable queue.** `SKIP LOCKED`, leases and fencing provide restart recovery without Redis. External execution is at least once; terminal/rating effects are idempotent.
5. **Published problem versions are immutable.** A battle references one version, backed by triggers, composite foreign keys and seed checksums. New content gets a new version.
6. **Java 21 first, explicit stdin/stdout contract.** `public class Main` avoids a complex source-rewriting harness. The gateway remains language-independent for later adapters.
7. **No runtime-based cross-language ranking.** Speed mode measures accepted receipt order. Remote runtime is diagnostic, never a win criterion.
8. **Original content and verifiable oracles.** Fifteen original tasks use deterministic cases and independent oracles. Reference validation only executes trusted repository code.
9. **Explicit JDBC for core writes.** Native locks/constraints/claim SQL are visible at the transaction boundary. JPA is not needed to demonstrate aggregate consistency and would not simplify these operations.
10. **REST plus persisted SSE.** The browser sends commands over REST; ordered SSE handles notification and replay. Two-second snapshots recover missed events. Spring Session JDBC persists authentication across restarts and instances; multi-instance capacity is a separate scaling exercise.
11. **Explicit fixture demo, no simulated arbitrary-code judging.** Known sample programs exercise persistence and game flow without host execution. Unknown source is rejected rather than guessed correct/incorrect. The real Java path uses a dedicated execution VM; fixture mode remains a clearly labelled optional demonstration.
12. **No silent Elo floor.** A future floor must define its effect on deltas, expectations and history. Every displayed rating/delta already uses symmetric integer HALF_UP, including negative values; internal history retains six decimals.

13. **Native go-judge execution VM.** Pin go-judge v1.13.3 and validate Java 21 compilation/runtime rather than assume an old Judge0 image supplies the requested language/runtime. Keep the Judge0 adapter interchangeable. Native ARM64 support avoids emulating execution on the development Mac. The namespace/cgroup supervisor runs inside a dedicated mount-free VM; user programs remain unprivileged.
14. **Separate IDE roots, shared repository.** `CodeDuelBackend` is the existing IntelliJ/Maven project; `CodeDuelFrontend` is a WebStorm/npm project. Both live under `CodeDuel`, share Git, and build independently. Operational files and trusted problem content stay in the parent.
15. **Freeze before reference verification.** Publishing requires an accepted real-executor reference result tied to immutable REVIEW content and recorded executor provenance. Draft edits cannot race a reference check into publishing untested cases.

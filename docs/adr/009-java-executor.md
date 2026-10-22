# ADR-009: Java 21 through go-judge in a dedicated execution VM

Status: accepted for local implementation, 20 October 2026.

The stock Judge0 CE image ships Java 13 and its compiler image targets AMD64. This project needs a reproducible Java 21 path on both an Apple Silicon development machine and Linux deployment infrastructure. The gateway remains interchangeable; the existing Judge0 adapter is retained for independently provisioned Java 21 installations.

The default real execution adapter uses [go-judge v1.13.3](https://github.com/criyle/go-judge/releases/tag/v1.13.3), which supplies Linux namespaces, cgroups and seccomp, rather than implementing an isolation mechanism in the application. Its downloaded native binary is verified against a checked-in SHA-256 digest. The Java 21 Ubuntu security package version is recorded on installation; release operators must record and deliberately update their execution VM image/package versions.

The sandbox supervisor runs in a **separate VM**. It has no application database, production secrets, Docker socket, home directory mount, project mount or forwarded SSH agent. Only runtime files are mounted read-only into individual sandboxes; working directories are disposable tmpfs filesystems. The public browser never connects to this service. The trusted worker authenticates over a private connection and constructs every command and resource limit itself.

Compilation has separate CPU/wall/memory/output/process limits and disables annotation processors. Each official execution receives only one server-owned input. Hidden output/stderr is not returned or persisted as public feedback. Compilation diagnostics do not receive testcase input. Programs cannot select commands, compiler options, environment variables, callbacks, network mode or mount configuration through the public API.

A remote artifact ID is checkpointed in PostgreSQL between compilation and execution. A cache restart can cause re-execution after retry; fenced database writes ensure a terminal result and rating transaction are committed once. Cache expiry removes abandoned compiled artifacts.

This is a local controlled-demo boundary, not a guarantee against kernel vulnerabilities. A public deployment requires a dedicated patched VM/host, private authenticated transport, outbound policy, capacity measurement and a reviewed threat model. Never place this supervisor in the application VM or a shared privileged container.

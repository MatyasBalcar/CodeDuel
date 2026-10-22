# Java 21 execution VM

This directory provisions the **execution** service, separate from the trusted orchestration worker and database. It uses [go-judge v1.13.3](https://github.com/criyle/go-judge) (MIT; notice in `licenses/`), Linux cgroups/namespaces/seccomp, and the Ubuntu Java 21 security package. Installation records the exact Java package/runtime version. No application secret, database, project/home mount, Docker daemon or SSH-agent forwarding belongs in this VM.

On macOS with Lima 2+ (`brew install lima`), from the repository root:

```sh
worker/sandbox/setup-local.sh
docker compose --env-file .env.judge.local --profile worker up -d --build
```

The script creates a 2 CPU / 3 GiB VM named `codeduel-executor`, with **zero host directory mounts**. It downloads a native binary with a pinned SHA-256 checksum, installs the Java 21 package, copies only the sandbox configuration, generates a bearer secret, and starts a silent systemd service. It does not change the default Docker context or existing Colima VM. Existing secret values are preserved on rerun. The generated `.env.judge.local` is private and ignored by Git.

The service port is forwarded to **127.0.0.1:5058**, not a public interface. Docker applications under Colima use `http://host.lima.internal:5058`; host processes use `http://127.0.0.1:5058`. Other Docker providers may use `host.docker.internal` or a private route. Verify that route before changing configuration.

Run the actual adapter, database worker and adversarial tests with JDK 21:

```sh
python3 worker/sandbox/verify-local.py
```

`--sandbox-only` runs the real execution tests without the PostgreSQL suite. Ordinary `./mvnw verify` deliberately skips the real-executor tests unless `CODEDUEL_EXECUTOR_URL` and `CODEDUEL_EXECUTOR_TOKEN` are provided. It still requires Docker for the normal PostgreSQL tests. Fixture tests are separate and never establish real execution security.

The public contract is a default-package `public class Main`, standard input and standard output, Java 21. Compiler flags, commands, environment, mounts and limits are controlled exclusively by the worker. Annotation processing is disabled. Compilation has 10 CPU / 15 wall seconds and 512 MiB; each run has its problem CPU/memory limit, a separate wall limit, 48 process/thread slots, 16 KiB output collectors and bounded writable filesystems. Accepted output is compared as case-sensitive tokens. JVM heap is limited within the process memory budget. Never rank by measured runtime across execution machines.

Compiled JAR checkpoints are reusable across the submission's cases and survive worker restarts. Results are fenced by a PostgreSQL lease. Unavailable/expired artifacts cause a fresh compilation on retry. Worker cleanup plus a five-minute artifact TTL bounds abandoned cached source artifacts. User source is never passed to a host shell or executed by the API. Only server-owned, constant compile commands cross the internal executor boundary.

Useful operations (no token values in command arguments):

```sh
limactl shell --workdir=/tmp codeduel-executor sudo systemctl status codeduel-judge
limactl shell --workdir=/tmp codeduel-executor sudo systemctl restart codeduel-judge
limactl shell --workdir=/tmp codeduel-executor cat /opt/codeduel-judge/java-version.txt
limactl shell --workdir=/tmp codeduel-executor findmnt -rn -t virtiofs,9p,fuse.sshfs
limactl stop codeduel-executor
limactl start --tty=false codeduel-executor
```

The `findmnt` command should return no host mounts (exit 1). Stopping the executor while submissions are queued exercises real infrastructure failures; affected duels may be cancelled without rating after retries. Finished verdicts and rating events are immutable.

For a dedicated Linux deployment VM, copy `mount.yaml` to `/tmp/codeduel-mount.yaml`, then run `install-linux.sh` as root **there**. Supply `/etc/codeduel-judge.env` with a strong `ES_AUTH_TOKEN` and `ES_SILENT=true`, mode 0600; start the service. Use a private authenticated/encrypted transport (private VPN or TLS proxy), restrict inbound access to the trusted worker and enforce host outbound rules. Never expose this raw execution API to browsers. Keep the VM and Java package patched and record a reproducible VM image for releases. The local VZ template is for macOS; Linux cloud providers supply their own VM.

Passing local adversarial cases is evidence for the configured controls, not a kernel penetration audit or authorization to open an unaudited shared execution host to public hostile traffic.

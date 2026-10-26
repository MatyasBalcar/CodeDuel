# CodeDuel — local run guide

Run shared commands from the project root, which contains both the backend and frontend folders. Write a **Java 21** `public class Solution` with the problem's `solve(...)` method. The judge passes named, typed arguments and checks the return value. The library contains 100 problems: 36 easy, 47 medium, and 17 hard. Signatures and examples are visible before joining a match. Older matches keep their original version and contract.

## Windows without Docker

Open PowerShell in the folder containing `CodeDuelBackend` and `CodeDuelFrontend`. Node 24
and PostgreSQL 17 or newer must be installed.

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\windows.ps1 setup
```

Setup downloads a checksum-verified [Temurin JDK 21](https://adoptium.net/installation/ci-scripts)
and [Mailpit 1.31.6](https://github.com/axllent/mailpit/releases/tag/v1.31.6) into `.local/tools`.
It reuses the installed PostgreSQL binaries to create a separate database cluster in
`.local/postgres`, installs frontend dependencies, and starts PostgreSQL and captured email
on loopback addresses. An existing PostgreSQL service on port 5432 can keep running;
the project uses port 5444.

Start the backend and frontend in separate PowerShell terminals:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\windows.ps1 backend
```

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\windows.ps1 frontend
```

Open **http://localhost:5173**. Captured verification and password reset emails appear at
**http://localhost:8026**. The demo accounts are `nova` and `atlas`, with password
`CodeDuel-demo-2026!`. This is fixture mode: select a supplied demo program to submit.
Real execution uses the isolated Linux judge described below.

For IntelliJ, select the JDK path printed by setup, set the `demo` profile, and keep the
database and SMTP defaults from `application.yml`. The `services` action starts PostgreSQL
and Mailpit before starting the application in the IDE. A separate Maven installation is
unnecessary because the backend includes its wrapper.

Stop backend and frontend terminals with Ctrl+C. Run the following to stop this project's
database and captured email without removing their data:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\windows.ps1 stop
```

Local tools, database files, email, and logs stay in `.local/`, outside the source files.

## Windows with Docker CLI

Docker Desktop is optional. The Docker CLI, Compose, and Buildx can be installed separately:

```powershell
winget install --id Docker.DockerCLI --exact --source winget
winget install --id Docker.DockerCompose --exact --source winget
winget install --id Docker.Buildx --exact --source winget
```

For `docker compose` and `docker buildx` to work, Docker must find their executables as
CLI plugins. The `cliPluginsExtraDirs` setting in `%USERPROFILE%\.docker\config.json`
can point to `%LOCALAPPDATA%\Microsoft\WinGet\Links`. Use the full path in the JSON file.
This is configured on the current PC. Reopen PowerShell after installing the tools to
pick up the updated PATH.

The client tools need a running engine. This project uses Linux containers, so the
native Windows `dockerd.exe` cannot run it. One local option is
[Docker Engine in Ubuntu](https://docs.docker.com/engine/install/ubuntu/) under WSL 2.

If WSL is new to the machine, install Ubuntu from PowerShell:

```powershell
wsl --install --distribution Ubuntu-24.04
```

Restart Windows if requested, finish Ubuntu's initial user setup, then install Docker
Engine inside Ubuntu using the linked instructions. Run this project's Compose commands
from its folder inside WSL. Windows drives are mounted under `/mnt/c`, `/mnt/d`, and so on.

```sh
docker version
docker compose up -d --build db mailpit api frontend
```

`docker version` should show both a client and a server. The Windows CLI can also connect
to an existing Linux engine through a Docker context. Installing the client alone does
not start a Linux engine.

On this PC, the `wsl-linux` context connects PowerShell to Docker Engine in `Ubuntu-24.04`.
It starts Ubuntu when a Docker command connects, using SSH over WSL's standard input and
output. A background WSL process keeps detached containers running after the command
exits. No Docker Desktop or network listener is needed for this connection.

```powershell
docker context use wsl-linux
docker version
docker compose up
```

If you see an error about `//./pipe/docker_engine`, check `docker context ls`. The default
Windows context points to a Windows engine; select `wsl-linux` to use the configured Linux
engine. The context and SSH keys belong to this Windows user and are stored outside the
project.

To stop the containers and release the WSL instance when finished:

```powershell
docker compose down
wsl --terminate Ubuntu-24.04
```

This stops all processes in that Ubuntu distribution. The next command using the
`wsl-linux` context starts the engine again.

## Open the projects

```text
CodeDuel/                        shared setup files and orchestration
├── CodeDuelBackend/             IntelliJ: open this folder and import pom.xml
├── CodeDuelFrontend/            WebStorm: open this folder and package.json
├── problems/                   trusted content/oracles, never frontend assets
├── worker/                     executor VM provisioning
├── deploy/                     production configuration and local rehearsal
└── docs/
```

In IntelliJ select JDK 21 for the project **and Maven runner**, then reload Maven. In WebStorm select Node 24 and the project TypeScript installation. Do not open the parent as the Java source project. Each subproject has its own build and IDE configuration.

## Quick fixture demonstration

Requires Docker with Compose v2 and about 4 GiB available for application containers. This mode recognizes exact supplied programs and does not execute arbitrary code.

```sh
docker compose up -d --build db mailpit api frontend
```

Open **http://localhost:5173** in two separate browser profiles. Log in as `nova` and `atlas`, both with password `CodeDuel-demo-2026!`. Choose **Challenge a friend**, open the invite in the other profile, and both choose **I’m ready**. Select wrong/accepted samples from **Demo program**, then run examples or submit. Ranked play updates Elo; private rooms and practice do not. Private rematches require the other player's consent.

| Service             | Local address                                                        |
| ------------------- | -------------------------------------------------------------------- |
| Application         | http://localhost:5173                                                |
| API / health        | http://localhost:8081/api/v1 / http://localhost:8081/actuator/health |
| PostgreSQL          | localhost:5444, database/user `codeduel`, password `codeduel_local`  |
| Captured email      | http://localhost:8026                                                |
| SMTP for a host API | localhost:1026                                                       |

Use the same hostname throughout a session. Host ports bind only to loopback. `docker compose down` preserves the named database volume. Do not remove volumes to switch modes.

## Real Java execution on this Mac

Requires macOS virtualization, Lima 2+, Docker/Colima, and approximately 3 GiB additional RAM for a dedicated execution VM. Install Lima with `brew install lima` if needed.

```sh
worker/sandbox/setup-local.sh
docker compose stop api worker
docker compose --env-file .env.judge.local --profile worker up -d --build
```

The setup creates `codeduel-executor` with **no host/home/project mounts**, downloads a checksum-pinned go-judge binary, installs Java 21, and writes private settings to `.env.judge.local`. It preserves an existing token and leaves your Docker context and other VMs alone. This environment file explicitly removes the demo profile. Known demo accounts remain only if previously seeded; otherwise register new accounts. There is no sample selector in real mode.

The worker in Docker reaches `http://host.lima.internal:5058` under Colima. A different Docker provider may require `host.docker.internal`; change the private `JUDGE_URL` only after checking that route. The host endpoint is `127.0.0.1:5058`. No executor token belongs in frontend variables.

A real submission is compiled in the execution VM, queued immutably, and checked against all private cases. `Run examples` uses public cases only. Compiler feedback and public stdout are bounded; private test data is never returned. See [the executor guide](../worker/sandbox/README.md) for exact limits and systemd operations.

To return to fixture mode, stop **both** application processes first:

```sh
docker compose --profile worker stop api worker
docker compose up -d --build db mailpit api frontend
```

Do not run a fixture worker and a real worker on the same database simultaneously.

## Develop in IntelliJ and WebStorm

Start infrastructure from the parent:

```sh
docker compose up -d db mailpit
docker compose --profile worker stop api worker frontend
```

From `CodeDuelBackend`:

```sh
./mvnw -B -ntp package -DskipTests
java -jar target/backend-0.1.0-SNAPSHOT.jar --spring.profiles.active=demo
```

Or run `dev.codeduel.CodeDuelApplication` in IntelliJ with active profile `demo`. The default host DB URL is `jdbc:postgresql://localhost:5444/codeduel`. Fixture judging runs in that demo API only. For real mode, remove `demo`, configure `JUDGE_MODE=GO_JUDGE`, the host `JUDGE_URL`, and the private auth token in your local run environment; start a second JVM with profile `worker`. Spring does not automatically load root dotenv files. Keep credentials out of shared IDE run configurations.

From `CodeDuelFrontend`:

```sh
npm ci
npm run dev
```

Vite keeps REST and SSE under `/api` and proxies to port 8081. Monaco and fonts are bundled locally. Avoid running Docker and host processes on the same ports. API restarts retain JDBC sessions; password resets revoke all of that account's sessions.

## Email, practice and administration

Registration queues a verification email. Use Mailpit to open its link and explicitly confirm verification; password recovery works the same way. All local email is captured rather than delivered externally. Production requires verified email before ranked queue entry; the basic local demo relaxes that requirement.

Open a challenge and choose **Practice this challenge**. Drafts expire after seven days and clear on sign-out. Submitted source is author-private and redacted after 90 days by default. Account settings support recovery and permanent anonymization with the current password; finish or forfeit an active match first. Match/rating history remains intact.

For the provisioned administrator and immutable draft → real reference check → publish flow, use the [HTTPS deployment rehearsal](deployment-guide.md). Its separate database has an `operator` account and a generated password file, not a public default admin password. Fixture results cannot authorize publication.

## Verify

On Windows, [testing locally](testing.md) shows how to run the same checks as GitHub
Actions, including the Docker Engine in WSL setup:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\check.ps1 -Wsl -Browser
```

From the shared parent, JDK 21 selected and Docker running:

```sh
./mvnw -B -ntp verify
python3 problems/verify_catalog.py
```

On Colima, Testcontainers may need its active endpoint:

```sh
export DOCKER_HOST="$(docker context inspect --format '{{.Endpoints.docker.Host}}')"
export TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock
./mvnw -B -ntp verify
```

The full real-executor wrapper resolves the Docker endpoint and reads the private token without printing it:

```sh
python3 worker/sandbox/verify-local.py
```

Without the configured executor, Maven skips only the optional real sandbox tests; PostgreSQL integration tests still require Docker and fail if unavailable. Trusted reference verification executes repository-owned programs, never submitted user source on the host.

From `CodeDuelFrontend`:

```sh
npm run format:check
npm run lint
npm test
npm run build
npm audit --audit-level=moderate
npx playwright install chromium
MAILPIT_URL=http://127.0.0.1:8026 E2E_BASE_URL=http://127.0.0.1:5173 npm run test:e2e
```

The normal duel tests require fixture mode. With real mode running, run only its dedicated journey:

```sh
REAL_JUDGE=true E2E_BASE_URL=http://127.0.0.1:5173 npx playwright test real-judge.spec.ts
```

To verify the administrator UI against the local HTTPS rehearsal, from `CodeDuelFrontend`:

```sh
E2E_LOCAL_TLS=true E2E_BASE_URL=https://localhost:8443 E2E_ADMIN_PASSWORD_FILE=../.secrets/deploy/bootstrap_password npx playwright test admin.spec.ts
```

The local TLS flag applies only to this browser rehearsal; the CLI verifier validates the extracted CA explicitly. The captured-email browser flow is enabled by `MAILPIT_URL`; real/admin journeys skip unless explicitly configured.

Browser tests create local accounts and persistent matches. Repeated registration runs may hit the deliberate eight-signups/hour/IP limit. Run them against local test data.

Formatting: `./mvnw -f CodeDuelBackend/pom.xml spotless:apply` from the parent, and `npm run format` from the frontend. After authoring new seed content, regenerate with `python3 problems/build_catalog.py` and verify the oracles. Never change an already-deployed published version; clone a new version instead.

## Troubleshooting

- IntelliJ unresolved dependencies: open `CodeDuelBackend/pom.xml` as Maven, reload, and check JDK 21. WebStorm should open only `CodeDuelFrontend`.
- 502 during startup: the API may still be migrating. Check `docker compose logs api`; Nginx re-resolves recreated API containers automatically.
- Login/CSRF: refresh the browser and use the frontend origin. Secure cookies require HTTPS; plain local HTTP uses `COOKIE_SECURE=false`.
- Real jobs pending: check `docker compose --env-file .env.judge.local --profile worker logs worker` and the executor systemd status. A worker outage is recoverable; repeated infrastructure failures cancel the battle without Elo.
- Ranked pairing: keep both lobbies open, allow rating windows to grow, and remember the ten-minute recent-opponent cooldown. The UI does not invent wait estimates.
- Shared laptop memory: run the normal stack or the HTTPS rehearsal one at a time. They use separate persistent databases. Never restart an unrelated Docker VM to free resources.

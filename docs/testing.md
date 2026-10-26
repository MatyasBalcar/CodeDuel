# Testing before pushing

Run these checks from the folder containing `CodeDuelBackend` and `CodeDuelFrontend`.
They follow [.github/workflows/ci.yml](../.github/workflows/ci.yml). A successful Docker
build alone does not run the database tests, reference checks, or browser journeys.

On your Windows setup with Docker Engine in Ubuntu WSL, use:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\check.ps1 -Wsl -Browser
```

This uses Node 24 on Windows and Java 21 in Linux for the backend and references.
PostgreSQL integration tests create disposable containers. They do not use your app's
database. The browser tests build a separate demo on port 5175 with a fresh database,
create test accounts and matches, then remove their test containers and database.
Your regular app on port 5173 stays separate. The script stops at the first failed check.
Docker Desktop is not required.

The WSL option expects the [configured Ubuntu Docker Engine](local-run-guide.md#windows-with-docker-cli),
Python 3 in Ubuntu, and the Windows Docker CLI/Compose. It uses `Ubuntu-24.04` by default;
pass `-Distro YourDistro` if needed. It reuses the pinned Maven/Java 21 image and caches
Maven downloads. It prepares Java 21 reference tools in `/opt/codeduel-java21` if missing.

Leave off `-Browser` to run the checks before the browser stage:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\check.ps1 -Wsl
```

Run just the part you are fixing for quicker feedback:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\check.ps1 -Wsl -Only backend
powershell -ExecutionPolicy Bypass -File .\scripts\check.ps1 -Wsl -Only problems
powershell -ExecutionPolicy Bypass -File .\scripts\check.ps1 -Only frontend
powershell -ExecutionPolicy Bypass -File .\scripts\check.ps1 -Wsl -Only browser
```

If you use a native Windows JDK 21 and a Docker endpoint reachable by Testcontainers,
omit `-Wsl`. The WSL option handles the SSH-based Docker CLI setup used on this PC;
a working CLI context alone does not configure the Java Docker client on Windows.

## What runs

| Check | Same command used by CI |
| --- | --- |
| Backend format, unit and PostgreSQL tests | `./mvnw -B -ntp verify` |
| All trusted problem references | `python3 problems/verify_catalog.py` |
| Clean frontend dependency install | `npm ci` |
| Frontend formatting | `npm run format:check` |
| Frontend lint | `npm run lint` |
| Frontend unit tests | `npm test` |
| TypeScript and production bundle | `npm run build` |
| Dependency advisory check | `npm audit --audit-level=moderate` |
| Browser tests against the Docker demo | `npm run test:e2e` |

Use JDK 21 and Node 24 to match CI. Java 17 cannot compile this backend. The browser
stage uses Chromium, `E2E_BASE_URL=http://127.0.0.1:5175`, and
`MAILPIT_URL=http://127.0.0.1:8027`. Both the workflow and local script use
`docker-compose.test.yml`, wait for the seed accounts, and clean up their own test data.
On Windows, Playwright installs Chromium without
the Linux system dependency installation used on GitHub's Ubuntu runner.

The browser journeys check sessions, saved drafts, verdicts, winners, rating changes,
and replay data. They do not require specific heading or success-message wording.

Real-executor and administrator browser checks require their extra environment and
are skipped in ordinary CI. See the [local run guide](local-run-guide.md#verify) for
those checks. Ordinary CI verifies the fixture demo and PostgreSQL behavior; it does
not claim to test a separately provisioned Java sandbox.

## Fixing a failed check

For formatting failures, add `-Fix`. This applies Spotless and Prettier before running
the selected checks:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\check.ps1 -Wsl -Fix
```

You can also apply just one formatter. From the project root with JDK 21 selected:

```powershell
.\mvnw.cmd -f CodeDuelBackend/pom.xml spotless:apply
```

From `CodeDuelFrontend`:

```powershell
npm.cmd run format
```

For other failures, use the first error and its file/line, fix that issue, and rerun
the affected check. Maven reports list the failing test and assertion; frontend lint
and TypeScript report the source location. Do not skip failing checks to make a push
green. After the affected check passes, run the full command before pushing.

| Failure | Where to look |
| --- | --- |
| Maven unit tests | `CodeDuelBackend/target/surefire-reports` |
| PostgreSQL integration tests | `CodeDuelBackend/target/failsafe-reports` |
| Reference mismatch | Console prints the problem, version, and case number |
| Browser journey | `CodeDuelFrontend/playwright-report` and `test-results` |
| Demo startup | Console logs from the temporary test stack |

Open a browser report with `npx playwright show-report` from `CodeDuelFrontend`.
GitHub uploads these reports and `compose.log` in the `failure-reports` artifact when
a workflow fails. Compare the failing step with its local command above.

If the error is from Docker connecting to `docker_engine`, run `docker info` and
check your Docker context first. If it is from Testcontainers while the CLI works,
use `-Wsl` for the backend check. You do not need to install PostgreSQL for these tests.

Problem edits also need regeneration before verification:

```powershell
python problems/build_catalog.py
```

Keep published v1 content unchanged. New function versions belong in `functions.json`;
the generator and verifier cover both contracts.

# CodeDuel backend

The Spring Boot API handles accounts, matchmaking, battles, ratings, and judge jobs.
PostgreSQL stores application data and sessions. Submitted programs run in a separate
execution environment.

Open this folder in IntelliJ, import `pom.xml`, and select **JDK 21** for both the project
and Maven runner. You can use the included Maven wrapper instead of installing Maven.

## Run locally on Windows

From the parent `CodeDuel` folder, run setup once:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\windows.ps1 setup
```

Then start the backend:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\windows.ps1 backend
```

The API runs at **http://localhost:8081**. The script starts the project's PostgreSQL and
Mailpit processes and selects the `demo` profile. This profile accepts supplied sample
programs without executing arbitrary source code.

To run from IntelliJ, select the JDK path printed by setup and use the `demo` profile.
Run the Windows script's `services` action first so the database and captured email are
available. Start the frontend separately from `../CodeDuelFrontend`.

For Docker, macOS, and real Java execution, follow the [local run guide](../docs/local-run-guide.md).

## Tests

From this folder:

```powershell
.\mvnw.cmd verify
```

Integration tests use Testcontainers and need a running Docker engine. On macOS/Linux,
use `./mvnw verify`. Real execution tests also need the separate judge environment.

See the [API reference](../docs/api.md), [database notes](../docs/database.md), and
[deployment guide](../docs/deployment-guide.md) for more detail.

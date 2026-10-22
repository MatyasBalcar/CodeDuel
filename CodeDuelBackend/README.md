# CodeDuelBackend

Open **this folder** in IntelliJ IDEA, import `pom.xml` as a Maven project, and select **JDK 21** as the project and Maven runner SDK. The frontend is a separate WebStorm project at `../CodeDuelFrontend`; both share the Git repository in the parent directory.

```sh
./mvnw verify
```

PostgreSQL integration tests require a running Docker engine. For Colima, resolve the active context into `DOCKER_HOST` as described in the [local run guide](../docs/local-run-guide.md). The project's Maven wrapper is self-contained; Node is not required to build the backend.

Start the database and local SMTP capture from the shared parent, then run `dev.codeduel.CodeDuelApplication` with the `demo` Spring profile for exact-fixture mode. The API uses port **8081**. For real Java execution, follow the separate executor/worker setup in the guide; submitted programs never execute in this JVM.

- [Local run guide](../docs/local-run-guide.md)
- [Deployment guide](../docs/deployment-guide.md)
- [Architecture](../docs/architecture.md)
- [API](../docs/api.md)

Business packages and migrations live in `src/`. Shared problem authoring, worker provisioning, deployment, and documentation remain in the repository's parent directory.

# Judge orchestration process

The worker reuses the `CodeDuelBackend` image with profile `worker`; it has no HTTP server and does not compile or execute submitted source in its JVM. It claims/checkpoints jobs in short database transactions and calls the isolated execution service outside those transactions.

```sh
java -jar CodeDuelBackend/target/backend-0.1.0-SNAPSHOT.jar --spring.profiles.active=worker
```

Supply the private DB and `JUDGE_MODE=GO_JUDGE` / `JUDGE_URL` / `JUDGE_AUTH_TOKEN` configuration. Production mounts separate DB/executor secrets into the worker and gives its DB role no access to accounts, sessions or mail. The API does not receive executor credentials.

Provision execution with [sandbox instructions](sandbox/README.md). This is a separate Linux VM with no application/data-zone access. The alternative Judge0 adapter is available for a secured Java 21 installation. The explicit demo profile embeds only an exact-fixture orchestrator.

See the separate [local guide](../docs/local-run-guide.md) and [deployment guide](../docs/deployment-guide.md).

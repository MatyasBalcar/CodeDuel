# CodeDuelFrontend

Open **this folder** in WebStorm. Select a **Node 24** interpreter, use the project TypeScript installation, and run the `dev` script from `package.json`. Java and Maven are not required to build the frontend.

```sh
npm ci
npm run dev
```

Open **http://localhost:5173**. Vite proxies `/api` to the backend at `http://127.0.0.1:8081`, preserving same-origin sessions and CSRF. Start the IntelliJ backend separately or use the shared Docker setup in the parent directory. Stop the Docker frontend before running Vite on the same port.

```sh
npm run format:check
npm run lint
npm test
npm run build
```

Browser tests use the running backend. `E2E_BASE_URL` selects an existing frontend; otherwise Playwright starts Vite. The optional real-judge journey reads trusted test source from the sibling backend, and needs a separately provisioned execution VM.

```sh
npx playwright install chromium
E2E_BASE_URL=http://127.0.0.1:5173 npm run test:e2e
```

`node_modules`, `dist`, browser reports and test output are excluded from project indexing. Both IDE projects share the Git repository one directory above.

- [Local run guide](../docs/local-run-guide.md)
- [Deployment guide](../docs/deployment-guide.md)

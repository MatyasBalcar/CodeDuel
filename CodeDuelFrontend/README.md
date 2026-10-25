# CodeDuel frontend

The React and TypeScript app for CodeDuel. It uses Vite for local development and Monaco
for the Java editor. Open this folder in WebStorm and select **Node.js 24**.

## Run locally

On Windows, the parent folder has a [setup script](../docs/local-run-guide.md#windows-without-docker)
that prepares the backend services and installs frontend dependencies. Once setup is done,
you can use its `frontend` action or run these commands from this folder:

```sh
npm ci
npm run dev
```

Open **http://localhost:5173**. Start the backend separately on port **8081**. Vite forwards
`/api` requests to it, so login and CSRF requests use the same origin as the frontend.
If the Docker frontend is running, stop it before starting Vite on the same port.

## Checks

```sh
npm run format:check
npm run lint
npm test
```

To create a production bundle, run `npm run build`.

## Browser tests

Start the backend in demo mode, then run:

```sh
npx playwright install chromium
npm run test:e2e
```

Playwright starts Vite unless `E2E_BASE_URL` points to a running frontend. Email recovery,
real judge, and administrator tests need additional services or credentials; the
[local run guide](../docs/local-run-guide.md#verify) lists those options.

The [deployment guide](../docs/deployment-guide.md) covers serving the frontend with the
rest of the application.

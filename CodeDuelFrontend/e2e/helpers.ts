import type { BrowserContext } from '@playwright/test'

/** Release test-created queues/battles even when an assertion fails halfway through a duel. */
export async function leaveCompetition(context: BrowserContext) {
  try {
    const csrfResponse = await context.request.get('/api/v1/auth/csrf')
    if (!csrfResponse.ok()) return
    const csrf = (await csrfResponse.json()) as { headerName: string; token: string }
    const headers = { [csrf.headerName]: csrf.token }
    const response = await context.request.delete('/api/v1/matchmaking/queue', { headers })
    if (!response.ok()) return
    const state = (await response.json()) as { battleId?: string }
    if (state.battleId)
      await context.request.post(`/api/v1/battles/${state.battleId}/forfeit`, { headers })
  } catch {
    // Cleanup must not hide the original browser failure when the server/context is unavailable.
  }
}

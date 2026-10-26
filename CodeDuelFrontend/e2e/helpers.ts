import { expect, type BrowserContext, type Page } from '@playwright/test'
import type { Battle, Verdict } from '../src/api/types'

export async function expectSignedIn(page: Page, username: string) {
  await expect
    .poll(async () => {
      const response = await page.request.get('/api/v1/auth/me')
      return response.ok() ? (await response.json()).username : null
    })
    .toBe(username)
}

export async function expectWinner(
  page: Page,
  battleId: string,
  username: string,
  timeout = 30_000,
) {
  await expect(page.locator('.result-card')).toBeVisible({ timeout })
  const result = (await (await page.request.get(`/api/v1/battles/${battleId}`)).json()) as Battle
  expect(result.status).toBe('FINISHED')
  expect(result.players.find((player) => player.userId === result.winnerId)?.username).toBe(
    username,
  )
  return result
}

export async function expectVerdict(
  page: Page,
  battleId: string,
  official: boolean,
  verdict: Verdict,
) {
  await expect
    .poll(
      async () => {
        const battle = (await (
          await page.request.get(`/api/v1/battles/${battleId}`)
        ).json()) as Battle
        return battle.submissions.find((submission) => submission.official === official)?.verdict
      },
      { timeout: 30_000 },
    )
    .toBe(verdict)
}

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
    return
  }
}

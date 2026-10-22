import { test, expect, type Page } from '@playwright/test'
import { leaveCompetition } from './helpers'

const password = 'browser-test-password-2026'

async function register(page: Page, name: string) {
  await page.goto('/register')
  await page.getByLabel('Username', { exact: true }).fill(name)
  await page.getByLabel('Email', { exact: true }).fill(`${name}@example.invalid`)
  await page.getByLabel('Password', { exact: true }).fill(password)
  await page.getByRole('button', { name: 'Create account', exact: true }).click()
  await expect(page.getByRole('heading', { name: `Good to see you, ${name}.` })).toBeVisible()
}

async function readyBoth(first: Page, second: Page) {
  await expect(first.getByRole('button', { name: 'I’m ready' })).toBeVisible()
  await expect(second.getByRole('button', { name: 'I’m ready' })).toBeVisible()
  await Promise.all([
    first.getByRole('button', { name: 'I’m ready' }).click(),
    second.getByRole('button', { name: 'I’m ready' }).click(),
  ])
  await expect(first.getByLabel('Demo program')).toBeVisible()
  await expect(second.getByLabel('Demo program')).toBeVisible()
}

test('private duel: registration, shared problem, wrong answer, draft recovery, accepted result, replay', async ({
  browser,
}) => {
  const a = await browser.newContext()
  const b = await browser.newContext()
  const first = await a.newPage()
  const second = await b.newPage()
  const suffix = Date.now().toString(36)
  try {
    await register(first, `p${suffix}a`)
    await register(second, `p${suffix}b`)
    await first.getByRole('button', { name: 'Challenge a friend', exact: true }).click()
    await expect(first).toHaveURL(/\/arena\//)
    const invite = await first.getByLabel('PRIVATE INVITE LINK').inputValue()
    await second.goto(invite)
    await second.getByRole('button', { name: 'Join private duel', exact: true }).click()
    await readyBoth(first, second)
    const title = await first.locator('.problem-statement h1').textContent()
    await expect(second.locator('.problem-statement h1')).toHaveText(title!)
    const id = first.url().split('/').at(-1)!
    const own = await (await a.request.get(`/api/v1/battles/${id}`)).json()
    const rival = await (await b.request.get(`/api/v1/battles/${id}`)).json()
    expect(own.startsAt).toBe(rival.startsAt)
    expect(own.endsAt).toBe(rival.endsAt)
    expect(own.problem.versionId).toBe(rival.problem.versionId)

    await first.getByLabel('Demo program').selectOption('wrong')
    await first.getByRole('button', { name: 'Run examples', exact: true }).click()
    await expect(first.locator('.submission-row').first()).toContainText('wrong answer')
    await first.getByRole('button', { name: 'Submit solution', exact: true }).click()
    await expect(
      first.locator('.submission-row').filter({ hasText: 'Official submission' }).first(),
    ).toContainText('wrong answer')
    await expect(first.locator('.editor-status')).toContainText('Draft saved locally')
    await first.reload()
    await expect(first.locator('.monaco-editor')).toContainText('demo-wrong')
    await expect(second.locator('.problem-statement h1')).toHaveText(title!)
    const snapshot = await (await a.request.get(`/api/v1/battles/${id}`)).json()
    expect(
      (await b.request.get(`/api/v1/submissions/${snapshot.submissions[0].id}`)).status(),
    ).toBe(404)

    await first.getByLabel('Demo program').selectOption('accepted')
    await first.getByRole('button', { name: 'Submit solution', exact: true }).click()
    await expect(first.getByRole('heading', { name: 'That’s your win.' })).toBeVisible()
    await expect(second.getByRole('heading', { name: 'A lesson for the next one.' })).toBeVisible()
    await expect(first.getByText('UNRANKED · NO RATING CHANGE')).toBeVisible()
    await expect(first.getByRole('heading', { name: 'Battle replay' })).toBeVisible()
    await expect(first.getByText('Final result recorded')).toBeVisible()
    await first.reload()
    await expect(first.getByRole('heading', { name: 'That’s your win.' })).toBeVisible()
    await expect(first.getByText('Final result recorded')).toBeVisible()
    await first.getByRole('button', { name: 'Private rematch', exact: true }).click()
    await expect(first.getByLabel('PRIVATE INVITE LINK')).toBeVisible()
    await second.getByRole('button', { name: 'Accept rematch', exact: true }).click()
    await expect(first.getByRole('button', { name: 'I’m ready' })).toBeVisible()
    await expect(second.getByRole('button', { name: 'I’m ready' })).toBeVisible()
    const rematch = first.url().split('/').at(-1)!
    const csrf = await (await a.request.get('/api/v1/auth/csrf')).json()
    await a.request.post(`/api/v1/battles/${rematch}/forfeit`, {
      headers: { [csrf.headerName]: csrf.token },
    })
  } finally {
    await leaveCompetition(a)
    await leaveCompetition(b)
    await a.close()
    await b.close()
  }
})

test('ranked duel: queue, judge completion, durable Elo, history and leaderboard', async ({
  browser,
}) => {
  const a = await browser.newContext()
  const b = await browser.newContext()
  const first = await a.newPage()
  const second = await b.newPage()
  const suffix = Date.now().toString(36)
  const name = `r${suffix}a`
  try {
    await register(first, name)
    await register(second, `r${suffix}b`)
    await first.getByRole('button', { name: 'Play ranked', exact: true }).click()
    await second.getByRole('button', { name: 'Play ranked', exact: true }).click()
    await first.getByRole('link', { name: /Your battle is ready/ }).click()
    await second.getByRole('link', { name: /Your battle is ready/ }).click()
    await readyBoth(first, second)
    await first.getByLabel('Demo program').selectOption('accepted')
    await first.getByRole('button', { name: 'Submit solution', exact: true }).click()
    await expect(first.getByRole('heading', { name: 'That’s your win.' })).toBeVisible()
    await expect(first.getByText('+20 Elo', { exact: true })).toBeVisible()
    await expect(second.getByText('-20 Elo', { exact: true })).toBeVisible()
    const id = first.url().split('/').at(-1)!
    await first.reload()
    await expect(first.getByText('+20 Elo', { exact: true })).toBeVisible()
    const result = await (await a.request.get(`/api/v1/battles/${id}`)).json()
    expect(
      result.players.find((player: { username: string }) => player.username === name).newRating,
    ).toBe(1220)
    await first.getByRole('link', { name: 'Your profile', exact: true }).click()
    await expect(first.locator('.profile-rating')).toContainText('1220')
    await expect(first.locator('.history-row').first()).toContainText('Win')
    await first.getByRole('link', { name: 'Leaderboard', exact: true }).click()
    await expect(first.locator('.leaderboard-table-row').filter({ hasText: name })).toContainText(
      '1220',
    )
  } finally {
    await leaveCompetition(a)
    await leaveCompetition(b)
    await a.close()
    await b.close()
  }
})

test('library and mobile lobby stay readable without horizontal overflow', async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 844 })
  await page.goto('/')
  await expect(page.getByRole('heading', { name: 'Welcome to your next level.' })).toBeVisible()
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true)
  await page.getByRole('link', { name: 'Challenges', exact: true }).click()
  await page.getByRole('textbox', { name: 'Search challenges' }).fill('signal')
  await expect(page.getByRole('link', { name: /Signal Window/ })).toBeVisible()
  await expect(page.getByRole('link', { name: /Balanced Relay/ })).toHaveCount(0)
})

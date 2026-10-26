import { test, expect, type Page } from '@playwright/test'
import { expectSignedIn, expectVerdict, expectWinner, leaveCompetition } from './helpers'

const password = 'browser-test-password-2026'

async function register(page: Page, name: string) {
  await page.goto('/register')
  await page.getByLabel('Username', { exact: true }).fill(name)
  await page.getByLabel('Email', { exact: true }).fill(`${name}@example.invalid`)
  await page.getByLabel('Password', { exact: true }).fill(password)
  await page.getByRole('button', { name: 'Create account', exact: true }).click()
  await expectSignedIn(page, name)
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
    const fixtures = (await (
      await a.request.get(`/api/v1/demo/battles/${id}/fixtures`)
    ).json()) as { acceptedSource: string }

    await first.getByLabel('Demo program').selectOption('wrong')
    await first.getByRole('button', { name: 'Run examples', exact: true }).click()
    await expectVerdict(first, id, false, 'WRONG_ANSWER')
    await first.getByRole('button', { name: 'Submit solution', exact: true }).click()
    await expectVerdict(first, id, true, 'WRONG_ANSWER')
    await first.getByLabel('Demo program').selectOption('accepted')
    await expect
      .poll(() =>
        first.evaluate(
          (source) =>
            Object.keys(localStorage)
              .filter((key) => key.startsWith('codeduel:draft:'))
              .some((key) => JSON.parse(localStorage.getItem(key) ?? 'null')?.code === source),
          fixtures.acceptedSource,
        ),
      )
      .toBe(true)
    await first.reload()
    const firstBodyLine = fixtures.acceptedSource
      .split('\n')
      .find((line) => line.startsWith('    ') && line.trim())!
      .trim()
    await expect(first.locator('.monaco-editor')).toContainText(firstBodyLine)
    await expect(second.locator('.problem-statement h1')).toHaveText(title!)
    const snapshot = await (await a.request.get(`/api/v1/battles/${id}`)).json()
    expect(
      (await b.request.get(`/api/v1/submissions/${snapshot.submissions[0].id}`)).status(),
    ).toBe(404)

    await first.getByLabel('Demo program').selectOption('accepted')
    await first.getByRole('button', { name: 'Submit solution', exact: true }).click()
    const result = await expectWinner(first, id, `p${suffix}a`)
    expect(result.mode).toBe('PRIVATE')
    expect(result.players.every((player) => player.delta === null)).toBe(true)
    await expect(second.locator('.result-card')).toBeVisible()
    await expect(first.locator('.replay-panel .timeline li').first()).toBeVisible()
    await first.reload()
    await expectWinner(first, id, `p${suffix}a`)
    await expect(first.locator('.replay-panel .timeline li').first()).toBeVisible()
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
    const id = first.url().split('/').at(-1)!
    const result = await expectWinner(first, id, name)
    expect(result.players.map((player) => player.delta)).toEqual(expect.arrayContaining([-20, 20]))
    await expect(second.locator('.result-card')).toBeVisible()
    await first.reload()
    await expectWinner(first, id, name)
    expect(result.players.find((player) => player.username === name)?.newRating).toBe(1220)
    await first.getByRole('link', { name: 'Your profile', exact: true }).click()
    await expect(first.locator('.profile-rating')).toContainText('1220')
    await expect(first.locator('.history-row').first()).toHaveAttribute('href', `/arena/${id}`)
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
  await expect(page.getByRole('heading', { level: 1 })).toBeVisible()
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true)
  await page.getByRole('link', { name: 'Challenges', exact: true }).click()
  await page.getByRole('textbox', { name: 'Search challenges' }).fill('signal')
  await expect(page.getByRole('link', { name: /Signal Window/ })).toBeVisible()
  await expect(page.getByRole('link', { name: /Balanced Relay/ })).toHaveCount(0)
})

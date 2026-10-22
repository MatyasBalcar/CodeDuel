import { test, expect } from '@playwright/test'
import { readFileSync } from 'node:fs'
import { leaveCompetition } from './helpers'

test('real Java duel: compiler feedback, hidden verdict and persisted replay', async ({
  browser,
}) => {
  test.skip(process.env.REAL_JUDGE !== 'true', 'Requires the isolated Java execution VM')
  test.setTimeout(180_000)
  const a = await browser.newContext()
  const b = await browser.newContext()
  const first = await a.newPage()
  const second = await b.newPage()
  try {
    for (const [page, username] of [
      [first, process.env.DUEL_USER_1 ?? 'nova'],
      [second, process.env.DUEL_USER_2 ?? 'atlas'],
    ] as const) {
      await page.goto('/login')
      await page.getByLabel('Username', { exact: true }).fill(username)
      await page
        .getByLabel('Password', { exact: true })
        .fill(process.env.DUEL_PASSWORD ?? 'CodeDuel-demo-2026!')
      await page.getByRole('button', { name: 'Sign in', exact: true }).click()
      await expect(
        page.getByRole('heading', { name: `Good to see you, ${username}.` }),
      ).toBeVisible()
    }
    await first.getByRole('button', { name: 'Challenge a friend', exact: true }).click()
    const invite = await first.getByLabel('PRIVATE INVITE LINK').inputValue()
    await second.goto(invite)
    await second.getByRole('button', { name: 'Join private duel', exact: true }).click()
    await first.getByRole('button', { name: 'I’m ready' }).click()
    await second.getByRole('button', { name: 'I’m ready' }).click()
    await expect(first.locator('.monaco-editor')).toBeVisible()
    const id = first.url().split('/').at(-1)!
    const snapshot = await (await a.request.get(`/api/v1/battles/${id}`)).json()
    const catalog = JSON.parse(
      readFileSync(
        new URL('../../CodeDuelBackend/src/main/resources/problems/catalog.json', import.meta.url),
        'utf8',
      ),
    ) as { slug: string; referenceSource: string }[]
    const source = catalog.find((p) => p.slug === snapshot.problem.slug)!.referenceSource
    // Monaco's native EditContext also has a hidden read-only IME textarea.
    // Focus the accessible editor itself so keyboard input changes the source model.
    const editor = first.getByRole('textbox', { name: /Java solution editor/ })
    const selectAll = (await first.evaluate(() => navigator.userAgent.includes('Macintosh')))
      ? 'Meta+A'
      : 'Control+A'
    await editor.focus()
    await first.keyboard.press(selectAll)
    await first.keyboard.insertText('public class Main { this does not compile }')
    await first.getByRole('button', { name: 'Run examples', exact: true }).click()
    await expect(first.locator('.submission-row').first()).toContainText('compilation error', {
      timeout: 30_000,
    })
    await first.locator('.submission-row').first().click()
    await expect(first.getByRole('heading', { name: 'Compiler feedback' })).toBeVisible()
    await expect(first.locator('.submission-feedback pre').first()).toContainText('Main.java')
    await editor.focus()
    await first.keyboard.press(selectAll)
    await first.keyboard.insertText(source)
    const [sent] = await Promise.all([
      first.waitForRequest(
        (request) =>
          request.method() === 'POST' && request.url().endsWith(`/battles/${id}/submissions`),
      ),
      first.getByRole('button', { name: 'Submit solution', exact: true }).click(),
    ])
    expect(sent.postDataJSON().sourceCode).toBe(source)
    await expect(first.getByRole('heading', { name: 'That’s your win.' })).toBeVisible({
      timeout: 90_000,
    })
    await expect(second.getByRole('heading', { name: 'A lesson for the next one.' })).toBeVisible()
    await first.reload()
    await expect(first.getByRole('heading', { name: 'Battle replay' })).toBeVisible()
    await expect(first.getByText('Final result recorded')).toBeVisible()
  } finally {
    await leaveCompetition(a)
    await leaveCompetition(b)
    await a.close()
    await b.close()
  }
})

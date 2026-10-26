import { expect, test } from '@playwright/test'
import { readFileSync } from 'node:fs'

test('administrator authors, verifies, publishes and retires an immutable challenge', async ({
  page,
}) => {
  test.skip(
    !process.env.E2E_ADMIN_PASSWORD_FILE,
    'Requires the local release-rehearsal administrator',
  )
  test.setTimeout(120_000)
  const password = readFileSync(process.env.E2E_ADMIN_PASSWORD_FILE!, 'utf8').trim()
  await page.goto('/login')
  await page
    .getByLabel('Username', { exact: true })
    .fill(process.env.E2E_ADMIN_USERNAME ?? 'operator')
  await page.getByLabel('Password', { exact: true }).fill(password)
  await page.getByRole('button', { name: 'Sign in', exact: true }).click()
  await page.getByRole('link', { name: 'Administration', exact: true }).click()
  await page.getByRole('button', { name: 'New problem', exact: true }).click()
  const slug = `browser-beacon-${Date.now().toString(36)}`
  const fields = {
    'URL slug': slug,
    Title: 'Browser Beacon',
    Statement: 'Return twice the numeric signal.',
    Constraints: '-1000000 <= signal <= 1000000',
    Arguments: 'signal is the reading to double.',
    'Return value': 'Twice the supplied signal.',
    'Java starter': 'public class Solution { public long solve(long signal) { return 0; } }',
    'Private reference solution':
      'public class Solution { public long solve(long signal) { return 2 * signal; } }',
  }
  for (const [label, value] of Object.entries(fields))
    await page.getByRole('textbox', { name: label, exact: true }).fill(value)
  await page.getByLabel('Return type', { exact: true }).selectOption('long')
  await page.getByLabel('Parameter name', { exact: true }).fill('signal')
  await page.getByLabel('Parameter type', { exact: true }).selectOption('long')
  await page
    .getByRole('textbox', { name: 'Arguments (JSON object)', exact: true })
    .nth(0)
    .fill('{"signal":7}')
  await page
    .getByRole('textbox', { name: 'Expected return value (JSON)', exact: true })
    .nth(0)
    .fill('14')
  await page
    .getByRole('textbox', { name: 'Arguments (JSON object)', exact: true })
    .nth(1)
    .fill('{"signal":-9}')
  await page
    .getByRole('textbox', { name: 'Expected return value (JSON)', exact: true })
    .nth(1)
    .fill('-18')
  await page.getByRole('button', { name: 'Save draft', exact: true }).click()
  await page.getByRole('button', { name: 'Freeze & verify saved draft', exact: true }).click()
  await expect(
    page.getByRole('button', { name: 'Publish verified version', exact: true }),
  ).toBeEnabled({ timeout: 60_000 })
  await expect(
    page.getByRole('textbox', { name: 'Private reference solution', exact: true }),
  ).toBeDisabled()
  await page.getByRole('button', { name: 'Publish verified version', exact: true }).click()
  const published = await page.request.get(`/api/v1/problems/${slug}`)
  expect(published.status()).toBe(200)
  expect(JSON.stringify(await published.json())).not.toContain('referenceSource')
  await page.getByRole('button', { name: 'Retire problem', exact: true }).click()
  await expect
    .poll(async () => (await page.request.get(`/api/v1/problems/${slug}`)).status())
    .toBe(404)
})

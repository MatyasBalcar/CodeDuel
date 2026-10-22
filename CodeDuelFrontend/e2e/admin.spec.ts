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
    Statement: 'A beacon repeats twice its numeric signal. Print twice the supplied integer.',
    Constraints: '-1000000 <= signal <= 1000000',
    'Input format': 'One integer signal.',
    'Output format': 'Twice the supplied signal.',
    'Java starter': 'public class Main { public static void main(String[] args) {} }',
    'Private reference solution':
      'public class Main { public static void main(String[] args) { var in = new java.util.Scanner(System.in); System.out.println(2 * in.nextLong()); } }',
  }
  for (const [label, value] of Object.entries(fields))
    await page.getByRole('textbox', { name: label, exact: true }).fill(value)
  await page.getByRole('textbox', { name: 'Input', exact: true }).nth(0).fill('7\n')
  await page.getByRole('textbox', { name: 'Expected output', exact: true }).nth(0).fill('14\n')
  await page.getByRole('textbox', { name: 'Input', exact: true }).nth(1).fill('-9\n')
  await page.getByRole('textbox', { name: 'Expected output', exact: true }).nth(1).fill('-18\n')
  await page.getByRole('button', { name: 'Save draft', exact: true }).click()
  await expect(page.getByRole('heading', { name: 'Browser Beacon · v1' })).toBeVisible()
  await page.getByRole('button', { name: 'Freeze & verify saved draft', exact: true }).click()
  await expect(
    page.getByRole('button', { name: 'Publish verified version', exact: true }),
  ).toBeEnabled({ timeout: 60_000 })
  await expect(
    page.getByRole('textbox', { name: 'Private reference solution', exact: true }),
  ).toBeDisabled()
  await page.getByRole('button', { name: 'Publish verified version', exact: true }).click()
  await expect(
    page.locator('.section-heading').getByText('PUBLISHED', { exact: true }),
  ).toBeVisible()
  const published = await page.request.get(`/api/v1/problems/${slug}`)
  expect(published.status()).toBe(200)
  expect(JSON.stringify(await published.json())).not.toContain('referenceSource')
  await page.getByRole('button', { name: 'Retire problem', exact: true }).click()
  await expect
    .poll(async () => (await page.request.get(`/api/v1/problems/${slug}`)).status())
    .toBe(404)
})

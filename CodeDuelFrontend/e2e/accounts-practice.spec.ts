import { expect, test, type APIRequestContext } from '@playwright/test'

async function tokenFromMail(request: APIRequestContext, email: string, subject: string) {
  let token = ''
  await expect
    .poll(
      async () => {
        const response = await request.get(`${process.env.MAILPIT_URL}/api/v1/messages`)
        const data = (await response.json()) as {
          messages: { ID: string; Subject: string; To: { Address: string }[] }[]
        }
        const message = data.messages.find(
          (mail) => mail.Subject === subject && mail.To.some((person) => person.Address === email),
        )
        if (!message) return false
        const detail = await request.get(`${process.env.MAILPIT_URL}/api/v1/message/${message.ID}`)
        const text = ((await detail.json()) as { Text: string }).Text
        token = /#token=([A-Za-z0-9_-]{43})/.exec(text)?.[1] ?? ''
        return token.length === 43
      },
      { timeout: 30_000 },
    )
    .toBe(true)
  return token
}

test('email verification, practice, password recovery and account deletion', async ({
  page,
  browser,
  request,
}) => {
  test.skip(!process.env.MAILPIT_URL, 'Requires the local captured-email service')
  test.setTimeout(120_000)
  const name = `account${Date.now().toString(36)}`
  const email = `${name}@example.invalid`
  const password = 'browser-account-password-2026'
  await page.goto('/register')
  await page.getByLabel('Username', { exact: true }).fill(name)
  await page.getByLabel('Email', { exact: true }).fill(email)
  await page.getByLabel('Password', { exact: true }).fill(password)
  await page.getByRole('button', { name: 'Create account', exact: true }).click()
  await expect(page.getByRole('heading', { name: `Good to see you, ${name}.` })).toBeVisible()
  const verify = await tokenFromMail(request, email, 'Verify your CodeDuel email')
  await page.goto(`/verify-email#token=${verify}`)
  await page.getByRole('button', { name: 'Verify email', exact: true }).click()
  await expect(page.getByRole('status')).toContainText('Email verified.')
  expect(new URL(page.url()).hash).toBe('')

  await page.goto('/practice/balanced-relay')
  await expect(page.locator('.monaco-editor')).toBeVisible()
  await page.getByLabel('Demo program').selectOption('accepted')
  await page.getByRole('button', { name: 'Submit solution', exact: true }).click()
  await expect(page.locator('.submission-row').first()).toContainText('accepted')
  await page.reload()
  await expect(page.locator('.submission-row').first()).toContainText('accepted')
  const profile = await (await page.request.get('/api/v1/auth/me')).json()
  expect(profile.rankedGames).toBe(0)

  const recovery = await browser.newContext()
  try {
    const guest = await recovery.newPage()
    await guest.goto('/forgot-password')
    await guest.getByLabel('Email address', { exact: true }).fill(email)
    await guest.getByRole('button', { name: 'Email a reset link', exact: true }).click()
    await expect(guest.getByRole('status')).toContainText('If this address belongs to an account')
    const reset = await tokenFromMail(request, email, 'Reset your CodeDuel password')
    await guest.goto(`/reset-password#token=${reset}`)
    const replacement = 'replacement-browser-password-2026'
    await guest.getByLabel('New password', { exact: true }).fill(replacement)
    await guest.getByLabel('Confirm new password', { exact: true }).fill(replacement)
    await guest.getByRole('button', { name: 'Update password', exact: true }).click()
    await expect(guest.getByRole('status')).toContainText('Password updated.')
    expect((await page.request.get('/api/v1/auth/me')).status()).toBe(401)
    await guest.goto('/login')
    await guest.getByLabel('Username', { exact: true }).fill(name)
    await guest.getByLabel('Password', { exact: true }).fill(replacement)
    await guest.getByRole('button', { name: 'Sign in', exact: true }).click()
    await expect(guest.getByRole('heading', { name: `Good to see you, ${name}.` })).toBeVisible()
    await guest.getByRole('link', { name: 'Account settings', exact: true }).click()
    await guest.getByText('Delete this account', { exact: true }).click()
    await guest.getByLabel('Current password', { exact: true }).fill(replacement)
    await guest.getByRole('checkbox').check()
    await guest.getByRole('button', { name: 'Permanently delete account', exact: true }).click()
    await expect(guest.getByRole('heading', { name: 'Welcome to your next level.' })).toBeVisible()
    expect((await guest.request.get('/api/v1/auth/me')).status()).toBe(401)
  } finally {
    await recovery.close()
  }
})

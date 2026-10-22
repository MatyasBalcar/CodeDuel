import AxeBuilder from '@axe-core/playwright'
import { expect, test } from '@playwright/test'

test('lobby and sign-in pass automated WCAG A/AA checks', async ({ page }) => {
  for (const path of ['/', '/login']) {
    await page.goto(path)
    await expect(page.getByRole('heading', { level: 1 })).toBeVisible()
    const results = await new AxeBuilder({ page })
      .withTags(['wcag2a', 'wcag2aa', 'wcag21aa'])
      .analyze()
    expect(
      results.violations.map((violation) => ({
        id: violation.id,
        nodes: violation.nodes.map((node) => ({
          target: node.target,
          summary: node.failureSummary,
        })),
      })),
    ).toEqual([])
  }
})

import { expect, it } from 'vitest'
import { formatClock, remainingSeconds } from './clock'

it('uses the server-derived clock and clamps after deadline', () => {
  expect(remainingSeconds('2026-10-20T20:00:00Z', Date.parse('2026-10-20T19:59:59.001Z'))).toBe(1)
  expect(remainingSeconds('2026-10-20T20:00:00Z', Date.parse('2026-10-20T20:00:05Z'))).toBe(0)
  expect(remainingSeconds(null, 0)).toBe(0)
})
it('formats minute boundaries consistently', () => {
  expect(formatClock(900)).toBe('15:00')
  expect(formatClock(59)).toBe('00:59')
  expect(formatClock(0)).toBe('00:00')
})

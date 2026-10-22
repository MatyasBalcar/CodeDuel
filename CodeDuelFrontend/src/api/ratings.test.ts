import { expect, it } from 'vitest'
import { displayRating } from './ratings'

it('rounds ratings and deltas HALF_UP symmetrically and normalizes negative zero', () => {
  expect(displayRating(1200.5)).toBe(1201)
  expect(displayRating(1200.499999)).toBe(1200)
  expect(displayRating(-15.5)).toBe(-16)
  expect(displayRating(-0.2)).toBe(0)
  expect(Object.is(displayRating(-0.2), -0)).toBe(false)
})

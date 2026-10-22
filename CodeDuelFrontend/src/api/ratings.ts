/** One public policy for ratings and deltas, including negative half values. */
export function displayRating(value: number): number {
  const rounded = value < 0 ? Math.ceil(value - 0.5) : Math.floor(value + 0.5)
  return rounded === 0 ? 0 : rounded
}

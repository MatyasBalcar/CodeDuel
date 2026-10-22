export function remainingSeconds(deadline: string | null, serverNow: number): number {
  if (!deadline) return 0
  return Math.max(0, Math.ceil((Date.parse(deadline) - serverNow) / 1000))
}

export function formatClock(seconds: number): string {
  return `${Math.floor(seconds / 60)
    .toString()
    .padStart(2, '0')}:${(seconds % 60).toString().padStart(2, '0')}`
}

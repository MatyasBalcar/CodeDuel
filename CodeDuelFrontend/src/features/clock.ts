export function remainingSeconds(deadline: string | null, serverNow: number): number {
  if (!deadline) return 0
  return Math.max(0, Math.ceil((Date.parse(deadline) - serverNow) / 1000))
}

export function formatClock(seconds: number): string {
  const minutes = String(Math.floor(seconds / 60)).padStart(2, '0')
  const remainder = String(seconds % 60).padStart(2, '0')
  return `${minutes}:${remainder}`
}

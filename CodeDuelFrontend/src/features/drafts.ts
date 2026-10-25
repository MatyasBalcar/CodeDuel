const PREFIX = 'codeduel:draft:'
const RETENTION_MS = 7 * 24 * 60 * 60 * 1000

export function draftKey(user: string, battle: string, version: string) {
  return `${PREFIX}${user}:${battle}:${version}`
}

export function loadDraft(key: string, fallback: string): string {
  try {
    const value: unknown = JSON.parse(localStorage.getItem(key) ?? 'null')
    if (
      value &&
      typeof value === 'object' &&
      'code' in value &&
      typeof value.code === 'string' &&
      'expiresAt' in value &&
      typeof value.expiresAt === 'number' &&
      value.expiresAt > Date.now()
    )
      return value.code
    localStorage.removeItem(key)
  } catch {
    return fallback
  }
  return fallback
}

export function saveDraft(key: string, code: string): boolean {
  try {
    localStorage.setItem(key, JSON.stringify({ code, expiresAt: Date.now() + RETENTION_MS }))
    return true
  } catch {
    return false
  }
}

export function clearDrafts(user: string) {
  try {
    const prefix = `${PREFIX}${user}:`
    for (let index = localStorage.length - 1; index >= 0; index--) {
      const key = localStorage.key(index)
      if (key?.startsWith(prefix)) localStorage.removeItem(key)
    }
  } catch {
    return
  }
}

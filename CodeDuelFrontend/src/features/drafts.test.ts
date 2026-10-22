import { describe, expect, it, vi } from 'vitest'
import { clearDrafts, draftKey, loadDraft, saveDraft } from './drafts'

describe('private local drafts', () => {
  it('separates players, battles, and immutable versions', () => {
    const one = draftKey('alice', 'battle', 'v1')
    saveDraft(one, 'private code')
    expect(loadDraft(one, 'starter')).toBe('private code')
    expect(loadDraft(draftKey('bob', 'battle', 'v1'), 'starter')).toBe('starter')
    expect(loadDraft(draftKey('alice', 'battle', 'v2'), 'starter')).toBe('starter')
  })
  it('expires source after seven days', () => {
    const clock = vi.spyOn(Date, 'now').mockReturnValue(0)
    saveDraft('draft', 'code')
    clock.mockReturnValue(7 * 24 * 60 * 60 * 1000 + 1)
    expect(loadDraft('draft', 'starter')).toBe('starter')
    expect(localStorage.getItem('draft')).toBeNull()
    clock.mockRestore()
  })
  it('clears only the signing-out player’s drafts', () => {
    const alice = draftKey('alice', 'battle', 'v1')
    const bob = draftKey('bob', 'battle', 'v1')
    saveDraft(alice, 'alice')
    saveDraft(bob, 'bob')
    clearDrafts('alice')
    expect(loadDraft(alice, 'starter')).toBe('starter')
    expect(loadDraft(bob, 'starter')).toBe('bob')
  })
  it('recovers from malformed local storage', () => {
    localStorage.setItem('draft', 'not json')
    expect(loadDraft('draft', 'starter')).toBe('starter')
  })
})

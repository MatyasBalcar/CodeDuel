import { useQuery } from '@tanstack/react-query'
import { api } from './client'

export const sessionKey = ['session'] as const
export function useSession() {
  return useQuery({ queryKey: sessionKey, queryFn: api.me, retry: false })
}
export function useSystem() {
  return useQuery({ queryKey: ['system'], queryFn: api.system, staleTime: 60_000 })
}

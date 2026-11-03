// Hash-based routing: no server-side fallback needed, and links survive a page reload.
import { useSyncExternalStore } from 'react'

function subscribe(callback) {
  window.addEventListener('hashchange', callback)
  return () => window.removeEventListener('hashchange', callback)
}

function snapshot() {
  return window.location.hash.slice(1) || '/'
}

/** Current route as "/path?query". */
export function useRoute() {
  return useSyncExternalStore(subscribe, snapshot)
}

export function navigate(path) {
  window.location.hash = path
}

/**
 * Matches "/courses/12?x=1" against "/courses/:id" and returns { id: '12', query: URLSearchParams },
 * or null when the path does not match.
 */
export function matchRoute(route, pattern) {
  const [path, search = ''] = route.split('?')
  const parts = path.split('/').filter(Boolean)
  const expected = pattern.split('/').filter(Boolean)
  if (parts.length !== expected.length) return null
  const params = {}
  for (let i = 0; i < expected.length; i++) {
    if (expected[i].startsWith(':')) params[expected[i].slice(1)] = decodeURIComponent(parts[i])
    else if (expected[i] !== parts[i]) return null
  }
  return { ...params, query: new URLSearchParams(search) }
}

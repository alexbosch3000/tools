// Loads an API resource and exposes it with its loading state, error and a reload function.
import { useCallback, useEffect, useState } from 'react'
import { get } from './api.js'

export function useResource(path) {
  const [state, setState] = useState({ path: null, data: null, error: null })
  const [version, setVersion] = useState(0)

  useEffect(() => {
    if (!path) return undefined
    let cancelled = false
    get(path)
      .then((data) => { if (!cancelled) setState({ path, data, error: null }) })
      .catch((error) => { if (!cancelled) setState({ path, data: null, error }) })
    return () => { cancelled = true }
  }, [path, version])

  const reload = useCallback(() => setVersion((v) => v + 1), [])
  const current = state.path === path
  return {
    data: current ? state.data : null,
    error: current ? state.error : null,
    loading: Boolean(path) && !current,
    reload,
    setData: (data) => setState({ path, data, error: null }),
  }
}

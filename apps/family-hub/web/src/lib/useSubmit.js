// Runs a form action, exposing whether it is running and the error it raised.
import { useCallback, useState } from 'react'

export function useSubmit() {
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState(null)
  const run = useCallback(async (action) => {
    setBusy(true)
    setError(null)
    try {
      return await action()
    } catch (e) {
      setError(e)
      return undefined
    } finally {
      setBusy(false)
    }
  }, [])
  return { busy, error, run, setError }
}

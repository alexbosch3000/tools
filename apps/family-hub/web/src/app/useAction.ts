// Runs a change against the API: refreshes the affected areas and tells the user how it went.
import { useMutation } from '@tanstack/react-query'
import { refresh, type Area } from './queryClient.ts'
import { useFeedback } from './useFeedback.ts'

interface Options<R> {
  /** Areas of the cache that the change may affect. */
  refresh?: Area[]
  /** Message shown once it succeeded. */
  success?: string | ((result: R) => string)
  /**
   * Errors of a form are shown next to it (`error` of the returned mutation); errors of an action on
   * a page are shown as a notification.
   */
  inline?: boolean
  onSuccess?: (result: R) => void
}

export function useAction<A, R>(action: (args: A) => Promise<R>, options: Options<R> = {}) {
  const { notify } = useFeedback()
  return useMutation({
    mutationFn: action,
    onSuccess: async (result) => {
      await refresh(...(options.refresh ?? []))
      const message = typeof options.success === 'function' ? options.success(result) : options.success
      if (message) notify(message)
      options.onSuccess?.(result)
    },
    onError: (error) => {
      if (!options.inline) notify(error.message, 'error')
    },
  })
}

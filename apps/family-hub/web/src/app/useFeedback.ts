// Access to notifications and confirmations from any component (provided by FeedbackProvider).
import { createContext, useContext } from 'react'

export interface Confirmation {
  title: string
  message: string
  confirmLabel?: string
}

export interface Feedback {
  notify: (message: string, tone?: 'success' | 'error') => void
  confirm: (confirmation: Confirmation) => Promise<boolean>
}

export const FeedbackContext = createContext<Feedback | null>(null)

export function useFeedback(): Feedback {
  const feedback = useContext(FeedbackContext)
  if (!feedback) throw new Error('useFeedback outside FeedbackProvider')
  return feedback
}

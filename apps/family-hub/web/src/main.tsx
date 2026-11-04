// Entry point: providers for server state, feedback, session and routing.
import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { QueryClientProvider } from '@tanstack/react-query'
import { RouterProvider } from '@tanstack/react-router'
import { queryClient } from './app/queryClient.ts'
import { FeedbackProvider } from './app/feedback.tsx'
import { SessionGate } from './app/session.tsx'
import { router } from './app/router.tsx'
import './index.css'

const root = document.getElementById('root')
if (!root) throw new Error('Missing #root element')

createRoot(root).render(
  <StrictMode>
    <QueryClientProvider client={queryClient}>
      <FeedbackProvider>
        <SessionGate>
          <RouterProvider router={router} />
        </SessionGate>
      </FeedbackProvider>
    </QueryClientProvider>
  </StrictMode>,
)

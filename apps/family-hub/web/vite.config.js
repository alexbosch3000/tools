// Vite build and Vitest configuration. The build lands in dist/, which Maven packages as static content.
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// `npm run dev` serves the UI on 5173 and forwards API calls to the service started by `make run`.
export default defineConfig({
  plugins: [react()],
  build: { sourcemap: true },
  server: {
    proxy: { '/api': 'http://localhost:8080' },
  },
  // Date arithmetic is local: tests pin the time zone of the family.
  test: { environment: 'node', include: ['src/**/*.test.js'], env: { TZ: 'Europe/Paris' } },
})

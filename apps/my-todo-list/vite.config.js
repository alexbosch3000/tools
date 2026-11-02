// Vite build and Vitest configuration.
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// BASE_PATH is the public path the app is served from:
//   "/"           container image and local development (default)
//   "/<repo>/"    GitHub Pages project site, set by CI
export default defineConfig({
  plugins: [react()],
  base: process.env.BASE_PATH || '/',
  build: { sourcemap: true },
  test: { environment: 'node', include: ['src/**/*.test.js'] },
})

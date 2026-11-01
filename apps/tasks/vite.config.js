// Vite build and Vitest configuration.
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// VITE_BASE is the public path the app is served from:
//   "/"        container image and local development (default)
//   "/tools/"  GitHub Pages project site
export default defineConfig({
  plugins: [react()],
  base: process.env.VITE_BASE ?? '/',
  build: { sourcemap: true },
  test: { environment: 'node', include: ['src/**/*.test.js'] },
})

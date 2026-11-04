// Vite build and Vitest configuration. The build lands in dist/, packaged in the jar or published on Pages.
import type { Plugin } from 'vite'
import { defineConfig } from 'vitest/config'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

const remote = process.env.VITE_REMOTE_API === 'true'

/**
 * A UI served by GitHub Pages cannot rely on response headers: its Content Security Policy is a meta
 * tag, allowing API calls to any HTTPS server (the family's server is chosen at first launch).
 */
function pagesCsp(): Plugin {
  const policy = "default-src 'self'; connect-src 'self' https:; img-src 'self' data:; object-src 'none'; "
    + "base-uri 'self'; form-action 'self'"
  return {
    name: 'pages-csp',
    transformIndexHtml: (html) => html.replace('<head>',
      `<head>\n    <meta http-equiv="Content-Security-Policy" content="${policy}" />`),
  }
}

// BASE_PATH is the public path: "/" in the service, "/<repo>/" on GitHub Pages.
export default defineConfig({
  plugins: [react(), tailwindcss(), ...(remote ? [pagesCsp()] : [])],
  base: process.env.BASE_PATH || '/',
  build: { sourcemap: true },
  server: { proxy: { '/api': 'http://localhost:8080' } },
  test: {
    environment: 'jsdom',
    include: ['src/**/*.test.{ts,tsx}'],
    // Date arithmetic is local: tests pin the time zone of the family.
    env: { TZ: 'Europe/Paris' },
  },
})

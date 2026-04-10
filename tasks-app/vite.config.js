import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// En production (GitHub Pages), le site est servi sous /tools/ (nom du dépôt)
export default defineConfig(({ command }) => ({
  plugins: [react()],
  base: command === 'build' ? '/tools/' : '/',
}))

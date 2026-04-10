import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// VITE_BASE permet d'ajuster le chemin selon l'hébergeur :
//   GitHub Pages → /tools/   (défini dans le workflow CI)
//   Vercel / Netlify / Surge → / (valeur par défaut)
export default defineConfig({
  plugins: [react()],
  base: process.env.VITE_BASE ?? '/',
})

import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
// PROXY TARGET: switch between local backend and Render backend.
//   Local dev (backend running):  target: 'http://localhost:8080'
//   Test against Render backend:  target: 'https://sapt-ix8h.onrender.com'
// PROXY TARGET: switch between local backend and Render backend.
//   Local dev (backend running):  target: 'http://localhost:8080'
//   Test against Render backend:  target: 'https://sapt-ix8h.onrender.com'
const BACKEND = process.env.VITE_PROXY_TARGET || 'http://localhost:8080';

export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/api': {
        target: BACKEND,
        changeOrigin: true,
        secure: false,
      },
    },
  },
})

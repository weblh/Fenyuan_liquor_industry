import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import path from 'path'
import { fileURLToPath } from 'url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))

export default defineConfig({
  base: '/Fenyuan_liquor_industry/',
  plugins: [react()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, 'src'),
    },
  },
  server: {
    port: 5173,
    proxy: {
      '/Fenyuan_liquor_industry/api': {
        target: 'http://localhost:6001',
        changeOrigin: true,
        rewrite: (p) => p.replace(/^\/Fenyuan_liquor_industry\/api/, '/api'),
      },
    },
  },
})

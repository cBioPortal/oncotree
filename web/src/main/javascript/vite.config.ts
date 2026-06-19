import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [react()],
  build: {
    emptyOutDir: true,
    outDir: '../resources/static'
  },
  server: {
    proxy: {
      '/api': {
        target: process.env.ONCOTREE_API_PROXY || 'http://localhost:8080',
        changeOrigin: true,
        secure: false,
      }
    }
  },
})

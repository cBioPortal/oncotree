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
        target: 'https://oncotree.mskcc.org',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, '/api'),
        secure: true,
      },
    },
  },
})

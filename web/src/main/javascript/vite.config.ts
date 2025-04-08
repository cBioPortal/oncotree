import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [react()],
  build: {
    emptyOutDir: true,
    outDir: '../resources/static'
  },
  define: {
    'process.env.API_URL': JSON.stringify(process.env.API_URL),
  }
})

import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

export default defineConfig({
  plugins: [react(), tailwindcss()],

  server: {
    // Proxy API and WebSocket calls to the Spring Boot backend
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      '/ws': {
        target: 'http://localhost:8080',
        ws: true,
      },
    },
    // Set security headers in dev mode (production should use reverse proxy)
    headers: {
      'X-Content-Type-Options': 'nosniff',
      'X-Frame-Options': 'DENY',
      'Referrer-Policy': 'strict-origin-when-cross-origin',
    },
  },

  build: {
    // Warn if any chunk is over 400KB (helps catch bundle bloat)
    chunkSizeWarningLimit: 400,

    // Do not emit source maps in production builds
    sourcemap: false,

    // Inline assets smaller than 4KB as base64 to reduce HTTP requests
    assetsInlineLimit: 4096,

    rollupOptions: {
      output: {
        // Split heavy libraries into separate cached chunks.
        // Users only re-download a chunk when THAT library changes,
        // not the whole app bundle.
        manualChunks: {
          // Core React — changes rarely, cached for a long time
          'react-vendor': ['react', 'react-dom', 'react-router-dom'],

          // Framer Motion is ~200KB — isolate it so other changes
          // don't invalidate this chunk
          'motion': ['framer-motion'],

          // Icon library used across many components
          'icons': ['lucide-react'],

          // WebSocket / real-time libraries
          'realtime': ['@stomp/stompjs', 'sockjs-client'],

          // Date + HTTP utilities
          'utils': ['date-fns', 'axios'],
        },
      },
    },
  },
})

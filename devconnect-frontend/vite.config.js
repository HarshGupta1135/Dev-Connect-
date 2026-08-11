import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

/**
 * The dev server proxies API calls to Spring Boot on 8080.
 *
 * This is why no CORS configuration is needed in the backend during development:
 * the browser only ever talks to http://localhost:3000, and Vite forwards the
 * request server-side, where the same-origin policy does not apply.
 *
 * For a deployed build (Vercel + a hosted backend) set VITE_API_BASE_URL to the
 * backend origin — at that point the backend does need to allow that origin.
 */
export default defineConfig({
  plugins: [react()],
  server: {
    // Vite moves to the next free port (3001, 3002…) if 3000 is taken by another
    // project. The proxy below is same-origin either way, so nothing breaks —
    // just read the URL Vite prints on startup.
    port: 3000,
    proxy: {
      '/api': { target: 'http://localhost:8080', changeOrigin: true },
      '/admin': { target: 'http://localhost:8080', changeOrigin: true },
      '/health': { target: 'http://localhost:8080', changeOrigin: true },
    },
  },
});

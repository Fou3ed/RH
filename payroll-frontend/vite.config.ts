import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import path from 'node:path';

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
  build: {
    rollupOptions: {
      output: {
        // Split the rarely-changing framework core into stable, long-cached
        // vendor chunks so app-code updates don't force a full re-download.
        manualChunks(id) {
          if (!id.includes('node_modules')) return undefined;
          if (/[\\/]node_modules[\\/](react|react-dom|react-router|react-router-dom|scheduler)[\\/]/.test(id))
            return 'react-vendor';
          if (/[\\/]node_modules[\\/](@mui|@emotion)[\\/]/.test(id)) return 'mui-vendor';
          if (/[\\/]node_modules[\\/]@tanstack[\\/]/.test(id)) return 'query-vendor';
          return 'vendor';
        },
      },
    },
  },
  server: {
    port: 3000,
    host: true,
    proxy: {
      // Forward API calls to the backend during local dev
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
  preview: {
    port: 3000,
  },
});

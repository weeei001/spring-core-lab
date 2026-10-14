import { fileURLToPath, URL } from 'node:url';
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  resolve: {
    // 與 tsconfig.app.json 的 paths 一致
    alias: { '@': fileURLToPath(new URL('./src', import.meta.url)) },
  },
  server: {
    port: 3000,
    strictPort: true,
    // 把 API 與登入相關網址轉給 web-server，瀏覽器只看到 localhost:3000，cookie 才不會跨網域。
    // 不改 Host（changeOrigin 預設 false），web-server 組出的登入 redirect URI 才會是 localhost:3000/auth/callback
    proxy: {
      '/api/': 'http://localhost:8080',
      '/auth/': 'http://localhost:8080',
    },
  },
});

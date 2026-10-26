import { fileURLToPath, URL } from 'node:url';
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  resolve: {
    // 與 tsconfig.app.json 的 paths 一致
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
      // frappe-gantt 的 exports 把 import 條件排在 style 前面，CSS @import 'frappe-gantt' 會解析到 JS；子路徑又被 exports 擋住，只好直接指到檔案
      'frappe-gantt/style.css': fileURLToPath(new URL('./node_modules/frappe-gantt/dist/frappe-gantt.css', import.meta.url)),
    },
  },
  server: {
    port: 3000,
    strictPort: true,
    // 把 API 與登入相關網址轉給 web-server，瀏覽器只看到 localhost:3000，cookie 才不會跨網域。
    // 後端 app.public-url 預設為 http://localhost:3000，OIDC 回呼也經此代理。
    proxy: {
      '/api/': 'http://localhost:8080',
      '/auth/': 'http://localhost:8080',
    },
  },
});

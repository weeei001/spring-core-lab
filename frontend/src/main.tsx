import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
// antd 官方的基礎重設：去掉瀏覽器預設的 body margin 等，否則版面外圍會多一圈白邊
import 'antd/dist/reset.css';
import { App } from '@/app/App';

const container = document.getElementById('root');
if (!container) {
  throw new Error('index.html 缺少 #root');
}

createRoot(container).render(
  <StrictMode>
    <App />
  </StrictMode>,
);

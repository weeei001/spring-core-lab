# spring-core-lab 前端

集中管理專案進度、任務、工時與請假的網頁介面，提供看板、甘特圖、行事曆、站內通知及角色權限管理。使用 React 19、TypeScript 5.9、Vite 8 與 Ant Design 6，套件由 Yarn 4 管理。

[後端啟動說明](../backend/README.md) · [協作規範](../AGENTS.md) · [已知問題](../HANDOFF.MD)

## 快速開始

需要 Node.js `^20.19.0 || >=22.12.0`，以及 Corepack。`package.json` 固定使用 Yarn 4.18.1。下列指令以 `frontend/` 為工作目錄；若位於儲存庫根目錄，先執行 `cd frontend`。

先依[後端 README](../backend/README.md)完成 MySQL、authentik 與服務啟動，再執行：

```bash
corepack enable
yarn install
yarn dev
```

若已安裝 Node 但沒有 `corepack` 指令，可使用 `npm exec --yes --package=corepack -- corepack yarn install`；啟動與建置時，將最後的 `install` 換成 `dev` 或 `build`。此方式仍使用 `package.json` 固定的 Yarn 版本。

開啟 [http://localhost:3000](http://localhost:3000)，使用 authentik 帳號登入。開發伺服器會把 `/api/` 與 `/auth/` 轉送至 `http://localhost:8080`；瀏覽器仍使用同一個來源，登入 Cookie 與 CSRF 才能正常配合。

`vite.config.ts` 固定使用連接埠 3000；若已被占用，啟動會失敗，不會自動切換。使用預設登入設定時，authentik 的開發環境回呼網址須包含 `http://localhost:3000/auth/callback`。

若 authentik 回呼固定為 `http://localhost:8080/auth/callback`，可依後端 README 明確設定 `registration.authentik.redirect-uri`；前端仍使用 3000，登入完成後返回前端。

登入後可開啟[目前登入者](http://localhost:3000/api/auth/me)確認帳號與權限，再操作帳號可見的頁面。前端依賴後端服務，沒有獨立的模擬資料模式。

## 功能範圍

| 區域 | 內容 |
|---|---|
| 專案 | 儀表板、專案清單、成員與標籤管理 |
| 任務 | 清單、看板、甘特圖、子任務、留言、附件與活動紀錄 |
| 排程與工時 | 行事曆、我的任務、工時表 |
| 請假 | 請假申請與相關管理流程 |
| 系統 | 使用者、角色、公告、流水號及站內通知 |

實際選單與操作能力由登入者權限決定。登入由後端處理，OAuth2 Token 不存放在前端。

## 常用指令

所有指令皆在 `frontend/` 執行。

| 指令 | 用途 |
|---|---|
| `yarn dev` | 啟動開發伺服器與熱更新 |
| `yarn typecheck` | 執行 TypeScript 專案型別檢查 |
| `yarn lint` | 執行 ESLint，包含嚴格型別及分層規則 |
| `yarn build` | 先檢查型別，再輸出靜態檔案至 `dist/` |
| `yarn preview` | 本機預覽已完成的建置產物 |

目前沒有自動化測試框架或 `yarn test` 指令。修改後執行型別檢查、Lint 與建置，並手動驗證受影響的畫面、登入和權限流程。

## 原始碼導覽

```text
src/
├─ main.tsx       掛載 App
├─ app/          Provider、路由、版面、選單與主題
├─ pages/        專案、行事曆、請假、基本資料與系統頁面
├─ components/   多個頁面共用的業務元件，依領域分類
├─ api/          服務呼叫、實體型別與查詢參數
├─ core/         HTTP Client、登入與權限基礎設施
├─ shared/       不含業務邏輯的元件、Hook 與工具
└─ types/        第三方套件型別補充
```

### 檔案放置與引用

- 一頁一個目錄，例如 `pages/project/project-detail/`。頁面專用元件放在該目錄的 `components/`，CSS 與操作導覽也留在頁面內。
- 兩個以上頁面使用的業務元件放到 `components/<領域>/`；不含業務邏輯的共用程式放到 `shared/`。
- API 型別使用 `xxx.types.ts`，請求使用 `xxx-api.ts`；`api/leave/` 對應後端 `basic-data` 的請假功能。
- 依賴方向為 `app → pages → components → api → shared/core`。頁面間不互相引用，ESLint 會檢查分層限制。
- 同一功能內使用相對路徑，跨功能以 `@/` 指向 `src/`；直接引用檔案，不建立 `index.ts` 集中轉出。

### 型別與 API

TypeScript 啟用 `strict`、`noUncheckedIndexedAccess`、`noImplicitReturns` 等檢查；ESLint 使用 `strictTypeChecked`，避免 `any`、未處理的 Promise 與多餘條件。

API 回應透過 `core/http/client.ts` 的 `send()` 取得 `Result.data`，錯誤統一為 `ApiError`。沿用共用 HTTP Client，讓同源請求的 CSRF Cookie 正確放入標頭。

## 新增頁面

1. 在 `api/<領域>/` 建立型別與 API。一般 CRUD 可沿用 `createCrudApi`。
2. 在 `pages/<群組>/<頁面>/` 建立 `XxxPage.tsx`；依需求使用既有的 `usePagedList`、`useSaveDelete` 與 `FormModal`。
3. 在 `app/router.tsx` 註冊路由，使用 `RequirePermission` 設定所需權限；在 `app/menu.ts` 加入選單。
4. 新增功能權限時，同步更新後端 `Permission` enum 與 `core/auth/permissions.ts`。
5. 執行檢查，驗證有權限與無權限帳號的行為；畫面變更在 PR 附截圖。

### 操作導覽

需要首次使用導覽的頁面，可使用 `shared/ui/PageTour.tsx`：

1. 在元素加上 `data-tour="key"`；要框選內層元素時，加上 `data-tour-inner=".選擇器"`。
2. 在頁面目錄的 `tour.ts`（包含 JSX 時為 `tour.tsx`）定義 `GuideStep[]`，以 `target` 指向對應的 key；未指定 target 的步驟會置中。
3. 在頁面加入 `<PageTour id="頁面代號" steps={steps} />`。找不到目標元素的步驟會略過。

有設定導覽的頁面會在首次進入時播放，上方「?」可重看或重設。已看過的狀態儲存在 localStorage 的 `tour-seen:<頁面代號>`。

## 建置與部署

```bash
yarn build
```

將產出的 `dist/` 交由靜態檔案服務提供，並設定：

- 前端路由回退至 `index.html`，讓直接開啟或重新整理子頁面可正常載入。
- 正式建置的 API、登入與附件網址使用 `/backend` 前綴；開發模式維持 `/api/` 與 `/auth/`。
- 同一個對外來源下，將 `/backend/` 移除前綴後反向代理至 `web-server`，範本見 [Nginx 設定](../deploy/nginx-spring.conf)。後端保留根 context，Cookie Path 使用 `/`，不傳入 `X-Forwarded-Prefix`。
- 本次正式入口為 `https://project-manage.jianwii.dev`；`APP_FRONTEND_URL` 使用此網址，`APP_PUBLIC_URL` 加上 `/backend`，authentik 回呼為 `https://project-manage.jianwii.dev/backend/auth/callback`。詳見[後端部署說明](../backend/README.md#部署設定)。

`yarn preview` 用於本機檢查建置產物，正式部署仍需上述代理與路由設定。

## 常見問題

| 現象 | 檢查方向 |
|---|---|
| 找不到 `corepack` | 確認 Node 版本與 Corepack 已安裝，再執行 `corepack enable` |
| 3000 已被占用 | 停止占用程式，再啟動開發伺服器 |
| API 連線失敗 | 確認 `web-server` 使用 8080，且業務服務已在 Eureka 註冊 |
| 登入回呼錯誤 | 核對 authentik 的回呼網址與目前使用的來源 |
| API 回傳 401／403 | 檢查登入狀態、角色權限，以及請求是否使用共用 Client |
| 型別或引用規則報錯 | 先修正 `yarn typecheck`／`yarn lint` 指出的型別或分層問題 |

完整協作與提交方式見[協作規範](../AGENTS.md)。

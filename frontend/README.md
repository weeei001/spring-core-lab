# spring-core-lab 前端

React 19 + TypeScript 5.9 + Vite 8 的管理後台，UI 使用 Ant Design 6，套件以 Yarn 4 管理。
登入由後端 web-server 處理（authentik），前端只拿到 session cookie。

## 啟動

需要 Node 20.19 以上（或 22.12 以上）。第一次先啟用 Corepack，它會依 `package.json` 的 `packageManager` 自動使用正確的 Yarn 版本：

```bash
corepack enable
yarn install
yarn dev          # http://localhost:3000，/api、/auth 會轉給 localhost:8080
```

後端要先啟動 eureka-server、basic-data、project-service、web-server。

| 指令 | 用途 |
|---|---|
| `yarn dev` | 開發伺服器 |
| `yarn typecheck` | TypeScript 型別檢查 |
| `yarn lint` | ESLint（typescript-eslint strictTypeChecked） |
| `yarn build` | 型別檢查後打包到 `dist/` |

## 分層

```
src/
├─ main.tsx            進入點，只負責掛載 App
├─ app/                應用程式組裝：Provider、路由、版面、選單（唯一知道所有模組的地方）
├─ core/               基礎設施，整個 app 只有一份
│  ├─ http/            axios、Result 解析、錯誤處理、CRUD 工廠
│  └─ auth/            登入者、功能權限、權限檢查
├─ shared/             可重用、不含業務邏輯的元件與工具
│  ├─ ui/              FormModal、表格欄位
│  ├─ hooks/           分頁列表、儲存刪除
│  └─ lib/             格式化工具
└─ modules/            業務模組，一個資料夾對應一個後端服務
   ├─ project/         ↔ backend/project-service（儀表板、專案、任務看板／甘特、我的任務、工時）
   └─ basic-data/      ↔ backend/basic-data（編號規則、使用者、角色）
```

每個功能資料夾（例如 `modules/basic-data/role/`）內的檔案：

| 檔案 | 內容 |
|---|---|
| `xxx.types.ts` | 對應後端 Entity 與查詢參數的型別 |
| `xxx-api.ts` | 呼叫後端 API |
| `XxxPage.tsx` | 頁面 |
| `useXxx.ts`、其他元件 | 這個功能自己用的 hook 與元件 |

### 引用規則

- 依賴方向只能往下：`app` → `modules` → `shared`、`core`。`shared`、`core` 不可引用 `modules`。
- 模組之間只能從對方的 `index.ts` 引用，例如 `import { useUserNames } from '@/modules/basic-data'`；ESLint 會擋下直接引用模組內部檔案。
- 用 `@/` 代表 `src/`，不寫 `../../../`。

### 型別嚴謹度

- `tsconfig.app.json` 開啟 `strict`，另外加上 `noUncheckedIndexedAccess`、`noImplicitReturns`、`verbatimModuleSyntax` 等。
- ESLint 使用 typescript-eslint 的 `strictTypeChecked`：禁止 `any`、未處理的 Promise、不必要的條件判斷等。
- 後端回應一律透過 `core/http/client.ts` 的 `send()` 取出 `Result.data`，錯誤統一轉成 `ApiError`。

## 新增一個功能

1. 在對應的模組下建立資料夾，寫 `xxx.types.ts`（對應後端 Entity）。
2. `xxx-api.ts`：一般 CRUD 用 `createCrudApi<Xxx, XxxQuery>('/api/...')`。
3. `XxxPage.tsx`：用 `usePagedList`、`useSaveDelete`、`FormModal` 組出列表與表單。
4. 在模組的 `index.ts` 匯出頁面。
5. `app/router.tsx` 加路由並用 `RequirePermission` 包住；`app/menu.ts` 加選單。
6. 需要新的功能權限時，後端 `Permission` enum 與 `core/auth/permissions.ts` 一起加。

## 操作導覽

每個頁面第一次進入時會自動播放導覽（antd `Tour`），之後可從上方列的「?」重看，或重設所有頁面的導覽。

新頁面要加導覽時：

1. 在要介紹的元素加上 `data-tour="key"`；只想框住裡面的一部分（例如分頁列）時，再加 `data-tour-inner=".ant-tabs-nav"`。
2. 在模組的 `guide/tours.tsx` 寫步驟（`GuideStep[]`），`target` 填上面的 key；不填 `target` 則置中顯示。
3. 頁面裡放 `<PageTour id="頁面代號" steps={...} />`。畫面上找不到的步驟（例如沒有權限而沒顯示的按鈕）會自動略過。

「看過」的紀錄存在瀏覽器的 localStorage（`tour-seen:頁面代號`）。

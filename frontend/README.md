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
├─ app/                應用程式組裝：Provider、路由、版面、選單
├─ pages/              畫面，層級與側邊選單一致；照選單就能找到檔案
│  ├─ project/         「專案」：dashboard、project-list、project-detail、my-tasks、timesheet
│  ├─ basic-data/      「基本資料」：serial-number
│  └─ system/          「系統管理」：role、user
├─ components/         兩個以上頁面共用的業務元件，依領域分
│  ├─ project/         任務抽屜（task-drawer/）、專案卡片、標籤、專案共用的 hook 與工具
│  ├─ user/            使用者頭像、選擇器、名稱查詢
│  ├─ notification/    通知鈴鐺
│  └─ search/          全站搜尋
├─ api/                呼叫後端，一個資料夾對應一個後端服務
│  ├─ project/         ↔ backend/project-service
│  ├─ basic-data/      ↔ backend/basic-data
│  └─ notification/    ↔ backend/notification-service
├─ core/               基礎設施，整個 app 只有一份
│  ├─ http/            axios、Result 解析、錯誤處理、CRUD 工廠
│  └─ auth/            登入者、功能權限、權限檢查
└─ shared/             可重用、不含業務邏輯的元件與工具
   ├─ ui/              FormModal、表格欄位、圖表、統計卡片
   ├─ hooks/           分頁列表、儲存刪除
   └─ lib/             格式化工具
```

結構參考 [Ant Design Pro](https://github.com/ant-design/ant-design-pro)：一頁一個資料夾，頁面專用的東西都放在裡面。

```
pages/project/project-detail/
├─ ProjectDetailPage.tsx   頁面
├─ tour.ts                 這一頁的操作導覽
└─ components/             只有這一頁用的元件（各分頁；tasks/ 是任務分頁的看板、表格、甘特）
```

| 放哪裡 | 內容 |
|---|---|
| `api/<服務>/xxx.types.ts` | 對應後端 Entity 與查詢參數的型別 |
| `api/<服務>/xxx-api.ts` | 呼叫後端 API |
| `pages/<選單群組>/<頁面>/` | 頁面、`tour.ts`，以及只有這一頁用的元件（放 `components/`） |
| `components/<領域>/` | 多個頁面共用的業務元件、hook；只剩一頁使用時搬回該頁 |
| `shared/` | 和業務無關、任何地方都能用的元件與工具 |

### 引用規則

- 依賴方向只能往下：`app` → `pages` → `components` → `api` → `shared`、`core`，ESLint 會擋下反方向的引用。
- 頁面之間不互相引用；要共用就移到 `components/`。
- 不用 `index.ts` 轉出，直接引用檔案。
- 同一個頁面（或同一個 `components/<領域>/`）裡用相對路徑，跨資料夾用 `@/` 代表 `src/`，不寫 `../../../`。

### 型別嚴謹度

- `tsconfig.app.json` 開啟 `strict`，另外加上 `noUncheckedIndexedAccess`、`noImplicitReturns`、`verbatimModuleSyntax` 等。
- ESLint 使用 typescript-eslint 的 `strictTypeChecked`：禁止 `any`、未處理的 Promise、不必要的條件判斷等。
- 後端回應一律透過 `core/http/client.ts` 的 `send()` 取出 `Result.data`，錯誤統一轉成 `ApiError`。

## 新增一個功能

1. 在 `api/<服務>/` 寫 `xxx.types.ts`（對應後端 Entity）。
2. `api/<服務>/xxx-api.ts`：一般 CRUD 用 `createCrudApi<Xxx, XxxQuery>('/api/...')`。
3. 在 `pages/<選單群組>/` 建立頁面資料夾，寫 `XxxPage.tsx`：用 `usePagedList`、`useSaveDelete`、`FormModal` 組出列表與表單。
4. `app/router.tsx` 加路由並用 `RequirePermission` 包住；`app/menu.ts` 加選單。
5. 需要新的功能權限時，後端 `Permission` enum 與 `core/auth/permissions.ts` 一起加。

## 操作導覽

每個頁面第一次進入時會自動播放導覽（antd `Tour`），之後可從上方列的「?」重看，或重設所有頁面的導覽。

新頁面要加導覽時：

1. 在要介紹的元素加上 `data-tour="key"`；只想框住裡面的一部分（例如分頁列）時，再加 `data-tour-inner=".ant-tabs-nav"`。
2. 在頁面資料夾的 `tour.ts` 寫步驟（內容有 JSX 時用 `tour.tsx`）（`GuideStep[]`），`target` 填上面的 key；不填 `target` 則置中顯示。
3. 頁面裡放 `<PageTour id="頁面代號" steps={...} />`。畫面上找不到的步驟（例如沒有權限而沒顯示的按鈕）會自動略過。

「看過」的紀錄存在瀏覽器的 localStorage（`tour-seen:頁面代號`）。

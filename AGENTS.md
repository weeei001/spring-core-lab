# Repository Guidelines

## 協作流程與簡化原則

- 不使用 Docker；開發與驗證使用本機安裝的服務，資料庫驗證使用本機 MySQL 的獨立測試資料庫。
- 開始任務前先讀取 `HANDOFF.MD` 與 `TODO.md`，確認踩坑經驗與未完成事項；新發現的陷阱補入 `HANDOFF.MD`，待辦集中記在 `TODO.md`。結束任務時更新待辦，完成項目移除，只將值得保留的踩坑經驗留在 `HANDOFF.MD`。
- 從真實目標、約束與成功標準出發，先說明假設與驗證方式；會影響實作方向的歧義應先釐清。
- 只做滿足需求的最小修改，保留無關變更。避免預先設計抽象層，或拆出沒有必要、只使用一次的輔助函式。
- 為推測性的競態或安全邊界增加處理前，先說明具體失敗情境、發生機率與複雜度成本，取得共識後才擴大範圍。
- 在外部資料進入系統時驗證；內部已有型別或驗證保證的資料，不要重複檢查。避免捕捉例外後忽略錯誤。

## 專案結構與模組分工

- `backend/`：Spring Boot 多模組專案。`common` 放實體與共用基礎設施，`common-api` 放服務契約；執行模組為 `eureka-server`、`basic-data`、`project-service`、`notification-service` 與前端入口 `web-server`。各模組的程式與設定位於 `src/main/java/`、`src/main/resources/`。
- `frontend/src/`：React／TypeScript 分為 `app`、`pages`、`components`、`api`、`core`、`shared`。頁面專用元件與 CSS 放在該頁目錄，細節見 `frontend/README.md`。
- `http/` 放手動 API 請求；`deploy/application.properties.example` 列出部署設定。前後端啟動方式見各自的 `README.md`。

## 建置、測試與開發指令

使用 JDK 21、Node `^20.19.0 || >=22.12.0` 與 Yarn 4.18.1。

在 `backend/` 執行（Windows PowerShell 改用 `.\mvnw.cmd`）：

- `./mvnw clean install`：建置並安裝所有模組；`./mvnw test`：執行測試生命週期。
- `./mvnw spotless:check`／`./mvnw spotless:apply`：檢查／修正排版。一般建置也會在 `process-sources` 自動排版，完成後須檢查差異。
- `./mvnw -pl eureka-server spring-boot:run`：啟動服務探索。完成安裝後，替換模組名稱，在不同終端啟動其他服務。

在 `frontend/` 執行：

- `corepack enable`，再執行 `yarn install`：準備套件。
- `yarn dev`：使用連接埠 3000，將 API／登入請求代理至 8080。
- `yarn typecheck`、`yarn lint`：檢查型別與 ESLint 規則。
- `yarn build`：檢查型別並輸出至 `dist/`。

## 程式風格與命名

Java 使用 Palantir 排版、四個空白縮排，以及 `*Controller`／`*Service`／`*DAO` 命名。TypeScript 使用兩個空白、單引號與分號；元件命名為 `PascalCase.tsx`，Hook 為 `useXxx.ts`。避免 `any`，優先使用 `const` 與清楚的提早回傳。

遵守 ESLint 分層方向：`app → pages → components → api → core/shared`。頁面之間不互相引用，不使用集中轉出的 barrel 檔；功能內用相對路徑，跨功能用 `@/`。

## 測試規範

目前沒有自動化測試、測試框架或覆蓋率門檻。依修改範圍執行上述檢查，並驗證受影響的 HTTP／畫面流程。測試應檢查可觀察的行為，避免複製實作邏輯，並減少不必要的模擬物件。新增 Java 測試時，放在 `src/test/java/`、以 `*Test.java` 命名，並補上測試依賴。回報已執行的驗證與阻礙。

## Commit 與 Pull Request

既有提交使用簡短、動作導向的標題，可加範圍，例如 `calendar: ...`。每次提交聚焦單一目的。PR 應說明變更、驗證結果、設定影響與相關議題；涉及畫面時附上截圖。

## 安全與設定

在本機設定 MySQL、authentik 與郵件服務。憑證放在 Git 忽略的 `application-local.yml` 或部署環境檔案，不得提交機密。

# 任務檢視與返回驗收

日期：2026-10-27，Asia/Taipei。版本：`aed9b2b` 加上本次未提交修改。

本次使用真實 React 畫面、Google 登入與本機 HTTP 服務，沒有攔截或模擬 API。
資料位於新建的 `spring_core_lab_t2_basic_data`、`spring_core_lab_t2_project`、`spring_core_lab_t2_notification`、`spring_core_lab_t2_session`；未寫入原業務資料庫。

## 測資

- 專案 ID 1，名稱 `T2 表格驗收`，正常狀態為 `ACTIVE`。
- 60 筆主任務、3 筆子任務，共 63 筆。子任務 61、63 屬於 1，62 屬於 2。
- 取消項目有 12 筆主任務及子任務 63。預設顯示 50 筆任務，包含 2 筆子任務。
- 任務 1–5、61–63 的標題含「回歸」。任務 62 有超過 100 字元的連續英文長標題。
- 使用者 ID 1 為 Google 登入的測試帳號；ID 2 為合成測試成員。
- 成員驗收用預設 `USER` 角色；唯讀驗收將同一測試帳號暫時改為 `T2_READONLY`，僅有 `project:read`，完成後還原。
- 窄螢幕曾將任務 2 改成 `REVIEW`、指派給 1，資料庫確認成功；結束時已還原為 `IN_PROGRESS`、指派給 2。

## 實際結果

| 案例 | 重現方式與預期結果 | 2026-10-27 結果 |
|---|---|---|
| TL-01 預設 | 開 `/projects/1`，維持原有看板並隱藏取消項目 | 通過；看板含符合條件的子任務，標示父任務編號 |
| TL-02 集合一致 | 搜尋「回歸」、顯示取消，切換表格並展開父任務 1、2，再切看板 | 通過；兩者皆為 001–005、061–063 共 8 筆 |
| TL-03 負責人 | 上述條件選成員 2、指派給我、未指派 | 通過；依序為 002/005/063、001/004/061、003/062；未指派的兩種檢視相同 |
| TL-04 重整 | 表格帶「回歸」、成員 2、顯示取消，重新整理 | 通過；四個條件與三筆結果均還原 |
| TL-05 父子關係 | 展開表格父任務；再讓只有子任務命中篩選 | 通過；保留樹狀展開，未命中父任務的子任務列於上層 |
| TL-06 排序與分頁 | 到期日預設遞增；點編號遞增、翻到第二頁，開 39 抽屜及完整頁後返回 | 通過；排序、第二頁、展開項目保留，抽屜已關閉 |
| TL-07 捲動 | 20 筆一頁的第二頁下方開詳情，再返回；另測完整頁重整後返回 | 通過；原位置約 282px，返回約 266–282px，任務列仍在相近位置 |
| TL-08 來源參數 | 開帶 `tab=tasks`、`task=63`、四個檢視條件及 `keep=t2` 的網址，進完整頁再返回 | 通過；保留 `tab`、條件與額外參數，返回時移除 `task` |
| TL-09 歷史 | 從完整頁返回，搜尋欄逐字加「子」，按瀏覽器上一頁 | 通過；直接回前一個完整頁，未停在逐字輸入的歷史 |
| TL-10 窄螢幕 | 390px 查看長標題，水平捲動後操作狀態及負責人 | 通過；文件寬度 390px；表格視窗 342px、內容 800px；長標題欄 280px 並換行，更新寫入真實資料庫 |
| TL-11 唯讀 | 將測試帳號切為只有 `project:read` 的角色，重整並切換檢視 | 通過；表格編輯選單 0、新增按鈕 0；看板快速新增 0，任務仍可查看 |
| TL-12 結案 | 還原成員角色，暫將專案改為 `DONE` 後重整 | 通過；顯示結案，表格編輯選單 0、新增按鈕 0；完成後還原 `ACTIVE` |

## URL 契約

- `view=board|table`，缺省或未知值使用看板。
- `q` 為原始關鍵字，篩選時才 trim 並忽略大小寫。
- `assignee` 為成員 ID、`__me__` 或 `__none__`；缺省表示全部。
- `cancelled=1` 顯示取消項目，缺省隱藏。
- `taskPage`、`taskPageSize`、`taskSort`、`taskOrder`、`taskExpanded` 保留表格位置、排序與樹狀展開。
- 編輯篩選與排序／展開使用 replace；切換檢視、翻頁及開任務為明確導航。
- 所有更新複製目前 query，只修改自身欄位。改篩選時重設 `taskPage`。

## 檢查與限制

- `yarn typecheck`、`yarn lint`、`yarn build` 均通過。Vite 仍提示大型 JavaScript chunk。
- Node 內建 `assert` 的 7 個導航斷言通過：抽屜與清單共用 key、query 次序不影響 key、不同檢視有不同 key、非專案路徑使用 history key、完整來源可往返、拒絕兩種外站來源。
- 為啟動目前來源的本機服務，用 JDK 21 執行 `mvnw install -DskipTests`，建置通過且沒有後端 diff。五個服務健康端點均為 `UP`。
- 後端沒有程式變更，未執行條件式要求的 Maven test 與 `spotless:check`。上述建置不代表有測試覆蓋。
- 未新增測試依賴。專案沒有 React 自動化測試設施；以上保留為可重現的手動回歸案例。若另核准自動化，最小方案為 `vitest`、`jsdom`、`@testing-library/react`、`@testing-library/user-event`，測試真實 router 與元件，只替換外部 API。
- 本次未重跑全部 T1 草稿、留言、附件及非成員案例，也沒有宣稱這些流程通過。

## 參考

比較 [React Router ScrollRestoration](https://reactrouter.com/api/components/ScrollRestoration)、[TanStack Router scroll restoration](https://tanstack.com/router/latest/docs/guide/scroll-restoration) 與 [Ant Design Table](https://ant.design/components/table/) 的做法。採用現有 React Router 原生還原、URL 狀態與 Ant Design 樹狀表格；沒有新增狀態保存框架或路由依賴。

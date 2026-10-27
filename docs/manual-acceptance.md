# 手動驗收基準

供同事與少量手動驗收人員使用。每輪先填版本、環境與帳號，再依案例操作並保存結果。本文件只建立驗收基準，不開放新入口、不修改產品、角色或資料結構。

## 本次盤點與判讀方式

- 盤點日期：2026-10-27，時區 `Asia/Taipei`。
- 原始碼基準：`master`，commit `050b6c978931eb3390e1ad647edb23a000226a27`。開始時 `HANDOFF.MD`、`TODO.md` 已有未提交修改；本次保留。執行中的服務版本尚未確認。
- 「有程式碼」表示已找到實作；「已接 UI」表示路由、選單或父元件確實引用，仍須滿足帳號權限；「實際驗證通過」必須有該版本、環境與帳號的執行證據。三者不能互相代替。
- 本次只完成原始碼核對與匿名本機 GET 嘗試。沒有任何登入或業務案例可列為通過，詳見「本次本機入口檢查」。建置成功也不能算案例通過。

## 可達功能盤點

以下路徑皆相對於本機前端 `http://localhost:3000`。功能權限名稱以 `/api/auth/me` 的 `permissions` 為準。

| 功能／入口 | 有程式碼 | 已接 UI | 入口條件與目前範圍 | 實際驗證通過 |
|---|---|---|---|---|
| 未登入畫面、登入、登出 | 是 | 是 | `AuthProvider` 顯示提供者按鈕；登入後上方列有「登出」 | 未執行 |
| `/` 首頁 | 是 | 是 | 登入後前往第一個有權限的選單；無權限時顯示「尚未開通任何功能」 | 未執行 |
| `/dashboard` 儀表板 | 是 | 是 | `project:read`；可開任務抽屜 | 未執行 |
| `/calendar` 團隊行事曆 | 是 | 是 | `project:read`；事件依連結前往對應頁面 | 未執行 |
| `/projects` 專案列表 | 是 | 是 | `project:read`；有卡片／表格切換，建立專案另需 `project:write` | 未執行 |
| `/projects/:id` 專案詳情 | 是 | 是 | `project:read` 加專案範圍；只有「任務」「成員」分頁 | 未執行 |
| 任務看板、搜尋與負責人篩選 | 是 | 是 | `TasksTab` 只掛 `TaskBoardView`；主任務看板、顯示已取消開關 | 未執行 |
| 新增任務、指派、拖曳狀態 | 是 | 是 | 專案可編輯且具 `task:write`；新增表單有「更多設定（選填）」 | 未執行 |
| `?task=<ID>` 任務抽屜 | 是 | 是 | 專案頁、儀表板、我的任務、工時頁有掛載；沒有獨立 `/tasks/:id` 路由 | 未執行 |
| 任務子任務、留言與活動、附件、工時 | 是 | 是 | 抽屜內分頁；工時寫入另需 `work-log:write` | 未執行 |
| 成員清單與管理 | 是 | 是 | 同專案成員可看；未關閉專案的經理或 `project:admin`，且有 `project:write` 才可管理 | 未執行 |
| 任務表格 `TaskTableView` | 是 | 否 | 只有元件定義，沒有引用入口；不能要求驗收人切換到表格 | 不適用：UI 未開放 |
| 甘特圖 `TaskGanttView` | 是 | 否 | 只有元件定義，沒有引用入口；不因 README 描述就算已開放 | 不適用：UI 未開放 |
| 標籤管理 `LabelTab` | 是 | 否 | 後端有標籤 API，元件沒有引用入口；任務表單／抽屜仍可選已有標籤 | 不適用：管理 UI 未開放 |
| 專案概覽 `OverviewTab`、專案工時 `WorkLogTab` | 是 | 否 | 專案詳情目前沒有這兩個分頁；全站 `/timesheet` 與抽屜工時另有入口 | 不適用：這兩個分頁未開放 |
| `/my-tasks` 我的任務、`/timesheet` 工時 | 是 | 是 | `project:read`；可開抽屜，工時新增另需 `work-log:write` | 未執行 |
| `/leave/requests`、`/leave/reviews` | 是 | 是 | 分別需 `leave:apply`、`leave:approve` | 未執行；本輪不驗收請假業務 |
| `/serial-numbers` 編號規則 | 是 | 是 | `serial-number:read` | 未執行；本輪不修改規則 |
| `/roles`、`/users` | 是 | 是 | `role:manage` | 未執行；本輪不調整角色或帳號 |
| `/announcements` 發送公告 | 是 | 是 | `notification:send` | 未執行；本輪不發送公告 |
| 上方全站搜尋、通知、教學、主題切換 | 是 | 是 | 搜尋需 `project:read`；通知點擊可能標記已讀，不能當純讀取探測 | 未執行 |

任務表格、甘特圖、標籤管理、概覽及專案工時分頁是否恢復，另行決定。本基準不接回任何入口；專案列表的表格已接上，與未接上的任務表格要分開記錄。

任務中的標籤選取仍有 UI，但不等於標籤管理已開放。[LabelSelect.tsx](../frontend/src/components/project/LabelSelect.tsx) 在沒有標籤時仍提示「請專案經理到『標籤』分頁新增」，該分頁目前不可達。驗收時記錄這個文案與入口落差，不為了照提示操作而接回分頁或新增標籤。

### 已核對的原始碼證據

連結相對於本文件。重跑時以當輪版本重新核對，不能用這份盤點保證後續版本行為。

| 證據 | 位置與核對重點 |
|---|---|
| E01 | [router.tsx](../frontend/src/app/router.tsx)、[menu.ts](../frontend/src/app/menu.ts)、[HomePage.tsx](../frontend/src/app/HomePage.tsx)：實際路由、選單權限與首頁導向 |
| E02 | [ProjectDetailPage.tsx](../frontend/src/pages/project/project-detail/ProjectDetailPage.tsx) 的 `TABS`、`Tabs`、`TaskDrawer`；[TasksTab.tsx](../frontend/src/pages/project/project-detail/components/tasks/TasksTab.tsx) 的 imports 與 render：只有任務／成員與看板。[ProjectListPage.tsx](../frontend/src/pages/project/project-list/ProjectListPage.tsx) 的 `Segmented` 則確實掛上專案卡片／表格切換 |
| E03 | [hooks.ts](../frontend/src/components/project/hooks.ts) 的 `useProjectContext`、`useTaskDrawer`：專案權限、`DONE`／`CANCELLED` 唯讀、正整數 `?task=`、關閉時保留其他 query 參數 |
| E04 | [TaskTableView.tsx](../frontend/src/pages/project/project-detail/components/tasks/TaskTableView.tsx)、[TaskGanttView.tsx](../frontend/src/pages/project/project-detail/components/tasks/TaskGanttView.tsx)、[LabelTab.tsx](../frontend/src/pages/project/project-detail/components/LabelTab.tsx)：檔案存在；搜尋 `frontend/src` 的元件名稱只找到定義，未找到引用 |
| E05 | [TaskFormModal.tsx](../frontend/src/pages/project/project-detail/components/tasks/TaskFormModal.tsx)、[TaskDrawer.tsx](../frontend/src/components/project/task-drawer/TaskDrawer.tsx)、[TaskTimeline.tsx](../frontend/src/components/project/task-drawer/TaskTimeline.tsx)、[TaskAttachments.tsx](../frontend/src/components/project/task-drawer/TaskAttachments.tsx)：新增、即時指派、留言、附件與唯讀控制 |
| E06 | [SecurityConfig.java](../backend/web-server/src/main/java/dev/jianwii/web/security/SecurityConfig.java)、[WebAuthController.java](../backend/web-server/src/main/java/dev/jianwii/web/auth/WebAuthController.java)、[LoginSuccessHandler.java](../backend/web-server/src/main/java/dev/jianwii/web/security/LoginSuccessHandler.java)：提供者清單可匿名讀取；其他 API 要登入；CSRF；登入會同步使用者；登出目標依提供者決定 |
| E07 | [PermissionAuthorityFilter.java](../backend/web-server/src/main/java/dev/jianwii/web/security/PermissionAuthorityFilter.java)、[ProjectAccessGuard.java](../backend/web-server/src/main/java/dev/jianwii/web/project/ProjectAccessGuard.java)：逐次載入功能權限、成員範圍、經理限制與 `project:admin` 範圍例外 |
| E08 | [WebProjectController.java](../backend/web-server/src/main/java/dev/jianwii/web/project/WebProjectController.java)、[WebTaskController.java](../backend/web-server/src/main/java/dev/jianwii/web/project/WebTaskController.java)、[WebTaskCommentController.java](../backend/web-server/src/main/java/dev/jianwii/web/project/WebTaskCommentController.java)、[WebTaskAttachmentController.java](../backend/web-server/src/main/java/dev/jianwii/web/project/WebTaskAttachmentController.java)：查詢／寫入權限、附件內容 GET 與路徑 |
| E09 | [ProjectRules.java](../backend/project-service/src/main/java/dev/jianwii/project/service/ProjectRules.java)、[TaskService.java](../backend/project-service/src/main/java/dev/jianwii/project/service/TaskService.java)、[TaskCommentService.java](../backend/project-service/src/main/java/dev/jianwii/project/service/TaskCommentService.java)、[TaskAttachmentService.java](../backend/project-service/src/main/java/dev/jianwii/project/service/TaskAttachmentService.java)：關閉專案拒絕寫入、負責人必須是成員、留言作者與附件上傳者限制 |
| E10 | [auth.http](../http/auth.http)、[logout.http](../http/logout.http)、[project.http](../http/project.http)：既有登入、CSRF、任務、留言與結案範例。`logout.http` 註解仍寫 authentik；目前以 E06 的提供者行為為準 |
| E11 | [vite.config.ts](../frontend/vite.config.ts)、[web-server application.yml](../backend/web-server/src/main/resources/application.yml)、[後端 README](../backend/README.md)、[前端 README](../frontend/README.md)：3000 代理 8080、服務與管理連接埠、本機啟動需求 |
| E12 | [GlobalSearch.tsx](../frontend/src/components/search/GlobalSearch.tsx)、[NotificationBell.tsx](../frontend/src/components/notification/NotificationBell.tsx)：搜尋產生 `/projects/<ID>?tab=tasks&task=<ID>`；通知點擊連結並可能寫入已讀狀態 |

## 執行前先確認環境

MA-00 可先做匿名唯讀檢查。**MA-01 起，包括登入與登出，只能在已核准的本機獨立測試環境執行。** 登入會寫入使用者與 Session；查詢 API 雖用 POST，也需要有效 Session 與 CSRF。不得以為只看畫面就不會寫入。

本次沒有可確認的測試帳號、資料 ID 或環境核准紀錄，因此沒有登入、啟停服務、執行 SQL、建立資料、改角色或修改設定。歷史 HANDOFF 中的 `spring_core_lab_verify_*` 庫名只供環境負責人核對，不能直接當作本輪已核准環境。

執行人與環境負責人共同填完下表後，才開始登入與業務案例。不清楚任何資料指向時，將案例記為「阻擋」，先完成不受影響的紀錄。

| 檢查項目 | 本輪需填的確認資訊 | 本次狀態 |
|---|---|---|
| 環境授權 | 負責人、核准日期、允許登入與哪些案例寫入、測試資料可保留期限 | 未提供 |
| 本機資料隔離 | MySQL host／port；basic-data、project、notification、web-server Session／OIDC 的實際 schema 名稱；均與既有業務庫分開 | 未確認執行中 datasource |
| 檔案隔離 | `basic-data` 共用上傳目錄；有舊附件時另記 `project.attachment.dir`；只放人工測試檔 | 未確認 |
| 郵件與排程 | 寄信停用或使用已核准的測試收件方式；指派、留言、摘要不得寄給業務使用者 | 未確認 |
| OIDC 與帳號 | 已核准的測試登入身分、提供者 ID、本機 callback；不填 client secret、密碼或 token | 未確認 |
| 服務版本 | 前端與各後端服務實際版本、profiles、設定來源；不只記本地 Git HEAD | 未確認 |

需要現成結構、OIDC 設定與四個可登入帳號。若缺資料庫、角色或資料結構，由負責人先提出並核准準備方案，本文件不授權新增依賴、DDL、正式帳號或角色變更。公開 `local` 設定含自動建表／更新行為，不要為了探測入口直接用預設設定啟動服務。

環境確認後，服務需求為本機 MySQL、Eureka、basic-data、project-service、notification-service、web-server 與 Vite。沿用 Java 21、Node `^20.19.0 || >=22.12.0`、Yarn 4.18.1；啟動方式見 E11，Windows 使用 `mvnw.cmd`。不使用 Docker，也不新增服務。

### 每輪環境與版本紀錄

每輪複製下表及案例結果範本到團隊已核准的紀錄位置；本文件保留為基準。下一輪複製案例與預期結果，清空實際結果，不沿用上一輪的通過狀態。

| 欄位 | 本輪填寫 |
|---|---|
| Run ID | 例如 `MA-20261027-01` |
| 執行人／起訖時間 | 姓名或代號；`Asia/Taipei` |
| 原始碼 | branch、完整 commit；`git status --short`；有未提交產品修改時記錄差異檔與版本證據位置 |
| 實際執行版本 | 前端版本；各後端版本／建置時間；無法確認的服務逐一標示 |
| 工具 | Java、Node、Yarn、OS、瀏覽器名稱／版本、視窗尺寸 |
| 前後端網址 | 預設 `http://localhost:3000`、`http://localhost:8080`；不填正式站 |
| 本機設定 | profiles 與設定來源；只記檔案路徑或設定代號，不貼完整設定 |
| MySQL／上傳／郵件 | 上表已核准的非機密資訊與隔離確認人 |
| 登入提供者 | 例如 `google`；預設 callback `http://localhost:3000/auth/callback/google`，須與實際設定相符 |
| 帳號與測資 | 下方 A／B／N／G 的 user ID、有效 permissions、專案角色；本輪各專案、任務、留言、附件 ID |
| 結論 | 通過／失敗／阻擋／未執行／不適用各幾例；BUG 與複測紀錄連結 |

只記帳號代號與應用內 user ID，避免紀錄個人信箱。不要貼 Cookie、CSRF 值、OIDC code／token、密碼、完整請求標頭或未遮蔽 HAR。截圖只保留案例所需畫面與狀態。

## 最小帳號與資料

應用角色提供的功能權限，與專案成員的 `MANAGER`／`MEMBER` 是兩種設定。`role:manage` 不等於 `project:admin`；`project:admin` 只免除專案成員／經理範圍限制，API 仍檢查對應的功能權限。

| 代號 | 既有測試身分條件 | 最少有效 permissions | 用途 |
|---|---|---|---|
| A | 測試專案的 `MANAGER`，沒有 `project:admin` | `project:read`、`project:write`、`task:write` | 專案經理、被指派者、跨成員讀取 |
| B | 與 A 同專案的 `MEMBER`，沒有 `project:admin` | `project:read`、`task:write`；為驗證經理限制，另有 `project:write` | 建任務、指派、留言、附件；有功能權限仍不能管理專案 |
| N | 不是任何本輪測試專案成員，沒有 `project:admin` | `project:read`、`project:write`、`task:write` | 排除「缺功能權限」干擾，專門驗證非成員被擋 |
| G | 有 `project:admin`，不是本輪測試專案成員 | `project:read`、`project:admin`、`project:write`、`task:write` | 驗證管理者免成員限制，以及仍受結案唯讀限制 |

這是測試身分需求，不是授權變更指令。優先使用已核准帳號；欠缺任一身分就標示相關案例未執行，不臨時替人加權限。每個帳號用獨立瀏覽器設定檔，並用同一個設定檔的 `/api/auth/me` 核對 user ID 與有效權限，不能只憑顯示名稱判斷。

| 資料代號 | 需要的現成測資與紀錄 |
|---|---|
| `P_OPEN` | 名稱含 Run ID 的專用 `ACTIVE` 專案；A 是經理、B 是成員，N／G 不在成員清單。不要使用既有業務專案 |
| `T_OPEN` | MA-03 由 B 建立的主任務；記錄 ID、編號、標題、原始狀態與負責人。後續案例只改這個任務 |
| `P_DONE`／`T_DONE` | 獨立 `DONE` 專案及一個任務；A／B 仍為成員，預先備有 B 的留言與附件 |
| `P_CANCELLED`／`T_CANCELLED` | 獨立 `CANCELLED` 專案及一個任務；成員與預備留言／附件同上。`ON_HOLD` 不屬於本文件的結案唯讀樣本 |
| `C_OPEN`／`F_OPEN` | B 在 MA-06 的留言 ID、MA-07 的附件關聯 ID。附件內容入口使用 `TaskAttachment.id`，不是共用 `UploadFile.id` |
| 人工檔案 | 非空的 `MA-<Run ID>.txt` 與小張 PNG，各小於 10 MiB；記錄文字內容／檔案大小，必要時記 SHA-256。不含業務資料 |
| 舊連結 | 保存 `/projects/<P_OPEN>?tab=tasks&task=<T_OPEN>`；另保存 `/my-tasks?task=<T_OPEN>`。不是拿猜測的任務 ID 測試 |

fixture 的準備同樣會寫入。只在上面的環境授權涵蓋時，由指定負責人建立缺少的人工測資；本次未執行。保留各輪資料與證據供複測，不自行刪除檔案、清庫或改回角色。

### 最小驗收矩陣

「可」仍需具備表列功能權限。下表為程式推導的預期，不是本次實測結果。

| 行為 | A 經理 | B 同專案成員 | N 非成員 | G 管理者且非成員 |
|---|---|---|---|---|
| 登入／登出 | 可 | 可 | 可 | 可 |
| 讀 `P_OPEN`、任務、留言與附件 | 可 | 可 | 專案查詢空結果；明確任務／成員／留言／附件請求 403 | 可 |
| 建任務、指派給 A／B、留言、上傳 | 可 | 可 | 403 | 可；仍不能指派給非成員 |
| 編輯專案、管理成員 | 可 | 403；UI 無管理控制 | 403 | 可 |
| `DONE`／`CANCELLED` 下讀取 | 可 | 可 | 拒絕 | 可 |
| `DONE`／`CANCELLED` 下寫任務、留言、附件 | 400 | 400 | 403 | 400 |
| 關閉專案的狀態選單 | 可看到重新開啟 | 無狀態管理入口 | 專案不可見 | 可看到重新開啟 |

G 的管理權限不取代留言作者或附件上傳者。驗收只確認跨作者編輯／刪除控制不可見，不執行刪除。工時、請假、公告及角色管理業務不在這份最小矩陣內。

## 案例

依 MA-00 → MA-11 順序執行，MA-12 登出最後做。各帳號、兩種關閉狀態須分開留一筆結果，不以某一帳號的通過代表其他帳號通過。

本次未執行原因：`ENV`＝沒有可確認的入口／環境核准與隔離；`ACCOUNT`＝未確認四種測試身分；`DATA`＝未取得已核准測資 ID。實際執行後，填觀察值與證據，不只把「未執行」改成勾選。

### MA-00 匿名入口與服務可用性

| 前置條件 | 操作 | 預期結果 | 實際結果 |
|---|---|---|---|
| 只限 localhost；不沿用登入 Cookie、不跟隨外站轉址 | GET 前端 `/`、`/projects`、`/api/auth/providers`、`/api/auth/me`；再查 8080 的 providers／me 及五個管理 port 的 health，詳見下方入口表 | 前端 HTML 200；providers 200 且有核准提供者；匿名 me 401；health 200 且 `UP`。HTML 200 只能證明有回應，不能證明 React、代理或業務正常 | 已嘗試但阻擋：所有請求逾時，未取得 HTTP 狀態；見本次入口紀錄 |

### MA-01 登入與身分確認

| 前置條件 | 操作 | 預期結果 | 實際結果 |
|---|---|---|---|
| 環境授權已包含登入／Session 寫入；A、B、N、G 各自使用獨立瀏覽器設定檔 | 開前端，點「使用 <提供者名稱> 登入」，選核准身分；完成後在同一設定檔開 `/api/auth/me`；逐一記錄 user ID／permissions，再回 `/projects` | 登入返回本機前端；me 200 且身分、權限符合帳號表。登入提供者畫面完成或按鈕存在都不能單獨算通過 | 未執行：ENV、ACCOUNT |

### MA-02 專案範圍與實際 UI 入口

| 前置條件 | 操作 | 預期結果 | 實際結果 |
|---|---|---|---|
| 四帳號已登入；已知 `P_OPEN` | A／B／G 在專案列表切換卡片／表格，再開測試專案，檢查任務與成員分頁、看板及管理控制；N 搜尋同專案名稱，再直接開 `/projects/<P_OPEN>`；A 開 `?tab=labels` | 專案列表兩種視圖均可開同一專案。A／B／G 可讀；A／G 有專案管理控制，B 沒有。N 列表查不到，直連顯示「找不到專案」，不露出內容。專案詳情沒有任務表格／甘特圖切換、標籤管理／概覽／專案工時分頁；未知 tab 回到任務 | 未執行：ENV、ACCOUNT、DATA；E01–E04 已靜態核對 |

### MA-03 建立任務

| 前置條件 | 操作 | 預期結果 | 實際結果 |
|---|---|---|---|
| B 在 `P_OPEN`；允許新增人工任務 | 點「新增任務」，先留白標題按「儲存」；再輸入 `MA-<Run ID>-任務`，狀態「待辦」，負責人留空，儲存；記錄為 `T_OPEN`，重整後用標題搜尋 | 空白標題提示「請輸入標題」，不建立。有效提交只新增一個主任務，取得編號，待辦且未指派；重整仍存在 | 未執行：ENV、ACCOUNT、DATA |

### MA-04 指派與不同成員的讀取

| 前置條件 | 操作 | 預期結果 | 實際結果 |
|---|---|---|---|
| `T_OPEN` 仍未完成；A、B 是成員，N、G 非成員 | B 點看板卡片，在抽屜把「負責人」改為 A；重整再看。A 開「我的任務」找到此任務，回看板使用「指派給我」篩選；檢查負責人選單 | 指派保存，A 可找到同一 ID，B 仍可開任務；選單只列專案成員，不出現 N／G。G 有管理權限也不因此成為可指派成員 | 未執行：ENV、ACCOUNT、DATA |

### MA-05 看板狀態

| 前置條件 | 操作 | 預期結果 | 實際結果 |
|---|---|---|---|
| B 可編輯 `T_OPEN`；以無 WIP 阻擋的測試專案執行 | 將卡片由「待辦」拖到「進行中」，重整；再改為「完成」，重整後開抽屜檢查狀態與活動 | 同一任務在對應欄位，狀態與抽屜一致且有變更紀錄；不產生重複任務。任務完成不等於專案結案 | 未執行：ENV、ACCOUNT、DATA |

### MA-06 留言

| 前置條件 | 操作 | 預期結果 | 實際結果 |
|---|---|---|---|
| B 可編輯 `T_OPEN`；通知限測試環境 | B 開「留言與活動」，輸入 `MA-<Run ID>-留言` 按「送出」，記 `C_OPEN`；重整；A 開相同任務閱讀；B 編輯自己的留言並儲存 | 留言內容、作者與時間可辨識，重整及換帳號仍可讀；B 可編輯自己的留言；A 不顯示 B 留言的編輯／刪除控制。只看見成功提示還不算保存通過 | 未執行：ENV、ACCOUNT、DATA |

### MA-07 附件上傳與下載

| 前置條件 | 操作 | 預期結果 | 實際結果 |
|---|---|---|---|
| 上傳路徑已隔離；兩個人工檔案非空且小於 10 MiB；B 可編輯 `T_OPEN` | B 在「附件」上傳 txt 與 PNG，記附件關聯 ID、名稱、大小；重整；A 由任務附件入口下載 txt、點 PNG 預覽，對照內容／大小。只觀察刪除控制，不點刪除 | 檔案跨帳號可讀且重整仍在；txt 下載內容相同，PNG 可預覽；下載經 `/api/task-attachment/<F_OPEN>/content`。B 有自己附件的刪除控制，A 沒有 | 未執行：ENV、ACCOUNT、DATA |

### MA-08 結案與取消專案唯讀

| 前置條件 | 操作 | 預期結果 | 實際結果 |
|---|---|---|---|
| `P_DONE/T_DONE`、`P_CANCELLED/T_CANCELLED` 已備妥；A／B／G 登入；授權涵蓋預期被拒絕的寫入測試 | 三帳號分別對兩個專案重整，查看任務、成員、既有留言與附件；檢查新增、指派、拖曳、留言、上傳及成員管理控制；依下方請求表嘗試任務新增／修改／移動、留言與上傳 | 仍可讀／下載；抽屜顯示「專案已結案或取消，任務唯讀」，編輯控制停用或隱藏。A／G 可見重新開啟入口，本例不點選。後端寫入均 400，訊息為「專案已結案或取消，請先重新開啟」，資料不變；管理者也一樣 | 未執行：ENV、ACCOUNT、DATA；兩種狀態均須另記結果 |

### MA-09 非成員直接存取遭拒

| 前置條件 | 操作 | 預期結果 | 實際結果 |
|---|---|---|---|
| N 已確認具有功能權限但沒有專案成員資格；取得人工測資 `P_OPEN/T_OPEN/F_OPEN` | N 直接開專案與舊任務連結；依下方表查專案、成員、任務、留言、活動、附件清單；同一設定檔新分頁 GET 附件內容；另送一個測試任務新增請求 | 專案 query 為 200、空陣列；指定專案／任務的其餘查詢、附件 GET 與新增任務為 403，原因是非成員。UI 不顯示任務、留言或檔案內容；403 若是 CSRF 錯誤不能算通過 | 未執行：ENV、ACCOUNT、DATA |

### MA-10 專案經理與管理者邊界

| 前置條件 | 操作 | 預期結果 | 實際結果 |
|---|---|---|---|
| B 有 `project:write` 但只是 MEMBER；G 有 `project:admin` 且不在成員表；允許修改專用任務標題 | B 依請求表 PATCH `P_OPEN` 名稱，再嘗試把 `T_OPEN` 指派給 N；G 從列表開專案、讀任務，將 `T_OPEN` 標題加上 `-G`，重整。A／B 回看任務及成員表 | B 改專案 403「只有專案經理可以執行此操作」；指派 N 為 400，原指派不變。G 可讀且任務修改成功，仍不是成員；成員表沒有因操作而新增 G。不得改角色來讓本例通過 | 未執行：ENV、ACCOUNT、DATA |

### MA-11 舊 `?task=` 連結

| 前置條件 | 操作 | 預期結果 | 實際結果 |
|---|---|---|---|
| 已登入的 A／B 有 `T_OPEN` 存取權；保存本輪舊格式網址；無未送出草稿 | 直接開 `/projects/<P_OPEN>?tab=tasks&task=<T_OPEN>`，重整、複製到相同身分的新分頁；關閉抽屜後按瀏覽器上一頁／下一頁；另開 `/my-tasks?task=<T_OPEN>`；將 task 改為 `abc`、`0` 各檢查一次 | 有效 ID 開同一任務；重整／分享可重開；關閉只移除 task，保留 tab；關閉後上一頁可再開、下一頁關閉；非法值不開抽屜且頁面可用。N 的相同連結仍拒絕存取 | 未執行：ENV、ACCOUNT、DATA |

`useTaskDrawer` 仍是目前分享契約。登入成功 handler 固定回前端首頁，沒有還原原始 task query 的實作；匿名開舊連結時先顯示登入，登入後重新開保存的原始連結。不要把自動還原登入前網址當作已實作功能，也不要在 `/` 或 `/calendar` 加 task 就預期有抽屜。

### MA-12 登出與登出後存取

| 前置條件 | 操作 | 預期結果 | 實際結果 |
|---|---|---|---|
| 各帳號完成其他案例；環境授權包含登出 | A／B／N／G 各自點上方「登出」；回前端後在同一設定檔開 `/api/auth/me`，再開舊任務連結並重整 | 本站 me 401，舊連結顯示登入畫面且不露出任務。Google 沒有 OIDC 登出端點，返回本站前端即可，不要求 Google 帳號也登出。其他提供者依實際設定完成返回 | 未執行：ENV、ACCOUNT |

## 同一瀏覽器中的 API 核對方式

只在已核准測試環境、完成 MA-01 的 `http://localhost:3000` 分頁使用。開開發者工具 Network 保留狀態；先重整或讀取 `/api/auth/me` 取得當前 CSRF，再在 Console 定義以下函式。它只輸出 HTTP 狀態、錯誤與陣列筆數，不輸出 Cookie、token 或完整資料。

```js
async function maRequest(method, path, body) {
  if (location.origin !== 'http://localhost:3000') {
    throw new Error('請回已核准的 localhost:3000 測試分頁');
  }
  const csrf = document.cookie.split('; ').find((item) => item.startsWith('XSRF-TOKEN='));
  if (!csrf) throw new Error('請先在同一設定檔讀取 /api/auth/me，再重試');
  const multipart = body instanceof FormData;
  const response = await fetch(path, {
    method,
    credentials: 'same-origin',
    headers: {
      ...(multipart ? {} : { 'Content-Type': 'application/json' }),
      'X-XSRF-TOKEN': decodeURIComponent(csrf.slice('XSRF-TOKEN='.length)),
    },
    body: multipart ? body : JSON.stringify(body),
  });
  const payload = await response.json();
  console.log({
    status: response.status,
    result: payload.result,
    errorCode: payload.errorCode,
    message: payload.message,
    count: Array.isArray(payload.data) ? payload.data.length : undefined,
  });
}
```

下列 `P_OPEN`、`T_OPEN` 等符號，先在 Console 用本輪已記錄的數字 ID 宣告，例如 `const P_OPEN = ...;`，將 `...` 換成實際 ID。不可填業務資料 ID；帳號切換後重新確認 `/api/auth/me`。`await maRequest('POST', '/api/project/query', { ids: [P_OPEN] });` 是查詢範例。

| 案例／帳號 | method 與 path | body | 預期 |
|---|---|---|---|
| MA-09／N | POST `/api/project/query` | `{ ids: [P_OPEN] }` | 200，`result: true`、`count: 0` |
| MA-09／N | POST `/api/project-member/query` | `{ projectId: P_OPEN }` | 403，非成員 |
| MA-09／N | POST `/api/task/query` | 先 `{ projectId: P_OPEN }`，再 `{ ids: [T_OPEN] }` | 兩者均 403，非成員 |
| MA-09／N | POST `/api/task-comment/query`、`/api/task-activity/query`、`/api/task-attachment/query` | 各送 `{ taskId: T_OPEN }` | 各為 403，非成員 |
| MA-09／N | GET `/api/task-attachment/<F_OPEN>/content` | 無；同一瀏覽器新分頁開網址，不使用 JSON 函式 | 403；無檔案內容 |
| MA-09／N | POST `/api/task` | `{ projectId: P_OPEN, title: 'MA-非成員不得建立' }` | 403，非成員；不新增 |
| MA-10／B | PATCH `/api/project/<P_OPEN>` | `{ name: 'MA-一般成員不得修改' }` | 403，只有專案經理可以執行此操作 |
| MA-10／B | PATCH `/api/task/<T_OPEN>` | `{ assigneeId: '<N 的應用 user ID>' }` | 400，不是此專案的成員；user ID 用字串 |
| MA-08／A、B、G | POST `/api/task` | `{ projectId: P_CLOSED, title: 'MA-關閉專案不得建立' }` | 400，專案已結案或取消 |
| MA-08／A、B、G | PATCH `/api/task/<T_CLOSED>` | `{ title: 'MA-關閉專案不得修改' }` | 400，專案已結案或取消 |
| MA-08／A、B、G | POST `/api/task/<T_CLOSED>/move?status=IN_PROGRESS` | `{}` | 400，專案已結案或取消 |
| MA-08／A、B、G | POST `/api/task-comment` | `{ taskId: T_CLOSED, content: 'MA-關閉專案不得留言' }` | 400，專案已結案或取消 |
| MA-08／A、B、G | POST `/api/task-attachment/upload?taskId=<T_CLOSED>` | multipart，見下方 | 400，專案已結案或取消 |

`P_CLOSED/T_CLOSED` 先取 `P_DONE/T_DONE`，再取 `P_CANCELLED/T_CANCELLED`，三個帳號各跑一次。path 中 `<...>` 要換成對應數字。上傳負向案例使用人工文字，不需要讀取本機其他檔案：

```js
const form = new FormData();
form.append('file', new Blob(['manual acceptance'], { type: 'text/plain' }), 'MA-readonly.txt');
await maRequest('POST', `/api/task-attachment/upload?taskId=${T_CLOSED}`, form);
```

預期被拒絕的寫入如果意外成功，立即停止該案例，記錄新資料 ID／改變欄位、HTTP 狀態與 BUG；不要為了恢復畫面繼續寫入或刪除。由環境負責人處理測資。

401 代表本輪 Session 不可用；403 若訊息是「請求驗證失敗，請重新整理頁面後再試」，是 CSRF 尚未通過，不能算成員權限驗收成功。「沒有此功能的權限」也不能證明成員範圍正確。先修正測試前提，再分開記錄重跑。既有 `http/project.http` 含新增、修改、刪除及結案，不可直接整份 Run All；不得把 Cookie 值貼回驗收文件。

## 本次本機入口檢查

2026-10-27 09:48–09:50 `Asia/Taipei`，使用不帶 Cookie、不保存 Cookie、不跟隨轉址的 HTTP GET。sandbox 首次回通訊端權限不足；提權後 localhost 每次最多等待 3 秒、127.0.0.1 每次最多等待 2 秒，仍全部為 `TaskCanceledException`，沒有取得 HTTP 回應。不能據此判定是服務未啟動或服務故障，原因未確認。

下一輪可在 PowerShell 用下列指令重查 MA-00。`-q` 忽略個人 curl 設定，指令不帶 Cookie，也不跟隨轉址；前端只記 HTTP 狀態，匿名 API／health 可記錄回應的非機密內容。依入口表替換路徑與 port，代理入口也要查。`HTTP 000` 表示沒有取得 HTTP 回應，不是應用程式回傳的狀態碼。

```powershell
curl.exe -q --noproxy "*" --max-time 5 --silent --show-error --output NUL --write-out "HTTP %{http_code}\n" "http://localhost:3000/"
curl.exe -q --noproxy "*" --max-time 5 --silent --show-error --write-out "\nHTTP %{http_code}\n" "http://localhost:8080/api/auth/providers"
curl.exe -q --noproxy "*" --max-time 5 --silent --show-error --write-out "\nHTTP %{http_code}\n" "http://localhost:8080/api/auth/me"
curl.exe -q --noproxy "*" --max-time 5 --silent --show-error --write-out "\nHTTP %{http_code}\n" "http://127.0.0.1:9080/actuator/health"
```

| 已嘗試網址 | 實際結果 |
|---|---|
| `http://localhost:3000/`、`http://localhost:3000/projects` | 逾時；未驗證前端 HTML、畫面或路由 |
| `http://localhost:3000/api/auth/providers`、`http://localhost:3000/api/auth/me` | 逾時；未驗證 Vite 代理、提供者清單或匿名 401 |
| `http://localhost:8080/api/auth/providers`、`http://localhost:8080/api/auth/me` | 逾時；未取得提供者或 HTTP 狀態 |
| `http://localhost:9080/actuator/health` | 逾時；web-server 健康未知 |
| `http://localhost:9081/actuator/health` | 逾時；basic-data 健康未知 |
| `http://localhost:9082/actuator/health` | 逾時；project-service 健康未知 |
| `http://localhost:9083/actuator/health` | 逾時；notification-service 健康未知 |
| `http://localhost:9761/actuator/health` | 逾時；Eureka 健康未知 |
| 將 3000 `/`、8080 providers／me、上述五個 health 改用 `127.0.0.1` 再查 | 仍逾時；未取得 HTTP 狀態 |

health 應在獨立管理 port 查；不要把主服務 8080 的 `/actuator/health` 當可用入口。即使下輪 health `UP`，仍需另外驗證登入、成員範圍、資料保存與附件，不能直接通過整份清單。

本次沒有瀏覽器登入或 UI 操作；MA-01–MA-12 全部未執行。沒有跑 build、lint、typecheck 或 Maven 測試：本次只改文件，這些指令也無法證明手動案例通過。

## 結果、BUG 與複測紀錄

每個案例／帳號／資料狀態各留一列；有多個預期時，在觀察欄逐項寫結果。未具備前提用「未執行」或「阻擋」，實際操作與預期不符才用「失敗」。「不適用」須寫原因，不能拿來略過缺帳號的必測案例。

| Run ID | 案例 ID | 版本 | 環境代號 | 帳號代號／user ID／permissions／專案角色 | 專案／任務／附件 ID | 時間／執行人 | 狀態 | 實際觀察與 HTTP 狀態 | 證據／BUG | 複測來源 |
|---|---|---|---|---|---|---|---|---|---|---|
| 待填 | MA-xx | 待填 | 待填 | 待填 | 待填 | 待填 | 未執行 | 待填 | 待填 | 首次執行或前次失敗列連結 |

失敗時至少保留可重現的起始資料、操作、預期／實際、版本與去除機密的證據；若在核准測試專案建立 BUG 任務，將 ID／連結填回本列，不建立正式資料。修補後新增一列，連回原失敗紀錄，不覆蓋失敗結果。BUG 任務完成不等於驗收通過，必須由對應案例在修補版重新執行。

一輪完整通過需 A／B／N／G、兩種關閉狀態與所有適用的 MA-00–MA-12 都有實際結果；入口未開放的元件保留「不適用」，額外業務功能仍標為未涵蓋。只有部分案例執行時，結論寫清楚通過範圍與剩餘阻礙，不寫整體驗收通過。

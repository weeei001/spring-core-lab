# 會議紀錄與後續待辦 MVP

> 後續實作進度，2026-10-27：本輪已完成會議紀錄基礎讀寫與獨立編輯頁，並經另次核准在全新本機獨立庫套用增量 SQL。實測結果與未驗證項目見 [會議驗收](meeting-acceptance.md)。本文以下「未實作／未執行」描述保留為原 T3 核准時點；轉任務、關聯寫入與完整回退演練仍待後續處理。

> 後續變更：使用者要求建立 Task 與會議關聯在 project-service 的同一個本地交易完成。建立時改用 `POST /api/meeting/{id}/tasks`，同時要求 `meeting:write`、`task:write` 與該會議的修改資格，內部沿用 `TaskService.create`。既有 Task 的連結與解除仍使用下列關聯 API。若建立回應中斷，先查會議待辦或專案任務清單，再決定是否重試；取號服務的已取號碼不會隨本地交易回退。以下 R5 兩次請求的描述保留為原核准紀錄。

狀態：**R1–R8 方案已核准，2026-10-27**。使用者確認「核准方案」，作為後續實作基準。本次僅登記核准，保留 [增量 SQL 草稿](sql/meeting-mvp.draft.sql)，不修改應用程式、不執行 SQL、不接觸業務資料。已核准範圍涵蓋本文 R1–R8 的欄位、契約、權限、行為與驗收方式；功能尚未實作，所有驗收案例尚未執行。

## 1. 目標與現有依據

服務對象是同事與少量手動驗收人員。會後能補寫紀錄、查回決議，只有需要後續處理的事項才建立或連結 Task。Task 的 `assigneeId`、`dueDate`、`status` 是負責人、期限、完成狀態的唯一來源。

保留 Java 21、React／TypeScript、Ant Design 與現有分層，不新增依賴、微服務或 Docker。此次成功標準是完成可審查的欄位、權限矩陣、查詢／寫入 API、範例、增量 DDL、遷移／回退計畫及驗收案例；執行與實作須另行授權。

下列是已讀程式的現況，與後續提案分開：

| 現有權威來源 | 已確認行為與本方案的限制 |
| --- | --- |
| [`Project`、`ProjectMember`、`Task`](../backend/common/src/main/java/dev/jianwii/entity/project/) | `projectId` 使用 Long；使用者 ID 使用長度 100 的字串；Task 的專案建立後不可修改。成員關係以有效資料為準。 |
| [`BaseEntity`](../backend/common/src/main/java/dev/jianwii/common/entity/BaseEntity.java)、[`BaseService`](../backend/common/src/main/java/dev/jianwii/common/service/BaseService.java) | `dbMark=0` 有效、1 軟刪除；`createId/createTime/transId/transTime` 由系統維護。只改集合時須 `touch`，否則最後修改資訊不會更新。 |
| [`TaskApi`](../backend/common-api/src/main/java/dev/jianwii/api/project/TaskApi.java)、[`TaskQueryParameter`](../backend/common-api/src/main/java/dev/jianwii/api/project/TaskQueryParameter.java)、[`ProjectApi`](../backend/common-api/src/main/java/dev/jianwii/api/project/ProjectApi.java) | common-api 定義服務契約；query／queryPage 使用 POST，page 從 1 起算、size 1–100。內部成功回物件，web-server 包 `Result`。 |
| [`TaskService`](../backend/project-service/src/main/java/dev/jianwii/project/service/TaskService.java) | Task 的建立、修改、刪除會檢查專案未關閉；指派人須是有效成員；新增會維護編號、排序、活動與通知；刪除是軟刪除。不能直接 INSERT task 取代它。 |
| [`ProjectRules`](../backend/project-service/src/main/java/dev/jianwii/project/service/ProjectRules.java)、[`ProjectStatus`](../backend/common/src/main/java/dev/jianwii/entity/project/ProjectStatus.java) | `DONE` 與 `CANCELLED` 唯讀；`PLANNING`、`ACTIVE`、`ON_HOLD` 未關閉。 |
| [`ProjectAccessGuard`](../backend/web-server/src/main/java/dev/jianwii/web/project/ProjectAccessGuard.java)、[`Permission`](../backend/common/src/main/java/dev/jianwii/entity/basicdata/Permission.java) | web-server 檢查 authority 與有效專案身分。`project:admin` 略過成員／經理限制，但仍需操作的功能權限；不判斷全域角色名稱。project-service 信任 web-server 的授權邊界。 |
| [`OperatorHeaderFilter`](../backend/web-server/src/main/java/dev/jianwii/web/security/OperatorHeaderFilter.java) | 外部 `X-Operator` 會被登入 Session 的使用者 ID 覆寫；審計資訊不能採信 request body。 |
| [`ProjectService.beforeDelete`](../backend/project-service/src/main/java/dev/jianwii/project/service/ProjectService.java)、[`mysql-schema.sql`](../deploy/mysql-schema.sql) | 現況只擋仍有有效 Task 的專案刪除；既有主表 id 是 signed bigint、InnoDB、utf8mb4_unicode_ci。結構匯出只能供比對，不可整份重匯。 |

## 2. 最小操作流程【已核准 R1】

會後進專案新增紀錄，填寫標題、實際會議時間、參與者文字、紀錄／決議後儲存。`heldAt` 可早於 `createTime`，不要求事前議程、邀請、簽到、所有參與者都有帳號，也不以專案起訖日期限制補寫時間。未要求限制未來日期，MVP 只驗證日期時間有效。

一般決議直接寫在 `minutes`。需要指派、期限或完成追蹤時，選擇既有 Task，或先用現有 Task 建立流程新增，再連結到這份紀錄。Task 可以尚未指派或沒有期限，沿用現有契約。

不提供 Meeting 狀態、議程表、參與者帳號表、獨立 action item、附件、通知、全文搜尋或編輯歷程。MVP 不提供刪除 Meeting 的 API；誤植由有權限者修正文句。`dbMark` 只為沿用 BaseEntity 保留，不開放使用者改寫。

## 3. 資料模型【已核准 R2】

```text
Project 1 ── N Meeting
Meeting 1 ── N meeting_task N ── 1 Task
```

新增 `meeting`、`meeting_task` 兩張表，均放在 project-service 的資料庫。Meeting 繼承 BaseEntity；`taskIds` 可依既有 `@ElementCollection Set<Long>` 慣例映射關聯表。既有 project、task 與其他表不加欄位、不重建、不回填。

### meeting 欄位

以下必填與長度均為已核准 R2；字串上限以 Java `@Size` 的長度計算，API 先驗證。SQL 保留 NOT NULL 與儲存型別約束，不另做一份業務驗證。

| API／Java 欄位 | SQL 欄位與型別 | 寫入與意義 |
| --- | --- | --- |
| `id: Long` | `id bigint` PK、自增 | 系統產生，不另申請會議編號。 |
| `projectId: Long` | `project_id bigint NOT NULL` | 建立時必填，指向有效專案；建立後不可換專案。 |
| `title: String` | `title varchar(200) NOT NULL` | 必填、不可全空白；可修改。標題不要求唯一。 |
| `heldAt: LocalDateTime` | `held_at datetime(6) NOT NULL` | 必填，實際會議日期時間，與建立時間分開；可修正。 |
| `participants: String` | `participants varchar(2000) NOT NULL` | 必填、不可全空白；例如「小林、怡君、外部顧問陳先生」。不解析帳號、不授權。 |
| `minutes: String` | `minutes text NOT NULL` | 必填、不可全空白、最多 10000；紀錄與決議共用純文字欄位，可用段落區隔；畫面以文字呈現。 |
| `createId: String` | `create_id varchar(100) NOT NULL` | 系統填入建立者，定義為記錄人；永不隨後續編輯改變。跨 basic-data 不建 user FK。 |
| `createTime: LocalDateTime` | `create_time datetime(6) NOT NULL` | 系統建立時間。 |
| `transId: String` | `trans_id varchar(100) NOT NULL` | 系統填入最後修改人；建立時等於記錄人。 |
| `transTime: LocalDateTime` | `trans_time datetime(6) NOT NULL` | 系統最後修改時間；包含真正新增／移除任務關聯。 |
| `dbMark: Integer` | `db_mark int NOT NULL` | 沿用 0 有效、1 軟刪除；本 MVP 一律由系統建立為 0。 |

`heldAt` 的對外格式為無 offset 的 ISO local datetime，例如 `2026-10-26T14:00:00`，約定是 `Asia/Taipei` 的當地時間，不接受 `Z` 或帶 offset 的輸入以免默默轉時區。其 Java 型別沿用 BaseEntity 的 LocalDateTime。JVM 審計時間目前使用系統時間，JDBC 的 `serverTimezone=Asia/Taipei` 不等於 JVM 時區設定；驗證時須確認 JVM、連線與顯示均採相同約定，不修改既有時間資料。日期比較用解析後的時間值，不比字串。

### meeting_task 欄位與生命週期

| SQL 欄位 | 約束／用途 |
| --- | --- |
| `meeting_id bigint NOT NULL` | FK 到 meeting.id。 |
| `task_id bigint NOT NULL` | FK 到 task.id。 |
| `(meeting_id, task_id)` | 複合 PK；同一 Task 在同一 Meeting 只能連結一次。 |
| `(task_id, meeting_id)` | 反向索引，讓任務可查詢目前連結的會議。 |

允許多對多：同一個 Task 可在數次會議被追蹤；不強制標成「首次建立來源」。反查回的是目前關聯的會議，移除關聯後不再宣稱它仍是來源。本 MVP 不承諾完整歷史軌跡。

關聯表不複製 projectId、負責人、期限、狀態或任務標題。查回時從 Task 即時取得。每份 Meeting 最多連結 100 個 Task，對齊既有查詢上限；主／子任務皆可連結，但必須與 Meeting 同專案且尚未軟刪除。`DONE`／`CANCELLED` 的有效 Task 可以連結，顯示原狀態，不重新開啟；取消不冒充完成。

同專案與有效性由 project-service 在寫入關聯時檢查。FK 只能保證 ID 存在，不能保證 `dbMark=0` 或專案相同；不為此修改既有 Task 唯一鍵或增加 trigger。所有 FK 使用 RESTRICT，不串連刪除紀錄；不建立跨服務 user FK。

## 4. 權限矩陣【已核准 R3】

建議新增 **一個** `meeting:write`，用於新增紀錄、修改紀錄、加入／移除關聯；沿用 `project:read` 讀取，不新增 `meeting:read`。必要性是補寫紀錄與修改 Task 是不同能力：重用 `task:write` 會讓記錄人同時能修改／刪除任務；重用 `project:write` 又會把紀錄維護綁到專案／成員管理能力。若不需要這個能力分離，可另選重用權限，但本文推薦保留一個最小區分。

新 permission 未實作、未配置角色。未來須在 Permission enum 加入代碼，並由管理者明確授予；SQL 不寫 role 資料，也不根據「測試人員」「同事」等角色名稱判斷。既有角色不自動取得 `meeting:write`；超級管理員仍依既有權限展開機制處理。

表中 R=`project:read`、W=`meeting:write`、T=`task:write`。讀寫各自檢查對應 authority；W 不隱含 R，R 也不隱含 W。能否操作 Task 繼續由現有 Task API 的 authority 與專案限制決定。

| 登入者的有效專案身分 | 讀紀錄／關聯與反查 | 新增 Meeting | 改自己的 Meeting／關聯 | 改別人的 Meeting／關聯 | 建立／改 Task |
| --- | --- | --- | --- | --- | --- |
| 記錄人且仍是 MEMBER | 有 R 可 | 有 W 可 | 有 W 可 | 不可 | 有 T 可，仍走現有 Task 規則 |
| 其他 MEMBER | 有 R 可 | 有 W 可，成為新紀錄的記錄人 | 有 W 可 | 不可 | 有 T 可 |
| 專案 MANAGER | 有 R 可 | 有 W 可 | 有 W 可 | 有 W 可 | 有 T 可 |
| 非成員，包括已移除的記錄人 | 不可 | 不可 | 不可 | 不可 | 不可 |
| 持 `project:admin`，可為非成員 | 有 R 可跨專案讀 | 有 W 可 | 有 W 可 | 有 W 可 | 有 T 可；Task 自身資料規則仍有效 |

專案經理身分來自 `ProjectMember.role=MANAGER`，不是全域角色名稱。持 `project:admin` 只略過專案身分及作者／經理限制，不取代 R／W／T，也不略過同專案關聯限制。

**專案為 DONE 或 CANCELLED 時，所有 Meeting 寫入、關聯新增／移除皆拒絕，包括記錄人、MANAGER、project:admin。** 可依讀取權限查閱。先透過既有 `project:write`＋經理或 admin 的專案更新流程重新開啟後，才恢復紀錄寫入。ON_HOLD 沿用現況，仍可寫。

前端按鈕只呈現允許的操作。後端必須完成下列檢查：

1. web-server 用 `@PreAuthorize` 檢查 R 或 W。由持久資料取得 Meeting 的 projectId、createId，不能信任 body 宣稱的專案或作者；有效 MEMBER 只能修改 createId 等於登入者的紀錄，MANAGER／project:admin 可修改其他紀錄。
2. 詳情、列表、任務反查全部經 `ProjectAccessGuard` 的成員規則；必填 projectId 的查詢不提供任意跨專案列表。project:admin 也須給 projectId。Meeting、Project 都必須有效；反查 Task 必須有效且屬於指定專案。
3. project-service 沿用現有授權信任邊界，檢查專案有效／開啟、不可換專案、關聯 Task 同專案且有效、欄位與數量限制。一般寫入不重複做 web-server 的角色查詢；服務內 API 仍只允許經既有受信任入口呼叫。
4. 審計資訊由登入者及 JPA Auditing 填入。關聯真正改變時，在同一資料庫交易內更新 meeting 的 transId／transTime，沿用 `touch`；單純重查、重複連結或重複解除不修改時間。

## 5. 查詢與寫入契約【已核准 R4】

建議新增 common-api/project 的 `MeetingApi`／`MeetingQueryParameter`、project-service 的 Meeting controller／service／DAO、web-server 的 WebMeetingController 與 guard 擴充；前端未來依 `app → pages → components → api → core/shared` 接專案頁，以 Ant Design 表單與現有 Task 操作流程呈現。本次不建立這些程式檔。

路徑以下列 web-server 路徑為準；正式站沿既有 `/backend` 代理前綴。服務內同路徑回原始物件／PageResponse，web-server 包 `Result<T>`，成功沿用 HTTP 200。寫入 API 不直接操作 Task 的負責人、期限或狀態。

| 方法／路徑 | request／成功資料 | 後端規則 |
| --- | --- | --- |
| `POST /api/meeting/query` | MeetingQueryParameter → `List<Meeting>`，最多 size 筆 | R＋專案可讀；page 必須 1。 |
| `POST /api/meeting/queryPage` | 同條件 → `PageResponse<Meeting>` | R＋專案可讀；page 從 1 起算。 |
| `GET /api/meeting/{id}` | 無 body → Meeting | 先由有效 Meeting 取得專案，再檢查 R＋專案可讀；有效結案專案可讀。 |
| `POST /api/meeting` | `projectId,title,heldAt,participants,minutes` → Meeting | W＋有效成員或 admin＋requireOpen；初始 taskIds 為空。 |
| `PATCH /api/meeting/{id}` | 選填 `title,heldAt,participants,minutes` → Meeting | W＋記錄人或經理／admin＋requireOpen；null／省略表示不改，給空白必填欄位回 400。 |
| `GET /api/meeting/{id}/tasks` | 無 body → `List<MeetingTaskView>` | R＋Meeting 專案可讀；taskId 升冪；有效 Task 取即時欄位，軟刪除取占位資料。 |
| `POST /api/meeting/{id}/tasks/{taskId}` | 無 body → 更新後 Meeting | W＋可改這份 Meeting＋requireOpen；新增時 Task 必須有效、同專案；重複連結回原值。 |
| `DELETE /api/meeting/{id}/tasks/{taskId}` | 無 body → 更新後 Meeting | W＋可改這份 Meeting＋requireOpen；只解除這一對 ID；已無關聯回原值。 |

Meeting 回傳第 3 節全部欄位與 `taskIds: Long[]`，taskIds 升冪，未連結為 `[]`。Query 不回 Task 摘要，避免逐筆讀取任務；只在詳情使用 `/tasks`。系統欄位 `id/dbMark/createId/createTime/transId/transTime` 的輸入不生效，沿用既有系統維護行為。create／PATCH 若帶非 null 的 `taskIds` 回 400，必須走關聯 API；PATCH 若帶不同 projectId 回 400，帶同一 projectId 不改。其他未知輸入不新增相容規則。

### MeetingQueryParameter【已核准 R4】

| 欄位 | 契約 |
| --- | --- |
| `projectId` | 必填，包含 admin；不存在或已軟刪除的專案回 404，非成員回 403。 |
| `ids` | 選填，最多 100 個非 null ID；未給表示不以 ID 篩選，給 `[]` 回 400。與 projectId 用 AND。 |
| `taskId` | 選填，從 Task 反查目前會議；必須有效且 projectId 相同。以關聯表 EXISTS 篩選，避免重複列與錯誤 total。 |
| `fromHeldAt`、`toHeldAt` | 選填，包含上下界；from 不可晚於 to；以第 3 節的 local datetime 約定解析。 |
| `page`、`size` | 預設 1／20，size 1–100，沿用 QueryParameter／PageResponse 的 offset 上限。 |
| `sort` | 沿用 `[{"field":"heldAt","direction":"desc"}]` 格式；白名單 heldAt／createTime／transTime／id。未指定時 heldAt desc，再補 id asc 穩定分頁。 |

所有條件 AND，固定加 `meeting.db_mark=0`、`project.db_mark=0`。web-server 不接受使用者指定授權範圍；查詢前依 projectId 檢查成員。Meeting 詳情和 tasks 先解析真實所屬專案，不能用另一個已加入的 projectId 取代它。

### Task 即時資訊與解除關聯【已核准 R5】

`MeetingTaskView` 有 `taskId,deleted,code,title,assigneeId,dueDate,status`。有效 Task 的 `deleted=false`，其他值讀當下 Task；未指派／無期限保留 null。若 Task 後來被軟刪除，保留關聯，回 `taskId,deleted=true`，其餘欄位為 null，畫面顯示「任務已刪除」，不把它當成已完成，也不默默隱藏。這個 deleted 是由 Task.dbMark 計算的可用性，不是另一份工作狀態。

解除關聯只刪 `meeting_task` 對應列，不刪 Task、不改指派人／期限／狀態、不改 minutes、不影響其他會議關聯、不觸發 Task 活動或通知。解除既有關聯不要求 Task 仍有效，因此能清理已軟刪除 Task 的連結；仍需有權改 Meeting 且專案未關閉。讀取保留舊關聯，不因無效 Task 讓整份紀錄失敗。

建立後續待辦採兩個明確請求：先走既有 `POST /api/task`，成功取得 taskId 後再呼叫連結 API。第一步只需現有 Task 權限 T；第二步需要 W＋這份 Meeting 的修改資格。UI 應在開始前檢查兩種操作能力，實際請求仍由後端驗證。

若 Task 建立失敗，完全不建立關聯；若 Task 已建立但連結失敗，保留 Task，顯示其 ID 與「任務已建立，尚未連結」；只能重試連結，不能自動重送建立或補償刪除 Task。若建立回應中斷而不知道 taskId，先在專案任務清單確認，不盲目重建。這是兩次現有操作的可見結果，不引入跨服務交易、背景 retry 或新建任務捷徑。

內容更新與各次關聯變更分別使用 project-service 本地交易。MVP 不加版本欄位或鎖，沿用後送出的內容覆寫前次內容；目前只保留最後修改資訊，不保存歷次版本。若多人同時編輯成為實際問題，再另提衝突控制方案。

### 失敗回應【已核准 R4–R5】

沿用現有 ExceptionHandleController／WebExceptionHandleController 與 `Result.failure`：未登入 401；缺 authority／身分或作者資格 403；Meeting／Project／新增關聯的 Task 不存在或已軟刪除 404；欄位不合法、跨專案 Task、超過 100 個關聯或結案寫入為 400 BusinessException；資料庫約束衝突為 409。一般查詢在有效可讀專案下找不到符合紀錄時，回空 items 與 total 0。

新連結與重複連結均先檢查 authority、Meeting 修改資格、專案開啟，以及 Task 有效且同專案。解除關聯則先檢查 authority、Meeting 修改資格與專案開啟，再判斷是否存在；不得因重複請求跳過權限或結案檢查。錯誤訊息不包含其他專案的紀錄內容。

## 6. 範例資料【已核准 R2–R5】

以下 ID 與姓名全為虛構驗收資料。假設 projectId=701 有記錄人 `u-recorder`、經理 `u-manager`、成員 `u-member`；702 是另一專案。

會後隔天補寫，無需先建議程：

```json
{
  "projectId": 701,
  "title": "第一輪同事驗收回顧",
  "heldAt": "2026-10-26T14:00:00",
  "participants": "小林、怡君、外部顧問陳先生（無系統帳號）",
  "minutes": "紀錄：討論第一輪回饋。\n決議：本輪先驗證桌面版，行動版排下一輪。\n後續事項：修正報表日期，由連結任務追蹤。"
}
```

建立回應 `Result.data` 範例：

```json
{
  "id": 9001,
  "projectId": 701,
  "title": "第一輪同事驗收回顧",
  "heldAt": "2026-10-26T14:00:00",
  "participants": "小林、怡君、外部顧問陳先生（無系統帳號）",
  "minutes": "紀錄：討論第一輪回饋。\n決議：本輪先驗證桌面版，行動版排下一輪。\n後續事項：修正報表日期，由連結任務追蹤。",
  "taskIds": [],
  "createId": "u-recorder",
  "createTime": "2026-10-27T09:00:00",
  "transId": "u-recorder",
  "transTime": "2026-10-27T09:00:00",
  "dbMark": 0
}
```

先透過既有 Task API 建立 `{"projectId":701,"title":"修正報表日期","assigneeId":"u-member","dueDate":"2026-10-30","type":"BUG"}`，假設回 taskId=8101。再 `POST /api/meeting/9001/tasks/8101`，回傳 taskIds `[8101]`，並更新 Meeting 的 transId／transTime。

反查該 Task 的會議：

```json
{
  "projectId": 701,
  "taskId": 8101,
  "page": 1,
  "size": 20,
  "sort": [{ "field": "heldAt", "direction": "desc" }]
}
```

將上例送到 `/api/meeting/queryPage`，成功外層形狀為 `{"result":true,"data":{"items":[Meeting],"page":1,"size":20,"total":1},"errorCode":null,"message":null}`，此處 Meeting 是上方物件的示意佔位名稱。單份 Meeting 任務讀取的 `Result.data` 則為：

```json
[
  {
    "taskId": 8101,
    "deleted": false,
    "code": "TASK-EXAMPLE-001",
    "title": "修正報表日期",
    "assigneeId": "u-member",
    "dueDate": "2026-10-30",
    "status": "TODO"
  }
]
```

以上 code 只是顯示範例，實際由現有 SerialNumberApi 取號。之後 Task 改為 DONE，再讀即顯示 DONE；Meeting 不接收 status。若 Task 軟刪除，回 `{"taskId":8101,"deleted":true,"code":null,"title":null,"assigneeId":null,"dueDate":null,"status":null}`。

## 7. 增量遷移與舊程式回退【已核准 R6】

SQL 草稿存於 `docs/sql/meeting-mvp.draft.sql`，不放進 resources 自動初始化路徑。只有兩個 CREATE TABLE，沒有既有表 ALTER／重建／回填、沒有 permission seed、沒有 DROP 或清除資料的 down script。使用未帶 schema 的表名，執行者必須先核對目標資料庫；不硬編正式庫名。刻意不使用 IF NOT EXISTS，避免同名但結構錯誤時被當成成功。

| 實際風險 | 執行前與失敗時的處理 |
| --- | --- |
| 連錯既有業務庫；本機 verify／t2 庫已有前次資料 | 本次不連線。未來先建立全新 `spring_core_lab_meeting_mvp_<run>` 獨立庫與測試帳號，資料庫權限只給該庫；讀回 DATABASE()、版本與表清單後，才允許套用。不能只看名稱含 verify 就使用。 |
| 實際 project／task 的引擎、signed bigint 或 schema 與匯出不同 | 在已核准的目標檢查 SHOW CREATE TABLE、InnoDB、id 型別及 collation；FK 所指兩表須在同一庫。不一致即停下修訂草稿，不能自動改既有表來湊。 |
| DDL 隱含 commit，第二張表失敗會留下第一張 | 逐步記錄成功語句與 SHOW CREATE TABLE；修復原因後，只補未完成的新表，已存在者比對結構。不要重跑整份匯出或以 ROLLBACK 當成撤銷 DDL。 |
| FK 建立可能等待既有表的 metadata lock | 核對長交易，安排短維護時段，失敗先記錄原因；不加無限重試、不停用 FK 檢查。新表沒有舊資料搬移，但建立 FK 仍非零影響。 |
| Hibernate `ddl-auto=update` 提早建表，或與手寫 DDL 漂移 | 未來驗證與部署使用 validate；先套核准 DDL，再用預定 Entity 啟動驗證。舊 local profile 的 update 不能當 migration。 |
| 只有 Meeting 的專案仍可被舊 ProjectService 刪除 | 新版實作必須加「有有效 Meeting 就 409，改用結案／取消」的 beforeDelete 檢查；這是保留紀錄所需的最小既有行為變更，尚未實作。FK 擋不了軟刪除。 |
| 時區、長文字與參與者無帳號造成資料誤解 | 按第 3 節約定驗證跨日顯示、中文／emoji、長度邊界、無帳號參與者；不回填或改動舊時間與帳號。 |
| 誤以為關聯 FK 等同同專案與有效性檢查 | 執行第 9 節跨專案／軟刪除案例；只授權經 web-server 寫入，不能直接開放內部服務繞過 authority。 |

MySQL 的 CREATE TABLE 不能靠使用者 ROLLBACK 撤銷，依據 [官方 implicit commit 說明](https://dev.mysql.com/doc/refman/8.4/en/implicit-commit.html)。本次未核對目標 MySQL 實際版本，不宣稱已驗證其語法或 FK。

回退舊程式採 **保留新增資料表與所有紀錄**：

1. 停止新版會議寫入，備份 meeting、meeting_task 及相關 project／task 的一致資料與版本資訊；以隔離庫還原驗證筆數、內容、關聯與審計欄位。備份不可包含對外分享的憑證或 Session。
2. 在獨立庫驗證舊版使用 validate 能與額外兩張表共存，再切回舊版前後端。舊版無入口時，紀錄暫不可操作，但保留在資料庫與可還原備份；不以恢復遷移前的整庫備份覆蓋已輸入紀錄。
3. 舊版刪除專案不認識 Meeting。回退期間須在既有、唯一的後端入口阻擋 `DELETE /api/project/{id}`，正式代理路徑含 `/backend`，包含 admin 也要被擋；驗證無外部直連旁路。這項正式入口設定須另外核准，本次不操作。若環境無法落實，回退期間維持維護唯讀，不開放有此風險的寫入。只藏按鈕不成立，撤除一般角色的 project:write 也擋不了既有超級管理員。
4. 舊版 Task 可依既有規則軟刪除；保留關聯，重新上新版後依 R5 顯示已刪除占位。舊版不改寫會議表；再次升版先比對既有新表與資料，不重建、不重送 DDL。

沒有資料清除型 rollback。未來若真的要移除會議結構，必須另提包含匯出、還原演練、保留期限與明確刪除授權的方案。

## 8. 獨立測試庫驗證方式【已核准 R7；本次未執行】

本節的驗證方式已核准，但尚未執行。使用者原定本次停在方案，因此不建立測試庫、不啟動服務或執行 SQL。

1. 在本機 MySQL 新建本次專用庫 `spring_core_lab_meeting_mvp_<run>`。建立僅能操作此庫的測試連線；project-service 明確指向此庫，basic-data 與必要服務也使用獨立測試資料，不能連既有業務庫。使用現有本機服務，不用 Docker。
2. 以版本控制的既有結構建立測試基線，不複製使用者、任務等業務資料；可用結構匯出中的 project 區段建立完整專案表結構，但先確認沒有其他庫 USE 或資料 INSERT。保存既有表 SHOW CREATE TABLE 與合成測資筆數，再套兩張新表的草稿。重跑應因表已存在而失敗，不能視為冪等成功。
3. 寫入全為虛構的兩個專案、記錄人、其他成員、經理、非成員與 authority 組合；記錄人／經理不得用超級管理員代替驗收。驗證中文、emoji、10000 長度、過長與非法時間輸入。內容驗證走 API；資料庫層另驗 PK／FK 拒絕不存在 ID，並用診斷查詢確認無跨專案關聯。
4. 後續實作與測試庫 SQL 執行獲授權後，以 Java 21、`ddl-auto=validate` 啟動專案既有服務，用 Session 登入 web-server 測試 R／W／T、身分與結案規則。測試輸出遮蔽 Cookie、Token、憑證；直接 HTTP 呼叫驗證拒絕結果，不能只看按鈕。既有表結構與測資在遷移前後須一致。
5. 跑第 9 節案例，逐筆保存版本、環境、帳號能力、實際結果與證據。驗證新建任務失敗、已建立但連結失敗，以及重試只新增一次關聯；建置成功不代替這些驗收。
6. 將隔離庫的 Meeting 內容及關聯備份到另一個獨立還原庫，比對全部欄位；停新版，演練舊版可啟動、刪專案入口被後端阻擋、Task 軟刪除後重新升版仍能讀紀錄與占位關聯。測試庫或備份的清除需另外取得刪除授權。

後續取得 SQL 執行授權後，可用唯讀診斷 SQL 查跨專案關聯；正常應為 0 筆。這不是本次執行指令：

```sql
SELECT mt.meeting_id, mt.task_id
FROM meeting_task mt
JOIN meeting m ON m.id = mt.meeting_id
JOIN task t ON t.id = mt.task_id
WHERE m.project_id <> t.project_id;
```

## 9. 驗收案例【已核准 R8；全部尚未執行】

| ID | 情境／操作 | 預期可觀察結果 |
| --- | --- | --- |
| MM-01 | 有 R／W 的成員在隔天建立昨天的會議，參與者含無帳號的人 | 可儲存、重整查回；heldAt 與 createTime 不同；無須議程或參與者帳號。 |
| MM-02 | 只填紀錄與一般決議，沒有 Task | 可查回全文，taskIds=[]，不產生任務或追蹤狀態。 |
| MM-03 | 必填空白、非法時間、帶 Z／offset、文字剛好上限與超過上限；送偽造審計欄位 | 不合法輸入 400；邊界合法者可存；偽造操作者／時間不生效，系統審計正確。 |
| MM-04 | 記錄人／另一 MEMBER／MANAGER 各自直接 PATCH 同一紀錄；各備有 W | 記錄人與經理成功，另一成員 403；createId 不變，transId／transTime 更新。 |
| MM-05 | 有 R 無 W 的記錄人／經理、有 W 無 T 的記錄人、只有 T 的成員 | 前者可讀但寫入 403；第二者可改紀錄與關聯但不能建 Task；第三者不能寫 Meeting。 |
| MM-06 | 持 R／W 的非成員、被移除的記錄人，偽造 projectId 或 X-Operator | 讀寫均拒絕；換成自己專案的 ID 或宣稱作者不會取得他案紀錄。 |
| MM-07 | 具 R／W／project:admin 的非成員，以及僅 project:admin 無 R／W 的帳號 | 前者可跨專案操作，後者仍 403；前者仍不能跨專案連 Task 或寫結案專案。 |
| MM-08 | 分別在 DONE、CANCELLED 專案讀／建立／PATCH／連結／解除，包括重複解除與 admin | 讀取可依 R；所有寫入 400，審計與資料不變。依既有權限重新開啟 ACTIVE 後可寫；ON_HOLD 可寫。 |
| MM-09 | 同專案 Task 連結兩次、同 Task 連兩份會議、連主／子任務、有效 DONE／CANCELLED Task | 同一對 ID 僅一列；重複不改審計；多會議可反查；不改 Task 狀態或其他欄位。 |
| MM-10 | 連別案／不存在／軟刪除 Task，連第 101 個 Task，直接 FK 引用不存在 ID | API 拒絕且無殘留關聯；跨案 400、缺資料 404、超量 400；FK 拒絕不存在 ID。 |
| MM-11 | Task 改負責人、期限、DONE，再重讀 Meeting 的 tasks | 顯示即時值，Meeting 表沒有副本，Task 完成不自動改會議內容。 |
| MM-12 | Task 後來軟刪除，再讀／解除關聯；同一對 ID 重複解除 | 讀回 deleted=true 占位；第一次只移除關聯並更新 Meeting 審計，第二次回原值；minutes、Task 與其他會議關聯均不變。 |
| MM-13 | 建 Task 成功但無法連結，之後補權限並只重試連結；另一例 Task 建立失敗 | 成功 Task 保留且 ID 可見，只連出一筆，無重複 Task；建立失敗則無關聯。 |
| MM-14 | taskId 反查、projectId＋ids AND、時間上下界、空結果、多頁、相同 heldAt | 只回可讀且符合的會議；total 不因關聯重複；排序穩定；無結果為空，不當錯誤。 |
| MM-15 | 只有 Meeting 沒有 Task 的專案，經理／admin 呼叫刪除 | 新版回 409，保留紀錄可讀；軟刪除專案不可成為繞過保護的路徑。 |
| MM-16 | 備份還原、回退舊版、入口阻擋刪專案、再次升版 | 新表全部資料與關聯保留；舊版可啟動，後端刪專案拒絕；再次升版內容與審計不遺失。 |
| MM-17 | 只新增／解除關聯，不改其他欄位 | 真正變更時更新 transId／transTime；createId／createTime 不變。 |
| MM-18 | 桌面與 390px，補寫未儲存內容後離開，再儲存後重整與開 Task | 明確儲存／取消並沿用既有草稿離開確認；資料查回一致；Task 進入既有詳情；無前端權限遮蔽替代後端檢查。 |

未來 UI 的明確儲存、離開確認與 R8 顯示行為均已核准。未加入編輯歷程、自動儲存或新的完成狀態。

## 10. 參考做法與取捨

先比較兩個開源專案，作為模型思路參考；本專案規則仍以現有程式與使用者需求為準，沒有引入其程式碼或依賴。

| 參考來源 | 做法 | 本案取捨【已核准 R1–R5】 |
| --- | --- | --- |
| [OpenProject MeetingAgendaItem](https://github.com/opf/openproject/blob/dev/modules/meeting/app/models/meeting_agenda_item.rb) | 一般項目與 work package 連結分開，可從 work package 反查相關會議。 | 採用「文字決議＋既有任務連結／反查」；省去議程區段、項目類型、主持人與 outcome 階層。 |
| [ERPNext Quality Meeting](https://github.com/frappe/erpnext/blob/develop/erpnext/quality_management/doctype/quality_meeting/quality_meeting.json) | 將 agenda 與 minutes 分為子表，另有會議狀態。 | 本案會後補寫，用單一 minutes 文字即可；不引入議程表與會議狀態。 |

本次已檢查完整 diff、4 組 JSON 範例解析、15 個本機文件引用，以及 DDL 僅含 2 張新表／3 個外鍵，並核對 18 個驗收案例的編號與必要欄位。repo 只變更本文、SQL 草稿與 TODO.md。這些是靜態檢查；未執行 SQL、MySQL 語法／遷移驗證、API／畫面驗收或應用程式建置。

## 11. 核准紀錄

2026-10-27，使用者對「是否核准 R1–R8 作為後續實作基準？」確認 **核准方案**，沒有修改 R1–R8。原唯一待決事項已結束，不再重複詢問。

此核准確認設計基準；原定本次停在方案的範圍不變。本次不修改應用程式或執行 SQL，也不代表核准正式環境操作、資料清除或新增依賴。後續實作與獨立測試庫驗證仍待執行，MM-01–MM-18 全部尚未驗收。

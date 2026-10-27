# 手動驗收 MVP 方案

**狀態：R1–R8 已核准。** 批次、項目與執行紀錄已有程式初版，但尚未完成實際驗收。[增量 SQL 草稿](sql/manual-qa-mvp.draft.sql) 尚未獲得執行授權，也未執行；正式環境操作另行授權。以下保留原始提案內容，其中「後續實作」等敘述描述提案時的狀態。對象是同事與少量手動驗收人員。資料由 project-service 管理，沿用 Java 21、React／TypeScript、Ant Design 和現有分層；不新增微服務、依賴或完整案例管理平台。

## 現況與參考

- [`Project`、`Task`、`ProjectMember`](../backend/common/src/main/java/dev/jianwii/entity/project/) 使用 Long 專案／任務 ID 與字串使用者 ID。`TaskType.BUG` 已存在；`TaskStatus` 的 `DONE`、`CANCELLED` 是任務狀態，不代表驗收結果。任務軟刪除，描述、留言和附件已可記錄缺陷。
- [`ProjectRules`](../backend/project-service/src/main/java/dev/jianwii/project/service/ProjectRules.java) 對 `DONE`、`CANCELLED` 專案禁止寫入。`PLANNING`、`ACTIVE`、`ON_HOLD` 可寫。使用者 ID 屬 basic-data，不跨服務建立外鍵。
- [`ProjectAccessGuard`](../backend/web-server/src/main/java/dev/jianwii/web/project/ProjectAccessGuard.java) 檢查有效成員；`project:admin` 可略過成員範圍檢查，但仍需要功能權限。project-service 信任 web-server，入口須先依資源 ID 找出所屬專案再驗權。
- [`meeting-api.ts`](../frontend/src/api/project/meeting-api.ts) 與 [`WebMeetingController`](../backend/web-server/src/main/java/dev/jianwii/web/project/WebMeetingController.java) 使用 `/api/meeting`、`query`／`queryPage`，web-server 回傳 `Result<T>`。本案沿用此形狀，不直接套用會議專用的寫入權限。
- 參考 [Kiwi TCMS 的資料模型](https://github.com/kiwitcms/Kiwi/blob/master/docs/source/guide/introduction.rst)：執行關聯測試內容、結果與缺陷，執行時保留版本；本案保留逐次執行，省去獨立 TestPlan／TestCase。參考 [OpenProject 的測試管理做法](https://github.com/opf/openproject/blob/dev/docs/use-cases/test-management/README.md)：個別追蹤測試執行與缺陷；本案直接連既有 BUG Task，不複製其完整工作項目模型。

## 已核准規則

以下 R1–R8 已作為實作基準核准；核准方案不等於授權執行 SQL。

### R1：三個概念與欄位

| 概念 | 欄位 | 意義 |
| --- | --- | --- |
| 驗收批次 `manual_qa_batch` | `id`, `project_id`, `version_label`, `environment` | 一個專案的一輪驗收；版本或 commit 標記與測試環境文字皆必填。批次版本是起始參考，不代表每次實測版本。 |
| 檢查項目 `manual_qa_item` | `id`, `batch_id`, `title`, `steps`, `expected_result`, `task_id?`, `assignee_id?` | 標題、操作步驟、預期結果必填；可關聯同專案任務並指定有效成員。 |
| 執行紀錄 `manual_qa_execution` | `id`, `item_id`, `tester_id`, `executed_at`, `tested_version`, `result`, `actual`, `bug_task_id?` | 每次新增一筆，執行人與時間由伺服器依登入身分及存檔時間決定；實測版本、結果及實際現象必填。失敗可連同專案 BUG Task。 |

每表另有既有 `BaseEntity` 的 `create_id`、`create_time`、`trans_id`、`trans_time`、`db_mark`；ID 為 signed bigint。`tester_id` 與 `executed_at` 是執行事實，保留獨立欄位；審計欄位仍由系統維護。時間沿用專案 `LocalDateTime`／`datetime(6)` 契約，部署前須確認服務與驗收端時區；不依版本字串或人填時間排序。

關係為 Project 1:N 批次、批次 1:N 項目、項目 1:N 執行。項目可指一個 Task，失敗紀錄可指一個 BUG Task；多筆紀錄可指同一個 BUG。關聯不是缺陷副本，不另存 BUG 標題、狀態、留言或附件。

必填文字去除首尾空白後不可為空；長度與 SQL 一致：版本、標題最多 200 字；環境最多 1000 字；步驟、預期與現象使用 `text`，應在 API 邊界驗證可存入 MySQL `TEXT` 的 UTF-8 位元組長度。`assignee_id`／`tester_id` 最多 100 字。伺服器不接受客戶端覆寫 ID、審計欄位、執行人或執行時間。

### R2：修改、複製與歷史

- 批次所屬專案不可變；版本及環境可以更正，但畫面要標示它們是批次參考，不能更改任何既有 `tested_version`。
- 項目首次執行前可修改標題、步驟、預期、任務、負責人，也可軟刪除。首次執行後鎖定標題、步驟、預期及任務，禁止刪除，避免歷史結果指向改寫後的內容；負責人可變更。需不同步驟時新增項目或下一輪複製後修改。
- 執行紀錄只新增，不提供修改、刪除或重設結果；誤填時新增一筆更正並在現象說明。失敗後可透過專用操作補連、換連或解除 BUG；只更新該筆 `bug_task_id` 與審計資訊，不變更實測事實。連結時只接受有效、同專案、類型為 `BUG` 的 Task。
- 建立新批次時可複製同專案有效項目之標題、步驟、預期、任務及負責人，絕不複製執行紀錄或 BUG 關聯。新批次須自行填版本與環境。若來源任務已軟刪除或負責人已非有效成員，整個複製請求拒絕並列出受影響項目；可修改尚未鎖定的來源，或在新批次手動建立項目，不靜默清空資料。複製批次及項目在 project-service 同一交易完成。
- 既有 Task 後來軟刪或類型改變時保留關聯 ID。一般 Task 查詢排除軟刪資料，查不到時顯示「已刪除或無法取得」與 ID；查得到時顯示目前類型。不能把失效或非 BUG 任務當成有效 BUG 新連結；成員退出也保留歷史執行人與指派 ID。名稱查不到時顯示 ID，不清除歷史。Task 狀態變動不自動改驗收結果。
- 不提供批次刪除。專案若有驗收批次，未來 `ProjectService.beforeDelete` 應拒絕刪除，建議結案或取消；專案軟刪除不會觸發 SQL 外鍵。這是後續實作需求，目前尚無此保護。

### R3：狀態與統計

執行紀錄只存 `PASS`、`FAIL`、`BLOCKED`。`FAIL`／`BLOCKED` 的 `actual` 必須包含具體原因；`PASS` 也要記錄實際現象。沒有紀錄才顯示 `NOT_TESTED`（未測），不可新增 `NOT_TESTED` 紀錄。`BLOCKED` 表示無法完成驗證；`FAIL` 表示實際結果不符預期。

每個有效項目的目前狀態取最新一筆執行，以伺服器 `executed_at DESC, id DESC` 排序；無紀錄為未測。歷史紀錄全部保留，查詢使用相同排序。批次統計以**有效項目數**為分母，每個項目只算目前狀態一次：`total = notTested + passed + failed + blocked`，`passRate = passed / total`。`total > 0` 時通過率可顯示百分比；`total = 0` 顯示「尚無項目」，通過率為 `null`，不是 0% 或 100%。查詢失敗、資料缺失或尚未載入顯示錯誤／載入狀態，不用 0 代替。

批次沒有額外的通過／結案狀態，也不把 BUG 完成當成通過；使用者要看版本與每項最新結果作判斷。歷史總執行次數可由紀錄列表得知，不混入項目統計。

### R4：API 草稿

路徑為 web-server 公開 API，後續於 common-api 定義契約、project-service 實作；前端放 `frontend/src/api/project/`。JSON 成功沿用 `Result<T>`，列表分頁沿用 `PageResponse`（page 從 1 起、size 1–100），每頁排序明確；資源不存在 404、無權限 403、驗證錯誤沿用既有錯誤格式，已執行項目修改衝突回 409。以下是設計，端點目前不存在。

| 方法與路徑 | 輸入與效果 |
| --- | --- |
| `POST /api/manual-qa-batch/queryPage` | 必填 `projectId`、`page`、`size`；按 `id DESC` 列出批次及其統計。 |
| `GET /api/manual-qa-batch/{batchId}` | 批次欄位及 R3 統計。 |
| `POST /api/manual-qa-batch` | `projectId`, `versionLabel`, `environment`；建立空批次。 |
| `PATCH /api/manual-qa-batch/{batchId}` | 修改 `versionLabel`／`environment`。 |
| `POST /api/manual-qa-batch/{batchId}/copy` | `versionLabel`, `environment`；複製到同專案新批次，遵守 R2。 |
| `GET /api/manual-qa-batch/{batchId}/items?page=1&size=100` | 按項目 `id ASC` 分頁，包含每項目前狀態及最新紀錄摘要。統計涵蓋所有有效項目，不只目前頁。 |
| `POST /api/manual-qa-batch/{batchId}/items` | `title`, `steps`, `expectedResult`, 可選 `taskId`, `assigneeId`。 |
| `PATCH /api/manual-qa-item/{itemId}` | 修改 R2 允許的項目欄位；`taskId`／`assigneeId` 明確 `null` 為解除，省略為不變。 |
| `DELETE /api/manual-qa-item/{itemId}` | 僅無執行紀錄時軟刪。 |
| `GET /api/manual-qa-item/{itemId}/executions?page=1&size=100` | 按 R3 排序分頁，列出所有執行事實與 BUG 關聯。 |
| `POST /api/manual-qa-item/{itemId}/executions` | `testedVersion`, `result`, `actual`, 可選 `bugTaskId`；建立新紀錄。僅 `FAIL` 可附 BUG。 |
| `PATCH /api/manual-qa-execution/{executionId}/bug` | `bugTaskId` 為 ID 或明確 `null`；僅 `FAIL` 可操作。 |

登入使用者、時間由服務端寫入；task、BUG 與指派人均由 project-service 查驗所屬專案、有效性與類型。寫入使用 `ProjectRules.requireOpen`。非成員不能藉由猜 ID 查閱；跨專案關聯拒絕。BUG 建立走現有 Task API，再連到失敗紀錄；若連結失敗，保留已建立 Task 並以其 ID 重試連結，避免重建重複 BUG。

### R5：權限與畫面

| 操作 | 功能權限與專案範圍 |
| --- | --- |
| 查批次、項目、統計與歷史 | `project:read` 加有效專案成員；`project:admin` 可豁免成員檢查，仍需 `project:read`。 |
| 建立、編輯、複製、刪未執行項目、記錄或連結 BUG | `task:write` 加有效專案成員；`project:admin` 可豁免成員檢查，仍需 `task:write`；專案須未結案／取消。 |

可指派的同事須是有效成員；執行人取登入使用者。admin 的成員豁免沿用 guard，故 admin 可記錄執行但不必是成員。前端依權限和專案狀態控制按鈕，後端仍是權威。沿用專案詳情入口及 Ant Design 清單、表單和歷史；首次執行及複測使用同一新增紀錄流程，明確輸入實測版本。若未來確有「只能測、不能改任務」需求，再另提獨立權限；不先建立測試角色。

### R6：A 版失敗，B 版複測

以下是示意資料，不是已執行驗收：

| 批次參考 | 項目 | 時間／執行人 | 實測版本 | 結果 | 實際現象／BUG |
| --- | --- | --- | --- | --- | --- |
| A，測試環境 | 儲存會議 | 10:00／同事甲 | A | FAIL | 儲存回 500；連 BUG #123。 |
| A，測試環境 | 儲存會議 | 11:00／同事乙 | B | PASS | 儲存成功，重新開啟內容仍存在。 |

兩筆紀錄都可查，項目目前狀態是 `PASS`；若此批次只有該項，統計為總數 1、通過 1、失敗 0、受阻 0、未測 0、通過率 100%。BUG #123 即使仍未完成或已完成，都不改上述結果。下一輪複製後只有項目內容，無兩筆紀錄，狀態為未測。

### R7：遷移與回復

草稿只新增三表，不改既有 schema、角色、權限或資料；未指定資料庫名稱，無 `IF NOT EXISTS`。外鍵只保證實體存在，不能保證同專案、Task 有效、BUG 類型或軟刪狀態；這些由 service 檢查。`RESTRICT` 防實體刪除，不能防專案軟刪。尚未連線核對實際 MySQL 版本及表結構。

取得設計與 SQL 執行授權後，先確認獨立測試庫的 `DATABASE()`、`VERSION()`、`SHOW CREATE TABLE project`／`task`、字元集和外鍵型別，備份並確認可還原；同名表或不符即停。只在新獨立測試庫試建，核對 `SHOW CREATE TABLE`，再以測試資料驗證外鍵、應用服務驗證與 R8。實際正式環境另需明確授權、備份及部署計畫。

MySQL DDL 會隱含 commit，不能以 `ROLLBACK` 撤銷整份草稿；若中途失敗，記錄已建表、停止並核對後再提出處置，不自動刪除。參見 [MySQL DDL 隱含提交](https://dev.mysql.com/doc/refman/8.4/en/implicit-commit.html) 與 [外鍵規則](https://dev.mysql.com/doc/refman/8.4/en/create-table-foreign-keys.html)。

回退應用程式時停止新寫入、保留三表和資料、備份後切回舊版；舊版不會顯示新資料。舊版缺少 R2 專案刪除保護，回退期間須在操作入口禁止刪除含驗收批次的專案，並驗證無法透過 API 繞過；若無法可靠阻擋，暫不回退或先部署含保護的相容版本。再升版後驗證批次、項目與全部執行紀錄。刪表或清除資料會永久遺失驗收紀錄，須另案核准並先完成備份還原演練；本文沒有 down script。

### R8：後續驗證條件

以下皆未執行；實作後在獨立測試庫及本機服務逐項留下結果、版本與環境。

| 編號 | 情境 | 預期 |
| --- | --- | --- |
| QA-01 | 空批次、空項目清單 | 顯示尚無項目；通過率為 null，載入／失敗不顯示 0。 |
| QA-02 | 新增項目、未執行 | 未測 1，不產生執行紀錄。 |
| QA-03 | A 失敗連 BUG，B 複測通過 | 兩筆及各自版本、執行人、時間、現象可讀；目前為通過。 |
| QA-04 | 失敗／受阻不填原因，送未測紀錄 | 拒絕；資料不新增。 |
| QA-05 | 複製有結果的批次 | 項目內容複製、結果及 BUG 關聯不複製；失效關聯時整筆拒絕。 |
| QA-06 | 已執行項目改步驟／刪除；更改負責人 | 前兩者拒絕且歷史不變；有效成員可被指定。 |
| QA-07 | 多項失敗連同一 BUG；BUG 完成或軟刪 | 連結 ID 仍可讀且不改結果；查不到顯示「已刪除或無法取得」與 ID。 |
| QA-08 | 非成員、僅讀者、admin 缺功能權限、跨專案任務 | 查寫依 R5 拒絕；不得由 ID 繞過。 |
| QA-09 | DONE／CANCELLED 專案寫入與讀取 | 寫入拒絕，授權讀取仍可用；ON_HOLD 可寫。 |
| QA-10 | 統計含多頁、同時刻紀錄、混合結果 | 全部有效項目各算一次，按時間及 ID 決定最新；分母與分類加總一致。 |
| QA-11 | 備份、回退、禁止刪專案、重新升版 | 新表保留、舊版入口與 API 均無法刪除專案、升版後資料完整。 |
| QA-12 | 結果補連／解除 BUG、錯誤更正 | 僅關聯可更新；原執行事實不變，更正以新紀錄呈現。 |

使用者已確認 R1–R8 作為實作基準；此確認不包含執行 SQL、正式環境操作或新增依賴的授權。

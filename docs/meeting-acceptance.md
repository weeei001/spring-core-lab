# 會議紀錄實作與驗收

日期：2026-10-27，Asia/Taipei。版本：`31998e2` 加本輪未提交修改。基準為 [已核准方案](meeting-mvp.md) R1–R8；本輪實作會議紀錄讀寫，不提供轉任務、任務關聯寫入或刪除會議入口。

## 本輪實作

- project-service 保存 Meeting，提供 query、queryPage、GET 詳情、POST 新增與 PATCH 修改。沿用核准的 meeting／meeting_task 結構，taskIds 只讀；關聯操作另輪實作。
- 讀取需 project:read；新增／修改需 meeting:write。成員只能修改自己的紀錄，經理或 project:admin 可修改他人的紀錄；非成員拒絕。DONE／CANCELLED 的寫入一律拒絕。
- 專案內有效 Meeting 會阻擋專案刪除。系統審計欄位不採信使用者輸入，PATCH 不可換專案。
- 專案頁加入會議清單及時間篩選；獨立頁提供儲存、取消、站內離開確認及重整／關閉頁面提示。純文字內容上限 10000；儲存錯誤保留輸入並顯示錯誤。

## 已核准並執行的獨立測試環境

原 T3 對話核准設計；使用者於本輪明確確認「核准此獨立測試庫執行方案」，授權建立下列四庫、專用帳號、空白基線、合成測資與已核准增量 DDL。原有 JAR 啟動參數指向 spring_core_lab_t2_*，本輪沒有替換那些服務或修改其資料。local 檔未覆寫 datasource URL，公開 local profile 的預設為 basic_data／project／notification，因此驗收採命令列明確覆寫。

MySQL 8.0.46，localhost:3306。建立前已確認以下庫不存在，內容僅含空白結構與本次合成測資：

| 用途 | 資料庫 |
| --- | --- |
| project-service | spring_core_lab_meeting_mvp_20261027 |
| basic-data | spring_core_lab_meeting_mvp_20261027_basic |
| notification-service | spring_core_lab_meeting_mvp_20261027_notification |
| web-server Session／OIDC 設定 | spring_core_lab_meeting_mvp_20261027_session |

專用帳號僅有四庫的 SELECT／INSERT／UPDATE／DELETE；建庫與 DDL 由既有本機管理連線執行。憑證只保存在被 Git 忽略的本機驗收目錄，不輸出或提交。沒有 DROP、既有表 ALTER 或資料回填。

以 deploy/mysql-schema.sql 擷取各模組 CREATE TABLE，共 24 張基線表，另建現有 OIDC 設定表；沒有執行原 USE／CREATE DATABASE 或停用 FK 的指令。增量 DDL 逐句新增 meeting／meeting_task。遷移前後的 project／task SHOW CREATE TABLE 與零筆資料均一致，新版服務使用 validate 啟動成功。

已讀回 DATABASE()、VERSION()、SHOW CREATE TABLE project／task，確認 signed bigint、InnoDB 與 utf8mb4_unicode_ci，再套用 [已核准增量 SQL](sql/meeting-mvp.draft.sql)。meeting_task 最後仍為零筆，沒有產生 Task。

驗收服務使用 JDK 21、-Duser.timezone=Asia/Taipei、命令列明確指定以上 datasource，以 --spring.jpa.hibernate.ddl-auto=validate 覆寫 local 設定。獨立 Eureka 為 8861／9861，服務為 8180–8183／9180–9183，健康端點全部回 200。前端測試代理 3000 指向 8180。來源副本排除 target 與 application-local.*，包成獨立 JAR，原服務沒有熱重啟或被覆寫。

合成帳號 101–108 依序為記錄人、其他成員、經理、非成員、僅讀者、admin、只有 W、只有 admin。一般角色沒有 project:admin，未使用超級管理員設定。專案 701 為主驗收案；702 僅非成員帳號 104 可讀，703／704／705 分別為 DONE／CANCELLED／ON_HOLD。

使用真實 Spring Session JDBC 與 Spring Security 序列化建立合成 Session，之後全部請求通過真正 web-server、PermissionAuthorityFilter、ProjectAccessGuard、project-service 與 MySQL。暫時 Vite fixture 只負責在本機瀏覽器載入合成 Session，沒有加入產品路由。外部 OIDC 設定完全合成，沒有移轉既有 client_secret，也沒有重新驗證 Google OAuth 登入。自動核准審查曾拒絕原先複製既有憑證的腳本；移除該操作並限縮帳號權限後才執行。

MySQL DDL 隱含 commit，無法用 ROLLBACK 撤銷。回退時停止測試版服務，保留上述庫、兩張新表與測資；不 DROP、不清除資料。正式環境不在本輪範圍。

## 驗收紀錄

HTTP 九組驗收全數通過；瀏覽器採內建瀏覽器，原生提示另嘗試 Edge。測試生命週期成功不代表有測試覆蓋。

| ID | 實測操作與預期 | 實際結果 |
| --- | --- | --- |
| MC-01 | 成員新增昨天的會議，參與者含無帳號文字；儲存後重整並 GET 讀回一致 | HTTP 通過；瀏覽器建立 ID 7，時間 2026-10-26 14:30，重整後標題、參與者、換行與 emoji 一致 |
| MC-02 | 記錄人再次修改；createId／createTime 不變，transId／transTime 更新 | HTTP 通過；瀏覽器 ID 7 再次修改並儲存成功 |
| MC-03 | 中文、emoji、換行與 10000 UTF-16 長度內容可存取；10001、空白必填被拒絕 | HTTP 通過；瀏覽器輸入「中文🙂＋換行」重複 2000 次，儲存並重整後 length=10000 且全文相同；10001、title 201、participants 2001、全空白 PATCH 均 400 |
| MC-04 | 非法時間、Z／offset 被拒絕；有效秒與小數秒 local datetime 可往返 | HTTP 通過；非法日期、Z、+08:00 均 400；秒、1 位小數、6 位小數均可存取 |
| MC-05 | 未儲存時按取消、上一頁，繼續編輯保留輸入；重整出現原生離開提示 | 取消與上一頁均出現確認，選繼續編輯保留欄位。原生重整／關閉提示未完成實測：內建分頁失效，Chrome 工具不可用，Edge 程式化重整沒有可觀察對話框；Windows 原生控制因無法可靠判定目前網址而停止。站內其他連結未單獨操作 |
| MC-06 | 儲存失敗顯示錯誤；輸入保留，可修正後重送且不重複建立 | 瀏覽器編輯 ID 7 後，將合成專案改為 DONE；儲存顯示結案錯誤，標題與完整 10000 長度輸入保留。還原 ACTIVE 後重送成功，仍為同一 ID；403／網路中斷的畫面保留未另測 |
| MC-07 | 有 R／W 的非成員、已移除記錄人直接 GET／query／POST／PATCH 均拒絕；偽造 body／X-Operator 無效 | HTTP 通過；104 對 701 的讀寫皆 403，但可查自己 702；移除記錄人後 GET／PATCH 403，還原成員。偽造 projectId／作者不能繞過；X-Operator 被覆寫為 101 |
| MC-08 | 同案其他成員不能改別人的紀錄；經理可改；有 R 無 W 只能讀；admin 仍需 R／W | HTTP 通過；102 PATCH 403、103 可改且 createId 不變；105 寫入 403；107 無 R 仍能以 W 建立／改自己紀錄，但讀取 403；106 可跨專案，108 缺 R／W 仍 403 |
| MC-09 | DONE／CANCELLED 可讀、POST／PATCH 拒絕且資料不變；admin 同樣拒絕；ON_HOLD 可寫 | HTTP 通過；101／103／106 在 DONE 與 CANCELLED 寫入均 400，讀回審計及內容未變；ON_HOLD 新增成功。瀏覽器結案顯示唯讀、可編輯欄位 0、儲存按鈕 0。合成專案最後還原 ACTIVE |
| MC-10 | projectId 必填、ids 空陣列拒絕、時間上下界包含、空結果、相同 heldAt 穩定分頁與日期篩選 | HTTP 通過；空條件、空 ids、反向日期、帶 Z、非法 sort、query page=2 均 400；精確上下界 total=1、空結果 total=0，兩頁組合順序與完整排序一致。UI 清單顯示時間與修改人；日期控制項未單獨點選驗收 |
| MC-11 | 只有 Meeting 的專案刪除回 409；PATCH 換專案或帶 taskIds 回 400 | HTTP 通過；admin 刪除 701 回 409，Meeting 仍可讀；換專案／帶 taskIds 400，同一 projectId 可更新 |
| MC-12 | 桌面與 390px 可操作；長文字換行、按鈕與儲存結果可見 | 通過；桌面補寫、取消與再次編輯可操作；390px 長文字有欄內捲動，儲存／取消可見並成功儲存，innerWidth=390、document scrollWidth=375。截圖保存在本輪 visualizations |

## 已執行的靜態驗證

- frontend：yarn typecheck、yarn lint、yarn build 成功；Vite 提示 chunk 大於 500 kB。
- backend：JDK 21 執行 .\mvnw.cmd -pl web-server,project-service -am test 與 .\mvnw.cmd spotless:check 成功。沒有自動化測試案例；本輪沒有新增測試依賴。
- 建置的 Spotless 自動排版只涉及本輪修改的 Java 檔案；交付前檢查完整 diff，沒有夾帶其他 Java 排版變更。
- 本輪 SQL、validate 啟動及上表 HTTP／瀏覽器流程已實測。九組 HTTP 首輪有一個時間字串斷言失敗：JPA 新增回應帶 nanosecond，MySQL datetime(6) 會取整；改成數值時間比較後 9／9 通過，沒有因此修改 BaseEntity。
- 測試服務與暫時 Vite 最後停止，保留四個測試庫、合成資料與本機驗收產物。既有 9080–9083 健康檢查仍回 200。

若後續修 bug 需要自動回歸，最小 Java 方案為 spring-boot-starter-test；React 方案為 vitest、jsdom、@testing-library/react、@testing-library/user-event。新增前另行取得核准，目前以本表保留可重現案例。

## 參考與取捨

模型沿用 T3 已比較的 OpenProject 與 ERPNext 方案，採單一 minutes 純文字，不加入議程、狀態或獨立 action item。實作跟隨現有 ProjectLabel 的 API／Service／DAO 與 TaskDraftBoundary 的離開確認。站內導頁使用 [React Router useBlocker](https://reactrouter.com/api/hooks/useBlocker)，原生離開另用 [useBeforeUnload](https://reactrouter.com/api/hooks/useBeforeUnload)；兩者的驗收結果分開記錄。Request body 驗證依 [Spring 官方文件](https://docs.spring.io/spring/reference/7.1/web/webmvc/mvc-controller/ann-methods/requestbody.html) 與既有 ValidGroup，不新增依賴。

## 2026-10-27 會議任務關聯 HTTP 驗收

版本為 `c6cf10c` 加本輪未提交修改。沿用上述四個隔離庫，以 JDK 21 封裝獨立服務，使用 `ddl-auto=validate`、合成 Session 與合成專案；沒有使用真實 Google 登入。測試前 project 庫有 13 筆會議、0 筆任務及 0 筆關聯，701／702 均為 ACTIVE。本輪在隔離 basic-data 庫將合成角色 201 加上 `task:write`；使用者 101 與 104 屬於該角色，107 保持只有 `meeting:write`。

| 項目 | 實際 HTTP 結果 |
| --- | --- |
| 文字決議 | 101 在專案 701 建立會議 15 回 200；GET 會議及其 tasks 均回 200，`taskIds` 與任務清單為空。 |
| 建立失敗 | 101 對會議 15 以非成員 `not-a-member` 作負責人建立任務，POST 回 400；測試庫任務筆數不變，GET tasks 回 200 且仍為空。 |
| 僅有會議寫入權 | 107 建立自己的會議 16 回 200；對其 POST 新任務回 403，任務筆數不變。 |
| 建立與讀回 | 101 對會議 15 建立標題「會議新任務」、負責人 101、期限 2026-11-01 的任務，POST 回 200、任務 ID 1、狀態 TODO；GET tasks 回 200 並顯示同一任務。再次 GET 會議回 200，任務筆數不變。 |
| 反查與完成狀態 | 用專案 701 和 taskId 1 查詢會議回 200，結果包含會議 15；PATCH 任務 1 為 DONE 回 200，重新 GET 會議 tasks 回 200 且狀態為 DONE。 |
| 既有任務與解除 | 101 在專案 701 建立任務 2 回 200；連結兩次各回 200，清單中只有一筆；DELETE 關聯回 200，之後查詢任務 2 回 200 且仍存在。 |
| 跨專案 | 104 在專案 702 建立任務 4 回 200；101 將它連到專案 701 的會議 15 時回 400，GET tasks 回 200 且不含任務 4。 |
| 已刪除任務 | 101 軟刪除任務 1 回 200；GET 會議 15 tasks 回 200，保留關聯並顯示 `deleted=true`、`status=null`。 |

首輪 POST 會議因 web-server 尚未從 Eureka 發現 project-service 回 500，等待服務註冊後重試；第二輪因上輪合成角色未具備 `task:write`，新建任務回 403。補足隔離角色權限後，上表全部通過。驗收結束後停止本輪隔離服務，保留測試庫及測資。無效指派發生在 Task 寫入前，尚未人為製造 Task 已寫入但關聯寫入失敗的資料庫故障，因此不能據此宣稱已實測該中途 rollback 情境。

瀏覽器使用同一隔離環境與合成使用者 101：在會議 14 建立任務 3，送出中建立及取消按鈕停用；重新整理後仍為同一筆。從會議進入任務可見來源會議，改為完成後返回會議顯示「完成」。搜尋並連結既有任務 2，再解除連結；先前 HTTP 案例確認解除不刪任務。搜尋以標題「既有任務」找到任務 2。通知發送及真實 Google 登入本輪未實測。

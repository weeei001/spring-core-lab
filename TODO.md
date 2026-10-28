# 待辦事項

只保留尚未完成的具體事項，寫明完成條件與已知阻礙；完成後移除，值得保留的踩坑經驗整理到 [HANDOFF.MD](HANDOFF.MD)。開始與結束任務時同步確認本檔，避免遺漏。

## 產品功能與操作體驗

- 標籤設定分頁已接上專案頁；待登入後驗證 `?tab=labels`、新增／修改／刪除標籤及結案唯讀。現有前端沒有自動化 UI 測試框架，本次未新增測試依賴。

使用者決定不做人工測試，以 2026-10-27 的代理 UI 驗收為基準：本機 `spring_core_lab_t2_*` 測試庫、真實 Google 登入、1440／390 寬度，並實際停掉 project-service 製造請求失敗。三段情境均通過：篩選表格以鍵盤開任務、編輯說明、看附件、返回保留篩選；8000 字會議紀錄失敗保留、建立待辦並從任務返回會議；五項驗收含失敗與受阻、隔天重開以新版本複測並由失敗建立 BUG。測試庫角色 USER 已加 `meeting:write`。

- 已觀察、未修改（不阻斷、不遺失資料）：390 頁首使用者名稱逐字換行、側欄開關蓋住麵包屑首字；後端斷線時錯誤訊息重複堆疊遮住抽屜；從任務「返回來源」回會議頁時捲回頁首；新增待辦視窗中日期面板開著按 Escape 會連同視窗關閉；驗收「結果」下拉無搜尋。完成條件：逐項決定修或不修，修正後在 390 重測。
- 尚未覆蓋的案例：原生重新整理／關閉分頁提示（工具無法驗證）、留言草稿與分享／舊連結、驗收連結既有 BUG、B 版再失敗、多項共用 BUG、結案唯讀與非成員／管理者角色。需要時以同一測試庫補測。
- 驗收項目第一次執行與同時修改項目內容的歷史保護方式仍待確認。

## 軟刪除改用 @SoftDelete

- 測試庫 `spring_core_lab_softdelete_basic_data`、`spring_core_lab_softdelete_project`、`spring_core_lab_softdelete_validate` 僅供本次驗證使用，可刪除。
- 2026-10-27 角色驗證建立的 `spring_core_lab_role_test_*` 為獨立測試庫，保留供檢查；確認不再需要後可清除。

## AWS Ubuntu 正式部署

- 正式 DB 的會議與驗收資料表已由使用者更新（2026-10-27）。部署本版時同步更新 basic-data、web-server 與前端，確認 project-service 的 `ddl-auto: validate` 通過。既有角色須配置 `meeting:write` 與新增的 `manual-qa:write`；三個預設角色可用 `deploy/role-permissions.sql` 預覽再套用，自訂角色由管理員個別決定。此腳本已通過本機獨立 MySQL 驗證，尚未套用正式或既有本機環境。完成條件為正式網址的會議與驗收頁可讀寫、缺權限時後端拒絕。
- 已改為直接 Google OIDC；建立 Google Auth Platform Web Client，依 `deploy/google-oidc.md` 在 web-server 資料庫建立 oidc_client_registration、寫入並啟用 Google 設定，部署新版並重啟。本機 Google 設定已填入、服務已啟動；正式資料庫尚未確認增量建表與 OIDC 資料。移除舊 GOOGLE_CLIENT_*、AUTHENTIK_* 與 spring.security.oauth2.client.*，清除該應用舊 Session，還原 OAuth 日誌 INFO。完成條件：正式 Google 登入後 `/backend/api/auth/me` 回 200、登出後回 401，並驗證角色與業務操作。原 authentik Cloudflare 例外已非新方案必要條件。
- Google 帳號使用新的 issuer＋sub 識別；若須沿用舊 authentik 帳號的角色、專案成員與任務歸屬，先確認舊 user ID 與 Google 身分對照，再遷移。現有資料未改動，不會依相同 email 自動合併。第一位 Google 管理員須以 `/api/auth/me` 的 subject 配置。
- 2026-10-27 已在本機內建瀏覽器以 Google 按鈕完成真實登入（t2 測試庫）；仍須補驗取消登入、錯誤重試與登出，正式網址另需驗證。
- 使用者回報 EC2 重啟後公網 IP 改變，須核對 Reboot 與 Stop/Start 操作及現有 Elastic IP 關聯；已建議綁定 Elastic IP，尚未執行。完成條件：固定 IP 已關聯實例、Cloudflare 的 project-manage A 記錄已更新、SSH／HTTPS 可用，停止後啟動仍保留該 IP。舊紀錄 `54.249.10.179` 不作為目前 IP 依據。執行環境為 Ubuntu 24.04.4 x86_64、2 CPU、7.6 GiB RAM，Java 21／Nginx 1.24 已安裝。
- MySQL 實際監聽 `0.0.0.0:3306`，待核對所有附加 Security Group 的入站規則與 Workbench 連線需求；完成條件為資料庫不接受非必要來源連線。尚無證據顯示公網可直接連入。
- 使用正式應用帳號啟動 deploy profile，確認結構驗證、Session 與附件目錄正常；MySQL 表結構已由使用者確認完成，無須重複建表。完成條件為正式 HTTPS 登入與業務資料讀寫成功。
- 確認 SES 使用區域的 production access 狀態，以及退信／申訴處理流程；完成條件為可向未逐一驗證的合法收件者寄送，並能處理後續退信／申訴。此項不能僅依測試收信成功判定，寄信配置紀錄見 `deploy/ses-mail.md`。
- 驗證正式網址的登入、權限、業務操作、上下載、版本切換、異機備份還原與主機重開機恢復。

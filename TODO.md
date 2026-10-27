# 待辦事項

只保留尚未完成的具體事項，寫明完成條件與已知阻礙；完成後移除，值得保留的踩坑經驗整理到 [HANDOFF.MD](HANDOFF.MD)。開始與結束任務時同步確認本檔，避免遺漏。

## 產品功能與操作體驗（2026-10-27 盤點）

- 任務雙容器人工驗收：`/tasks/:taskId`、既有 `?task=` 抽屜、共用內容、來源返回及未送出內容確認已實作；依 [任務詳情驗收案例](docs/task-detail-acceptance.md) 的 TD-01–TD-12 驗收分享、重整、上一頁、舊連結、欄位／留言一致、草稿隔離與桌面／390px。前次 T1 未啟動服務驗收；本輪僅補上 [任務清單與返回驗收](frontend/src/pages/project/project-detail/components/tasks/acceptance.md)，尚未完整重跑草稿、留言、附件等 TD 案例。完成條件為所有案例留下實際結果並完成失敗複測。
- 會議紀錄後續驗收與任務關聯：依 [會議驗收](docs/meeting-acceptance.md) 補驗原生重整／關閉提示、其他站內連結、日期控制項及 403／網路失敗的畫面保留。Windows 控制因無法可靠判定網址停止；Google OAuth 尚未重跑，前次使用合成 Session。依 [Meeting MVP 方案](docs/meeting-mvp.md) 的 R5 完成連結／解除、反查整合，以及 MM-09–MM-13、MM-16–MM-17 的完整任務與保留資料回退演練。共用日期時間規則與已執行的 JSON 驗收另見 [日期時間 JSON 驗收](docs/json-time-acceptance.md)。
- 少量同事手動驗收：使用者已確認以同事／少量測試人員為主。先做版本與環境、驗收清單、逐次結果、BUG 任務關聯與複測，不先建完整案例庫。完成條件：失敗與修補版複測紀錄均保留，複製下一輪清單不複製結果，BUG 完成不自動等於驗收通過；資料方案待核准。
- 驗收基準與回歸：依 [手動驗收基準](docs/manual-acceptance.md) 的 MA-00–MA-12 執行並保存逐次結果。2026-10-27 已啟動本機 3000／8080 與業務服務，Google 登入可用；本次已核准並建立 `spring_core_lab_t2_*` 獨立測試庫與 63 筆任務，表格／篩選／返回及成員、唯讀、結案情境見 [本次任務驗收](frontend/src/pages/project/project-detail/components/tasks/acceptance.md)。完整 MA 案例、非成員與管理者尚未執行。完成條件：各角色與結案／取消唯讀案例均留下版本、環境、帳號與實際證據，失敗保留 BUG 關聯與修補版複測。自動化測試依賴仍須另提最小方案取得同意；建置成功不代替驗收通過。

## AWS Ubuntu 正式部署

- 已改為直接 Google OIDC；建立 Google Auth Platform Web Client，依 `deploy/google-oidc.md` 在 web-server 資料庫建立 oidc_client_registration、寫入並啟用 Google 設定，部署新版並重啟。本機 Google 設定已填入、服務已啟動；正式資料庫尚未確認增量建表與 OIDC 資料。移除舊 GOOGLE_CLIENT_*、AUTHENTIK_* 與 spring.security.oauth2.client.*，清除該應用舊 Session，還原 OAuth 日誌 INFO。完成條件：正式 Google 登入後 `/backend/api/auth/me` 回 200、登出後回 401，並驗證角色與業務操作。原 authentik Cloudflare 例外已非新方案必要條件。
- Google 帳號使用新的 issuer＋sub 識別；若須沿用舊 authentik 帳號的角色、專案成員與任務歸屬，先確認舊 user ID 與 Google 身分對照，再遷移。現有資料未改動，不會依相同 email 自動合併。第一位 Google 管理員須以 `/api/auth/me` 的 subject 配置。
- 本次尚未完成登入畫面人工驗證：Codex Browser 初始化被 node:process 匯入限制阻擋，Chrome 備援工具因無法確認網址停止。本機 Google 設定已填入且登入入口已驗證轉址；仍須補驗 Google 按鈕、取消登入、錯誤重試、真實登入與登出。
- 使用者回報 EC2 重啟後公網 IP 改變，須核對 Reboot 與 Stop/Start 操作及現有 Elastic IP 關聯；已建議綁定 Elastic IP，尚未執行。完成條件：固定 IP 已關聯實例、Cloudflare 的 project-manage A 記錄已更新、SSH／HTTPS 可用，停止後啟動仍保留該 IP。舊紀錄 `54.249.10.179` 不作為目前 IP 依據。執行環境為 Ubuntu 24.04.4 x86_64、2 CPU、7.6 GiB RAM，Java 21／Nginx 1.24 已安裝。
- MySQL 實際監聽 `0.0.0.0:3306`，待核對所有附加 Security Group 的入站規則與 Workbench 連線需求；完成條件為資料庫不接受非必要來源連線。尚無證據顯示公網可直接連入。
- 使用正式應用帳號啟動 deploy profile，確認結構驗證、Session 與附件目錄正常；MySQL 表結構已由使用者確認完成，無須重複建表。完成條件為正式 HTTPS 登入與業務資料讀寫成功。
- 確認 SES 使用區域的 production access 狀態，以及退信／申訴處理流程；完成條件為可向未逐一驗證的合法收件者寄送，並能處理後續退信／申訴。此項不能僅依測試收信成功判定，寄信配置紀錄見 `deploy/ses-mail.md`。
- 驗證正式網址的登入、權限、業務操作、上下載、版本切換、異機備份還原與主機重開機恢復。

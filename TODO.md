# 待辦事項

只保留尚未完成的具體事項，寫明完成條件與已知阻礙；完成後移除，值得保留的踩坑經驗整理到 [HANDOFF.MD](HANDOFF.MD)。開始與結束任務時同步確認本檔，避免遺漏。

## AWS Ubuntu 正式部署

- 已改為直接 Google OIDC；建立 Google Auth Platform Web Client，依 `deploy/google-oidc.md` 在 web-server 資料庫建立 oidc_client_registration、寫入並啟用 Google 設定，部署新版並重啟。本機 Google 設定已填入、服務已啟動；正式資料庫尚未確認增量建表與 OIDC 資料。移除舊 GOOGLE_CLIENT_*、AUTHENTIK_* 與 spring.security.oauth2.client.*，清除該應用舊 Session，還原 OAuth 日誌 INFO。完成條件：正式 Google 登入後 `/backend/api/auth/me` 回 200、登出後回 401，並驗證角色與業務操作。原 authentik Cloudflare 例外已非新方案必要條件。
- Google 帳號使用新的 issuer＋sub 識別；若須沿用舊 authentik 帳號的角色、專案成員與任務歸屬，先確認舊 user ID 與 Google 身分對照，再遷移。現有資料未改動，不會依相同 email 自動合併。第一位 Google 管理員須以 `/api/auth/me` 的 subject 配置。
- 本次尚未完成登入畫面人工驗證：Codex Browser 初始化被 node:process 匯入限制阻擋，Chrome 備援工具因無法確認網址停止。本機 Google 設定已填入且登入入口已驗證轉址；仍須補驗 Google 按鈕、取消登入、錯誤重試、真實登入與登出。
- Cloudflare DNS 與 HTTPS 現已能連到正式站；待確認 `54.249.10.179` 是否為固定公網 IP。執行環境為 Ubuntu 24.04.4 x86_64、2 CPU、7.6 GiB RAM，Java 21／Nginx 1.24 已安裝。
- MySQL 實際監聽 `0.0.0.0:3306`，待核對所有附加 Security Group 的入站規則與 Workbench 連線需求；完成條件為資料庫不接受非必要來源連線。尚無證據顯示公網可直接連入。
- 使用正式應用帳號啟動 deploy profile，確認結構驗證、Session 與附件目錄正常；MySQL 表結構已由使用者確認完成，無須重複建表。完成條件為正式 HTTPS 登入與業務資料讀寫成功。
- 驗證已配置的 SMTP 能實際寄達指定測試信箱；設定已由使用者確認完成，尚未驗收應用寄信。
- 驗證正式網址的登入、權限、業務操作、上下載、版本切換、異機備份還原與主機重開機恢復。

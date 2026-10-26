# 待辦事項

只保留尚未完成的具體事項，寫明完成條件與已知阻礙；完成後移除，值得保留的踩坑經驗整理到 [HANDOFF.MD](HANDOFF.MD)。開始與結束任務時同步確認本檔，避免遺漏。

## AWS Ubuntu 正式部署

- 正式登入阻礙已定位：EC2 POST `https://auth.ntubimdbirc.tw/application/o/token/` 被 Cloudflare Challenge 回 403。使用者已建立 Skip 規則，但確認建立位置是 `jianwii.dev`，未套用到 authentik 所屬 zone。待在 `ntubimdbirc.tw` 建立同樣的主機＋EC2 IP＋Token 路徑＋POST 條件與已選 Skip 項目，放最前面並啟用；錯誤 zone 的規則可停用。若使用者無該 zone 權限，須由其管理者處理。最新 12:24:12 UTC 失敗 Ray ID 為 `a5096fc4aaebe373`。修正後重新登入，驗證 `/backend/api/auth/me` 回 200，並將臨時 OAuth TRACE 還原 INFO；登入尚未驗收。
- Cloudflare DNS 與 HTTPS 現已能連到正式站；待確認 `54.249.10.179` 是否為固定公網 IP。執行環境為 Ubuntu 24.04.4 x86_64、2 CPU、7.6 GiB RAM，Java 21／Nginx 1.24 已安裝。
- MySQL 實際監聽 `0.0.0.0:3306`，待核對所有附加 Security Group 的入站規則與 Workbench 連線需求；完成條件為資料庫不接受非必要來源連線。尚無證據顯示公網可直接連入。
- 使用正式應用帳號啟動 deploy profile，確認結構驗證、Session 與附件目錄正常；MySQL 表結構已由使用者確認完成，無須重複建表。完成條件為正式 HTTPS 登入與業務資料讀寫成功。
- 驗證已配置的 SMTP 能實際寄達指定測試信箱；設定已由使用者確認完成，尚未驗收應用寄信。
- 驗證正式網址的登入、權限、業務操作、上下載、版本切換、異機備份還原與主機重開機恢復。

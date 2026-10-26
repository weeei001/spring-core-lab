# 曾踩過的坑

開始新任務前先閱讀本檔，避免重複踩坑。只記錄已遇到的限制、原因與處理方式；未完成事項放在 [TODO.md](TODO.md)，完成紀錄不留在本檔。以下經驗於 2026-10-26 整理，環境或設定改變後應重新確認。

## Spring Session 使用預設資料表

- 使用套件內建 MySQL schema，表名為 `SPRING_SESSION` 與 `SPRING_SESSION_ATTRIBUTES`；只改 `table-name` 不會替換內建 SQL 的表名，不要再單獨改成 `WEB_SESSION`。
- 舊 `WEB_SESSION` 的登入狀態不會自動搬移，切換後須重新登入；正式環境須先建立預設資料表。
- Spring Boot 4.0.8 的 Session 初始化預設會繼續處理 SQL 錯誤，允許已有資料表時重啟；若本機建表未成功，須檢查資料庫權限與初始化日誌。此機制不會自動升級既有結構。

## Spring Boot 不會自動讀取 `.env`

- 須由服務管理器或 shell 注入變數，不要只建立 `.env` 檔案就假設已載入；變數範本見 `deploy/.env.example`。

## Maven 建置會修改 Java 排版

- 依據：`backend/pom.xml` 在 `process-sources` 綁定 `spotless:apply`。
- 處理：只檢查排版時使用 `./mvnw spotless:check`；執行建置後檢查差異，避免夾帶無關修改。


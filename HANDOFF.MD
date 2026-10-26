# 曾踩過的坑

開始新任務前先閱讀本檔，避免重複踩坑。只記錄已遇到的限制、原因與處理方式；未完成事項放在 [TODO.md](TODO.md)，完成紀錄不留在本檔。以下經驗於 2026-10-26 整理，環境或設定改變後應重新確認。

## Spring Session 使用預設資料表

- 使用套件內建 MySQL schema，表名為 `SPRING_SESSION` 與 `SPRING_SESSION_ATTRIBUTES`；只改 `table-name` 不會替換內建 SQL 的表名，不要再單獨改成 `WEB_SESSION`。
- 舊 `WEB_SESSION` 的登入狀態不會自動搬移，切換後須重新登入；正式環境須先建立預設資料表。
- Spring Boot 4.0.8 的 Session 初始化預設會繼續處理 SQL 錯誤，允許已有資料表時重啟；若本機建表未成功，須檢查資料庫權限與初始化日誌。此機制不會自動升級既有結構。
- Windows MySQL 的 `lower_case_table_names=1` 會將資料表顯示成小寫 `spring_session`／`spring_session_attributes`；不要誤判為未建立預設資料表。

## 外部 properties 設定與環境變數不同

- `deploy/application.properties` 位於預設搜尋位置之外，須透過 `spring.config.import` 載入；Maven 的 `-Pdeploy` 已集中設定匯入參數與 Spring `deploy` profile。
- 匯入後是 properties 設定，不是作業系統環境變數；不要依賴檔案中的 `SPRING_PROFILES_ACTIVE` 啟用部署模式，應使用啟動參數。值的外層引號會成為內容，反斜線是跳脫字元，Windows 路徑使用 `/`。

## Maven 建置會修改 Java 排版

- 依據：`backend/pom.xml` 在 `process-sources` 綁定 `spotless:apply`。
- 處理：只檢查排版時使用 `./mvnw spotless:check`；執行建置後檢查差異，避免夾帶無關修改。


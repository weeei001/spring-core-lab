# 全域 JSON 日期時間驗收

2026-10-27，JDK 21／Spring Boot 4.0.8／Jackson 3.0.3。本輪將原 MeetingTimeDeserializer 檔案移至 common/config/JacksonConfiguration.java，集中註冊 LocalDate、LocalTime、LocalDateTime 的反序列化規則，移除 Meeting 與 MeetingQueryParameter 的三個欄位註解。

`CommonAutoConfiguration` 匯入設定；Spring Boot 將 JacksonModule bean 套用到自動設定的 JsonMapper。三種類型共用 helper，格式分別為 ISO_LOCAL_DATE、ISO_LOCAL_TIME、ISO_LOCAL_DATE_TIME。未新增依賴、修改 schema 或連線資料庫。

## 實際結果

- 在本機隨機 loopback 連接埠啟動真正的 Spring Boot／Spring MVC，用現有 JAR 依賴執行獨立驗收程式，結束後自動關閉。沒有 mock JsonMapper、message converter 或 HTTP client。
- 73 項 HTTP／RestClient 檢查通過，並另外確認 JsonMapper bean 能讀取 root LocalDateTime，以及序列化與未註冊自訂 module 的相同設定 mapper 一致。
- 合法 ISO local 字串、閏日、只有時分、秒及 9 位小數可讀回；Z、正負 offset、日期／時間／日期時間混用、非法日期／時分、空字串、前後空白、數字、陣列及物件均回 HTTP 400。
- 以實際 Meeting、MeetingQueryParameter、Project、TaskQueryParameter、Notification 型別驗證；包含繼承 BaseEntity 的 createTime／transTime、巢狀 List／Map 與可為 null 的欄位。
- Spring Boot 提供的 RestClient.Builder：三種類型的合法回應成功反序列化，帶 Z 的回應被拒絕。
- Instant／OffsetDateTime 仍接受其合法時區格式，三種 local 型別的序列化維持既有輸出。首輪對 OffsetDateTime 原 offset 的斷言失敗，已確認是既有 UTC 正規化行為；改成與同設定的基線 mapper 比較後通過，沒有修改產品時區設定。
- JDK 21：`./mvnw.cmd -pl web-server,project-service -am test`、`./mvnw.cmd spotless:check` 成功。Maven test 生命週期沒有測試案例，不能算自動覆蓋；本輪沒有新增測試依賴。
- Spotless 僅修正本輪 JacksonConfiguration 排版；原有未提交的會議功能與前端修改保留。

驗收程式在本機 `backend/target/jackson-verification/JacksonSmoke.java`，使用既有依賴執行；不納入 Maven 測試生命週期。Java source-file 模式沒有 Maven 的 `-parameters`，臨時 controller 的 PathVariable 須明示名稱。這是驗收程式的修正，產品 controller 沒有改動。

本輪未重新執行前端檢查、業務資料庫或登入／畫面驗收。URL query／path 的型別轉換不由此 Jackson module 處理；各服務自行建立的 mapper 亦不會自動繼承 Spring Boot 的 bean 配置。

## 參考

依 [Spring Boot 官方的 JacksonModule 註冊方式](https://docs.spring.io/spring-boot/how-to/spring-mvc.html) 集中設定，保留既有嚴格字串解析契約。沒有只改 lenient 開關，因為本次也要求保留拒絕空字串、陣列與不合法型別的行為。

# spring-core-lab 後端

提供專案、任務、工時、請假與通知功能的後端，並集中處理使用者登入、角色權限及檔案上傳。採用 Java 21、Spring Boot 4、Spring Cloud、MySQL 與 Google OIDC；前端透過 `web-server` 存取業務服務。

[前端啟動說明](../frontend/README.md) · [協作規範](../AGENTS.md) · [已知問題](../HANDOFF.MD)

## 開始前

| 項目 | 需求 |
|---|---|
| Java | JDK 21，確認 `JAVA_HOME` 指向 JDK |
| Maven | 使用隨附的 Maven Wrapper，首次執行需要下載套件 |
| MySQL | 本機預設為 `localhost:3306`；業務資料庫為 `basic_data`、`project`、`notification` |
| Google OIDC | Google Auth Platform 的 Web Client ID／Client Secret |
| 郵件 | 需要寄信時提供 SMTP 設定；本機驗證可關閉寄信 |

下列指令以 `backend/` 為工作目錄。若目前位於儲存庫根目錄，先執行 `cd backend`。Windows PowerShell 將 `./mvnw` 改為 `.\mvnw.cmd`。

## 本機啟動

### 1. 設定資料庫與登入

預設啟用 `local` profile。各服務的公開設定放在 `src/main/resources/application.yml`；在同一目錄建立 `application-local.yml` 補上本機帳密，這個檔名已被 Git 忽略，也不會包入 JAR。

在 `basic-data`、`project-service`、`notification-service` 各自的 `application-local.yml` 設定資料庫帳號；預設 URL 已分別指向三個業務資料庫：

```yaml
spring:
  datasource:
    username: "你的本機資料庫帳號"
    password: "你的本機資料庫密碼"
```

本機設定會嘗試建立資料庫，並透過 Hibernate 更新業務資料表；帳號需有對應權限，也可以事先建好資料庫。

在 `web-server/src/main/resources/application-local.yml` 設定登入與工作階段資料庫：

```yaml
spring:
  datasource:
    username: "你的本機資料庫帳號"
    password: "你的本機資料庫密碼"
```

在 web-server 的 datasource 資料庫執行 [OIDC 建表 SQL](web-server/src/main/resources/db/oidc-client-registration.sql)，再依 [Google SQL 範本](../deploy/oidc-clients.sql.example) 寫入憑證並啟用。OIDC 設定只從 MySQL 讀取，修改後重啟生效；沒有啟用的提供者時啟動會報錯。

Google Auth Platform 的回呼網址填 `http://localhost:3000/auth/callback/google`。
後端預設 `app.public-url=http://localhost:3000`，從 8080 開始登入也會回到 3000，因此須啟動前端代理。
若要直接經後端回呼，在本機設定加入 `app.public-url: http://localhost:8080`，並在 Google 登記 `http://localhost:8080/auth/callback/google`。

完整 Google Auth Platform 欄位、正式部署與新增 OIDC 的設定見 [Google OIDC 配置](../deploy/google-oidc.md)。
舊本機設定須刪除 `GOOGLE_CLIENT_*`、`AUTHENTIK_*` 與 `spring.security.oauth2.client.*`。網站 public-url／frontend-url 繼續保留在環境設定。

本機啟動時，業務服務以 `ddl-auto: update` 自動建立／更新資料表；`web-server` 預設把登入工作階段存入 `basic_data`，以 `initialize-schema: always` 使用 Spring Session 套件內建的 MySQL schema，自動建立預設的 `SPRING_SESSION` 與 `SPRING_SESSION_ATTRIBUTES`，不需維護自訂 SQL。此初始化不負責升級既有資料表結構；啟動 `web-server` 前須確保 `basic_data` 資料庫已存在。

若本機不需要寄信，在 `notification-service` 的 `application-local.yml` 加入：

```yaml
app:
  mail:
    enabled: false
```

需要寄信時，改為啟用並設定 `spring.mail.host`、`port`、`username`、`password` 與 `app.mail.from`。

### 2. 建置所有模組

```bash
./mvnw clean install
```

這會將共用模組安裝到本機 Maven 儲存庫，供各服務單獨啟動時使用。建置也會自動執行 Palantir Java Format；完成後檢查程式碼差異。

### 3. 依序啟動服務

先準備 MySQL 與 Google OIDC 憑證，再啟動 Eureka、三個業務服務，最後啟動 `web-server`。以下每條指令在獨立終端執行，工作目錄皆為 `backend/`：

```bash
./mvnw -pl eureka-server spring-boot:run
./mvnw -pl basic-data spring-boot:run
./mvnw -pl project-service spring-boot:run
./mvnw -pl notification-service spring-boot:run
./mvnw -pl web-server spring-boot:run
```

| 模組 | 職責 | 服務連接埠 | 健康檢查連接埠 |
|---|---|---:|---:|
| `eureka-server` | 服務註冊與探索 | 8761 | 9761 |
| `basic-data` | 使用者、角色、流水號、請假、上傳 | 8081 | 9081 |
| `project-service` | 專案、任務、工時、留言、附件 | 8082 | 9082 |
| `notification-service` | 站內通知、郵件佇列與派送 | 8083 | 9083 |
| `web-server` | 登入、權限與瀏覽器 API 入口 | 8080 | 9080 |

### 4. 確認可用

開啟 [Eureka](http://localhost:8761)，確認業務服務與 `web-server` 已註冊。在服務所在主機執行：

```bash
curl http://127.0.0.1:9080/actuator/health
```

健康狀態應為 `UP`；其他服務替換成表中的健康檢查連接埠。這些端點只綁定本機位址，主服務連接埠不提供健康檢查入口。

接著依[前端 README](../frontend/README.md)啟動畫面，登入後開啟[目前登入者](http://localhost:3000/api/auth/me)，確認回傳使用者資料。畫面可見功能依帳號權限而定。

## 模組與請求流程

```text
瀏覽器 → 前端 → web-server → 業務服務 → MySQL
                    ↕           ↕
                 Google OIDC  Eureka（服務探索）
```

- `common/`：共用實體、DAO／Service 基底、查詢、驗證、例外與回應格式。
- `common-api/`：Spring HTTP Interface、查詢參數與服務間傳輸型別。
- 各服務：`src/main/java/dev/jianwii/` 放程式，`src/main/resources/` 放設定。
- Controller 處理入口，Service 實作業務規則，DAO 存取資料。跨服務契約集中於 `common-api`。

瀏覽器只持有 `SPRING_CORE_SESSION` 工作階段 Cookie，OAuth2 Token 留在後端。修改資料的請求需帶 CSRF Cookie 與 `X-XSRF-TOKEN` 標頭；前端共用 HTTP Client 已處理此流程。

## 開發與驗證

| 指令 | 用途 |
|---|---|
| `./mvnw clean install` | 建置所有模組並安裝本機依賴 |
| `./mvnw test` | 執行 Maven 測試生命週期 |
| `./mvnw spotless:check` | 只檢查 Java 排版 |
| `./mvnw spotless:apply` | 修正 Java 排版 |

目前不保留自動化測試檔或測試依賴。修改後依範圍執行編譯與排版檢查，並實際驗證登入、權限及業務流程；Google 真實登入須配置憑證後驗收。

[`../http/`](../http/) 提供手動 API 範例，使用目前設定的 Cookie 名稱 `SPRING_CORE_SESSION`。依照 `auth.http` 的說明登入，將 Cookie 值填入被忽略的 `http-client.private.env.json` 的 `session`，不要提交 Cookie。

## 部署設定

將 [`../deploy/application.properties.example`](../deploy/application.properties.example) 複製成 `deploy/application.properties`，填妥帳密與部署設定；該檔案已被 Git 忽略。各服務透過 `spring.config.import` 直接讀取，不需額外載入腳本或套件。

檔案使用 `KEY=VALUE`，不加 `export` 或值的外層引號，註解獨立成行。Windows 路徑使用 `/`；其他值若含字面反斜線，須寫成 `\\`。`deploy` profile 由啟動參數指定，不在 properties 檔中設定 `SPRING_PROFILES_ACTIVE`。

完成建置與下方部署準備後，在 `backend/` 目錄的不同 PowerShell 終端依序執行：

```powershell
.\mvnw.cmd -Pdeploy -pl eureka-server spring-boot:run
.\mvnw.cmd -Pdeploy -pl basic-data spring-boot:run
.\mvnw.cmd -Pdeploy -pl project-service spring-boot:run
.\mvnw.cmd -Pdeploy -pl notification-service spring-boot:run
.\mvnw.cmd -Pdeploy -pl web-server spring-boot:run
```

Maven 的 `-Pdeploy` 會替各服務設定 Spring `deploy` profile，並以儲存庫的絕對路徑匯入 `deploy/application.properties`；缺少檔案時啟動會失敗。一般本機啟動指令維持原有的 `local` profile。

若直接使用 IntelliJ 啟動，在各服務的 **Run Configuration → Program arguments** 填入以下兩個參數，將路徑換成實際位置：

```text
--spring.profiles.active=deploy
"--spring.config.import=file:C:/Users/Wei/Desktop/spring-core-lab/deploy/application.properties"
```

啟動已打包的 JAR 時，也在 `java -jar <服務.jar>` 後加上這兩個參數。分機部署可改成各服務專用的設定檔路徑；外部 properties 檔不會打包進 JAR。環境變數仍可覆寫檔案中的同名設定。

載入方式依據 Spring Boot 的 [外部設定](https://docs.spring.io/spring-boot/4.0/reference/features/external-config.html) 與 [Maven 啟動參數](https://docs.spring.io/spring-boot/4.0/maven-plugin/run.html)。

- 部署前須建立資料庫與資料表；業務服務以 `ddl-auto: validate` 驗證結構，不會更新資料表。
- `deploy` 的 Spring Session 設定為 `initialize-schema: never`，不會自動建表；首次部署前，須在工作階段資料庫手動執行專案相依版本 `spring-session-jdbc` 套件內的 `org/springframework/session/jdbc/schema-mysql.sql`。升級 Spring Session 時，須核對相依版本的 schema 並手動套用結構變更。
- `web-server` 需要範本中的 `WEB_SESSION_DB_URL`、`WEB_SESSION_DB_USERNAME`、`WEB_SESSION_DB_PASSWORD`；範例沿用 `basic_data` 資料庫並使用專用帳號 `web_session_app`，部署前須授權帳號並填入密碼。
- 資料庫使用專用帳號；上傳及舊附件目錄依範本設定為已存在的絕對路徑。
- 對外提供前端與 `web-server`，Eureka、業務服務及資料庫留在內部網路。正式登入回呼由 `APP_PUBLIC_URL` 決定。本次同網域部署使用 `APP_PUBLIC_URL=https://project-manage.jianwii.dev/backend`，前端使用 `APP_FRONTEND_URL=https://project-manage.jianwii.dev`；Nginx 移除 `/backend/` 前綴後轉送，後端不設定 context-path。
- 驗收期間依範本保持 `APP_MAIL_ENABLED=false`。部署模式仍會檢查 SMTP 必填設定。

## 常見問題

| 現象 | 檢查方向 |
|---|---|
| 找不到 `common`／`common-api` | 先在 `backend/` 執行完整的 `./mvnw clean install` |
| 資料庫連線失敗 | 檢查 MySQL、服務所屬資料庫、本機設定與帳號權限 |
| 登入回呼不符或登入失敗 | 檢查 Google／OIDC 回呼網址、issuer、Client 資訊，以及 `basic-data` 是否已註冊 |
| 找不到 `SPRING_SESSION` 資料表 | 本機檢查初始化設定、啟動日誌與建表權限；正式環境先手動執行套件內建的 Session 建表 SQL |
| API 回傳 401／403 | 檢查登入狀態、Cookie 名稱、CSRF 與角色權限 |

提交前請閱讀[協作規範](../AGENTS.md)，並在 PR 說明實際完成的驗證。

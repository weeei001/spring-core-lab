# 曾踩過的坑

開始新任務前先閱讀本檔，避免重複踩坑。只記錄已遇到的限制、原因與處理方式；未完成事項放在 [TODO.md](TODO.md)，完成紀錄不留在本檔。以下經驗於 2026-10-26 整理，環境或設定改變後應重新確認。

## 正式後端使用 /backend 前綴

- API 共用 Axios 的 baseURL 不會自動套用到圖片與附件連結。`attachmentApi.contentUrl` 與 `uploadFileApi.contentUrl` 須使用 `http.getUri`；登入按鈕也須加前綴。2026-10-26 已用 Vite 載入實際模組，確認正式與開發模式的五種網址。
- 本次 Nginx 用 `proxy_pass http://127.0.0.1:8080/` 移除 `/backend/`，並清空 `X-Forwarded-Prefix`。後端保持根 context，讓前端可讀取 Path=/ 的 CSRF Cookie。`APP_PUBLIC_URL` 包含 `/backend`，`APP_FRONTEND_URL` 不包含；不要再另設 servlet context-path。
- EC2 上的正式密碼已由使用者填妥；本機 `deploy/artifacts/application.properties` 是早期準備檔，不能拿它覆蓋遠端設定。修改網域時只編輯遠端 URL 欄位。
- 排查登入後 401 時，API 網址須在使用者已登入的同一瀏覽器開啟。教學要明確分開「Ubuntu 終端機指令」與「瀏覽器網址」；曾出現把 API 路徑接在 journalctl 管線後當命令執行的情況。另開無 Cookie 的 curl 不能驗證該瀏覽器的登入狀態。

## Spring Session 使用預設資料表

- 使用套件內建 MySQL schema，表名為 `SPRING_SESSION` 與 `SPRING_SESSION_ATTRIBUTES`；只改 `table-name` 不會替換內建 SQL 的表名，不要再單獨改成 `WEB_SESSION`。
- 舊 `WEB_SESSION` 的登入狀態不會自動搬移，切換後須重新登入；正式環境須先建立預設資料表。
- Spring Boot 4.0.8 的 Session 初始化預設會繼續處理 SQL 錯誤，允許已有資料表時重啟；若本機建表未成功，須檢查資料庫權限與初始化日誌。此機制不會自動升級既有結構。
- Windows MySQL 的 `lower_case_table_names=1` 會將資料表顯示成小寫 `spring_session`／`spring_session_attributes`；不要誤判為未建立預設資料表。

## 外部 properties 設定與環境變數不同

- `deploy/application.properties` 位於預設搜尋位置之外，須透過 `spring.config.import` 載入；Maven 的 `-Pdeploy` 已集中設定匯入參數與 Spring `deploy` profile。
- 匯入後是 properties 設定，不是作業系統環境變數；不要依賴檔案中的 `SPRING_PROFILES_ACTIVE` 啟用部署模式，應使用啟動參數。值的外層引號會成為內容，反斜線是跳脫字元，Windows 路徑使用 `/`。
- 2026-10-26 正式站登入 302 的 `redirect_uri` 實測多出 `app_public_url=`，authentik 因此回 Redirect URI Error。排查時先解析登入入口 Location 的 `redirect_uri`，不要直接放寬 authentik Regex。properties 每行只寫一次 `KEY=`，URL 值不包含 key 名稱；實際遠端檔案仍待確認。

## Windows OpenSSH 私鑰 ACL

- 此機的 OpenSSH 會拒絕載入允許 `CodexSandboxUsers` 存取的私鑰，顯示 UNPROTECTED PRIVATE KEY FILE。2026-10-26 的受限臨時副本連線方案亦遭工具自動審核阻擋；不要改用其他途徑繞過拒絕。未連入前不能聲稱讀取或修正過 EC2 設定。

## OAuth 登入失敗日誌層級

- 本機 Spring Security 7.0.7 sources JAR 確認：`AbstractAuthenticationProcessingFilter.unsuccessfulAuthentication` 用 TRACE 記錄 `Failed to process authentication request` 與例外，INFO／DEBUG 看不到這段原因。診斷時可暫設 `logging.level.org.springframework.security.oauth2.client.web.OAuth2LoginAuthenticationFilter=TRACE`，重啟並重現一次後還原 INFO。不要因此開啟整個 Security 或 HTTP 層的 TRACE；成功日誌會帶帳號屬性，分享前需遮蔽。
- 2026-10-26 12:12:28 UTC 正式日誌已證實：web-server POST `https://auth.ntubimdbirc.tw/application/o/token/` 收到 Cloudflare 403 HTML（Just a moment），導致 `invalid_token_response`，網站登入未完成。應在 authentik 所屬 `ntubimdbirc.tw` 的 Cloudflare 查 Security Event，依命中的安全產品對 EC2 出口 IP＋主機＋Token 路徑＋POST 建精確例外；不要因後續 API 401 就先改 Session／Cookie 或放寬 Redirect URI。Cloudflare Bot Fight Mode 不支援 Skip，需與 Super Bot Fight Mode 區分。
- 後續使用者確認把 `spring-authentik-token` 規則建在 `jianwii.dev`，因此無法處理目的地主機 `auth.ntubimdbirc.tw` 的 Challenge。Cloudflare 網域層規則須建立在目的地主機所屬 zone；規則 Active 不代表它會跨網域生效，Events=0 也不能單獨判斷根因。填規則前先核對左上角選取的網域。

## Maven 建置會修改 Java 排版

- 依據：`backend/pom.xml` 在 `process-sources` 綁定 `spotless:apply`。
- 處理：只檢查排版時使用 `./mvnw spotless:check`；執行建置後檢查差異，避免夾帶無關修改。
- Windows 本機服務執行中的 JAR 會鎖住 `target`，造成 Maven clean 失敗。正式產物可在獨立目錄複製後端來源後建置，複製時排除 `target` 與 `application-local.*`，不用中止使用者正在執行的服務。
- 本機預設 Java 目前為 25，部署建置需在該次命令設定 `JAVA_HOME=C:/Users/Wei/.jdks/ms-21.0.12.1` 使用 JDK 21。Corepack 未在 PATH 時可依前端 README 使用 `npx --yes --package=corepack -- corepack yarn ...`，仍遵循 package.json 的 Yarn 版本。

## 正式啟動的 SMTP 與目錄限制

- 2026-10-26 部署盤點確認：`DeployConfigurationValidator` 在 `notification-service` 無條件要求 SMTP 欄位非空，即使 `APP_MAIL_ENABLED=false` 也一樣。關閉寄信不能省略 SMTP 設定；健康檢查亦已停用 mail，`UP` 不代表寄信成功。
- 同一驗證器要求上傳目錄已存在、可讀寫且位於程式工作目錄之外；systemd 須明確設定 WorkingDirectory，附件使用獨立資料目錄，不能隨版本目錄一起切換。

## 本機結構匯出與正式 schema 映射

- 2026-10-26 實際本機設定指向 `spring_core_lab_verify_basic_data`、`spring_core_lab_verify_project`、`spring_core_lab_verify_notification`、`spring_core_lab_verify_session`，不是 README 的預設庫名。匯出前須以實際 datasource URL 為準；YAML 也可能採 `spring.datasource.url` 這種扁平 key。
- 正式沿用 `basic_data`、`project`、`notification`，獨立 Session 庫的兩張表合併至 `basic_data`。Windows 匯出時須將 Session 表與外鍵引用轉成 `SPRING_SESSION`／`SPRING_SESSION_ATTRIBUTES`，不能只改 schema 名稱。
- mysqldump 匯入後，SHOW CREATE TABLE 可能多出明示的 CHARACTER SET，而 COLLATE 未變；比對時應辨識等價語法。用不同連線匯入後驗證新建表，驗證連線使用 autocommit，避免舊交易快照造成 MySQL 1412。


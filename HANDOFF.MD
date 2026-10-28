# 曾踩過的坑

開始新任務前先閱讀本檔，避免重複踩坑。只記錄已遇到的限制、原因與處理方式；未完成事項放在 [TODO.md](TODO.md)，完成紀錄不留在本檔。以下經驗於 2026-10-26 整理，環境或設定改變後應重新確認。

## MySQL 時間精度與驗收 Session

- 症狀：驗證共用 Jackson 配置時，OffsetDateTime 的原始 +08:00 與回應的 Z 不相等。原因：既有 mapper 會依 context 正規化為 UTC，並非 local 型別的 module 改了 OffsetDateTime。正確做法：同時比對未註冊自訂 module、其餘設定相同的基線 mapper，或在只驗證時間點時比較 Instant，不為符合錯誤斷言改動產品時區設定。
- 症狀：新增回應與後續 PATCH 的 createTime 比字串不相等。原因：JPA Auditing 的即時物件可能含 7 位以上小數，MySQL datetime(6) 會取整；資料庫中的建立時間沒有被 PATCH 改動。正確做法：以解析後時間值或兩次持久化讀回值驗證，不因小數位差異修改共用 BaseEntity。
- 症狀：隔離驗收想複製現有 OIDC 設定時遭自動核准審查拒絕。原因：client_secret 移轉不包含在一般建測試庫授權，ALL PRIVILEGES 也超過執行期所需。正確做法：可只驗業務授權時，用合成 OIDC／Spring Session fixture，服務帳號只給核准測試庫的 CRUD，並明確回報沒有驗證真實 Google 登入；不能把秘密複製換成其他隱藏途徑。

## 原生離開提示的瀏覽器工具限制

- 症狀：站內未儲存確認可見，但程式化 reload 沒有可觀察的 beforeunload 對話框，內建分頁曾失效；Edge 的快捷鍵呼叫也未觸發真正重整。原因：這些工具結果不能證明使用者按瀏覽器重新整理時的行為。正確做法：保留路由確認與原生提示兩種獨立結果；Windows 原生控制若因無法可靠判定網址而停止，停止 UI 操作，標記原生重整／關閉尚未驗證，不以其他自動化繞過。

## 功能檔案存在不代表畫面已開放

- 2026-10-27 架構盤點時，README 的功能描述容易讓人誤認專案頁已提供表格與甘特圖。實際 `ProjectDetailPage` 只有 `tasks`／`members`，`TasksTab` 只掛 `TaskBoardView`；`TaskTableView`、`TaskGanttView` 與 `LabelTab` 雖有檔案，當時沒有被其他元件引用。盤點功能時須核對路由與引用，再區分已有程式、已接 UI、實測可用；要恢復入口應先檢查舊元件是否符合目前契約。
- 任務抽屜已由 `useTaskDrawer` 的 `?task=` 支援分享與重整；UI 改善不應以「沒有深連結」為前提。新增完整任務頁時保留現有通知／搜尋連結，重點驗證返回清單與未送出草稿。

## 任務離開確認與文字編輯

- 症狀：瀏覽器驗收在點「返回來源」後讀到捲動為 0，誤判還原失敗。原因：任務詳情與清單有相同標題，以標題文字等待會在舊頁仍顯示時提早通過。正確做法：等待目標 `table` 顯示、抽屜關閉，再量測捲動；本次用第二頁測資及完整頁重整返回確認 React Router `ScrollRestoration` 可還原。清單 key 保留篩選與表格狀態，移除 `task` 並排序 query；抽屜／篩選導航須加 `preventScrollReset`。

- 症狀：點導頁連結時，尚未確認放棄的欄位已送出更新。原因：Ant Design `Typography.editable` 的 `onBlur` 會呼叫儲存；原本預估工時也綁在 blur 儲存。需要可放棄的草稿時，使用明確的儲存／取消，不靠路由確認撤銷已送出的更新。
- 症狀：只阻擋 pathname 變化，關閉抽屜或切換任務仍會遺失草稿。原因：這些動作只改 `?task=`。任務的路由阻擋需同時判斷 pathname 與 `task`，內容以任務 ID 作 key，並合併未顯示分頁的草稿狀態。來源網址可保留 URL 參數，無法保存原頁只存在元件 state 的看板篩選。

## React Query 背景錯誤與任務連結

- 症狀：抽屜內有未儲存說明時後端暫停，視窗聚焦觸發的背景重新查詢失敗，整頁變成「找不到專案」，草稿隨抽屜卸載。原因：React Query 重新查詢失敗仍保留舊 `data`，但 `isError` 為 true；把 `isError` 當成沒有資料就會換掉整頁。判斷錯誤頁時看 `data === undefined`，有舊資料只顯示提示。
- 連到 `/tasks/:id` 的站內連結須用 `taskDetailPath(id, 來源)`，否則完整頁只有「返回專案」。驗收頁的來源帶 `?item=` 才會選回原項目。
- 只有 `onClick` 的 `Typography.Link` 沒有 `href`，Tab 不會停留；要讓鍵盤可開啟就給真實 href，再在 `onClick` 中 `preventDefault`。

## 內建瀏覽器 UI 驗收

- 2026-10-28 共用版面驗證：通知 Popover 在 320px 即使內容縮窄、改 bottom 與 shiftX，實測仍超出右緣；僅縮內容寬度不能證明浮層可用。手機通知改用 Drawer，實測 dialog 左右為 0／320、頁面 scrollWidth=320；桌面保留 Popover。驗證浮層須等待開啟動畫結束再量測實際邊界。
- 瀏覽器改 viewport 或元件熱更新後立即連續點擊，曾讀到舊尺寸結果或點中移位前的位置。先讀更新後畫面，再點控制項並等待結果；測試頁使用 MemoryRouter 時須有 index route，否則品牌回 `/` 只有版面、沒有測試內容。

- 2026-10-28 前端建置在 sandbox 內遇到 Vite `spawn EPERM`；提權後預設 WindowsApps `pwsh.exe` 又發生程序啟動存取拒絕。改用 `C:\Windows\System32\WindowsPowerShell\v1.0\powershell.exe` 提權執行 `yarn.cmd build` 可完成。這是執行環境限制，不需修改 Vite 設定。

- 內建瀏覽器窗格窄時，1440 模擬畫面會縮小，座標點擊常落空；用 `find` 的 ref 或元素 `click()` 操作，以 `scrollWidth`、`activeElement` 等 DOM 量測當證據。窗格寬度變動會清掉模擬尺寸，需重新設定。
- Ant Design Select 選完立即送 Tab，偶爾焦點仍留在 Select；連續按鍵間稍等再查 `activeElement`，不要直接判定為焦點 bug。Modal 中日期面板開著按 Escape 會連同 Modal 關閉。
- 製造請求失敗可在測試中強制停止本機 `project-service`，web-server 會回 500；重啟後約 30 秒才完成 Eureka 註冊。
- Windows 上以 Python 文字模式寫檔會變成 CRLF；改檔用 `newline=''` 或二進位讀寫，提交前檢查換行。

## HTML 說明頁的輸出與預覽

- 症狀：`am render` 拒絕嵌入副檔名為 png 的瀏覽器截圖。原因：本輪 screenshot API 實際回傳 JPEG，檔頭為 FFD8，直接存成 png 不會轉換格式。正確做法：保存前檢查檔頭或 MIME type，使用對應副檔名；不要將改檔名稱作影像格式轉換。
- 2026-10-27 `answer-me-with-html` 預設輸出因家目錄不可寫，改存 sandbox 臨時目錄；下一個指令無法讀到該路徑。交付時使用 `render ... --no-open -o <本次可寫的 visualizations 絕對路徑>`，並在後續指令確認檔案存在。
- 當次 Codex Browser 拒絕 `file://`，政策只允許 HTTP／HTTPS，且明確禁止繞道達成相同預覽。應保留本機 HTML、完成靜態檢查，回報尚未完成畫面／互動驗證，不將產生成功說成瀏覽器驗證成功。

## EC2 公網 IP 與 Cloudflare DNS

- 2026-10-26 使用者回報重啟 EC2 後 IP 改變，需反覆修改 Cloudflare；尚未核對實際操作是 Reboot 或 Stop/Start。AWS 的 Reboot 保留公網 IPv4，未綁 Elastic IP 的 Stop/Start 通常會重新分配公網 IPv4。可配置同 Region 的 Elastic IP 並關聯實例，再將 Cloudflare A 記錄改到該固定 IP；尚未執行。
- 綁定 Elastic IP 會取代原本自動分配的公網 IPv4，SSH／WinSCP 使用的 IP 亦須更新。網站仍使用原網域時，APP_PUBLIC_URL、APP_FRONTEND_URL 與 Google 回呼 URI 不需隨公網 IP 更動；單純重啟 spring@web-server 不會重啟 EC2 或改變 IP。

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

- PowerShell 呼叫 `mvnw.cmd` 時，未加引號的 `-Dspotless.skip=true`、`-Dmdep.outputFile=...` 曾被拆成不同參數，出現 `Unknown lifecycle phase ".skip=true"`。完整 `-D...` 參數加上引號，例如 `'-Dspotless.skip=true'`。
- Spotless 的 `spotlessFiles` 使用逗號分隔的 regex，不能使用 `**/` glob；Windows `mvnw.cmd` 也會再次解析 `|`。縮小排版範圍時用 `'-DspotlessFiles=.*RoleSetup.*\.java,.*ManualQaPermissionTest\.java'` 這種不含管線的形式。
- 以 JDK main 執行 Spring Boot 整合檢查時，DevTools 的 restart 執行緒可能讓失敗仍回 exit code 0。須在 `SpringApplication.run` 前用 `System.setProperty("spring.devtools.restart.enabled", "false")` 停用；只傳 Spring 啟動參數太晚。

- 依據：`backend/pom.xml` 在 `process-sources` 綁定 `spotless:apply`。
- 處理：只檢查排版時使用 `./mvnw spotless:check`；執行建置後檢查差異，避免夾帶無關修改。
- Windows 本機服務執行中的 JAR 會鎖住 `target`，造成 Maven clean 失敗。正式產物可在獨立目錄複製後端來源後建置，複製時排除 `target` 與 `application-local.*`，不用中止使用者正在執行的服務。
- 本機預設 Java 目前為 25，部署建置需在該次命令設定 `JAVA_HOME=C:/Users/Wei/.jdks/ms-21.0.12.1` 使用 JDK 21。Corepack 未在 PATH 時可依前端 README 使用 `npx --yes --package=corepack -- corepack yarn ...`，仍遵循 package.json 的 Yarn 版本。

## 正式啟動的 SMTP 與目錄限制

- 2026-10-26 正式寄信出現 `Domain contains illegal character`，使用者提供的 SMTP_FROM 是 `no-reply@` 再接一個完整 Gmail 信箱，形成兩個 @。寄件者必須使用 SMTP 允許的合法信箱；no-reply 不能當前綴加到完整地址。EmailDispatcher 先 setFrom 再 setTo，資料列的 recipient 正常也可能在寄件地址解析時失敗。修改外部設定後須重啟 notification-service；預設每分鐘重試、最多 3 次，已達上限不會因重啟自動重設次數。
- 郵件顯示名稱為 No Reply 不代表寄件地址已切換。此次曾收到 `No Reply <原 Gmail 地址>`，使用者確認 SMTP 仍是 Gmail；切換 SES 時須一起修改 SMTP_HOST、SMTP_USERNAME、SMTP_PASSWORD 與 SMTP_FROM，再重啟 notification-service，並查看新郵件的完整寄件地址。設定範例見 `deploy/ses-mail.md`。
- SES 網域驗證、SMTP endpoint／憑證及 sandbox 狀態須核對同一區域。sandbox 寄信成功只證明已驗證收件者可收信，不能據此判定已獲 production access；程式的 SENT 也不代表後續沒有退信或申訴。
- 2026-10-26 部署盤點確認：`DeployConfigurationValidator` 在 `notification-service` 無條件要求 SMTP 欄位非空，即使 `APP_MAIL_ENABLED=false` 也一樣。關閉寄信不能省略 SMTP 設定；健康檢查亦已停用 mail，`UP` 不代表寄信成功。
- 同一驗證器要求上傳目錄已存在、可讀寫且位於程式工作目錄之外；systemd 須明確設定 WorkingDirectory，附件使用獨立資料目錄，不能隨版本目錄一起切換。

## 本機結構匯出與正式 schema 映射

- 2026-10-26 實際本機設定指向 `spring_core_lab_verify_basic_data`、`spring_core_lab_verify_project`、`spring_core_lab_verify_notification`、`spring_core_lab_verify_session`，不是 README 的預設庫名。匯出前須以實際 datasource URL 為準；YAML 也可能採 `spring.datasource.url` 這種扁平 key。
- 正式沿用 `basic_data`、`project`、`notification`，獨立 Session 庫的兩張表合併至 `basic_data`。Windows 匯出時須將 Session 表與外鍵引用轉成 `SPRING_SESSION`／`SPRING_SESSION_ATTRIBUTES`，不能只改 schema 名稱。
- mysqldump 匯入後，SHOW CREATE TABLE 可能多出明示的 CHARACTER SET，而 COLLATE 未變；比對時應辨識等價語法。用不同連線匯入後驗證新建表，驗證連線使用 autocommit，避免舊交易快照造成 MySQL 1412。


## 多 OIDC 的帳號與登出

- `sub` 只在單一 issuer 下唯一，Google 也未提供 `preferred_username`。現在用已驗證 ID token 的 `issuer + NUL + sub` 做 SHA-256，加 `oidc:` 前綴，沿用既有 255 字元 subject 欄位；登入同步與超級管理員檢查必須使用同一算法。`APP_SUPER_ADMIN_SUBJECTS` 填 `/api/auth/me` 回傳值，不能再填提供者原始 sub。
- Google Discovery 沒有 `end_session_endpoint`。Spring 的 `setPostLogoutRedirectUri` 只用於提供者登出；還須設定 `setDefaultTargetUrl(frontendUrl)`，Google 或匿名登出才會回前端而非後端 `/`。不要用撤銷授權 API 代替登出。
- 使用 properties 註冊 OIDC 時，Spring 會合併 base、local 與外部 registration map；僅修改公開設定不會移除 local 的 authentik。目前已改成 MySQL 專用 repository bean，不再從這些 properties 註冊；舊設定應移除以免誤用。
- OIDC 回呼已改為 `/auth/callback/{registrationId}`；正式 Google URI 為 `https://project-manage.jianwii.dev/backend/auth/callback/google`。本機改用明確 `app.public-url`，預設 3000，不再隨登入入口 Host 變動。
- authentik 的 Cloudflare 診斷段落是切換前的歷史經驗；直接 Google OIDC 不再經過該服務。

## MySQL OIDC 設定

- `oidc_client_registration` 與 Spring Session 使用 web-server 同一個 datasource；目前本機是 `spring_core_lab_verify_session`，正式是 `basic_data`。DDL 在 `backend/web-server/src/main/resources/db/oidc-client-registration.sql`，只需執行此增量腳本，不要重匯全部 schema。
- 設定於啟動時載入 InMemoryClientRegistrationRepository，新增、修改、停用均須重啟；不會即時重讀 SQL。停用提供者不等於撤銷既有 Session。沒有啟用的設定或 discovery 不通會使啟動失敗，不會退回舊 properties。

## 本機 basic-data 資料庫不存在時

- 2026-10-26 本機 `basic-data` 的 application-local.yml 實際指向 `spring_core_lab_verify_basic_data`，覆寫了公開設定中含 `createDatabaseIfNotExist=true` 的 URL。即使 local 的 Hibernate 設為 `ddl-auto=update`，也必須先有資料庫才能建表；不能只看公開 application.yml 就認為會自動建庫。
- 此次補建上述空資料庫後，以 local profile 啟動，Hibernate 建立 7 張資料表。這是新建結構，不是恢復舊資料。JAR 不含 application-local.yml，以 JAR 啟動時須保留 `--spring.config.additional-location=file:./src/main/resources/application-local.yml`，並以各模組目錄為工作目錄。

## Hibernate @SoftDelete 軟刪除

- JDK main 整合檢查若直接用 `Set.of(...)` 填入更新請求的 JPA 集合，Hibernate merge 清集合時會拋 `UnsupportedOperationException`。測試請求應用 `new HashSet<>(...)`，與實際 JSON 反序列化得到的可變集合一致；權限期望值仍可用 `Set.of(...)`。

- 2026-10-27 起 `BaseEntity` 以 `@SoftDelete(columnName = "db_mark", converter = NumericBooleanConverter.class)` 軟刪除，`db_mark` 不再是 Entity 欄位。JPQL、Criteria、衍生查詢、`findById` 與 entity join 都會自動加 `db_mark=0`；原生 SQL 不會，須自行加條件。
- 無法針對單一 JPQL 關閉過濾。需要包含已刪除資料時（角色代碼、編號規則的唯一檢查，以及判斷帳號已停用）改用原生 SQL 的 count，不要改回衍生查詢，否則會撞上唯一約束或重建帳號。
- 軟刪除 UPDATE 只改 `db_mark`，不經 JPA Auditing。`BaseService.delete` 先 touch＋saveAndFlush 記錄刪除者，再 `dao.delete`；自行寫刪除時須照做。
- 已用 SQL 日誌確認：軟刪除實體時，Hibernate 會**實刪**其 `@ElementCollection` 中間表的列（例如 task_label、task_predecessor、task_watcher、sys_role_permission）。HQL `delete` 也會先清中間表。使用者已接受此行為（2026-10-27），手動把 db_mark 改回 0 不會救回這些關聯；若要保留，只能讓集合也使用 `@SoftDelete`，但中間表須新增欄位。
- 新建表時，Hibernate 產生 `db_mark integer not null` 與 check (0,1)；既有 `int NOT NULL` 欄位可通過 `ddl-auto=validate`（已用 deploy schema 的 basic_data 驗證）。

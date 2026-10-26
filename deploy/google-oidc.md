# Google Auth Platform 配置

應用已改成直接連 Google OIDC。前端從 `/api/auth/providers` 取得登入方式；各 OIDC 設定存 MySQL，啟動時載入；SQL 修改後重啟生效。
本機已寫入 Google 設定並啟動服務；正式環境仍須配置並驗收登入。

## 1. Branding 與 Audience

開啟 [Google Auth Platform](https://console.cloud.google.com/auth/overview)，選取要使用的 Google Cloud 專案。未初始化時按 **Get started**。

| 欄位 | 填寫內容 |
|---|---|
| App name | `Spring Core Lab`，或你要顯示的產品名稱 |
| User support email | 你可接收使用者問題的信箱 |
| Audience / User type | `External`；僅限自己 Workspace 組織且專案符合條件時才選 `Internal` |
| Developer contact email | 你可收信的管理信箱 |
| Application home page | `https://project-manage.jianwii.dev` |
| Authorized domains | `jianwii.dev`；填網域，不含協定與路徑 |
| Privacy policy / Terms of service | 填實際已發布的頁面網址；目前專案未提供這兩頁，不要填不存在的路徑 |

先保留 **Testing**，在 **Audience → Test users → Add users** 加入自己的 Google 信箱。準備正式開放時再切換為 **In production**；依 Console 顯示完成網域與品牌驗證。
Testing 與 Test users 是 Google 授權平台設定，不能取代本站的角色權限管理。

依據：[同意畫面設定](https://developers.google.com/workspace/guides/configure-oauth-consent)、[品牌驗證](https://developers.google.com/identity/protocols/oauth2/production-readiness/brand-verification)。

## 2. Data Access

在 **Data Access → Add or remove scopes** 選取並儲存：

```text
openid
https://www.googleapis.com/auth/userinfo.email
https://www.googleapis.com/auth/userinfo.profile
```

後端使用等價的 `openid,profile,email`。不需加入 Drive、Gmail 或 Calendar 權限，也不需要 Service Account。
依據：[Google OpenID Connect](https://developers.google.com/identity/openid-connect/openid-connect)。

## 3. Clients → Create client

| 欄位 | 填寫內容 |
|---|---|
| Application type | `Web application` |
| Name | `spring-core-lab-web` |
| Authorized JavaScript origins | 留空；本專案由 Spring 後端進行 Authorization Code Flow，沒有使用前端 Google SDK |
| Authorized redirect URIs | 逐筆加入下方本機與正式回呼網址 |

本機使用前端 Vite 代理：

```text
http://localhost:3000/auth/callback/google
```

正式環境：

```text
https://project-manage.jianwii.dev/backend/auth/callback/google
```

只有將本機 `app.public-url` 改成 `http://localhost:8080` 時，才另加：

```text
http://localhost:8080/auth/callback/google
```

網址須完全一致，結尾不加 `/`，不能用萬用字元。不要填 `/auth/login/google`，它是開始登入的入口。
建立後將 Client ID 與 Client Secret 寫入下方 MySQL 資料表。
依據：[Web Server OAuth 流程](https://developers.google.com/identity/protocols/oauth2/web-server)。

## 4. MySQL 與網站網址

OIDC 使用 `web-server` 的同一個 datasource。依實際 `WEB_SESSION_DB_URL` 選取資料庫；目前正式部署為 `basic_data`，本機為獨立 Session 測試庫。

先執行 [建表 SQL](../backend/web-server/src/main/resources/db/oidc-client-registration.sql)。本機設定指向的獨立測試庫已建表並填入 Google 設定，不需重複建表；EC2 正式資料庫尚未確認。已有正式資料庫只執行此增量腳本，不要重匯整份 `mysql-schema.sql`。程式不會自動建立此表。

接著複製 [Google 資料範本](oidc-clients.sql.example)，填入真實憑證後在選取的資料庫執行：

```sql
INSERT INTO oidc_client_registration
    (registration_id, client_name, issuer_uri, client_id, client_secret, enabled)
VALUES
    ('google', 'Google', 'https://accounts.google.com',
     '你的 Google Client ID', '你的 Google Client Secret', TRUE);
```

| 欄位 | 用途／預設 |
|---|---|
| registration_id | 登入入口識別，例如 `google`，限小寫英數與連字號 |
| client_name | 登入按鈕名稱，例如 `Google` |
| issuer_uri | OIDC issuer，用來取得授權、Token、JWKS 與登出端點 |
| client_id / client_secret | 提供者核發的應用憑證 |
| scopes | 預設 `openid,profile,email`，必須含 `openid` |
| client_authentication_method | 預設 `client_secret_basic`；也支援 `client_secret_post` |
| enabled | 預設 FALSE；只有 TRUE 的設定會載入 |

Client Secret 以原值存入資料表，後端換取 Token 時需要使用。資料表與資料庫備份須限制存取；程式資料庫帳號只需要此表的 SELECT 權限。匿名 API 僅回傳 ID 與名稱。

本機 `application-local.yml` 保留資料庫帳密與網站網址：

```yaml
app.public-url: http://localhost:3000
app.frontend-url: http://localhost:3000/
```

正式外部 properties 保留：

```properties
APP_PUBLIC_URL=https://project-manage.jianwii.dev/backend
APP_FRONTEND_URL=https://project-manage.jianwii.dev
APP_SUPER_ADMIN_SUBJECTS=
logging.level.org.springframework.security.oauth2.client.web.OAuth2LoginAuthenticationFilter=INFO
```

資料庫連線設定 `WEB_SESSION_DB_URL`、`WEB_SESSION_DB_USERNAME`、`WEB_SESSION_DB_PASSWORD` 保持原值。
移除舊 `GOOGLE_CLIENT_*`、`AUTHENTIK_*` 與 `spring.security.oauth2.client.*` 設定；現在只有 MySQL 是 OIDC 設定來源。

重啟 `web-server` 後生效。若表不存在、沒有啟用的提供者、設定無效或 discovery 無法取得，啟動會失敗，須修正 SQL／連線後再啟動。
`APP_PUBLIC_URL` 已含 `/backend`，Nginx 繼續移除前綴後轉送，後端不加 context-path。

## 5. 登入、管理員與舊帳號

登入後，在同一瀏覽器開啟：

```text
https://project-manage.jianwii.dev/backend/api/auth/me
```

回傳 200 與使用者資料才算成功。複製其中 `subject`（`oidc:` 開頭）填入 `APP_SUPER_ADMIN_SUBJECTS`，重啟 `web-server`，該帳號即有全部權限，可建立角色並指派其他帳號。
多位管理員用逗號分隔；不要填 email 或 Google 原始 `sub`。

`subject` 為 `oidc:` 加上 `SHA-256(issuer + NUL + sub)` 的小寫十六進位字串。使用 ID token 內已驗證的 issuer 與 sub；同一 issuer 的同一帳號不受 registration 名稱影響，不同 issuer 不會共用身分。

既有 authentik 使用者、角色與業務資料仍保留。Google 首次登入會建立新使用者，取得既有預設角色；不會自動繼承舊帳號的角色、專案成員或任務指派，也不以相同 email 自動合併。
若須承接舊帳號，先核對舊 user ID 與 Google 身分，再規劃保留舊 ID 的資料遷移；目前沒有執行帳號合併。
切換正式版本時，清除該應用專用的舊 Spring Session，讓既有登入失效；此步驟須配合正式部署執行。

按登出會清除本站 Session。Google 沒有標準 `end_session_endpoint`，所以會回本站首頁，Google／Gmail 的登入狀態仍在；再次按登入可能直接通過。沒有以撤銷 OAuth 授權冒充登出。
其他 OIDC 如提供標準登出端點，會導到該提供者登出，並要求回到 `APP_FRONTEND_URL`。
依據：[Google Discovery](https://accounts.google.com/.well-known/openid-configuration)、[Spring OIDC Logout](https://docs.spring.io/spring-security/reference/servlet/oauth2/login/logout.html)。

## 6. 新增、修改與停用 OIDC

使用 SQL 維護。以下範例的 `company` 是新提供者的 ID：

```sql
INSERT INTO oidc_client_registration
    (registration_id, client_name, issuer_uri, client_id, client_secret, enabled)
VALUES
    ('company', 'Company SSO', 'https://你的提供者/issuer',
     '你的 Client ID', '你的 Client Secret', TRUE);

UPDATE oidc_client_registration
SET client_secret = '新的 Client Secret'
WHERE registration_id = 'company';

UPDATE oidc_client_registration
SET enabled = FALSE
WHERE registration_id = 'company';
```

新增時在提供者登記 `https://project-manage.jianwii.dev/backend/auth/callback/company`。
如支援 OIDC 登出，也登記允許的登出返回網址 `https://project-manage.jianwii.dev`。
新增、修改或停用之後都須重啟 `web-server`。新增並啟用後，首頁自動增加登入按鈕。
停用只影響重啟後的提供者設定，不會自動撤銷既有 Session；舊登入依 Session 到期或清除而失效。

## 驗收

- 匿名讀取 `/backend/api/auth/providers` 只顯示提供者 ID 與名稱；`/backend/api/auth/me` 回 401。
- Google 登入成功後 `/backend/api/auth/me` 回 200，確認角色與業務操作。
- 取消授權或回呼失敗會回首頁，能重新登入。
- 登出後重新讀取 `/backend/api/auth/me` 回 401；缺少 CSRF 的 POST 登出回 403。
- 本次使用獨立 MySQL 測試庫啟動 Spring Boot，確認從資料表載入 Google、取得 discovery、列出提供者並產生正式 `/backend/auth/callback/google` 轉址。此檢查使用測試 Client 值，未交換真實 Token；結束後已清除測試表。
- 前端開發與正式模式的 Google／第二提供者登入網址已實際載入模組核對。畫面檢查受瀏覽器工具限制，未完成。
- 完整 Google 登入驗收、正式部署與舊帳號遷移尚待完成；尚未更動遠端服務。

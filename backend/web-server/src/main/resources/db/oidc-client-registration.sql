-- 在 web-server 的 datasource 資料庫執行一次；正式環境不會自動建表。
-- 不含憑證與預設資料。Client Secret 只能由受信任的後端／資料庫管理者讀取。
CREATE TABLE oidc_client_registration (
    registration_id VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    client_name VARCHAR(128) NOT NULL,
    issuer_uri VARCHAR(1024) NOT NULL,
    client_id VARCHAR(512) NOT NULL,
    client_secret VARCHAR(2048) NOT NULL,
    scopes VARCHAR(1024) NOT NULL DEFAULT 'openid,profile,email',
    client_authentication_method VARCHAR(32) NOT NULL DEFAULT 'client_secret_basic',
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (registration_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

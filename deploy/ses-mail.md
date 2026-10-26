# SES 郵件部署

2026-10-26 使用者確認：修正 Gmail SMTP 殘留設定後，已完成 SES／`No Reply <no-reply@jianwii.dev>` 寄信切換。本紀錄依使用者回報，未由代理登入正式機複驗；SES production access 與退信／申訴處理狀態仍待確認，追蹤於 [TODO](../TODO.md)。

## 正式設定

正式服務透過 `spring.config.import` 讀取 `/etc/spring/application.properties`。以下以東京區 `ap-northeast-1` 為例；實際 endpoint 應使用 SES Console 所選區域的值。

```properties
SMTP_HOST=email-smtp.ap-northeast-1.amazonaws.com
SMTP_PORT=587
SMTP_USERNAME=填入_SES_SMTP_username
SMTP_PASSWORD=填入_SES_SMTP_password
SMTP_FROM=No Reply <no-reply@jianwii.dev>
APP_MAIL_ENABLED=true
spring.mail.properties.mail.smtp.starttls.required=true
```

- SMTP username／password 使用 SES → SMTP settings → Create SMTP credentials 產生的值，不是 Gmail 帳密、IAM 使用者顯示名稱或 AWS Console 密碼。
- `SMTP_FROM` 保留尖括號，整個值不加外層引號。No Reply 是顯示名稱；`no-reply@jianwii.dev` 才是實際寄件地址。
- `notification-service` 已啟用 SMTP authentication 與 STARTTLS，使用 TCP 587 連出 SES；不需要開放 EC2 的 SMTP 入站連接埠。
- 網站網址、MySQL 與 OIDC 設定不因切換 SMTP 而變更。正式憑證只保存在受保護的外部設定，不提交 Git。
- [設定範本](application.properties.example) 為新部署保留 `APP_MAIL_ENABLED=false`；要實際寄信必須設為 `true`。部署模式即使停用寄信，仍要求 SMTP 必填欄位有值。

修改後重啟通知服務，不必重新打包 JAR 或重啟 EC2：

```bash
sudo nano /etc/spring/application.properties
sudo systemctl restart spring@notification-service
sudo systemctl status spring@notification-service --no-pager
sudo journalctl -u spring@notification-service -n 80 --no-pager
```

依據：[SES SMTP 憑證](https://docs.aws.amazon.com/ses/latest/dg/smtp-credentials.html)、[TLS 與連接埠](https://docs.aws.amazon.com/ses/latest/dg/smtp-connect.html)、[區域 endpoint](https://docs.aws.amazon.com/general/latest/gr/ses.html)。

## 新環境設定順序

1. 在選定區域的 SES 建立 Domain identity：`jianwii.dev`，使用 Easy DKIM。
2. 將 SES 產生的 DKIM CNAME 加入 Cloudflare 的 `jianwii.dev` DNS，設定為 DNS only，等待身分 Verified、DKIM Successful。不要修改網站 A 記錄；此寄信流程也不需要把根網域 MX 指向 EC2。
3. 若仍在 sandbox，另建立測試收件者的 Email address identity，由收件者點擊 AWS 驗證信中的連結。寄件網域與測試收件者是兩種不同用途的驗證。
4. 在相同區域建立 SES SMTP 憑證，填入上方正式設定並重啟通知服務。
5. 對一般使用者寄信前，確認收件者同意接收、建立退信／申訴處理流程，再申請該區域的 production access；不要把測試寄送成功視為已通過申請。

驗證網域後可從該網域的地址寄信，不必先購買 `no-reply` 收件信箱。這不會建立收件功能，也不會阻止收件者按回覆。

依據：[SES 身分驗證](https://docs.aws.amazon.com/ses/latest/dg/creating-identities.html)、[Cloudflare CNAME 驗證](https://developers.cloudflare.com/dns/manage-dns-records/troubleshooting/cname-domain-verification/)、[SES sandbox 與正式權限](https://docs.aws.amazon.com/ses/latest/dg/request-production-access.html)。

## 驗收與常見問題

在網站建立一封寄給允許收件者的新通知，確認實際收到郵件，並展開寄件者查看完整地址為 `no-reply@jianwii.dev`。在郵件原始標頭檢查 DKIM；有配置 DMARC 時也核對結果。

資料庫 `email_message.status=SENT` 表示 SMTP 呼叫成功返回，不保證送達收件匣，也不涵蓋後續退信。服務停用了 mail 健康檢查，Actuator `UP` 不能替代實際寄信驗收。

| 現象 | 處理方向 |
|---|---|
| `Domain contains illegal character` | 檢查 `SMTP_FROM` 與 recipient；`no-reply@` 加完整 Gmail 地址會形成兩個 `@`。程式先解析寄件者，所以收件者正確仍可能報錯。 |
| 顯示 No Reply，但地址仍是 Gmail | 檢查是否只改顯示名稱，或 `SMTP_HOST`／帳密仍使用 Gmail。切換整組 SES 設定並重啟，再檢查新信。 |
| `Email address is not verified` | 核對所選區域的寄件身分；sandbox 還要驗證收件者。 |
| SMTP 驗證失敗 | 核對 SES SMTP username／password 與 endpoint 區域；不能沿用 Gmail 應用程式密碼。 |
| 連線逾時 | 核對 SMTP_HOST、TCP 587 出站與路由。 |
| 修正後舊信仍未重寄 | 預設每分鐘排程、最多嘗試 3 次；已達上限不會因重啟重設，驗收可建立新通知。 |

寄信實作位於 [EmailDispatcher](../backend/notification-service/src/main/java/dev/jianwii/notification/service/EmailDispatcher.java)，重試條件位於 [EmailMessageService](../backend/notification-service/src/main/java/dev/jianwii/notification/service/EmailMessageService.java)。

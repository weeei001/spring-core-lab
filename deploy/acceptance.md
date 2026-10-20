# 驗收紀錄

日期：2026-10-20。依本次調整，不提供 SQL 匯入、migration framework 或部署 README；資料庫由人工維護。本次未修改 Entity schema。

| 項目 | 結果 |
| --- | --- |
| 後端 `mvnw.cmd -DskipTests clean package`，調整打包排除規則後再次 `package` | 通過，全部模組 |
| 前端 `yarn build`，包含 TypeScript 型別檢查 | 通過 |
| 前端 `yarn lint` | 通過 |
| `git diff --check` | 通過 |
| 五個服務 JAR 內容 | 都有 deploy 設定，沒有 `application-local.yml/.yaml/.properties` |
| 五個服務缺少部署必要設定 | 實際啟動確認拒絕，錯誤列出缺少的變數 |
| root、自動建庫、`ddl-auto:update`、相對上傳路徑 | basic-data 啟動實測全部拒絕，未連線 DB |
| Eureka 健康檢查 | 獨立本機實例回 UP；管理 port 綁 127.0.0.1，業務 port 的 `/actuator/health` 回 404；驗證後已停止 |
| 功能權限與資料範圍 | 已核對程式：單筆／批次查詢、更新、成員關係、擁有者及通用檔案限制；實際帳號操作尚未驗收 |

前端第一次建置缺少 Rolldown 的 Windows 原生套件，執行 `yarn install --immutable` 後通過，未修改鎖定版本。仍有既有套件 peer dependency 與大型 bundle 提醒，本次未擴大修改。

下列項目沒有可用的隔離資料庫、驗收帳號或檔案備份，均為**未驗證**，不可視為通過：

- 管理員、專案經理、一般成員、非成員的實際操作及直接 API 越權驗收。
- 移除角色、收回權限、退出專案與停用帳號後，既有登入的下一次請求；超級管理員設定變更。
- basic-data 中斷時回 503、恢復後重新授權；未沿用舊權限。
- 建專案、成員、任務、留言、工時、附件、完成任務、站內通知的完整流程。驗收須使用 `APP_MAIL_ENABLED=false`，未寄送真實郵件。
- 三庫實際 `ddl-auto: validate`、資料庫 health、新舊附件讀取與重啟保留。
- 停止所有業務寫入後，備份 `basic_data`、`project`、`notification`、新上傳及所有舊附件目錄；在隔離位置還原並核對紀錄、檔案與權限後再切換。更新失敗須依資料相容性回復程式或整組備份，不刪表回滾。

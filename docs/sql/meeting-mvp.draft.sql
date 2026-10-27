-- 會議 MVP 增量 DDL 草稿，2026-10-27；R2／R6 設計已核准，仍為未執行草稿。
-- 對照 docs/meeting-mvp.md。作為後續實作基準，本次不得執行。
-- 取得 SQL 執行授權後，先在全新本機獨立測試庫驗證，禁止連既有業務資料庫。
-- 前提：同一庫內已有 project(id)、task(id)，皆為 InnoDB、signed bigint PK。
-- 執行前另外讀回 DATABASE()／VERSION()／SHOW CREATE TABLE 核對目標與結構。
-- 不指定 USE，不重建／修改既有表，不停用 FK，不寫角色權限或業務資料。
-- 刻意不用 IF NOT EXISTS：同名表須停下比對，不能略過結構不符。
-- CREATE TABLE 可能隱含 commit；兩句不是可用 ROLLBACK 撤銷的單一交易。
-- 回退舊程式時保留這兩張表及全部資料；沒有清除資料的 down script。

CREATE TABLE `meeting` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `create_id` varchar(100) NOT NULL,
  `create_time` datetime(6) NOT NULL,
  `db_mark` int NOT NULL,
  `trans_id` varchar(100) NOT NULL,
  `trans_time` datetime(6) NOT NULL,
  `project_id` bigint NOT NULL,
  `title` varchar(200) NOT NULL,
  `held_at` datetime(6) NOT NULL,
  `participants` varchar(2000) NOT NULL,
  `minutes` text NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_meeting_project_held_id` (`project_id`, `held_at`, `id`),
  CONSTRAINT `fk_meeting_project` FOREIGN KEY (`project_id`)
    REFERENCES `project` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 同專案、有效性與每會議至多 100 個關聯由 service 驗證；FK 不處理軟刪除。
-- Task 負責人、期限、狀態與標題均不複製到此表。
CREATE TABLE `meeting_task` (
  `meeting_id` bigint NOT NULL,
  `task_id` bigint NOT NULL,
  PRIMARY KEY (`meeting_id`, `task_id`),
  KEY `idx_meeting_task_task_meeting` (`task_id`, `meeting_id`),
  CONSTRAINT `fk_meeting_task_meeting` FOREIGN KEY (`meeting_id`)
    REFERENCES `meeting` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_meeting_task_task` FOREIGN KEY (`task_id`)
    REFERENCES `task` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

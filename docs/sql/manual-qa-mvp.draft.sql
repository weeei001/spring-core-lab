-- 手動驗收 MVP 增量草稿，執行待核准；不得執行。設計與限制見 docs/manual-qa-mvp.md。
-- 不指定 USE、不使用 IF NOT EXISTS、不改既有資料表或資料、不停用外鍵。
-- 執行須另行核准；先在全新本機獨立測試庫核對 DATABASE()、VERSION()、
-- SHOW CREATE TABLE project／task，確認 InnoDB 與 signed bigint 主鍵。
-- CREATE TABLE 隱含 commit，不能用交易 ROLLBACK 撤銷整份草稿。
-- 回退舊版程式時保留本次新表及資料；刪表須另案核准。

CREATE TABLE `manual_qa_batch` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `create_id` varchar(100) NOT NULL,
  `create_time` datetime(6) NOT NULL,
  `db_mark` int NOT NULL,
  `trans_id` varchar(100) NOT NULL,
  `trans_time` datetime(6) NOT NULL,
  `project_id` bigint NOT NULL,
  `version_label` varchar(200) NOT NULL,
  `environment` varchar(1000) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_qa_batch_project_id` (`project_id`, `id`),
  CONSTRAINT `fk_qa_batch_project` FOREIGN KEY (`project_id`)
    REFERENCES `project` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `manual_qa_item` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `create_id` varchar(100) NOT NULL,
  `create_time` datetime(6) NOT NULL,
  `db_mark` int NOT NULL,
  `trans_id` varchar(100) NOT NULL,
  `trans_time` datetime(6) NOT NULL,
  `batch_id` bigint NOT NULL,
  `title` varchar(200) NOT NULL,
  `steps` text NOT NULL,
  `expected_result` text NOT NULL,
  `task_id` bigint DEFAULT NULL,
  `assignee_id` varchar(100) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_qa_item_batch_id` (`batch_id`, `id`),
  KEY `idx_qa_item_task` (`task_id`),
  CONSTRAINT `fk_qa_item_batch` FOREIGN KEY (`batch_id`)
    REFERENCES `manual_qa_batch` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_qa_item_task` FOREIGN KEY (`task_id`)
    REFERENCES `task` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 未測不寫入；結果、必要說明、BUG 類型與同專案由 service 驗證。
-- 指派人須為有效成員；執行者由入口驗權，admin 可豁免成員檢查。
CREATE TABLE `manual_qa_execution` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `create_id` varchar(100) NOT NULL,
  `create_time` datetime(6) NOT NULL,
  `db_mark` int NOT NULL,
  `trans_id` varchar(100) NOT NULL,
  `trans_time` datetime(6) NOT NULL,
  `item_id` bigint NOT NULL,
  `tester_id` varchar(100) NOT NULL,
  `executed_at` datetime(6) NOT NULL,
  `tested_version` varchar(200) NOT NULL,
  `result` varchar(16) NOT NULL,
  `actual` text NOT NULL,
  `bug_task_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_qa_execution_item_time_id` (`item_id`, `executed_at`, `id`),
  KEY `idx_qa_execution_bug` (`bug_task_id`),
  CONSTRAINT `fk_qa_execution_item` FOREIGN KEY (`item_id`)
    REFERENCES `manual_qa_item` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_qa_execution_bug` FOREIGN KEY (`bug_task_id`)
    REFERENCES `task` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

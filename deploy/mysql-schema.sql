-- spring-core-lab: MySQL schema-only export, 2026-10-26
-- Source server: MySQL 8.0.46 (Windows). Target: MySQL 8.0+ (Ubuntu).
-- No rows, users, grants, DROP TABLE, or DROP DATABASE statements.
-- Import into empty target schemas; existing tables cause an error.
-- Session tables are in basic_data and use uppercase names for Linux.
-- Local AUTO_INCREMENT counters are omitted.
-- spring_core_lab_verify_basic_data -> basic_data
CREATE DATABASE IF NOT EXISTS `basic_data` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
-- spring_core_lab_verify_project -> project
CREATE DATABASE IF NOT EXISTS `project` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
-- spring_core_lab_verify_notification -> notification
CREATE DATABASE IF NOT EXISTS `notification` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
-- spring_core_lab_verify_session -> basic_data


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

USE `basic_data`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `leave_request` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `create_id` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `create_time` datetime(6) NOT NULL,
  `db_mark` int NOT NULL,
  `trans_id` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `trans_time` datetime(6) NOT NULL,
  `applicant_id` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `approver_id` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `cancel_reason` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `cancelled_at` datetime(6) DEFAULT NULL,
  `end_date` date NOT NULL,
  `reason` varchar(1000) COLLATE utf8mb4_unicode_ci NOT NULL,
  `review_comment` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `reviewed_at` datetime(6) DEFAULT NULL,
  `start_date` date NOT NULL,
  `status` enum('APPROVED','CANCELLED','PENDING','REJECTED','WITHDRAWN') COLLATE utf8mb4_unicode_ci NOT NULL,
  `type` enum('ANNUAL','OTHER','PERSONAL','SICK') COLLATE utf8mb4_unicode_ci NOT NULL,
  `version` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `IDXj5w12l89g3n00p6qe6k1gkaan` (`applicant_id`,`status`),
  KEY `IDXp9xy8ugmkqsu4iycw9kcpvpcc` (`approver_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `serial_number` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `create_id` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `create_time` datetime(6) NOT NULL,
  `db_mark` int NOT NULL,
  `trans_id` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `trans_time` datetime(6) NOT NULL,
  `last_index` bigint NOT NULL,
  `name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `reset_cycle` enum('DAY','MONTH','NONE','YEAR') COLLATE utf8mb4_unicode_ci NOT NULL,
  `rule` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `serial_key` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK9vwgwk5qicbdxe8feg3deikxr` (`serial_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_role` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `create_id` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `create_time` datetime(6) NOT NULL,
  `db_mark` int NOT NULL,
  `trans_id` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `trans_time` datetime(6) NOT NULL,
  `code` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKplpigyqwsqfn7mn66npgf9ftp` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_role_permission` (
  `role_id` bigint NOT NULL,
  `permission_code` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`role_id`,`permission_code`),
  CONSTRAINT `FK9q28ewrhntqeipl1t04kh1be7` FOREIGN KEY (`role_id`) REFERENCES `sys_role` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_user` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `create_id` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `create_time` datetime(6) NOT NULL,
  `db_mark` int NOT NULL,
  `trans_id` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `trans_time` datetime(6) NOT NULL,
  `email` varchar(254) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `subject` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `username` varchar(150) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK2aed90yxpxhp8fkhvh8h25w20` (`subject`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_user_role` (
  `user_id` bigint NOT NULL,
  `role_id` bigint NOT NULL,
  PRIMARY KEY (`user_id`,`role_id`),
  CONSTRAINT `FKb40xxfch70f5qnyfw8yme1n1s` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `upload_file` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `create_id` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `create_time` datetime(6) NOT NULL,
  `db_mark` int NOT NULL,
  `trans_id` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `trans_time` datetime(6) NOT NULL,
  `business_managed` bit(1) NOT NULL,
  `content_type` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `file_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `size` bigint NOT NULL,
  `storage_path` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

USE `project`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `project` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `create_id` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `create_time` datetime(6) NOT NULL,
  `db_mark` int NOT NULL,
  `trans_id` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `trans_time` datetime(6) NOT NULL,
  `code` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `color` varchar(7) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `description` varchar(2000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `end_date` date DEFAULT NULL,
  `name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `start_date` date DEFAULT NULL,
  `status` enum('ACTIVE','CANCELLED','DONE','ON_HOLD','PLANNING') COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKeh3nusutt0qy84a4yr9pfxkyg` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `project_label` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `create_id` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `create_time` datetime(6) NOT NULL,
  `db_mark` int NOT NULL,
  `trans_id` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `trans_time` datetime(6) NOT NULL,
  `color` varchar(7) COLLATE utf8mb4_unicode_ci NOT NULL,
  `name` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `project_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `IDX5xowf0v9scxmqwigu2fjctveb` (`project_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `project_member` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `create_id` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `create_time` datetime(6) NOT NULL,
  `db_mark` int NOT NULL,
  `trans_id` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `trans_time` datetime(6) NOT NULL,
  `project_id` bigint NOT NULL,
  `role` enum('MANAGER','MEMBER') COLLATE utf8mb4_unicode_ci NOT NULL,
  `user_id` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  KEY `IDXc8fotdlbae78y2l7v7wq94jmx` (`project_id`),
  KEY `IDXfm5ny600uh9eo5pgorwk6paah` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `project_wip_limit` (
  `project_id` bigint NOT NULL,
  `max_tasks` int NOT NULL,
  `status` enum('CANCELLED','DONE','IN_PROGRESS','REVIEW','TODO') COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`project_id`,`status`),
  CONSTRAINT `FKqll0mpsigh84y0d4dgghnh46h` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `task` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `create_id` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `create_time` datetime(6) NOT NULL,
  `db_mark` int NOT NULL,
  `trans_id` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `trans_time` datetime(6) NOT NULL,
  `assignee_id` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `code` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` varchar(5000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `due_date` date DEFAULT NULL,
  `estimate_hours` decimal(6,1) DEFAULT NULL,
  `parent_id` bigint DEFAULT NULL,
  `priority` enum('HIGH','LOW','MEDIUM','URGENT') COLLATE utf8mb4_unicode_ci NOT NULL,
  `project_id` bigint NOT NULL,
  `sort_order` bigint NOT NULL,
  `start_date` date DEFAULT NULL,
  `status` enum('CANCELLED','DONE','IN_PROGRESS','REVIEW','TODO') COLLATE utf8mb4_unicode_ci NOT NULL,
  `title` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
  `type` enum('BUG','CHORE','FEATURE','SPIKE') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'FEATURE',
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKtkeh0tonhc8t13k0928epnjp2` (`code`),
  KEY `IDXfeas56rqeuwqir4hgpy50wxw` (`project_id`,`status`),
  KEY `IDXrb8jsuwoimkias723h7dtdaa3` (`assignee_id`),
  KEY `IDXnool83f5wg75sepiw5o598w8o` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `task_activity` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `create_id` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `create_time` datetime(6) NOT NULL,
  `db_mark` int NOT NULL,
  `trans_id` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `trans_time` datetime(6) NOT NULL,
  `field` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `new_value` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `old_value` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `task_id` bigint NOT NULL,
  `type` enum('CHANGED','COMMENTED','CREATED') COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  KEY `IDXffu09ibl9ljdyxfsggej084vg` (`task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `task_attachment` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `create_id` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `create_time` datetime(6) NOT NULL,
  `db_mark` int NOT NULL,
  `trans_id` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `trans_time` datetime(6) NOT NULL,
  `content_type` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `file_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `size` bigint NOT NULL,
  `storage_path` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `task_id` bigint NOT NULL,
  `upload_file_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `IDXns6mjujxi4b19r1xsnisxk3bl` (`task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `task_comment` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `create_id` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `create_time` datetime(6) NOT NULL,
  `db_mark` int NOT NULL,
  `trans_id` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `trans_time` datetime(6) NOT NULL,
  `content` varchar(2000) COLLATE utf8mb4_unicode_ci NOT NULL,
  `task_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `IDXlgsyut495rq17lsry0ljdxb2t` (`task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `task_comment_mention` (
  `comment_id` bigint NOT NULL,
  `user_id` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`comment_id`,`user_id`),
  CONSTRAINT `FKp910cbdasr2i38kr9d0pi5x49` FOREIGN KEY (`comment_id`) REFERENCES `task_comment` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `task_label` (
  `task_id` bigint NOT NULL,
  `label_id` bigint NOT NULL,
  PRIMARY KEY (`task_id`,`label_id`),
  KEY `IDX7grj9buxw5ec8ioixaxocm0cb` (`label_id`),
  CONSTRAINT `FKbp361h72empe2dgbbv3fc6n4n` FOREIGN KEY (`task_id`) REFERENCES `task` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `task_predecessor` (
  `task_id` bigint NOT NULL,
  `predecessor_id` bigint NOT NULL,
  PRIMARY KEY (`task_id`,`predecessor_id`),
  CONSTRAINT `FK7lq1va0rd3p33j1au2s0uhsi` FOREIGN KEY (`task_id`) REFERENCES `task` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `task_watcher` (
  `task_id` bigint NOT NULL,
  `user_id` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`task_id`,`user_id`),
  KEY `IDXoafmculdolrh6j859k4j812yd` (`user_id`),
  CONSTRAINT `FKrj71e06exsed4cks6gedhcx6x` FOREIGN KEY (`task_id`) REFERENCES `task` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `work_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `create_id` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `create_time` datetime(6) NOT NULL,
  `db_mark` int NOT NULL,
  `trans_id` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `trans_time` datetime(6) NOT NULL,
  `hours` decimal(3,1) NOT NULL,
  `note` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `project_id` bigint NOT NULL,
  `task_id` bigint NOT NULL,
  `user_id` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `work_date` date NOT NULL,
  PRIMARY KEY (`id`),
  KEY `IDX19j5otyqbwwnogr5m3eynfftd` (`project_id`,`work_date`),
  KEY `IDX926r3ov9qoqouyg2l25qpxklo` (`user_id`,`work_date`),
  KEY `IDX7g3vgcwbyy945vd7x7j561rnq` (`task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

USE `notification`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `email_message` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `create_id` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `create_time` datetime(6) NOT NULL,
  `db_mark` int NOT NULL,
  `trans_id` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `trans_time` datetime(6) NOT NULL,
  `attempts` int NOT NULL,
  `body` varchar(5000) COLLATE utf8mb4_unicode_ci NOT NULL,
  `last_error` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `notification_id` bigint DEFAULT NULL,
  `recipient` varchar(254) COLLATE utf8mb4_unicode_ci NOT NULL,
  `sent_time` datetime(6) DEFAULT NULL,
  `status` enum('FAILED','PENDING','SENDING','SENT') COLLATE utf8mb4_unicode_ci NOT NULL,
  `subject` varchar(300) COLLATE utf8mb4_unicode_ci NOT NULL,
  `version` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `IDX2095ibc7tvj9ggnugfs3u359i` (`status`,`trans_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `notification` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `create_id` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `create_time` datetime(6) NOT NULL,
  `db_mark` int NOT NULL,
  `trans_id` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `trans_time` datetime(6) NOT NULL,
  `content` varchar(2000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `dedup_key` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `link` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `read_time` datetime(6) DEFAULT NULL,
  `recipient_id` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `title` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
  `type` enum('ANNOUNCEMENT','LEAVE_CANCELLED','LEAVE_REVIEWED','LEAVE_SUBMITTED','TASK_ASSIGNED','TASK_COMMENTED','TASK_DAILY_DIGEST','TASK_MENTIONED','TASK_STATUS_CHANGED') COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_notification_recipient_dedup` (`recipient_id`,`dedup_key`),
  KEY `IDXaksfrkfo5mtkf62vrs58maq8d` (`recipient_id`,`read_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

USE `basic_data`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `SPRING_SESSION` (
  `PRIMARY_ID` char(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `SESSION_ID` char(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `CREATION_TIME` bigint NOT NULL,
  `LAST_ACCESS_TIME` bigint NOT NULL,
  `MAX_INACTIVE_INTERVAL` int NOT NULL,
  `EXPIRY_TIME` bigint NOT NULL,
  `PRINCIPAL_NAME` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`PRIMARY_ID`),
  UNIQUE KEY `SPRING_SESSION_IX1` (`SESSION_ID`),
  KEY `SPRING_SESSION_IX2` (`EXPIRY_TIME`),
  KEY `SPRING_SESSION_IX3` (`PRINCIPAL_NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `SPRING_SESSION_ATTRIBUTES` (
  `SESSION_PRIMARY_ID` char(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `ATTRIBUTE_NAME` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
  `ATTRIBUTE_BYTES` blob NOT NULL,
  PRIMARY KEY (`SESSION_PRIMARY_ID`,`ATTRIBUTE_NAME`),
  CONSTRAINT `SPRING_SESSION_ATTRIBUTES_FK` FOREIGN KEY (`SESSION_PRIMARY_ID`) REFERENCES `SPRING_SESSION` (`PRIMARY_ID`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;


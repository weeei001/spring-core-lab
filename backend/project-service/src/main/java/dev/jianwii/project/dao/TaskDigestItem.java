package dev.jianwii.project.dao;

import java.time.LocalDate;

/** 排程只讀需要的欄位，避免載入任務關聯及持有跨服務交易。 */
public record TaskDigestItem(Long id, String code, String title, String assigneeId, LocalDate dueDate) {}

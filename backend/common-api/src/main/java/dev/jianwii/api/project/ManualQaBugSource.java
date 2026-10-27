package dev.jianwii.api.project;

import java.time.LocalDateTime;

/** BUG 的驗收來源：失敗紀錄加上所屬項目與批次，任務詳情用來連回驗收頁。 */
public record ManualQaBugSource(
        Long executionId,
        Long itemId,
        String itemTitle,
        Long batchId,
        String versionLabel,
        String environment,
        String testedVersion,
        String actual,
        LocalDateTime executedAt) {}

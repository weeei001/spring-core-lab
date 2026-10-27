package dev.jianwii.api.basicdata.leave;

import java.time.LocalDate;

/** 團隊只看休假人員及日期，不回傳假別、原因、審核意見或申請單號。 */
public record LeaveAbsence(String applicantId, LocalDate startDate, LocalDate endDate) {}

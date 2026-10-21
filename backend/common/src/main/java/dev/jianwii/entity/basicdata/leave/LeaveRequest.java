package dev.jianwii.entity.basicdata.leave;

import dev.jianwii.common.entity.BaseEntity;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/** 原始申請與審核資訊保留；撤回或銷假另記原因與時間。 */
@Entity
@Table(
        name = "leave_request",
        indexes = {@Index(columnList = "applicant_id, status"), @Index(columnList = "approver_id, status")})
@Getter
@Setter
public class LeaveRequest extends BaseEntity {
    @Column(name = "applicant_id", nullable = false, updatable = false, length = 100)
    private String applicantId;

    @Column(name = "approver_id", nullable = false, updatable = false, length = 100)
    private String approverId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, updatable = false, length = 20)
    private LeaveType type;

    /** 整天請假，含起訖日。 */
    @Column(name = "start_date", nullable = false, updatable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false, updatable = false)
    private LocalDate endDate;

    @Column(name = "reason", nullable = false, updatable = false, length = 1000)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private LeaveStatus status;

    @Column(name = "review_comment", length = 1000)
    private String reviewComment;

    @Column(name = "reviewed_at", columnDefinition = "datetime(6)")
    private LocalDateTime reviewedAt;

    @Column(name = "cancel_reason", length = 1000)
    private String cancelReason;

    @Column(name = "cancelled_at", columnDefinition = "datetime(6)")
    private LocalDateTime cancelledAt;

    /** 同時核准／退回時只允許一筆成功，衝突由共用例外處理回 409。 */
    @Version
    @Column(name = "version", nullable = false)
    private Long version;
}

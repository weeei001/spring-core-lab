package dev.jianwii.entity.basicdata.leave;

import dev.jianwii.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 送出後不修改內容，只審核一次；建立與審核資訊即為完整操作紀錄。 */
@Entity
@Table(name = "leave_request",
        indexes = {@Index(columnList = "applicant_id, status"),
                @Index(columnList = "approver_id, status")})
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

    /** 同時核准／退回時只允許一筆成功，衝突由共用例外處理回 409。 */
    @Version
    @Column(name = "version", nullable = false)
    private Long version;
}

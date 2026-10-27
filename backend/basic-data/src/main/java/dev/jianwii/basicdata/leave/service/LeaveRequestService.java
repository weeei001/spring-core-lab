package dev.jianwii.basicdata.leave.service;

import dev.jianwii.api.basicdata.UserAuthorization;
import dev.jianwii.api.basicdata.leave.LeaveAbsence;
import dev.jianwii.api.basicdata.leave.LeaveCalendarQuery;
import dev.jianwii.api.basicdata.leave.LeaveCancellation;
import dev.jianwii.api.basicdata.leave.LeaveQueryParameter;
import dev.jianwii.api.basicdata.leave.LeaveReview;
import dev.jianwii.api.basicdata.leave.LeaveSubmission;
import dev.jianwii.basicdata.leave.dao.LeaveRequestDAO;
import dev.jianwii.basicdata.service.UserService;
import dev.jianwii.common.dao.QuerySpecifications;
import dev.jianwii.common.dto.PageResponse;
import dev.jianwii.common.exception.BusinessException;
import dev.jianwii.common.exception.ConflictException;
import dev.jianwii.common.exception.ForbiddenException;
import dev.jianwii.common.exception.NotFoundException;
import dev.jianwii.common.web.Operator;
import dev.jianwii.entity.basicdata.Permission;
import dev.jianwii.entity.basicdata.leave.LeaveRequest;
import dev.jianwii.entity.basicdata.leave.LeaveStatus;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 功能權限由 web-server 檢查；資料範圍與狀態轉換在服務內強制執行。 */
@Service
@Transactional
@AllArgsConstructor
public class LeaveRequestService {
    private final LeaveRequestDAO leaveRequestDAO;
    // ponytail: 同一服務直接呼叫 UserService，沒有必要繞 HTTP 查自己的帳號資料。
    private final UserService userService;
    private final LeaveNotifier leaveNotifier;

    public LeaveRequest submit(LeaveSubmission submission) {
        String applicant = currentUser();
        if (submission.startDate().isAfter(submission.endDate())) {
            throw new BusinessException("開始日期不可晚於結束日期");
        }
        if (applicant.equals(submission.approverId())) {
            throw new BusinessException("不可指定自己審核請假");
        }
        UserAuthorization approver = userService.authorization(Long.valueOf(submission.approverId()));
        if (!approver.active() || !approver.permissions().contains(Permission.LEAVE_APPROVE.getCode())) {
            throw new BusinessException("審核人不存在、已停用或沒有請假審核權限");
        }
        LeaveRequest request = new LeaveRequest();
        request.setApplicantId(applicant);
        request.setApproverId(submission.approverId());
        request.setType(submission.type());
        request.setStartDate(submission.startDate());
        request.setEndDate(submission.endDate());
        request.setReason(submission.reason().strip());
        request.setStatus(LeaveStatus.PENDING);
        LeaveRequest saved = leaveRequestDAO.saveAndFlush(request);
        leaveNotifier.submitted(saved);
        return saved;
    }

    @Transactional(readOnly = true)
    public PageResponse<LeaveRequest> mine(LeaveQueryParameter parameter) {
        return page(parameter, "applicantId");
    }

    @Transactional(readOnly = true)
    public PageResponse<LeaveRequest> reviews(LeaveQueryParameter parameter) {
        return page(parameter, "approverId");
    }

    @Transactional(readOnly = true)
    public LeaveRequest mineDetail(Long id) {
        LeaveRequest request = find(id);
        requireUser(request.getApplicantId());
        return request;
    }

    @Transactional(readOnly = true)
    public LeaveRequest reviewDetail(Long id) {
        LeaveRequest request = find(id);
        requireUser(request.getApproverId());
        return request;
    }

    public LeaveRequest review(Long id, LeaveReview review) {
        LeaveRequest request = reviewDetail(id);
        if (request.getApplicantId().equals(currentUser())) {
            throw new ForbiddenException("不可審核自己的請假");
        }
        if (request.getStatus() != LeaveStatus.PENDING) {
            throw new ConflictException("這筆申請已處理，請重新讀取");
        }
        if (review.decision() != LeaveStatus.APPROVED && review.decision() != LeaveStatus.REJECTED) {
            throw new BusinessException("只能核准或退回申請");
        }
        String comment = review.comment() == null ? "" : review.comment().strip();
        if (review.decision() == LeaveStatus.REJECTED && comment.isEmpty()) {
            throw new BusinessException("退回時請填寫原因");
        }
        request.setStatus(review.decision());
        request.setReviewComment(comment.isEmpty() ? null : comment);
        request.setReviewedAt(LocalDateTime.now());
        LeaveRequest saved = leaveRequestDAO.saveAndFlush(request);
        leaveNotifier.reviewed(saved);
        return saved;
    }

    public LeaveRequest withdraw(Long id, LeaveCancellation cancellation) {
        return cancel(id, cancellation, LeaveStatus.PENDING, LeaveStatus.WITHDRAWN);
    }

    public LeaveRequest cancel(Long id, LeaveCancellation cancellation) {
        return cancel(id, cancellation, LeaveStatus.APPROVED, LeaveStatus.CANCELLED);
    }

    private LeaveRequest cancel(Long id, LeaveCancellation cancellation, LeaveStatus expected, LeaveStatus next) {
        LeaveRequest request = mineDetail(id);
        if (request.getStatus() != expected) {
            throw new ConflictException("申請狀態已變更，請重新讀取後操作");
        }
        String reason =
                cancellation.reason() == null ? "" : cancellation.reason().strip();
        if (reason.isEmpty() || reason.length() > 1000) {
            throw new BusinessException("請填寫 1 至 1000 字的撤回或銷假原因");
        }
        request.setStatus(next);
        request.setCancelReason(reason);
        request.setCancelledAt(LocalDateTime.now());
        LeaveRequest saved = leaveRequestDAO.saveAndFlush(request);
        leaveNotifier.cancelled(saved);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<LeaveAbsence> calendar(LeaveCalendarQuery query) {
        if (query.fromDate().isAfter(query.toDate())
                || ChronoUnit.DAYS.between(query.fromDate(), query.toDate()) > 61) {
            throw new BusinessException("行事曆日期區間需為 1 至 62 天");
        }
        return leaveRequestDAO.calendar(query.userIds(), query.fromDate(), query.toDate(), LeaveStatus.APPROVED);
    }

    private PageResponse<LeaveRequest> page(LeaveQueryParameter parameter, String ownerField) {
        String userId = currentUser();
        Specification<LeaveRequest> scope = (root, query, builder) -> builder.equal(root.get(ownerField), userId);
        return new PageResponse<>(leaveRequestDAO.findAll(
                scope.and(QuerySpecifications.of(parameter.toQueryConditions())),
                PageResponse.pageable(parameter, Set.of("id", "createTime", "startDate", "endDate"))));
    }

    private LeaveRequest find(Long id) {
        return leaveRequestDAO.findById(id).orElseThrow(() -> new NotFoundException("查無請假申請，id：" + id));
    }

    private static void requireUser(String userId) {
        if (!currentUser().equals(userId)) {
            throw new ForbiddenException("只能存取自己的申請或指定給自己的審核單");
        }
    }

    private static String currentUser() {
        String userId = Operator.current();
        if (Operator.SYSTEM.equals(userId)) {
            throw new ForbiddenException("請假操作需要登入者");
        }
        return userId;
    }
}

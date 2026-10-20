package dev.jianwii.web.leave;

import dev.jianwii.api.basicdata.leave.*;
import dev.jianwii.common.dto.PageResponse;
import dev.jianwii.common.web.Result;
import dev.jianwii.entity.basicdata.leave.LeaveRequest;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** 功能權限與服務內的申請人／審核人範圍都必須通過。 */
@RestController
@AllArgsConstructor
@RequestMapping("/api/leave-request")
public class WebLeaveRequestController {
    private final LeaveApi leaveApi;

    @PreAuthorize("hasAuthority('leave:apply')")
    @PostMapping
    public Result<LeaveRequest> submit(@Valid @RequestBody LeaveSubmission submission) {
        return Result.success(leaveApi.submit(submission));
    }

    @PreAuthorize("hasAuthority('leave:apply')")
    @PostMapping("/mine/queryPage")
    public Result<PageResponse<LeaveRequest>> mine(
            @Valid @RequestBody LeaveQueryParameter parameter) {
        return Result.success(leaveApi.mine(parameter));
    }

    @PreAuthorize("hasAuthority('leave:approve')")
    @PostMapping("/review/queryPage")
    public Result<PageResponse<LeaveRequest>> reviews(
            @Valid @RequestBody LeaveQueryParameter parameter) {
        return Result.success(leaveApi.reviews(parameter));
    }

    @PreAuthorize("hasAuthority('leave:apply')")
    @GetMapping("/mine/{id}")
    public Result<LeaveRequest> mineDetail(@PathVariable Long id) {
        return Result.success(leaveApi.mineDetail(id));
    }

    @PreAuthorize("hasAuthority('leave:approve')")
    @GetMapping("/review/{id}")
    public Result<LeaveRequest> reviewDetail(@PathVariable Long id) {
        return Result.success(leaveApi.reviewDetail(id));
    }

    @PreAuthorize("hasAuthority('leave:approve')")
    @PostMapping("/{id}/review")
    public Result<LeaveRequest> review(@PathVariable Long id,
            @Valid @RequestBody LeaveReview review) {
        return Result.success(leaveApi.review(id, review));
    }
}

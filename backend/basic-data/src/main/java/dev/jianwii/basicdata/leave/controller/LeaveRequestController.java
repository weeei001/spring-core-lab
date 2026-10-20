package dev.jianwii.basicdata.leave.controller;

import dev.jianwii.api.basicdata.leave.*;
import dev.jianwii.common.dto.PageResponse;
import dev.jianwii.entity.basicdata.leave.LeaveRequest;
import dev.jianwii.basicdata.leave.service.LeaveRequestService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class LeaveRequestController implements LeaveApi {
    private final LeaveRequestService leaveRequestService;

    @Override
    public LeaveRequest submit(LeaveSubmission submission) {
        return leaveRequestService.submit(submission);
    }

    @Override
    public PageResponse<LeaveRequest> mine(LeaveQueryParameter parameter) {
        return leaveRequestService.mine(parameter);
    }

    @Override
    public PageResponse<LeaveRequest> reviews(LeaveQueryParameter parameter) {
        return leaveRequestService.reviews(parameter);
    }

    @Override
    public LeaveRequest mineDetail(Long id) {
        return leaveRequestService.mineDetail(id);
    }

    @Override
    public LeaveRequest reviewDetail(Long id) {
        return leaveRequestService.reviewDetail(id);
    }

    @Override
    public LeaveRequest review(Long id, LeaveReview review) {
        return leaveRequestService.review(id, review);
    }
}

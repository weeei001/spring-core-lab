package dev.jianwii.api.basicdata.leave;

import dev.jianwii.common.dto.PageResponse;
import dev.jianwii.entity.basicdata.leave.LeaveRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

@HttpExchange("/api/basic-data/leave-request")
public interface LeaveApi {
    @PostExchange
    LeaveRequest submit(@Valid @RequestBody LeaveSubmission submission);

    @PostExchange("/mine/queryPage")
    PageResponse<LeaveRequest> mine(@Valid @RequestBody LeaveQueryParameter parameter);

    @PostExchange("/review/queryPage")
    PageResponse<LeaveRequest> reviews(@Valid @RequestBody LeaveQueryParameter parameter);

    @GetExchange("/mine/{id}")
    LeaveRequest mineDetail(@PathVariable("id") Long id);

    @GetExchange("/review/{id}")
    LeaveRequest reviewDetail(@PathVariable("id") Long id);

    @PostExchange("/{id}/review")
    LeaveRequest review(@PathVariable("id") Long id, @Valid @RequestBody LeaveReview review);

    @PostExchange("/{id}/withdraw")
    LeaveRequest withdraw(@PathVariable("id") Long id, @Valid @RequestBody LeaveCancellation cancellation);

    @PostExchange("/{id}/cancel")
    LeaveRequest cancel(@PathVariable("id") Long id, @Valid @RequestBody LeaveCancellation cancellation);

    @PostExchange("/calendar")
    List<LeaveAbsence> calendar(@Valid @RequestBody LeaveCalendarQuery query);
}

package dev.jianwii.api.basicdata.dto;

import dev.jianwii.common.query.QueryParameter;
import dev.jianwii.entity.basicdata.leave.LeaveRequest;
import dev.jianwii.entity.basicdata.leave.LeaveStatus;
import lombok.Getter;
import lombok.Setter;

/** 申請人與審核人範圍由服務依操作者強制設定，不接受外部指定。 */
@Getter
@Setter
public class LeaveQueryParameter extends QueryParameter<LeaveRequest> {
    private LeaveStatus status;
}

package dev.jianwii.api.project;

import dev.jianwii.common.query.QueryParameter;
import dev.jianwii.entity.project.TaskActivity;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/** 任務活動紀錄查詢條件；一定要指定任務，web-server 依任務所屬專案檢查成員資格。 */
@Getter
@Setter
public class TaskActivityQueryParameter extends QueryParameter<TaskActivity> {

    @NotNull
    private Long taskId;
}

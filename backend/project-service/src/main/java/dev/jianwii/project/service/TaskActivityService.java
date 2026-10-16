package dev.jianwii.project.service;

import dev.jianwii.api.project.TaskActivityQueryParameter;
import dev.jianwii.common.service.BaseService;
import dev.jianwii.entity.project.TaskActivity;
import dev.jianwii.project.dao.TaskActivityDAO;
import org.springframework.stereotype.Service;

/** 活動紀錄只開放查詢；寫入見 {@link TaskActivityRecorder}。 */
@Service
public class TaskActivityService extends BaseService<TaskActivity, TaskActivityQueryParameter> {

    public TaskActivityService(TaskActivityDAO taskActivityDAO) {
        super(taskActivityDAO);
    }
}

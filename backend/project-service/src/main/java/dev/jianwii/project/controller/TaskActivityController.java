package dev.jianwii.project.controller;

import dev.jianwii.api.project.TaskActivityApi;
import dev.jianwii.api.project.TaskActivityQueryParameter;
import dev.jianwii.common.dto.PageResponse;
import dev.jianwii.entity.project.TaskActivity;
import dev.jianwii.project.service.TaskActivityService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 路徑、參數與驗證註解都寫在 {@link TaskActivityApi} 介面上，Spring MVC 會沿用，這裡只要轉呼叫 Service。
 */
@RestController
@AllArgsConstructor
public class TaskActivityController implements TaskActivityApi {
    private final TaskActivityService taskActivityService;

    @Override
    public List<TaskActivity> query(TaskActivityQueryParameter parameter) {
        return taskActivityService.query(parameter);
    }

    @Override
    public PageResponse<TaskActivity> queryPage(TaskActivityQueryParameter parameter) {
        return taskActivityService.queryPage(parameter);
    }
}

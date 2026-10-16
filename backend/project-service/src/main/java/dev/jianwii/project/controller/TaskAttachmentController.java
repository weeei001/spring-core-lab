package dev.jianwii.project.controller;

import dev.jianwii.api.project.TaskAttachmentApi;
import dev.jianwii.api.project.TaskAttachmentQueryParameter;
import dev.jianwii.entity.project.TaskAttachment;
import dev.jianwii.project.service.TaskAttachmentService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 路徑、參數與驗證註解都寫在 {@link TaskAttachmentApi} 介面上，Spring MVC 會沿用，這裡只要轉呼叫 Service。
 */
@RestController
@AllArgsConstructor
public class TaskAttachmentController implements TaskAttachmentApi {
    private final TaskAttachmentService taskAttachmentService;

    @Override
    public List<TaskAttachment> query(TaskAttachmentQueryParameter parameter) {
        return taskAttachmentService.query(parameter);
    }

    @Override
    public TaskAttachment upload(Long taskId, MultipartFile file) {
        return taskAttachmentService.upload(taskId, file);
    }

    @Override
    public ResponseEntity<byte[]> content(Long id) {
        return taskAttachmentService.content(id);
    }

    @Override
    public void delete(Long id) {
        taskAttachmentService.delete(id);
    }
}

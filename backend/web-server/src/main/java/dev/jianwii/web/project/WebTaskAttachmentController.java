package dev.jianwii.web.project;

import dev.jianwii.api.project.TaskAttachmentApi;
import dev.jianwii.api.project.TaskAttachmentQueryParameter;
import dev.jianwii.common.web.Result;
import dev.jianwii.entity.project.TaskAttachment;
import jakarta.validation.Valid;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 前端用的任務附件 API，寫法同 {@link WebProjectController}；只能刪自己上傳的附件由 project-service 檢查。 檔案內容不包
 * Result，直接回傳檔案，給 img src、下載連結使用。
 */
@RestController
@AllArgsConstructor
@RequestMapping(path = "/api/task-attachment")
public class WebTaskAttachmentController {
    private final TaskAttachmentApi taskAttachmentApi;
    private final ProjectAccessGuard projectAccessGuard;

    @PreAuthorize("hasAuthority('project:read')")
    @PostMapping("/query")
    public Result<List<TaskAttachment>> query(@Valid @RequestBody TaskAttachmentQueryParameter parameter) {
        projectAccessGuard.scopeAttachments(parameter);
        return Result.success(taskAttachmentApi.query(parameter));
    }

    @PreAuthorize("hasAuthority('task:write')")
    @PostMapping(path = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<TaskAttachment> upload(@RequestParam Long taskId, @RequestPart("file") MultipartFile file) {
        projectAccessGuard.requireMember(projectAccessGuard.projectOfTask(taskId));
        return Result.success(taskAttachmentApi.upload(taskId, file));
    }

    /** 只轉 Content-Type 與 Content-Disposition，並禁止瀏覽器猜測類型。 */
    @PreAuthorize("hasAuthority('project:read')")
    @GetMapping("/{id}/content")
    public ResponseEntity<byte[]> content(@PathVariable Long id) {
        projectAccessGuard.requireMember(projectAccessGuard.projectOfAttachment(id));
        ResponseEntity<byte[]> remote = taskAttachmentApi.content(id);
        HttpHeaders headers = remote.getHeaders();
        return ResponseEntity.ok()
                .contentType(
                        headers.getContentType() == null
                                ? MediaType.APPLICATION_OCTET_STREAM
                                : headers.getContentType())
                .header(HttpHeaders.CONTENT_DISPOSITION, headers.getFirst(HttpHeaders.CONTENT_DISPOSITION))
                .header("X-Content-Type-Options", "nosniff")
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .body(remote.getBody());
    }

    @PreAuthorize("hasAuthority('task:write')")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        projectAccessGuard.requireMember(projectAccessGuard.projectOfAttachment(id));
        taskAttachmentApi.delete(id);
        return Result.success();
    }
}

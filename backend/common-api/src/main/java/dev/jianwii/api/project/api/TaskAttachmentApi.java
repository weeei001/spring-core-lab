package dev.jianwii.api.project.api;

import dev.jianwii.api.project.dto.TaskAttachmentQueryParameter;
import dev.jianwii.entity.project.TaskAttachment;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.service.annotation.DeleteExchange;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

/**
 * project-service 的任務附件 API，用法同 {@link ProjectApi}。 上傳用 multipart，單檔上限
 * 10MB（spring.servlet.multipart.max-file-size）。
 */
@HttpExchange("/api/task-attachment")
public interface TaskAttachmentApi {

    /** 不分頁查詢：page 必須為 1，最多回傳 size 筆。 */
    @PostExchange("/query")
    List<TaskAttachment> query(@Valid @RequestBody TaskAttachmentQueryParameter parameter);

    /** 上傳附件到任務；檔案由 basic-data 管理，專案必須未結案。 */
    @PostExchange(value = "/upload", contentType = MediaType.MULTIPART_FORM_DATA_VALUE)
    TaskAttachment upload(@RequestParam("taskId") Long taskId, @RequestPart("file") MultipartFile file);

    /** 檔案內容；Content-Type、Content-Disposition 已依檔案類型設定好，呼叫端照轉即可。 */
    @GetExchange("/{id}/content")
    ResponseEntity<byte[]> content(@PathVariable("id") Long id);

    /** 軟刪除；只有上傳者可以刪。 */
    @DeleteExchange("/{id}")
    void delete(@PathVariable("id") Long id);
}

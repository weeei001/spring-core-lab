package dev.jianwii.project.service;

import dev.jianwii.api.basicdata.api.UploadFileApi;
import dev.jianwii.api.project.dto.TaskAttachmentQueryParameter;
import dev.jianwii.common.exception.BusinessException;
import dev.jianwii.common.exception.ForbiddenException;
import dev.jianwii.common.exception.NotFoundException;
import dev.jianwii.common.service.BaseService;
import dev.jianwii.common.web.Operator;
import dev.jianwii.entity.basicdata.UploadFile;
import dev.jianwii.entity.project.Task;
import dev.jianwii.entity.project.TaskAttachment;
import dev.jianwii.project.dao.TaskAttachmentDAO;
import dev.jianwii.project.dao.TaskDAO;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

/**
 * 任務附件的新檔案透過 UploadFileApi 交給 basic-data；專案規則與任務關聯留在這裡。 project.attachment.dir
 * 只供舊附件讀取。只有上傳者能刪除附件關聯。
 */
@Service
public class TaskAttachmentService extends BaseService<TaskAttachment, TaskAttachmentQueryParameter> {
    private static final Logger log = LoggerFactory.getLogger(TaskAttachmentService.class);
    /**
     * 可以直接在瀏覽器顯示的類型；其他（含 SVG、HTML）一律下載，並固定為 octet-stream， 避免上傳的檔案在同網域執行 script。
     */
    private static final Set<String> INLINE_TYPES = Set.of("image/png", "image/jpeg", "image/gif", "image/webp");

    private final TaskAttachmentDAO taskAttachmentDAO;
    private final TaskDAO taskDAO;
    private final ProjectRules projectRules;
    private final UploadFileApi uploadFileApi;
    private final Path root;

    public TaskAttachmentService(
            TaskAttachmentDAO taskAttachmentDAO,
            TaskDAO taskDAO,
            ProjectRules projectRules,
            UploadFileApi uploadFileApi,
            @Value("${project.attachment.dir}") String dir) {
        super(taskAttachmentDAO);
        this.taskAttachmentDAO = taskAttachmentDAO;
        this.taskDAO = taskDAO;
        this.projectRules = projectRules;
        this.uploadFileApi = uploadFileApi;
        this.root = Path.of(dir).toAbsolutePath().normalize();
    }

    /** 先檢查業務規則，再呼叫共用上傳，任務只保存檔案 id 與列表顯示資訊。 */
    public TaskAttachment upload(Long taskId, MultipartFile file) {
        Task task = taskDAO.findById(taskId).orElseThrow(() -> new BusinessException("任務不存在：" + taskId));
        projectRules.requireOpen(task.getProjectId());
        UploadFile uploaded = uploadFileApi.uploadManaged(file);
        // basic-data 已獨立提交；本地關聯存檔失敗時，盡可能停用尚未被引用的共用檔案。
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    try {
                        uploadFileApi.delete(uploaded.getId());
                    } catch (RuntimeException e) {
                        log.warn("無法停用未完成的任務上傳，檔案 id：{}", uploaded.getId(), e);
                    }
                }
            }
        });
        TaskAttachment attachment = new TaskAttachment();
        attachment.setTaskId(taskId);
        attachment.setUploadFileId(uploaded.getId());
        attachment.setFileName(uploaded.getFileName());
        attachment.setContentType(uploaded.getContentType());
        attachment.setSize(uploaded.getSize());
        attachment.setStoragePath("");
        return taskAttachmentDAO.saveAndFlush(attachment);
    }

    /** 檔案內容與下載用的 header。 */
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> content(Long id) {
        TaskAttachment attachment = find(id);
        if (attachment.getUploadFileId() != null) {
            return uploadFileApi.content(attachment.getUploadFileId());
        }
        // 相容切換共用 API 之前上傳的附件。
        Path path = root.resolve(attachment.getStoragePath()).normalize();
        if (!path.startsWith(root) || !Files.isRegularFile(path)) {
            throw new NotFoundException("附件檔案不存在：" + attachment.getFileName());
        }
        byte[] bytes;
        try {
            bytes = Files.readAllBytes(path);
        } catch (IOException e) {
            throw new UncheckedIOException("附件讀取失敗：" + path, e);
        }
        boolean inline = INLINE_TYPES.contains(attachment.getContentType());
        ContentDisposition disposition = (inline ? ContentDisposition.inline() : ContentDisposition.attachment())
                .filename(attachment.getFileName(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(
                        inline
                                ? MediaType.parseMediaType(attachment.getContentType())
                                : MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(bytes);
    }

    @Override
    protected void beforeDelete(TaskAttachment entity) {
        if (!entity.getCreateId().equals(Operator.current())) {
            throw new ForbiddenException("只能刪除自己上傳的附件");
        }
        Task task = taskDAO.findById(entity.getTaskId())
                .orElseThrow(() -> new BusinessException("任務不存在：" + entity.getTaskId()));
        projectRules.requireOpen(task.getProjectId());
    }

    @Override
    protected Set<String> sortableFields() {
        return Set.of("id", "createTime");
    }
}

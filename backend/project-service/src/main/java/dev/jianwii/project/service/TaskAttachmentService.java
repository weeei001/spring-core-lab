package dev.jianwii.project.service;

import dev.jianwii.api.project.TaskAttachmentQueryParameter;
import dev.jianwii.common.entity.BaseEntity;
import dev.jianwii.common.exception.BusinessException;
import dev.jianwii.common.exception.ForbiddenException;
import dev.jianwii.common.exception.NotFoundException;
import dev.jianwii.common.service.BaseService;
import dev.jianwii.common.web.Operator;
import dev.jianwii.entity.project.Task;
import dev.jianwii.entity.project.TaskAttachment;
import dev.jianwii.project.dao.TaskAttachmentDAO;
import dev.jianwii.project.dao.TaskDAO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

/**
 * 任務附件：檔案存在 project.attachment.dir（預設工作目錄下的 data/attachments），
 * 依上傳年月分資料夾、以 UUID 命名，不用原始檔名，避免路徑字元與重名問題。只有上傳者能刪除。
 */
@Service
public class TaskAttachmentService extends BaseService<TaskAttachment, TaskAttachmentQueryParameter> {
    /** 單檔上限，與 spring.servlet.multipart.max-file-size 一致。 */
    private static final long MAX_SIZE = 10L * 1024 * 1024;
    /**
     * 可以直接在瀏覽器顯示的類型；其他（含 SVG、HTML）一律下載，並固定為 octet-stream，
     * 避免上傳的檔案在同網域執行 script。
     */
    private static final Set<String> INLINE_TYPES =
            Set.of("image/png", "image/jpeg", "image/gif", "image/webp");

    private final TaskAttachmentDAO taskAttachmentDAO;
    private final TaskDAO taskDAO;
    private final ProjectRules projectRules;
    private final Path root;

    public TaskAttachmentService(TaskAttachmentDAO taskAttachmentDAO, TaskDAO taskDAO,
            ProjectRules projectRules,
            @Value("${project.attachment.dir:data/attachments}") String dir) {
        super(taskAttachmentDAO);
        this.taskAttachmentDAO = taskAttachmentDAO;
        this.taskDAO = taskDAO;
        this.projectRules = projectRules;
        this.root = Path.of(dir).toAbsolutePath().normalize();
    }

    /** 先寫檔再存資料；存資料失敗時交易 rollback，檔案留在磁碟上但不會被引用。 */
    public TaskAttachment upload(Long taskId, MultipartFile file) {
        Task task = taskDAO.findById(taskId).filter(entity -> entity.getDbMark() == BaseEntity.VALID)
                .orElseThrow(() -> new BusinessException("任務不存在：" + taskId));
        projectRules.requireOpen(task.getProjectId());
        if (file.isEmpty()) {
            throw new BusinessException("檔案是空的");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new BusinessException("檔案不可超過 10MB");
        }
        String fileName = StringUtils.getFilename(StringUtils.cleanPath(
                file.getOriginalFilename() == null ? "" : file.getOriginalFilename()));
        if (fileName == null || fileName.isBlank()) {
            fileName = "attachment";
        }
        LocalDate today = LocalDate.now();
        String relative = today.getYear() + "/" + String.format("%02d", today.getMonthValue())
                + "/" + UUID.randomUUID();
        Path target = root.resolve(relative).normalize();
        try {
            Files.createDirectories(target.getParent());
            file.transferTo(target);
        } catch (IOException e) {
            throw new UncheckedIOException("附件寫入失敗：" + target, e);
        }
        TaskAttachment attachment = new TaskAttachment();
        attachment.setTaskId(taskId);
        attachment.setFileName(fileName.length() > 255 ? fileName.substring(fileName.length() - 255)
                : fileName);
        String contentType = file.getContentType();
        attachment.setContentType(contentType == null || contentType.length() > 100
                ? MediaType.APPLICATION_OCTET_STREAM_VALUE : contentType);
        attachment.setSize(file.getSize());
        attachment.setStoragePath(relative);
        attachment.setDbMark(BaseEntity.VALID);
        return taskAttachmentDAO.saveAndFlush(attachment);
    }

    /** 檔案內容與下載用的 header。 */
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> content(Long id) {
        TaskAttachment attachment = find(id);
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
        ContentDisposition disposition =
                (inline ? ContentDisposition.inline() : ContentDisposition.attachment())
                        .filename(attachment.getFileName(), StandardCharsets.UTF_8).build();
        return ResponseEntity.ok()
                .contentType(inline ? MediaType.parseMediaType(attachment.getContentType())
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

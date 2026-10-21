package dev.jianwii.basicdata.service;

import dev.jianwii.basicdata.dao.UploadFileDAO;
import dev.jianwii.common.entity.BaseEntity;
import dev.jianwii.common.exception.BusinessException;
import dev.jianwii.common.exception.ForbiddenException;
import dev.jianwii.common.exception.NotFoundException;
import dev.jianwii.common.web.Operator;
import dev.jianwii.entity.basicdata.UploadFile;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;
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
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/** 集中處理檔案儲存與 metadata；業務模組不需要自行操作磁碟。 */
@Service
@Transactional
public class UploadFileService {
    private static final Logger log = LoggerFactory.getLogger(UploadFileService.class);
    private static final long MAX_SIZE = 10L * 1024 * 1024;
    private static final Set<String> INLINE_TYPES = Set.of("image/png", "image/jpeg", "image/gif", "image/webp");

    private final UploadFileDAO uploadFileDAO;
    private final Path root;
    private final Path legacyRoot;

    public UploadFileService(
            UploadFileDAO uploadFileDAO,
            @Value("${app.upload.dir}") String dir,
            @Value("${app.upload.legacy-dir:}") String legacyDir) {
        this.uploadFileDAO = uploadFileDAO;
        this.root = Path.of(dir).toAbsolutePath().normalize();
        this.legacyRoot =
                legacyDir.isBlank() ? null : Path.of(legacyDir).toAbsolutePath().normalize();
    }

    public UploadFile upload(MultipartFile file, boolean businessManaged) {
        if (file.isEmpty()) {
            throw new BusinessException("檔案是空的");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new BusinessException("檔案不可超過 10MB");
        }
        String name = StringUtils.getFilename(
                StringUtils.cleanPath(file.getOriginalFilename() == null ? "" : file.getOriginalFilename()));
        if (name == null || name.isBlank()) {
            name = "file";
        }
        // 檔名只用於顯示與下載 header；實際路徑一律由系統產生。
        name = name.replaceAll("[\\p{Cntrl}]", "_");
        LocalDate today = LocalDate.now();
        String relative = today.getYear() + "/" + today.getMonthValue() + "/" + UUID.randomUUID();
        Path target = root.resolve(relative);
        // 包含 flush 後、commit 才失敗的情況，交易未提交就清除本次新檔案。
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    if (status == STATUS_ROLLED_BACK) {
                        removeFailedUpload(target);
                    }
                }
            });
        }
        try {
            Files.createDirectories(target.getParent());
            file.transferTo(target);
            UploadFile uploaded = new UploadFile();
            uploaded.setFileName(name.length() > 255 ? name.substring(name.length() - 255) : name);
            String contentType = file.getContentType();
            uploaded.setContentType(
                    contentType == null || contentType.length() > 100
                            ? MediaType.APPLICATION_OCTET_STREAM_VALUE
                            : contentType);
            uploaded.setSize(file.getSize());
            uploaded.setBusinessManaged(businessManaged);
            uploaded.setStoragePath(relative);
            uploaded.setDbMark(BaseEntity.VALID);
            return uploadFileDAO.saveAndFlush(uploaded);
        } catch (IOException e) {
            removeFailedUpload(target);
            throw new UncheckedIOException("檔案寫入失敗", e);
        } catch (RuntimeException e) {
            removeFailedUpload(target);
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public UploadFile get(Long id) {
        return uploadFileDAO
                .findById(id)
                .filter(file -> Integer.valueOf(BaseEntity.VALID).equals(file.getDbMark()))
                .orElseThrow(() -> new NotFoundException("查無檔案，id：" + id));
    }

    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> content(Long id) {
        UploadFile file = get(id);
        Path path = root.resolve(file.getStoragePath()).normalize();
        if (path.startsWith(root) && !Files.exists(path) && legacyRoot != null) {
            path = legacyRoot.resolve(file.getStoragePath()).normalize();
            if (!path.startsWith(legacyRoot)) {
                throw new NotFoundException("檔案不存在：" + file.getFileName());
            }
        } else if (!path.startsWith(root)) {
            throw new NotFoundException("檔案不存在：" + file.getFileName());
        }
        if (!Files.isRegularFile(path)) {
            throw new NotFoundException("檔案不存在：" + file.getFileName());
        }
        byte[] bytes;
        try {
            bytes = Files.readAllBytes(path);
        } catch (IOException e) {
            throw new UncheckedIOException("檔案讀取失敗", e);
        }
        // HTML、SVG 等可執行內容一律下載，避免同網域執行 script。
        boolean inline = INLINE_TYPES.contains(file.getContentType());
        ContentDisposition disposition = (inline ? ContentDisposition.inline() : ContentDisposition.attachment())
                .filename(file.getFileName(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(
                        inline ? MediaType.parseMediaType(file.getContentType()) : MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .header("X-Content-Type-Options", "nosniff")
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .body(bytes);
    }

    public void delete(Long id) {
        UploadFile file = get(id);
        if (!Operator.current().equals(file.getCreateId())) {
            throw new ForbiddenException("只能刪除自己上傳的檔案");
        }
        file.setDbMark(BaseEntity.INVALID);
        uploadFileDAO.saveAndFlush(file);
    }

    private void removeFailedUpload(Path target) {
        try {
            Files.deleteIfExists(target);
        } catch (IOException e) {
            log.warn("無法清除未完成的上傳檔案：{}", target, e);
        }
    }
}

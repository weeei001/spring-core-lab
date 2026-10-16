package dev.jianwii.entity.project;

import com.fasterxml.jackson.annotation.JsonIgnore;
import dev.jianwii.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * 任務附件（例如 Bug 的截圖）；檔案存在 project-service 的附件目錄，這裡只存中繼資料。
 * 上傳者即 createId，只有上傳者能刪除；刪除是軟刪除，檔案保留在磁碟上。
 */
@Entity
@Table(name = "task_attachment", indexes = @Index(columnList = "task_id"))
@Getter
@Setter
public class TaskAttachment extends BaseEntity {

    @Column(name = "task_id", nullable = false, updatable = false)
    private Long taskId;

    /** 上傳時的檔名，下載時沿用。 */
    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    /** 上傳時瀏覽器給的 MIME type；下載時只有安全的圖片格式會照用，見 AttachmentContent。 */
    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    /** 檔案大小（bytes）。 */
    @Column(name = "size", nullable = false)
    private Long size;

    /** 附件目錄下的相對路徑，不給前端。 */
    @JsonIgnore
    @Column(name = "storage_path", nullable = false, updatable = false, length = 255)
    private String storagePath;
}

package dev.jianwii.entity.project;

import com.fasterxml.jackson.annotation.JsonIgnore;
import dev.jianwii.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * 任務附件（例如 Bug 的截圖）；新檔案由 basic-data 管理，這裡保存關聯與顯示用資訊。 上傳者即
 * createId，只有上傳者能刪除；刪除只停用任務附件關聯，共用檔案紀錄與實體檔案保留。
 */
@Entity
@Table(name = "task_attachment", indexes = @Index(columnList = "task_id"))
@Getter
@Setter
public class TaskAttachment extends BaseEntity {

    @Column(name = "task_id", nullable = false, updatable = false)
    private Long taskId;

    /** basic-data.upload_file 的 id；舊附件為 null，仍從原本的儲存路徑讀取。 */
    @Column(name = "upload_file_id", updatable = false)
    private Long uploadFileId;

    /** 上傳時的檔名，下載時沿用。 */
    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    /** 顯示用的 MIME type；只有安全的圖片格式可在瀏覽器直接顯示。 */
    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    /** 檔案大小（bytes）。 */
    @Column(name = "size", nullable = false)
    private Long size;

    /** 舊附件的相對路徑，不給前端；新附件填空字串以相容既有 NOT NULL 欄位。 */
    @JsonIgnore
    @Column(name = "storage_path", nullable = false, updatable = false, length = 255)
    private String storagePath;
}

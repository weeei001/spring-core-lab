package dev.jianwii.entity.basicdata;

import com.fasterxml.jackson.annotation.JsonIgnore;
import dev.jianwii.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * 共用上傳檔案。只有 basic-data 建表及存檔；其他服務保存檔案 id，透過 UploadFileApi 讀取。 業務關聯與存取權限由使用檔案的模組管理，實體檔案不放進資料庫。
 */
@Entity
@Table(name = "upload_file")
@Getter
@Setter
public class UploadFile extends BaseEntity {

    @Column(name = "file_name", nullable = false, updatable = false, length = 255)
    private String fileName;

    @Column(name = "content_type", nullable = false, updatable = false, length = 100)
    private String contentType;

    /** 檔案大小，單位 bytes。 */
    @Column(name = "size", nullable = false, updatable = false)
    private Long size;

    /** 業務模組管理的檔案，只能經該模組授權後存取，不開放通用前端入口。 */
    @Column(name = "business_managed", nullable = false, updatable = false)
    private boolean businessManaged;

    /** 相對於儲存目錄的路徑，不透過 API 輸出。 */
    @JsonIgnore
    @Column(name = "storage_path", nullable = false, updatable = false, length = 255)
    private String storagePath;
}

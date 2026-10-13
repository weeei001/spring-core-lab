package dev.jianwii.common.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 共用資料庫產生的主鍵，以及 Spring 維護的建立／修改者與時間。
 * 操作者來自 {@link org.springframework.data.domain.AuditorAware}，預設見 CommonAutoConfiguration。
 * Entity 直接當 API 輸入／輸出：前端送來的 id 與審計欄位由 BaseService 忽略（新增時清空 id、更新時不複製），
 * 審計值由 JPA Auditing 覆寫。不設 JSON 唯讀，其他服務收到 Entity 時才拿得到這些值。
 */
@Getter
@Setter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreatedBy
    @Column(name = "create_id", nullable = false, updatable = false, length = 100)
    // 建立者
    private String createId;

    @CreatedDate
    @Column(name = "create_time", nullable = false, updatable = false, columnDefinition = "datetime(6)")
    // 建立時間
    private LocalDateTime createTime;

    @LastModifiedBy
    @Column(name = "trans_id", nullable = false, length = 100)
    // 最後修改者
    private String transId;

    @LastModifiedDate
    @Column(name = "trans_time", nullable = false, columnDefinition = "datetime(6)")
    // 最後修改時間
    private LocalDateTime transTime;
}

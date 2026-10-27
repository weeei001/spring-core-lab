package dev.jianwii.common.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SoftDelete;
import org.hibernate.type.NumericBooleanConverter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * 所有 Entity 的共用欄位：主鍵 id（資料庫自動遞增）、軟刪除欄位 db_mark，以及建立／最後修改的人與時間（審計欄位）。 審計欄位由 JPA Auditing
 * 在存檔時自動填入（看 @CreatedBy 等註解），操作者來源見 CommonAutoConfiguration。 createId、transId 存的是 user 資料表的
 * id，不是名字；要顯示名字由前端以 id 查詢 user 資料表。
 *
 * <p>
 * Entity 直接當 API 的輸入／輸出：前端送來的 id 與審計欄位不會生效（BaseService 新增時清空 id、更新時不複製， 審計值也會被 JPA Auditing
 * 覆寫）。這些欄位不設成 JSON 唯讀，是因為其他服務透過 XxxApi 收到 Entity 時需要這些值。
 */
@Getter
@Setter
@MappedSuperclass
// LAZY 關聯載入的是 Hibernate 產生的代理物件（proxy），多了這兩個內部欄位，轉 JSON 時略過
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@EntityListeners(AuditingEntityListener.class)
// 軟刪除：delete 改成把 db_mark 設為 1（0 為有效），Hibernate 會在 JPQL、Criteria、衍生查詢與 findById 自動排除已刪除資料；
// 原生 SQL 不會過濾，需自行加 db_mark = 0。db_mark 不是 Entity 欄位，JSON 也不會帶出
@SoftDelete(columnName = "db_mark", converter = NumericBooleanConverter.class)
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 建立者的使用者 id；系統操作為 system */
    @CreatedBy
    @Column(name = "create_id", nullable = false, updatable = false, length = 100)
    private String createId;

    /** 建立時間 */
    @CreatedDate
    @Column(name = "create_time", nullable = false, updatable = false, columnDefinition = "datetime(6)")
    private LocalDateTime createTime;

    /** 最後修改者的使用者 id；系統操作為 system */
    @LastModifiedBy
    @Column(name = "trans_id", nullable = false, length = 100)
    private String transId;

    /** 最後修改時間 */
    @LastModifiedDate
    @Column(name = "trans_time", nullable = false, columnDefinition = "datetime(6)")
    private LocalDateTime transTime;
}

package dev.jianwii.entity.basicdata;

import dev.jianwii.common.entity.BaseEntity;
import dev.jianwii.common.validation.ValidGroup;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * 系統編號規則（參考 tvbs TSerialNumberSetting），例如專案編號 PRJ2610001。 Entity 放在 common 讓各服務共用類別，但只有 basic-data
 * 會建這張表（它的 @EntityScan 只掃這個 package）； 其他服務透過 SerialNumberApi 取號。欄位驗證的分組規則見 ValidGroup。
 */
@Entity
@Table(name = "serial_number")
@Getter
@Setter
public class SerialNumber extends BaseEntity {

    /** 規則代碼，取號時指定，例如 PROJECT；建立後不可修改。 */
    @NotBlank(groups = ValidGroup.Create.class)
    @Size(max = 50)
    @Pattern(regexp = "[A-Z0-9_]+")
    @Column(name = "serial_key", nullable = false, unique = true, updatable = false, length = 50)
    private String serialKey;

    /** 規則名稱，例如「專案系統編號」；不可全為空白。 */
    @NotBlank(groups = ValidGroup.Create.class)
    @Size(max = 100)
    @Pattern(regexp = "(?sU).*\\S.*")
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /**
     * 編號格式：{0000} 為補零流水號（0 的個數即位數，必須剛好一個），{yyyyMM} 等為日期（只限 y、M、d）， 其餘照原樣輸出。例：P{yyyyMM}{0000} →
     * P2026100001。
     */
    @NotBlank(groups = ValidGroup.Create.class)
    @Size(max = 50)
    @Column(name = "rule", nullable = false, length = 50)
    private String rule;

    /** 最後使用的流水號，由取號維護；建立時可指定起始值。 */
    @PositiveOrZero
    @Column(name = "last_index", nullable = false)
    private Long lastIndex;

    /** 歸零週期，依最後取號時間（transTime）判斷是否進入新週期。 */
    @NotNull(groups = ValidGroup.Create.class)
    @Enumerated(EnumType.STRING)
    @Column(name = "reset_cycle", nullable = false, length = 10)
    private ResetCycle resetCycle;
}

package dev.jianwii.entity.basicdata;

import dev.jianwii.common.entity.BaseEntity;
import dev.jianwii.common.validation.ValidGroup;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

/**
 * 角色：一組功能權限，由管理員在畫面上組合，例如「專案經理 = 查詢專案 + 建立專案 + 維護任務」。
 * 使用者透過角色取得權限（{@link User#getRoleIds()}），不直接指派權限給個人。
 */
@Entity
@Table(name = "sys_role")
@Getter
@Setter
public class Role extends BaseEntity {

    /** 預設角色的代碼：新使用者第一次登入時自動擁有，啟動時建立；可調整權限，不可刪除。 */
    public static final String DEFAULT_CODE = "USER";

    /** 角色代碼，例如 WAREHOUSE；建立後不可修改。 */
    @NotBlank(groups = ValidGroup.Create.class)
    @Size(max = 50)
    @Pattern(regexp = "[A-Z0-9_]+")
    @Column(name = "code", nullable = false, unique = true, updatable = false, length = 50)
    private String code;

    /** 角色名稱，例如「倉管」；不可全為空白。 */
    @NotBlank(groups = ValidGroup.Create.class)
    @Size(max = 100)
    @Pattern(regexp = "(?sU).*\\S.*")
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /**
     * 角色擁有的功能權限 code（{@link Permission#getCode()}），存在中間表 sys_role_permission。
     * 更新時不給（null）表示不改，給清單則整批取代；不存在的 code 會被擋下。
     */
    @Size(max = 200)
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "sys_role_permission", joinColumns = @JoinColumn(name = "role_id"))
    @Column(name = "permission_code", nullable = false, length = 100)
    @BatchSize(size = 100)
    private Set<@NotBlank String> permissionCodes;
}

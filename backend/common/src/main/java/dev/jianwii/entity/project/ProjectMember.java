package dev.jianwii.entity.project;

import dev.jianwii.common.entity.BaseEntity;
import dev.jianwii.common.validation.ValidGroup;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * 專案成員。一般使用者只看得到自己是成員的專案；每個專案至少要有一位經理。 同一人在同一專案只能有一筆有效資料；不設資料庫唯一約束，被移除（軟刪除）後可以再加入。
 */
@Entity
@Table(name = "project_member",
        indexes = {@Index(columnList = "project_id"), @Index(columnList = "user_id")})
@Getter
@Setter
public class ProjectMember extends BaseEntity {

    /** 所屬專案；建立後不可修改。 */
    @NotNull(groups = ValidGroup.Create.class)
    @Column(name = "project_id", nullable = false, updatable = false)
    private Long projectId;

    /** basic-data 的 user id；建立後不可修改。 */
    @NotBlank(groups = ValidGroup.Create.class)
    @Size(max = 100)
    @Column(name = "user_id", nullable = false, updatable = false, length = 100)
    private String userId;

    @NotNull(groups = ValidGroup.Create.class)
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private ProjectRole role;
}

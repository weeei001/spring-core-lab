package dev.jianwii.entity.project;

import dev.jianwii.common.entity.BaseEntity;
import dev.jianwii.common.validation.ValidGroup;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** 任務留言；作者即 createId，只有作者能修改或刪除。 */
@Entity
@Table(name = "task_comment", indexes = @Index(columnList = "task_id"))
@Getter
@Setter
public class TaskComment extends BaseEntity {

    /** 所屬任務；建立後不可修改。 */
    @NotNull(groups = ValidGroup.Create.class)
    @Column(name = "task_id", nullable = false, updatable = false)
    private Long taskId;

    @NotBlank(groups = ValidGroup.Create.class)
    @Size(max = 2000)
    @Pattern(regexp = "(?sU).*\\S.*")
    @Column(name = "content", nullable = false, length = 2000)
    private String content;
}

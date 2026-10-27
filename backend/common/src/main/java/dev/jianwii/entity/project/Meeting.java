package dev.jianwii.entity.project;

import dev.jianwii.common.entity.BaseEntity;
import dev.jianwii.common.validation.ValidGroup;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "meeting", indexes = @Index(name = "idx_meeting_project_held_id", columnList = "project_id,held_at,id"))
@Getter
@Setter
public class Meeting extends BaseEntity {
    @NotNull(groups = ValidGroup.Create.class)
    @Column(name = "project_id", nullable = false, updatable = false)
    private Long projectId;

    @NotBlank(groups = ValidGroup.Create.class)
    @Size(max = 200)
    @Pattern(regexp = "(?sU).*\\S.*")
    @Column(nullable = false, length = 200)
    private String title;

    /** Asia/Taipei 當地時間，不接受 offset 或 Z。 */
    @NotNull(groups = ValidGroup.Create.class)
    @Column(name = "held_at", nullable = false, columnDefinition = "datetime(6)")
    private LocalDateTime heldAt;

    @NotBlank(groups = ValidGroup.Create.class)
    @Size(max = 2000)
    @Pattern(regexp = "(?sU).*\\S.*")
    @Column(nullable = false, length = 2000)
    private String participants;

    @NotBlank(groups = ValidGroup.Create.class)
    @Size(max = 10000)
    @Pattern(regexp = "(?sU).*\\S.*")
    @Column(nullable = false, columnDefinition = "text")
    private String minutes;

    // 關聯沿用已核准結構；本輪不提供轉任務或關聯寫入入口。
    @ElementCollection
    @CollectionTable(name = "meeting_task", joinColumns = @JoinColumn(name = "meeting_id"))
    @Column(name = "task_id", nullable = false)
    @OrderBy
    private Set<Long> taskIds;
}

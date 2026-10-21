package dev.jianwii.entity.project;

import dev.jianwii.common.entity.BaseEntity;
import dev.jianwii.common.validation.ValidGroup;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

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

    /**
     * 留言中 @ 到的人（user id），存在中間表 task_comment_mention；必須是專案成員。 由前端依選到的成員送出，內文的「@名字」只是顯示用。被 @
     * 的人會收到通知並自動關注任務。 更新時不給（null）表示不改；新加入的人才會再收到通知。
     */
    @Size(max = 20)
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "task_comment_mention", joinColumns = @JoinColumn(name = "comment_id"))
    @Column(name = "user_id", nullable = false, length = 100)
    @BatchSize(size = 100)
    private Set<@NotBlank String> mentionIds;
}

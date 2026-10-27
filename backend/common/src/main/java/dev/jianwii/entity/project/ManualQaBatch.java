package dev.jianwii.entity.project;

import dev.jianwii.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "manual_qa_batch")
@Getter
@Setter
public class ManualQaBatch extends BaseEntity {
    @Column(name = "project_id", nullable = false, updatable = false)
    private Long projectId;

    @Column(name = "version_label", nullable = false, length = 200)
    private String versionLabel;

    @Column(nullable = false, length = 1000)
    private String environment;
}

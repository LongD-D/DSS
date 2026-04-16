package dss.model.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import dss.model.entity.enums.OptimizationDirection;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class TaskParameter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String name;

    /**
     * 一级指标名称（两级指标体系中的父指标）
     */
    private String parentCriterion;

    /**
     * 指标节点ID（用于构建多级树）
     */
    @Column(length = 64)
    private String nodeId;

    /**
     * 父节点ID，根节点为空
     */
    @Column(length = 64)
    private String parentId;

    /**
     * 层级路径，如 root/tech/cost
     */
    @Column(length = 255)
    private String path;

    /**
     * 当前节点层级，根为 1
     */
    private Integer level;

    /**
     * 同级排序号
     */
    private Integer sortOrder;

    @NotNull
    private double weight;

    private String unit;

    @Enumerated(EnumType.STRING)
    private OptimizationDirection optimizationDirection;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id")
    @JsonBackReference
    private Task task;
}

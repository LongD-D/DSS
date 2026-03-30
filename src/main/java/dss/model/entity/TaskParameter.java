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

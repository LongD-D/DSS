package dss.dto;

import dss.model.entity.enums.OptimizationDirection;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TaskParameterDto {
    private String nodeId;
    private String parentId;
    private String path;
    private Integer level;
    private Integer sortOrder;
    @NotBlank
    private String name;
    private String parentCriterion;
    @NotNull
    private double weight;
    @NotNull
    private String unit;
    @NotNull
    private OptimizationDirection optimizationDirection;
}

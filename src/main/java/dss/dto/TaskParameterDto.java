package dss.dto;

import dss.model.entity.enums.OptimizationDirection;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TaskParameterDto {
    @NotBlank
    private String name;
    @NotBlank
    private String parentCriterion;
    @NotNull
    private double weight;
    @NotNull
    private String unit;
    @NotNull
    private OptimizationDirection optimizationDirection;
}

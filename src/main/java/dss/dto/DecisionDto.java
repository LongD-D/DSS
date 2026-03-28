package dss.dto;

import dss.model.entity.enums.DecisionCategory;
import dss.model.entity.enums.DecisionStatus;
import dss.model.entity.enums.DecisionCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DecisionDto {


    @NotNull(message = "方案类型不能为空")
    private DecisionCategory decisionCategory;

    @NotBlank
    private String title;

    @NotBlank(message = "方案描述不能为空")
    private String description;

    @NotNull
    private DecisionStatus decisionStatus;

    @NotNull
    private Long taskId;

    @NotNull
    private Long userId;

    @Null
    private List<ScenarioDto> scenariosDto;

    @NotNull
    private List<DecisionParameterDto> decisionParameterDtoList;
}


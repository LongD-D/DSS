package dss.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ExpertEvaluationDto {
    @NotNull
    private Long decisionId;
    @NotNull
    @Min(0)
    @Max(10)
    private double score;
    @NotNull
    @Min(0)
    @Max(1)
    private Double ca;
    @NotNull
    @Min(0)
    @Max(1)
    private Double cs;
    private String comment;
}

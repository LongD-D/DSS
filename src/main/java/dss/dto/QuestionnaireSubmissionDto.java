package dss.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@AllArgsConstructor
public class QuestionnaireSubmissionDto {
    private final LocalDateTime submittedAt;
    private final Map<String, Double> dimensionAverage;
}

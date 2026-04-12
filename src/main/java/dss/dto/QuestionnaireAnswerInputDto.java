package dss.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class QuestionnaireAnswerInputDto {
    private final Long questionId;
    private final String dimension;
    private final String questionText;
    private final Integer score;
}

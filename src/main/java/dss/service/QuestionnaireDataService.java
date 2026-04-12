package dss.service;

import dss.dto.QuestionnaireAnswerInputDto;
import dss.dto.QuestionnaireSubmissionDto;
import dss.model.entity.User;

import java.util.List;
import java.util.Map;

public interface QuestionnaireDataService {
    void recordSubmission(User user, double overallAverage, List<QuestionnaireAnswerInputDto> answers, Map<String, Double> dimensionAverage);

    Map<String, Double> getLatestDimensionAverage();

    List<QuestionnaireSubmissionDto> getSubmissionHistory();

    void clearSubmissionHistory();
}

package dss.service;

import dss.dto.QuestionnaireSubmissionDto;

import java.util.List;
import java.util.Map;

public interface QuestionnaireDataService {
    void recordSubmission(Map<String, Double> dimensionAverage);

    Map<String, Double> getLatestDimensionAverage();

    List<QuestionnaireSubmissionDto> getSubmissionHistory();

    void clearSubmissionHistory();
}

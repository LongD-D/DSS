package dss.service;

import java.util.Map;

public interface QuestionnaireDataService {
    void recordSubmission(Map<String, Double> dimensionAverage);

    Map<String, Double> getLatestDimensionAverage();
}

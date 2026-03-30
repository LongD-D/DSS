package dss.service.impl;

import dss.service.QuestionnaireDataService;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class QuestionnaireDataServiceImpl implements QuestionnaireDataService {

    private Map<String, Double> latestDimensionAverage = new LinkedHashMap<>();

    @Override
    public synchronized void recordSubmission(Map<String, Double> dimensionAverage) {
        latestDimensionAverage = new LinkedHashMap<>(dimensionAverage);
    }

    @Override
    public synchronized Map<String, Double> getLatestDimensionAverage() {
        return new LinkedHashMap<>(latestDimensionAverage);
    }
}

package dss.service.impl;

import dss.dto.QuestionnaireSubmissionDto;
import dss.service.QuestionnaireDataService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class QuestionnaireDataServiceImpl implements QuestionnaireDataService {

    private Map<String, Double> latestDimensionAverage = new LinkedHashMap<>();
    private final List<QuestionnaireSubmissionDto> submissionHistory = new ArrayList<>();

    @Override
    public synchronized void recordSubmission(Map<String, Double> dimensionAverage) {
        latestDimensionAverage = new LinkedHashMap<>(dimensionAverage);
        submissionHistory.add(0, new QuestionnaireSubmissionDto(LocalDateTime.now(), new LinkedHashMap<>(dimensionAverage)));
    }

    @Override
    public synchronized Map<String, Double> getLatestDimensionAverage() {
        return new LinkedHashMap<>(latestDimensionAverage);
    }

    @Override
    public synchronized List<QuestionnaireSubmissionDto> getSubmissionHistory() {
        return new ArrayList<>(submissionHistory);
    }

    @Override
    public synchronized void clearSubmissionHistory() {
        submissionHistory.clear();
        latestDimensionAverage = new LinkedHashMap<>();
    }
}

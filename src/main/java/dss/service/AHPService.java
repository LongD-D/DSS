package dss.service;

import dss.dto.AHPAnalysisRequestDto;
import dss.dto.AHPAnalysisResultDto;
import dss.model.entity.Decision;
import dss.model.entity.Task;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public interface AHPService {

//    Decision selectBestDecision(List<Decision> decisions, double[][] comparisonMatrix);

    Map<String, Double> evaluateDecisionsAHP(Task task, double[][] comparisonMatrix);

    AHPAnalysisResultDto evaluateTask(Task task, AHPAnalysisRequestDto requestDto);
}

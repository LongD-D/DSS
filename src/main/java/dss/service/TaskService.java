package dss.service;

import dss.dto.TaskDto;
import dss.dto.TaskParameterDto;
import dss.dto.AHPAnalysisRequestDto;
import dss.dto.AHPAnalysisResultDto;
import dss.model.entity.Decision;
import dss.model.entity.Task;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public interface TaskService {

    List<Task> getAllTasks();

    Task getTaskById(Long id);

    Task createTask(TaskDto taskDto, Authentication authentication);

    void deleteTaskById(Long id);

    Task addTaskParameterToTask(Long taskId, List<TaskParameterDto> taskParameterDtoList);

    Task updateTask(Long id, TaskDto taskDto);

    Map<String, Double> findBestDecisionAHP(Long taskId, double[][] comparisonMatrix);

    AHPAnalysisResultDto analyzeTaskByAHP(Long taskId, AHPAnalysisRequestDto requestDto);

    Map<String, Double> findBestDecisionTOPSIS(Long taskId);

//    Map<String, Double> findBestDecisionELECTRE(Long taskId);

    Map<String, Double> findBestDecisionELECTRE(Long taskId, double cThreshold, double dThreshold);

    Task setSolution(Long taskId, Long decisionId);

    String recommendBestMethodForTask(Long taskId);
}

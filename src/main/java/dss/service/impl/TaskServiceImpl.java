package dss.service.impl;

import dss.dto.TaskDto;
import dss.dto.TaskParameterDto;
import dss.dto.AHPAnalysisRequestDto;
import dss.dto.AHPAnalysisResultDto;
import dss.model.entity.Decision;
import dss.model.entity.Task;
import dss.model.entity.TaskParameter;
import dss.model.entity.enums.DecisionStatus;
import dss.model.entity.enums.OptimizationDirection;
import dss.model.entity.enums.TaskStatus;
import dss.repository.DecisionRepository;
import dss.repository.TaskRepository;
import dss.service.*;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class TaskServiceImpl implements TaskService {

    private TaskRepository taskRepository;
    private UserService userService;
    @Qualifier("AHPService")
    private AHPService ahpService;
    private TopsisService topsisService;
    private ElectreService electreService;
    private DecisionRepository decisionRepository;

    @Override
    public List<Task> getAllTasks(){
        return taskRepository.findAll();
    }

    @Override
    public Task getTaskById(Long id){
        return taskRepository.findById(id).get();
    }

    @Override
    public Task createTask(TaskDto taskDto, Authentication authentication) {
        var task = new Task();
        task.setTitle(taskDto.getTitle());
        task.setDescription(taskDto.getDescription());
        task.setCategory(taskDto.getCategory());
        task.setStatus(taskDto.getStatus());
        task.setCreated(LocalDateTime.now());
        task.setUser(userService.findUserByEmail(authentication.getName()));

        // 创建参数
        var parameterList = new ArrayList<TaskParameter>();
        if (taskDto.getTaskParameters() != null) {
            for (TaskParameterDto paramDto : taskDto.getTaskParameters()) {
                var param = new TaskParameter();
                param.setName(paramDto.getName());
                param.setParentCriterion(paramDto.getParentCriterion());
                param.setWeight(paramDto.getWeight());
                param.setUnit(paramDto.getUnit());
                param.setOptimizationDirection(paramDto.getOptimizationDirection());
                param.setTask(task); // 与 task
                parameterList.add(param);
            }
        }

        task.setTaskParameters(parameterList);
        return taskRepository.save(task);
    }


    @Override
    public void deleteTaskById(Long id){
        if(taskRepository.existsById(id)){
            taskRepository.deleteById(id);
        }
        else throw new RuntimeException("No such task");
    }

    @Override
    public Task addTaskParameterToTask(Long taskId ,List<TaskParameterDto> taskParameterDtoList){
        var task = taskRepository.findById(taskId).get();
        var taskParameterList = new ArrayList<TaskParameter>();
        for(TaskParameterDto taskParameterDto : taskParameterDtoList){
            var taskParameter = new TaskParameter();
            taskParameter.setTask(task);
            taskParameter.setName(taskParameterDto.getName());
            taskParameter.setParentCriterion(taskParameterDto.getParentCriterion());
            taskParameter.setWeight(taskParameterDto.getWeight());
            taskParameter.setUnit(taskParameterDto.getUnit());
            taskParameter.setOptimizationDirection(taskParameterDto.getOptimizationDirection());
            System.out.println(taskParameter.getOptimizationDirection().getLabel());
            taskParameterList.add(taskParameter);
        }
        task.setTaskParameters(taskParameterList);
        return taskRepository.save(task);
    }

    @Override
    public Task updateTask(Long id, TaskDto taskDto) {
        var task = taskRepository.findById(id).orElseThrow();

        if (taskDto.getTitle() != null)
            task.setTitle(taskDto.getTitle());
        if (taskDto.getDescription() != null)
            task.setDescription(taskDto.getDescription());
        if (taskDto.getCategory() != null)
            task.setCategory(taskDto.getCategory());
        if (taskDto.getStatus() != null)
            task.setStatus(taskDto.getStatus());

        if (taskDto.getTaskParameters() != null) {
            // 创建现有参数映射以便快速访问
            Map<String, TaskParameter> existing = task.getTaskParameters().stream()
                    .collect(Collectors.toMap(TaskParameter::getName, p -> p));

            List<TaskParameter> updatedList = new ArrayList<>();

            for (TaskParameterDto dto : taskDto.getTaskParameters()) {
                TaskParameter param = existing.getOrDefault(dto.getName(), new TaskParameter());
                param.setName(dto.getName());
                param.setParentCriterion(dto.getParentCriterion());
                param.setWeight(dto.getWeight());
                param.setUnit(dto.getUnit());
                param.setTask(task);
                param.setOptimizationDirection(dto.getOptimizationDirection());
                updatedList.add(param);
            }

            task.getTaskParameters().clear();
            task.getTaskParameters().addAll(updatedList);
        }

        return taskRepository.save(task);
    }

    @Override
    public Map<String, Double> findBestDecisionAHP(Long taskId, double[][] comparisonMatrix) {
        var task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task with id: " + taskId + ", not found"));

        return ahpService.evaluateDecisionsAHP(task, comparisonMatrix);
    }

    @Override
    public AHPAnalysisResultDto analyzeTaskByAHP(Long taskId, AHPAnalysisRequestDto requestDto) {
        var task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task with id: " + taskId + ", not found"));
        return ahpService.evaluateTask(task, requestDto);
    }

// TaskServiceImpl.java

    @Override
    public Map<String, Double> findBestDecisionTOPSIS(Long taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("未找到任务"));

        var weights = new double[task.getTaskParameters().size()];

        for(int i = 0; i < weights.length; i++){
            weights[i] = task.getTaskParameters().get(i).getWeight();
        }

        return topsisService.evaluateDecisionsTOPSIS(task, weights);
    }

    @Override
    public Map<String, Double> findBestDecisionELECTRE(Long taskId, double cThreshold, double dThreshold) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("未找到任务"));

        List<Decision> decisions = task.getDecisions();
        if (decisions == null || decisions.isEmpty()) {
            throw new IllegalStateException("该任务没有已提议方案");
        }

        return electreService.evaluateDecisionsELECTRE(task, cThreshold, dThreshold);
    }


    @Override
    public Task setSolution(Long taskId, Long decisionId) {
        var task = taskRepository.findById(taskId).orElseThrow();

        var decision = task.getDecisions().stream()
                .filter(d -> d.getId().equals(decisionId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("在任务方案中未找到该方案"));

        task.setChosenDecision(decision);
        task.setStatus(TaskStatus.SOLVED);

        decision.setDecisionStatus(DecisionStatus.APPROVED);
        decisionRepository.save(decision);


        return taskRepository.save(task);
    }

    private int getStatusPriority(DecisionStatus status) {
        return switch (status) {
            case APPROVED -> 0;
            case PROPOSED -> 1;
            case REJECTED -> 2;
            default -> 3;
        };
    }

    @Override
    public String recommendBestMethodForTask(Long taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("未找到任务"));

        int numParams = task.getTaskParameters().size();

        // 如果参数小于等于 6，则使用 AHP
        if (numParams <= 6) {
            return "AHP";
        }

        // 统计优化方向
        long countMax = task.getTaskParameters().stream()
                .filter(p -> p.getOptimizationDirection() == OptimizationDirection.MAXIMIZE)
                .count();
        long countMin = task.getTaskParameters().stream()
                .filter(p -> p.getOptimizationDirection() == OptimizationDirection.MINIMIZE)
                .count();

        // 若所有方向一致，则使用 ELECTRE
        if (countMax == numParams || countMin == numParams) {
            return "ELECTRE";
        }

        // 若方向不同，则使用 TOPSIS
        return "TOPSIS";
    }


}

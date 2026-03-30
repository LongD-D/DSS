package dss.controller.rest;

import dss.dto.TaskDto;
import dss.dto.TaskParameterDto;
import dss.dto.AHPAnalysisRequestDto;
import dss.dto.AHPAnalysisResultDto;
import dss.model.entity.Decision;
import dss.model.entity.Task;
import dss.service.TaskService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/tasks")
@AllArgsConstructor
public class TaskRestController {

    private TaskService taskService;

    @GetMapping
    public ResponseEntity<List<Task>> getTasks() {
        return ResponseEntity.ok(taskService.getAllTasks());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Task> getTaskById(@PathVariable Long id) {
        return ResponseEntity.ok(taskService.getTaskById(id));
    }
    @PostMapping
    public ResponseEntity<Task> createTask(@RequestBody TaskDto taskDto,
                                           Authentication authentication) {
        return ResponseEntity.ok(taskService.createTask(taskDto, authentication));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
        taskService.deleteTaskById(id);
        return ResponseEntity.ok().build();
    }
    @PostMapping("/{taskId}")
    public ResponseEntity<Task> createTaskParameter(@PathVariable Long taskId ,
                                                    @RequestBody List<TaskParameterDto> taskParameterDtoList){
        return ResponseEntity.ok(taskService
                .addTaskParameterToTask(taskId,taskParameterDtoList)
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<Task> updateTask(@PathVariable Long id,
                                           @RequestBody TaskDto taskDto,
                                           Authentication authentication){
        return ResponseEntity.ok(taskService.updateTask(id,taskDto));
    }

    @PostMapping("/{taskId}/ahp")
    public ResponseEntity<Map<String, Double>> getAHPTask(@PathVariable Long taskId,
                                                          @RequestBody double[][] comparisonMatrix) {
        return ResponseEntity.ok(taskService.findBestDecisionAHP(taskId, comparisonMatrix));
    }

    @PostMapping("/{taskId}/ahp-analysis")
    public ResponseEntity<AHPAnalysisResultDto> analyzeTaskByAHP(@PathVariable Long taskId,
                                                                  @RequestBody AHPAnalysisRequestDto requestDto) {
        return ResponseEntity.ok(taskService.analyzeTaskByAHP(taskId, requestDto));
    }

    @PostMapping("/{taskId}/topsis")
    public ResponseEntity<Map<String, Double>> calculateTopsis(
            @PathVariable Long taskId) {
        return ResponseEntity.ok(taskService.findBestDecisionTOPSIS(taskId));
    }

    @PostMapping("/{taskId}/electre")
    public ResponseEntity<Map<String, Double>> calculateElectre(
            @PathVariable Long taskId,
            @RequestBody Map<String, Double> thresholds) {

        double cThreshold = thresholds.getOrDefault("cThreshold", 0.5);
        double dThreshold = thresholds.getOrDefault("dThreshold", 0.5);

        return ResponseEntity.ok(
                taskService.findBestDecisionELECTRE(taskId, cThreshold, dThreshold)
        );
    }
    @GetMapping("/{id}/recommend-method")
    public ResponseEntity<String> recommendMethod(@PathVariable Long id) {
        String method = taskService.recommendBestMethodForTask(id);
        return ResponseEntity.ok(method);
    }


}

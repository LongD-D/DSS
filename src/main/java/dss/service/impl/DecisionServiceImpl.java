package dss.service.impl;

import dss.dto.DecisionDto;
import dss.dto.DecisionImportErrorDto;
import dss.dto.DecisionImportResponseDto;
import dss.dto.ExpertEvaluationDto;
import dss.model.entity.*;
import dss.model.entity.enums.DecisionCategory;
import dss.model.entity.enums.DecisionStatus;
import dss.model.entity.enums.TaskStatus;
import dss.repository.DecisionRepository;
import dss.repository.TaskRepository;
import dss.repository.UserRepository;
import dss.service.DecisionService;
import dss.service.ExpertEvaluationService;
import dss.service.ScenarioService;
import dss.service.TaskService;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@AllArgsConstructor
public class DecisionServiceImpl implements DecisionService {

    private final DecisionRepository DecisionRepository;
    private final UserRepository userRepository;
    private final DecisionRepository decisionRepository;
    private final ScenarioService scenarioService;
    private final TaskService taskService;
    private final TaskRepository taskRepository;
    private final ExpertEvaluationService expertEvaluationService;

    @Override
    public List<Decision> getAllDecisions() {
        return DecisionRepository.findAll();
    }
    @Override
    public Decision getDecisionById(Long id){
        return DecisionRepository.findById(id).get();
    }

    @Override
    public List<Decision> getAllDesicionsByUser(User user){
        return DecisionRepository.findAllByUser(user);
    }
    @Override
    public Decision createDecision(DecisionDto decisionDto,
                                   Authentication authentication) {

        Decision decision = new Decision();
        Task task = taskService.getTaskById(decisionDto.getTaskId());

        decision.setTask(task);
        decision.setTitle(decisionDto.getTitle());
        decision.setDescription(decisionDto.getDescription());
        decision.setDecisionCategory(decisionDto.getDecisionCategory());
        decision.setCreated(LocalDateTime.now());
        decision.setDecisionStatus(decisionDto.getDecisionStatus());
        decision.setScenarios(
                scenarioService.MapScenariosDtoToScenarios(
                        decisionDto.getScenariosDto(), authentication
                )
        );
        decision.getScenarios().forEach(scenario -> scenario.setDecision(decision));
        decision.setUser(findUserByAuthentication(authentication));

        List<DecisionParameter> decisionParameters = decisionDto.getDecisionParameterDtoList().stream()
                .map(dto -> {
                    DecisionParameter param = new DecisionParameter();
                    param.setDecision(decision);
                    param.setComment(dto.getComment());
                    param.setValue(dto.getValue());

                    TaskParameter taskParam = task.getTaskParameters().stream()
                            .filter(tp -> tp.getId().equals(dto.getTaskParameterId()))
                            .findFirst()
                            .orElseThrow(() -> new RuntimeException("Task parameter not found"));
                    param.setTaskParameter(taskParam);
                    return param;
                })
                .toList();

        decision.setDecisionParameters(decisionParameters);

        return decisionRepository.save(decision);
    }

    @Override
    @Transactional
    public Decision updateDecision(Long id, DecisionDto decisionDto, Authentication authentication) {
        Decision decision = decisionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("未找到 ID 对应的方案: " + id));

        decision.setTitle(decisionDto.getTitle());
        decision.setDescription(decisionDto.getDescription());
        decision.setDecisionCategory(decisionDto.getDecisionCategory());
        decision.setDecisionStatus(decisionDto.getDecisionStatus());

        decision.getScenarios().clear();

        if (decisionDto.getScenariosDto() != null) {
            List<Scenario> scenarios = scenarioService.MapScenariosDtoToScenarios(decisionDto.getScenariosDto(), authentication);
            for (Scenario scenario : scenarios) {
                scenario.setDecision(decision);
            }
            decision.getScenarios().addAll(scenarios);
        }

        return decisionRepository.save(decision);
    }

    @Override
    public void deleteDecisionById(Long id, Authentication authentication){
        if (decisionRepository.existsById(id)){
            if (decisionRepository.findById(id).get().getUser()
                    .equals(findUserByAuthentication(authentication))){

                decisionRepository.deleteById(id);
            }
            else {
                throw new RuntimeException("Decision is not made by authenticated user." +
                        " It's not possible to delete the decision.");
            }
        }
        else {
            throw new RuntimeException("Desision with id = " + id +" not found");
        }
    }

    private User findUserByAuthentication(Authentication authentication){
        return userRepository.findByEmail(authentication.getName());
    }

    @Override
    public boolean existsById(Long id){
        return DecisionRepository.existsById(id);
    }

    @Override
    public Decision rateDecision(Long id,
                                 ExpertEvaluationDto expertEvaluationDto,
                                 Authentication authentication){
        var decision = decisionRepository.findById(id).get();

        if(expertEvaluationService.getByExpertAndDecision(
                findUserByAuthentication(authentication),
                decision)!=null){
             expertEvaluationService.updateEvaluation(decision,expertEvaluationDto,authentication);
        }
        else
        {
            var expertEvaluation = expertEvaluationService.submitEvaluation(expertEvaluationDto,authentication);
            decision.getExpertEvaluations().add(expertEvaluation);

        }

        return decisionRepository.save(decision);
    }

    @Override
    public Decision updateStatus(Long id, DecisionStatus decisionStatus, Authentication authentication){
        Decision decision = decisionRepository.findById(id).get();
        var task = decision.getTask();
        if (decision.getDecisionStatus().equals(DecisionStatus.APPROVED) && decisionStatus!=DecisionStatus.APPROVED){
            task.setStatus(TaskStatus.NEW);
            task.setChosenDecision(null);
            task.setSolved(null);
            taskRepository.save(task);
        }
        if(decisionStatus.equals(DecisionStatus.APPROVED)){
            task.setChosenDecision(decision);
            task.setSolved(LocalDateTime.now());
            task.setStatus(TaskStatus.SOLVED);
            taskRepository.save(task);
        }
        else if (decisionStatus.equals(DecisionStatus.REJECTED)){
            task.setStatus(TaskStatus.NEW);
            task.setChosenDecision(null);
            task.setSolved(null);
        }

        decision.setDecisionStatus(decisionStatus);

        return decisionRepository.save(decision);
    }

    @Override
    public List<Decision> findAllDecisionsByAuthUser(Authentication authentication){
        return decisionRepository.findAllByUser(userRepository.findByEmail(authentication.getName()));
    }


    @Override
    @Transactional
    public DecisionImportResponseDto importDecisionsFromCsv(MultipartFile file, Authentication authentication) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("请上传非空 CSV 文件。");
        }

        String fileName = file.getOriginalFilename();
        if (fileName == null || !fileName.toLowerCase(Locale.ROOT).endsWith(".csv")) {
            throw new IllegalArgumentException("仅支持 .csv 文件导入。");
        }

        User operator = findUserByAuthentication(authentication);
        Map<String, Decision> decisionByKey = new LinkedHashMap<>();
        List<DecisionImportErrorDto> errors = new ArrayList<>();
        int successRows = 0;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (!StringUtils.hasText(headerLine)) {
                throw new IllegalArgumentException("CSV 文件缺少表头。");
            }

            Map<String, Integer> headerIndex = buildHeaderIndex(parseCsvLine(headerLine));
            validateHeaders(headerIndex);

            String line;
            int lineNumber = 1;
            while ((line = reader.readLine()) != null) {
                lineNumber++;

                if (!StringUtils.hasText(line)) {
                    continue;
                }

                try {
                    List<String> columns = parseCsvLine(line);
                    Long taskId = parseLong(getCell(columns, headerIndex, "task_id"), "task_id");
                    String title = required(getCell(columns, headerIndex, "title"), "title");
                    String description = required(getCell(columns, headerIndex, "description"), "description");
                    String parameterName = required(getCell(columns, headerIndex, "parameter_name"), "parameter_name");
                    double parameterValue = parseDouble(getCell(columns, headerIndex, "parameter_value"), "parameter_value");
                    String comment = defaultText(getCell(columns, headerIndex, "parameter_comment"));

                    Decision decision = resolveOrCreateDecision(
                            decisionByKey,
                            taskId,
                            title,
                            description,
                            getCell(columns, headerIndex, "decision_category"),
                            getCell(columns, headerIndex, "decision_status"),
                            operator
                    );

                    TaskParameter taskParameter = decision.getTask().getTaskParameters().stream()
                            .filter(tp -> parameterName.equals(tp.getName()))
                            .findFirst()
                            .orElseThrow(() -> new IllegalArgumentException("任务未找到参数: " + parameterName));

                    DecisionParameter parameter = new DecisionParameter();
                    parameter.setDecision(decision);
                    parameter.setTaskParameter(taskParameter);
                    parameter.setValue(parameterValue);
                    parameter.setComment(comment);
                    decision.getDecisionParameters().add(parameter);
                    successRows++;
                } catch (Exception ex) {
                    errors.add(new DecisionImportErrorDto(lineNumber, ex.getMessage()));
                }
            }

            decisionByKey.values().forEach(decisionRepository::save);

            int successCount = successRows;
            int failureCount = errors.size();
            String message = failureCount == 0
                    ? "导入完成，所有数据均已入库。"
                    : "导入完成，部分数据存在错误。";

            return new DecisionImportResponseDto(message, successCount, failureCount, errors);
        } catch (IOException ex) {
            throw new IllegalArgumentException("CSV 读取失败: " + ex.getMessage());
        }
    }

    private Decision resolveOrCreateDecision(Map<String, Decision> decisionByKey,
                                             Long taskId,
                                             String title,
                                             String description,
                                             String categoryRaw,
                                             String statusRaw,
                                             User operator) {
        String key = taskId + "|" + title;
        Decision existing = decisionByKey.get(key);
        if (existing != null) {
            return existing;
        }

        Task task = taskService.getTaskById(taskId);
        Decision decision = new Decision();
        decision.setTask(task);
        decision.setTitle(title);
        decision.setDescription(description);
        decision.setDecisionCategory(parseCategory(categoryRaw));
        decision.setDecisionStatus(parseStatus(statusRaw));
        decision.setCreated(LocalDateTime.now());
        decision.setUser(operator);
        decision.setScenarios(new ArrayList<>());
        decision.setExpertEvaluations(new ArrayList<>());
        decision.setDecisionParameters(new ArrayList<>());

        decisionByKey.put(key, decision);
        return decision;
    }

    private DecisionStatus parseStatus(String statusRaw) {
        if (!StringUtils.hasText(statusRaw)) {
            return DecisionStatus.PROPOSED;
        }
        return DecisionStatus.valueOf(statusRaw.trim().toUpperCase(Locale.ROOT));
    }

    private DecisionCategory parseCategory(String categoryRaw) {
        if (!StringUtils.hasText(categoryRaw)) {
            return DecisionCategory.ECOLOGY;
        }
        return DecisionCategory.valueOf(categoryRaw.trim().toUpperCase(Locale.ROOT));
    }

    private void validateHeaders(Map<String, Integer> headerIndex) {
        List<String> requiredHeaders = List.of("task_id", "title", "description", "parameter_name", "parameter_value");
        for (String header : requiredHeaders) {
            if (!headerIndex.containsKey(header)) {
                throw new IllegalArgumentException("CSV 缺少必要列: " + header);
            }
        }
    }

    private Map<String, Integer> buildHeaderIndex(List<String> headers) {
        Map<String, Integer> index = new LinkedHashMap<>();
        for (int i = 0; i < headers.size(); i++) {
            String normalized = headers.get(i).trim().toLowerCase(Locale.ROOT);
            if (!normalized.isEmpty()) {
                index.put(normalized, i);
            }
        }
        return index;
    }

    private String getCell(List<String> columns, Map<String, Integer> headerIndex, String header) {
        Integer idx = headerIndex.get(header);
        if (idx == null || idx >= columns.size()) {
            return "";
        }
        return columns.get(idx);
    }

    private String required(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException("字段缺失: " + fieldName);
        }
        return value.trim();
    }

    private String defaultText(String value) {
        return value == null ? "" : value.trim();
    }

    private Long parseLong(String raw, String fieldName) {
        try {
            return Long.parseLong(required(raw, fieldName));
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("字段格式错误: " + fieldName);
        }
    }

    private double parseDouble(String raw, String fieldName) {
        try {
            return Double.parseDouble(required(raw, fieldName));
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("字段格式错误: " + fieldName);
        }
    }

    private List<String> parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                values.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }

        values.add(current.toString().trim());
        return values;
    }

    @Override
    public List<Decision> findAllRelatedDecisions(Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName());
        return decisionRepository.findAllByTaskUserOrUser(user, user).stream()
                .sorted(Comparator.comparing(Decision::getCreated).reversed())
                .toList();
    }
}

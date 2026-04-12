package dss.service.impl;

import dss.dto.DecisionDto;
import dss.dto.ExpertEvaluationDto;
import dss.model.entity.*;
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

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

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
    public List<Decision> findAllRelatedDecisions(Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName());
        return decisionRepository.findAllByTaskUserOrUser(user, user).stream()
                .sorted(Comparator.comparing(Decision::getCreated).reversed())
                .toList();
    }
}

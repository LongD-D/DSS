package dss.service;

import dss.dto.DecisionDto;
import dss.dto.ExpertEvaluationDto;
import dss.dto.ScenarioDto;
import dss.model.entity.Decision;
import dss.model.entity.Task;
import dss.model.entity.User;
import dss.model.entity.enums.DecisionStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface DecisionService {
    List<Decision> getAllDecisions();

    Decision getDecisionById(Long id);

    List<Decision> getAllDesicionsByUser(User user);

    Decision createDecision(DecisionDto decisionDto,
                            Authentication authentication);

    Decision updateDecision(Long id,
                            DecisionDto decisionDto,
                            Authentication authentication);

    void deleteDecisionById(Long id, Authentication authentication);

    boolean existsById(Long id);

    Decision rateDecision(Long id,
                          ExpertEvaluationDto expertEvaluationDto,
                          Authentication authentication);

    Decision updateStatus(Long id, DecisionStatus decisionStatus, Authentication authentication);

    List<Decision> findAllDecisionsByAuthUser(Authentication authentication);

    List<Decision> findAllRelatedDecisions(Authentication authentication);
}

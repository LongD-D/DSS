package dss.service.impl;

import dss.dto.ExpertEvaluationDto;
import dss.model.entity.Decision;
import dss.model.entity.ExpertEvaluation;
import dss.model.entity.User;
import dss.repository.DecisionRepository;
import dss.repository.ExpertEvaluationRepository;
import dss.service.ExpertEvaluationService;
import dss.service.UserService;
import lombok.AllArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@AllArgsConstructor
public class ExpertEvaluationServiceImpl implements ExpertEvaluationService {

    private DecisionRepository decisionRepository;
    private ExpertEvaluationRepository expertEvaluationRepository;
    private UserService userService;

    @Override
    public ExpertEvaluation submitEvaluation(ExpertEvaluationDto expertEvaluationDto, Authentication authentication) {
        ExpertEvaluation expertEvaluation = new ExpertEvaluation();
        expertEvaluation.setDecision(decisionRepository.findById(expertEvaluationDto.getDecisionId()).get());
        expertEvaluation.setExpert(userService.findUserByEmail(authentication.getName()));
        expertEvaluation.setScore(expertEvaluationDto.getScore());
        expertEvaluation.setCa(expertEvaluationDto.getCa());
        expertEvaluation.setCs(expertEvaluationDto.getCs());
        expertEvaluation.setC(calculateC(expertEvaluationDto.getCa(), expertEvaluationDto.getCs()));
        expertEvaluation.setComment(expertEvaluationDto.getComment());
        return expertEvaluationRepository.save(expertEvaluation);
    }

    @Override
    public ExpertEvaluation updateEvaluation(Decision decision ,ExpertEvaluationDto expertEvaluationDto, Authentication authentication) {
        var found = expertEvaluationRepository
                .findExpertEvaluationByExpertAndDecision(userService
                                .findUserByEmail(authentication
                                        .getName()),
                        decision);

        if (found!=null){
            found.setScore(expertEvaluationDto.getScore());
            found.setCa(expertEvaluationDto.getCa());
            found.setCs(expertEvaluationDto.getCs());
            found.setC(calculateC(expertEvaluationDto.getCa(), expertEvaluationDto.getCs()));
            found.setComment(expertEvaluationDto.getComment());
            return expertEvaluationRepository.save(found);
        }

        return null;
    }

    @Override
    public Map<Long, Double> calculateAverageRatingsForTask(Long taskId) {
        return Map.of();
    }

    @Override
    public ExpertEvaluation getByExpertAndDecision(User expert, Decision decision) {
        var found = expertEvaluationRepository.findExpertEvaluationByExpertAndDecision(expert, decision);

        if (found!=null){
            return found;
        }
        return null;
    }

    private double calculateC(Double ca, Double cs) {
        if (ca == null || cs == null) {
            return 0.0;
        }
        return (ca + cs) / 2.0;
    }
}

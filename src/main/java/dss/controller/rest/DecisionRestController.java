package dss.controller.rest;

import dss.dto.DecisionDto;
import dss.dto.ExpertEvaluationDto;
import dss.dto.ScenarioDto;
import dss.dto.StatusUpdateRequest;
import dss.model.entity.Decision;
import dss.model.entity.ExpertEvaluation;
import dss.model.entity.Task;
import dss.model.entity.User;
import dss.model.entity.enums.DecisionStatus;
import dss.repository.DecisionRepository;
import dss.repository.ExpertEvaluationRepository;
import dss.service.DecisionService;
import dss.service.ExpertEvaluationService;
import dss.service.TaskService;
import dss.service.UserService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/decisions")
@AllArgsConstructor
public class DecisionRestController {

    private final DecisionRepository decisionRepository;
    private final TaskService taskService;
    private DecisionService decisionService;
    private ExpertEvaluationService expertEvaluationService;
    private UserService userService;
    private ExpertEvaluationRepository expertEvaluationRepository;

    @GetMapping
    public ResponseEntity<List<Decision>> getDecisions() {

        var decisions = decisionService.getAllDecisions();

        return ResponseEntity.ok(decisions);
    }
    @GetMapping("/{id}")
    public ResponseEntity<Decision> getDecisionById(@PathVariable Long id) {
        return ResponseEntity.ok(decisionService.getDecisionById(id));
    }

    @GetMapping("my")
    public ResponseEntity<List<Decision>> getDecisionsByAuthUser(Authentication auth) {
        var decisions = decisionService.findAllDecisionsByAuthUser(auth);
        return ResponseEntity.ok(decisions);
    }

    @GetMapping("related")
    public ResponseEntity<List<Decision>> getRelatedDecisions(Authentication auth) {
        return ResponseEntity.ok(decisionService.findAllRelatedDecisions(auth));
    }


    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<Decision> createDecision(
            @RequestBody DecisionDto decisionDto,
            Authentication authentication) {
        return ResponseEntity.ok(decisionService.createDecision(decisionDto,authentication));
    }
    @PutMapping("/{id}")
    public ResponseEntity<Decision> updateDecision(@PathVariable Long id,
                                                   @RequestBody DecisionDto decisionDto,
                                                   Authentication authentication) {
        return ResponseEntity.ok(decisionService.updateDecision(id, decisionDto, authentication));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDecision(@PathVariable Long id,
                                               Authentication authentication) {

        decisionService.deleteDecisionById(id, authentication);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/rate")
    public ResponseEntity<Decision> rateDecision(@PathVariable Long id,
                                                  @RequestBody ExpertEvaluationDto expertEvaluationDto,
                                                  Authentication authentication){
        return ResponseEntity.ok(decisionService.rateDecision(id,expertEvaluationDto,authentication));
    }

    @GetMapping("/{id}/evaluation")
    public ResponseEntity<ExpertEvaluation> getMyEvaluation(@PathVariable Long id, Authentication auth) {
        User user = userService.findUserByEmail(auth.getName());
        Optional<ExpertEvaluation> evaluation = expertEvaluationRepository.findByDecisionIdAndExpertId(id, user.getId());

        if (evaluation.isPresent()) {
            return ResponseEntity.ok(evaluation.get());
        }
        else return ResponseEntity.notFound().build();
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<Decision> approveDecision(@PathVariable Long id, Authentication authentication) {
        User user = userService.findUserByEmail(authentication.getName());
        Decision decision = decisionService.getDecisionById(id);

        if (!decision.getUser().equals(user)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(taskService.setSolution(decision.getTask().getId(),id).getChosenDecision());
    }

    @PostMapping("/{id}/status")
    public ResponseEntity<Void> updateStatus(
            @PathVariable Long id,
            @RequestBody StatusUpdateRequest request,
            Authentication authentication) {

        User user = userService.findUserByEmail(authentication.getName());
        boolean isAnalyst = user.getRoles().stream()
                .anyMatch(role -> "ANALYST".equals(role.getName()));

        if (!isAnalyst) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        decisionService.updateStatus(id, request.getStatus(),authentication);
        return ResponseEntity.ok().build();
    }


    @GetMapping("/admin/all")
    public ResponseEntity<List<Decision>> getAllForAdmin(Authentication authentication) {
        if (!isAdmin(authentication)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(decisionService.getAllDecisions());
    }

    @DeleteMapping("/admin/{id}")
    public ResponseEntity<Void> deleteDecisionForAdmin(@PathVariable Long id,
                                                       Authentication authentication) {
        if (!isAdmin(authentication)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        if (!decisionRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        decisionRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private boolean isAdmin(Authentication authentication) {
        User user = userService.findUserByEmail(authentication.getName());
        return user.getRoles().stream().anyMatch(role -> "ROLE_ADMIN".equals(role.getName()));
    }

}

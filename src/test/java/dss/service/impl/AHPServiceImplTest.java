package dss.service.impl;

import dss.dto.AHPAnalysisRequestDto;
import dss.dto.AHPAnalysisResultDto;
import dss.model.entity.Decision;
import dss.model.entity.DecisionParameter;
import dss.model.entity.ExpertEvaluation;
import dss.model.entity.Task;
import dss.model.entity.TaskParameter;
import dss.model.entity.enums.OptimizationDirection;
import dss.service.QuestionnaireDataService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AHPServiceImplTest {

    @Mock
    private QuestionnaireDataService questionnaireDataService;

    private AHPServiceImpl ahpService;

    @BeforeEach
    void setUp() {
        ahpService = new AHPServiceImpl(questionnaireDataService);
    }

    @Test
    void evaluateTask_shouldThrowWhenTaskHasNoParameters() {
        Task task = new Task();
        task.setTaskParameters(List.of());
        task.setDecisions(List.of());

        AHPAnalysisRequestDto requestDto = new AHPAnalysisRequestDto();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> ahpService.evaluateTask(task, requestDto));

        assertEquals("任务未配置评价指标", ex.getMessage());
    }

    @Test
    void evaluateTask_shouldThrowWhenTaskHasNoDecisions() {
        TaskParameter p1 = taskParameter(1L, "成本", "经济性", OptimizationDirection.MINIMIZE);

        Task task = new Task();
        task.setTaskParameters(List.of(p1));
        task.setDecisions(List.of());

        AHPAnalysisRequestDto requestDto = new AHPAnalysisRequestDto();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> ahpService.evaluateTask(task, requestDto));

        assertEquals("任务下不存在候选技术", ex.getMessage());
    }

    @Test
    void evaluateTask_shouldUseEqualWeightWhenMatricesMissingAndReturnStableRanking() {
        when(questionnaireDataService.getLatestDimensionAverage())
                .thenReturn(Map.of("技术成熟度", 4.2, "经济可行性", 3.8));

        TaskParameter p1 = taskParameter(1L, "性能", "技术性", OptimizationDirection.MAXIMIZE);
        TaskParameter p2 = taskParameter(2L, "成本", "技术性", OptimizationDirection.MINIMIZE);

        Decision decisionA = decision("方案A", p1, 80, p2, 30, 8.0);
        Decision decisionB = decision("方案B", p1, 100, p2, 20, 5.0);

        Task task = new Task();
        task.setTaskParameters(List.of(p1, p2));
        task.setDecisions(List.of(decisionA, decisionB));

        AHPAnalysisRequestDto requestDto = new AHPAnalysisRequestDto();
        AHPAnalysisResultDto result = ahpService.evaluateTask(task, requestDto);

        assertEquals(0.5, result.getCriteriaWeights().get("性能"), 1e-6);
        assertEquals(0.5, result.getCriteriaWeights().get("成本"), 1e-6);

        List<AHPAnalysisResultDto.RankedResultDto> ranking = result.getRanking();
        assertEquals(2, ranking.size());
        assertEquals("方案B", ranking.get(0).getDecisionTitle());
        assertEquals(1, ranking.get(0).getRank());
        assertEquals("方案A", ranking.get(1).getDecisionTitle());
        assertEquals(2, ranking.get(1).getRank());

        assertEquals(0.9, ranking.get(0).getTotalScore(), 1e-6);
        assertEquals(0.7466667, ranking.get(1).getTotalScore(), 1e-6);

        assertEquals(2, result.getQuestionnaireDimensionScores().size());
        assertTrue(result.getConsistencyByLevel().containsKey("一级指标"));
        assertTrue(result.getConsistencyByLevel().containsKey("二级指标-技术性"));
    }

    @Test
    void evaluateTask_shouldThrowWhenPrimaryMatrixDimensionMismatch() {
        when(questionnaireDataService.getLatestDimensionAverage()).thenReturn(Map.of());

        TaskParameter p1 = taskParameter(1L, "性能", "技术", OptimizationDirection.MAXIMIZE);
        TaskParameter p2 = taskParameter(2L, "成本", "经济", OptimizationDirection.MINIMIZE);
        Decision decision = decision("方案", p1, 10, p2, 10, 7.0);

        Task task = new Task();
        task.setTaskParameters(List.of(p1, p2));
        task.setDecisions(List.of(decision));

        AHPAnalysisRequestDto requestDto = new AHPAnalysisRequestDto();
        requestDto.setPrimaryMatrix(List.of(List.of(1.0)));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> ahpService.evaluateTask(task, requestDto));

        assertEquals("一级指标判断矩阵维度与一级指标数量不一致", ex.getMessage());
    }

    private TaskParameter taskParameter(Long id, String name, String parent, OptimizationDirection direction) {
        TaskParameter parameter = new TaskParameter();
        parameter.setId(id);
        parameter.setName(name);
        parameter.setParentCriterion(parent);
        parameter.setOptimizationDirection(direction);
        parameter.setWeight(0.0);
        return parameter;
    }

    private Decision decision(String title,
                              TaskParameter p1,
                              double v1,
                              TaskParameter p2,
                              double v2,
                              double expertScore) {
        Decision decision = new Decision();
        decision.setTitle(title);
        decision.setDescription(title + "描述");

        DecisionParameter dp1 = DecisionParameter.builder()
                .taskParameter(p1)
                .decision(decision)
                .value(v1)
                .build();
        DecisionParameter dp2 = DecisionParameter.builder()
                .taskParameter(p2)
                .decision(decision)
                .value(v2)
                .build();
        decision.setDecisionParameters(List.of(dp1, dp2));

        ExpertEvaluation evaluation = new ExpertEvaluation();
        evaluation.setDecision(decision);
        evaluation.setScore(expertScore);
        decision.setExpertEvaluations(List.of(evaluation));

        return decision;
    }
}

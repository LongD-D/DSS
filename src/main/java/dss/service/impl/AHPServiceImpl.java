package dss.service.impl;

import dss.dto.AHPAnalysisRequestDto;
import dss.dto.AHPAnalysisResultDto;
import dss.model.entity.Decision;
import dss.model.entity.DecisionParameter;
import dss.model.entity.ExpertEvaluation;
import dss.model.entity.Task;
import dss.model.entity.TaskParameter;
import dss.model.entity.enums.OptimizationDirection;
import dss.service.AHPService;
import dss.service.QuestionnaireDataService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class AHPServiceImpl implements AHPService {

    private static final Map<Integer, Double> RI = Map.of(
            1, 0.0, 2, 0.0, 3, 0.58, 4, 0.90, 5, 1.12,
            6, 1.24, 7, 1.32, 8, 1.41, 9, 1.45, 10, 1.49
    );

    private final QuestionnaireDataService questionnaireDataService;

    @Override
    public Map<String, Double> evaluateDecisionsAHP(Task task, double[][] comparisonMatrix) {
        AHPAnalysisRequestDto requestDto = new AHPAnalysisRequestDto();
        requestDto.setPrimaryMatrix(toList(comparisonMatrix));
        requestDto.setSecondaryMatrices(Map.of());
        AHPAnalysisResultDto result = evaluateTask(task, requestDto);

        return result.getRanking().stream()
                .collect(Collectors.toMap(
                        AHPAnalysisResultDto.RankedResultDto::getDecisionTitle,
                        AHPAnalysisResultDto.RankedResultDto::getTotalScore,
                        (a, b) -> a,
                        LinkedHashMap::new
                ));
    }

    @Override
    public AHPAnalysisResultDto evaluateTask(Task task, AHPAnalysisRequestDto requestDto) {
        List<TaskParameter> parameters = task.getTaskParameters();
        if (parameters == null || parameters.isEmpty()) {
            throw new IllegalArgumentException("任务未配置评价指标");
        }
        if (task.getDecisions() == null || task.getDecisions().isEmpty()) {
            throw new IllegalArgumentException("任务下不存在候选技术");
        }

        Hierarchy hierarchy = buildHierarchy(parameters);
        Map<String, List<List<Double>>> nodeMatrices = resolveNodeMatrices(requestDto, hierarchy);

        Map<String, Double> finalCriteriaWeights = new LinkedHashMap<>();
        Map<String, AHPAnalysisResultDto.ConsistencyResultDto> consistency = new LinkedHashMap<>();
        propagateWeights("ROOT", hierarchy.rootNodes(), 1.0, nodeMatrices, hierarchy.childrenByParent(), finalCriteriaWeights, consistency);

        ExpertAggregationResult expertAggregationResult = aggregateExpertScores(task.getDecisions());
        Map<String, Double> expertScores = expertAggregationResult.aggregatedScores;
        Map<String, Double> questionnaire = questionnaireDataService.getLatestDimensionAverage();

        boolean consistencyPassed = consistency.values().stream()
                .allMatch(AHPAnalysisResultDto.ConsistencyResultDto::isConsistent);

        if (!consistencyPassed) {
            return AHPAnalysisResultDto.builder()
                    .criteriaWeights(finalCriteriaWeights)
                    .consistencyByLevel(consistency)
                    .aggregatedExpertScores(expertScores)
                    .expertWeightDetailsByDecision(expertAggregationResult.detailsByDecision)
                    .questionnaireDimensionScores(questionnaire)
                    .ranking(List.of())
                    .status("CONSISTENCY_NOT_PASSED")
                    .rankingCalculated(false)
                    .message("一致性检验未通过（存在 CR >= 0.1 的层级），结果已保存，请修正判断矩阵后重新提交。")
                    .build();
        }

        double[][] decisionMatrix = buildDecisionMatrix(task, parameters);
        double[][] normalized = normalizeDecisionMatrix(parameters, decisionMatrix);
        double[] ahpScores = calculateScores(normalized, parameters, finalCriteriaWeights);

        List<AHPAnalysisResultDto.RankedResultDto> ranking = new ArrayList<>();
        for (int i = 0; i < task.getDecisions().size(); i++) {
            Decision decision = task.getDecisions().get(i);
            double expert = expertScores.getOrDefault(decision.getTitle(), 0.0);
            double total = ahpScores[i] * 0.8 + expert * 0.2;
            ranking.add(AHPAnalysisResultDto.RankedResultDto.builder()
                    .decisionTitle(decision.getTitle())
                    .ahpScore(ahpScores[i])
                    .expertScore(expert)
                    .totalScore(total)
                    .build());
        }

        ranking.sort(Comparator.comparingDouble(AHPAnalysisResultDto.RankedResultDto::getTotalScore).reversed());
        for (int i = 0; i < ranking.size(); i++) {
            ranking.get(i).setRank(i + 1);
        }

        return AHPAnalysisResultDto.builder()
                .criteriaWeights(finalCriteriaWeights)
                .consistencyByLevel(consistency)
                .aggregatedExpertScores(expertScores)
                .expertWeightDetailsByDecision(expertAggregationResult.detailsByDecision)
                .questionnaireDimensionScores(questionnaire)
                .ranking(ranking)
                .status("CALCULATED")
                .rankingCalculated(true)
                .message("一致性通过，已完成综合排名计算。")
                .build();
    }

    private ExpertAggregationResult aggregateExpertScores(List<Decision> decisions) {
        Map<String, Double> aggregatedScores = new LinkedHashMap<>();
        Map<String, List<AHPAnalysisResultDto.ExpertWeightDetailDto>> detailsByDecision = new LinkedHashMap<>();

        for (Decision decision : decisions) {
            if (decision.getExpertEvaluations() == null || decision.getExpertEvaluations().isEmpty()) {
                aggregatedScores.put(decision.getTitle(), 0.0);
                detailsByDecision.put(decision.getTitle(), List.of());
                continue;
            }

            List<ExpertLocalScore> expertLocalScores = decision.getExpertEvaluations().stream()
                    .map(evaluation -> {
                        double indicatorWeightedScore = evaluation.getScore() / 10.0;
                        double c = resolveExpertWeight(evaluation);
                        return new ExpertLocalScore(evaluation, indicatorWeightedScore, c);
                    })
                    .toList();

            double cSum = expertLocalScores.stream().mapToDouble(ExpertLocalScore::c).sum();
            double weightedAverage = 0.0;
            List<AHPAnalysisResultDto.ExpertWeightDetailDto> details = new ArrayList<>();

            for (ExpertLocalScore localScore : expertLocalScores) {
                double normalizedWeight = cSum > 0 ? localScore.c() / cSum : 1.0 / expertLocalScores.size();
                double contribution = localScore.indicatorWeightedScore() * normalizedWeight;
                weightedAverage += contribution;

                details.add(AHPAnalysisResultDto.ExpertWeightDetailDto.builder()
                        .expertId(localScore.evaluation().getExpert().getId())
                        .expertName(localScore.evaluation().getExpert().getName())
                        .rawScore(localScore.evaluation().getScore())
                        .indicatorWeightedScore(localScore.indicatorWeightedScore())
                        .ca(defaultZero(localScore.evaluation().getCa()))
                        .cs(defaultZero(localScore.evaluation().getCs()))
                        .c(localScore.c())
                        .normalizedExpertWeight(normalizedWeight)
                        .weightedContribution(contribution)
                        .build());
            }

            aggregatedScores.put(decision.getTitle(), weightedAverage);
            detailsByDecision.put(decision.getTitle(), details);
        }

        return new ExpertAggregationResult(aggregatedScores, detailsByDecision);
    }

    private double resolveExpertWeight(ExpertEvaluation evaluation) {
        if (evaluation.getC() != null && evaluation.getC() > 0) {
            return evaluation.getC();
        }
        if (evaluation.getCa() != null && evaluation.getCs() != null) {
            return (evaluation.getCa() + evaluation.getCs()) / 2.0;
        }
        return 1.0;
    }

    private double defaultZero(Double value) {
        return value == null ? 0.0 : value;
    }

    private MatrixResult equalWeight(int size) {
        double[] weights = new double[size];
        Arrays.fill(weights, size == 0 ? 0.0 : 1.0 / size);
        return new MatrixResult(weights, 0.0, 0.0);
    }

    private AHPAnalysisResultDto.ConsistencyResultDto toConsistency(MatrixResult result) {
        return AHPAnalysisResultDto.ConsistencyResultDto.builder()
                .ci(result.ci)
                .cr(result.cr)
                .consistent(result.cr <= 0.1)
                .build();
    }

    private MatrixResult calculateMatrixResult(List<List<Double>> listMatrix) {
        double[][] matrix = toArray(listMatrix);
        int n = matrix.length;
        if (n == 0) {
            return new MatrixResult(new double[0], 0.0, 0.0);
        }
        double[][] normalized = normalizeComparisonMatrix(matrix);
        double[] weights = calculateWeights(normalized);

        double lambdaMax = 0.0;
        for (int i = 0; i < n; i++) {
            double sum = 0.0;
            for (int j = 0; j < n; j++) {
                sum += matrix[i][j] * weights[j];
            }
            lambdaMax += sum / (weights[i] == 0 ? 1 : weights[i]);
        }
        lambdaMax /= n;

        double ci = n <= 2 ? 0.0 : (lambdaMax - n) / (n - 1);
        double ri = RI.getOrDefault(n, 1.49);
        double cr = ri == 0 ? 0.0 : ci / ri;
        return new MatrixResult(weights, ci, cr);
    }

    private double[][] toArray(List<List<Double>> listMatrix) {
        int size = listMatrix == null ? 0 : listMatrix.size();
        double[][] matrix = new double[size][size];
        for (int i = 0; i < size; i++) {
            if (listMatrix.get(i) == null || listMatrix.get(i).size() != size) {
                throw new IllegalArgumentException("判断矩阵必须是方阵");
            }
            for (int j = 0; j < size; j++) {
                Double value = listMatrix.get(i).get(j);
                matrix[i][j] = (value == null || value <= 0) ? 1.0 : value;
            }
        }
        return matrix;
    }

    private MatrixResult calculatePrimaryMatrixResult(AHPAnalysisRequestDto requestDto, int primarySize) {
        if (primarySize == 1) {
            return new MatrixResult(new double[]{1.0}, 0.0, 0.0);
        }
        if (requestDto == null || requestDto.getPrimaryMatrix() == null || requestDto.getPrimaryMatrix().isEmpty()) {
            return equalWeight(primarySize);
        }
        if (requestDto.getPrimaryMatrix().size() != primarySize) {
            throw new IllegalArgumentException("一级指标判断矩阵维度与一级指标数量不一致");
        }
        return calculateMatrixResult(requestDto.getPrimaryMatrix());
    }

    private void propagateWeights(String parentKey,
                                  List<TaskParameter> children,
                                  double parentWeight,
                                  Map<String, List<List<Double>>> matrices,
                                  Map<String, List<TaskParameter>> childrenByParent,
                                  Map<String, Double> finalLeafWeights,
                                  Map<String, AHPAnalysisResultDto.ConsistencyResultDto> consistency) {
        if (children == null || children.isEmpty()) {
            return;
        }

        MatrixResult local = evaluateLocalMatrix(parentKey, children, matrices);
        consistency.put(parentKey, toConsistency(local));

        for (int i = 0; i < children.size(); i++) {
            TaskParameter node = children.get(i);
            double propagated = parentWeight * (i < local.weights.length ? local.weights[i] : 0.0);
            List<TaskParameter> subNodes = childrenByParent.getOrDefault(node.getNodeId(), List.of());
            if (subNodes.isEmpty()) {
                finalLeafWeights.put(node.getName(), propagated);
            } else {
                propagateWeights(node.getNodeId(), subNodes, propagated, matrices, childrenByParent, finalLeafWeights, consistency);
            }
        }
    }

    private MatrixResult evaluateLocalMatrix(String parentKey,
                                             List<TaskParameter> children,
                                             Map<String, List<List<Double>>> matrices) {
        if (children.size() == 1) {
            return new MatrixResult(new double[]{1.0}, 0.0, 0.0);
        }
        List<List<Double>> matrix = matrices.get(parentKey);
        if (matrix == null || matrix.isEmpty()) {
            return equalWeight(children.size());
        }
        if (matrix.size() != children.size()) {
            throw new IllegalArgumentException("判断矩阵维度与子节点数量不一致，父节点: " + parentKey);
        }
        return calculateMatrixResult(matrix);
    }

    private Hierarchy buildHierarchy(List<TaskParameter> parameters) {
        Map<String, TaskParameter> byNodeId = parameters.stream()
                .collect(Collectors.toMap(
                        p -> resolveNodeId(p),
                        p -> p,
                        (a, b) -> a,
                        LinkedHashMap::new
                ));

        Map<String, List<TaskParameter>> childrenByParent = parameters.stream()
                .filter(p -> p.getParentId() != null && !p.getParentId().isBlank() && byNodeId.containsKey(p.getParentId()))
                .collect(Collectors.groupingBy(TaskParameter::getParentId, LinkedHashMap::new, Collectors.toList()));
        childrenByParent.values().forEach(this::sortNodes);

        List<TaskParameter> roots = parameters.stream()
                .filter(p -> p.getParentId() == null || p.getParentId().isBlank() || !byNodeId.containsKey(p.getParentId()))
                .collect(Collectors.toCollection(ArrayList::new));
        sortNodes(roots);

        return new Hierarchy(roots, childrenByParent);
    }

    private Map<String, List<List<Double>>> resolveNodeMatrices(AHPAnalysisRequestDto requestDto, Hierarchy hierarchy) {
        if (requestDto == null) {
            return Map.of();
        }
        if (requestDto.getNodeMatrices() != null && !requestDto.getNodeMatrices().isEmpty()) {
            return requestDto.getNodeMatrices();
        }

        Map<String, List<List<Double>>> converted = new LinkedHashMap<>();
        if (requestDto.getPrimaryMatrix() != null && !requestDto.getPrimaryMatrix().isEmpty()) {
            converted.put("ROOT", requestDto.getPrimaryMatrix());
        }
        if (requestDto.getSecondaryMatrices() != null && !requestDto.getSecondaryMatrices().isEmpty()) {
            Map<String, TaskParameter> rootByName = hierarchy.rootNodes().stream()
                    .collect(Collectors.toMap(TaskParameter::getName, p -> p, (a, b) -> a, LinkedHashMap::new));
            requestDto.getSecondaryMatrices().forEach((parentName, matrix) -> {
                TaskParameter parent = rootByName.get(parentName);
                if (parent != null) {
                    converted.put(resolveNodeId(parent), matrix);
                }
            });
        }
        return converted;
    }

    private String resolveNodeId(TaskParameter node) {
        return (node.getNodeId() == null || node.getNodeId().isBlank())
                ? "PARAM-" + node.getId()
                : node.getNodeId();
    }

    private void sortNodes(List<TaskParameter> nodes) {
        nodes.sort(Comparator.comparingInt(p -> Optional.ofNullable(p.getSortOrder()).orElse(0)));
    }

    private List<List<Double>> toList(double[][] matrix) {
        List<List<Double>> result = new ArrayList<>();
        for (double[] row : matrix) {
            List<Double> r = new ArrayList<>();
            for (double v : row) {
                r.add(v);
            }
            result.add(r);
        }
        return result;
    }

    private double[][] buildDecisionMatrix(Task task, List<TaskParameter> parameters) {
        List<Decision> decisions = task.getDecisions();
        double[][] matrix = new double[decisions.size()][parameters.size()];

        for (int i = 0; i < decisions.size(); i++) {
            List<DecisionParameter> params = decisions.get(i).getDecisionParameters();
            Map<Long, Double> paramMap = params.stream().collect(Collectors.toMap(
                    p -> p.getTaskParameter().getId(),
                    DecisionParameter::getValue,
                    (a, b) -> a,
                    LinkedHashMap::new
            ));
            for (int j = 0; j < parameters.size(); j++) {
                matrix[i][j] = paramMap.getOrDefault(parameters.get(j).getId(), 0.0);
            }
        }
        return matrix;
    }

    private double[][] normalizeComparisonMatrix(double[][] matrix) {
        int n = matrix.length;
        double[] colSum = new double[n];

        for (int j = 0; j < n; j++) {
            for (int i = 0; i < n; i++) {
                colSum[j] += matrix[i][j];
            }
        }

        double[][] normalized = new double[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                normalized[i][j] = matrix[i][j] / (colSum[j] == 0 ? 1 : colSum[j]);
            }
        }

        return normalized;
    }

    private double[] calculateWeights(double[][] normalizedMatrix) {
        int rows = normalizedMatrix.length;
        int cols = normalizedMatrix[0].length;

        double[] weights = new double[rows];
        for (int i = 0; i < rows; i++) {
            double sum = 0;
            for (int j = 0; j < cols; j++) {
                sum += normalizedMatrix[i][j];
            }
            weights[i] = sum / cols;
        }
        return weights;
    }

    private double[][] normalizeDecisionMatrix(List<TaskParameter> parameters, double[][] matrix) {
        int rows = matrix.length;
        int cols = matrix[0].length;
        double[][] normalized = new double[rows][cols];

        for (int j = 0; j < cols; j++) {
            OptimizationDirection direction = parameters.get(j).getOptimizationDirection();
            int col = j;
            double extreme = direction == OptimizationDirection.MAXIMIZE
                    ? Arrays.stream(matrix).mapToDouble(row -> row[col]).max().orElse(1)
                    : Arrays.stream(matrix).mapToDouble(row -> row[col]).min().orElse(1);

            for (int i = 0; i < rows; i++) {
                normalized[i][j] = direction == OptimizationDirection.MAXIMIZE
                        ? matrix[i][j] / (extreme == 0 ? 1 : extreme)
                        : (extreme == 0 ? 1 : extreme) / (matrix[i][j] == 0 ? 1 : matrix[i][j]);
            }
        }

        return normalized;
    }

    private double[] calculateScores(double[][] matrix, List<TaskParameter> parameters, Map<String, Double> weights) {
        double[] scores = new double[matrix.length];

        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix[i].length; j++) {
                double weight = weights.getOrDefault(parameters.get(j).getName(), 0.0);
                scores[i] += matrix[i][j] * weight;
            }
        }

        return scores;
    }

    private record MatrixResult(double[] weights, double ci, double cr) {
    }

    private record ExpertLocalScore(ExpertEvaluation evaluation, double indicatorWeightedScore, double c) {
    }

    private record ExpertAggregationResult(Map<String, Double> aggregatedScores,
                                           Map<String, List<AHPAnalysisResultDto.ExpertWeightDetailDto>> detailsByDecision) {
    }

    private record Hierarchy(List<TaskParameter> rootNodes, Map<String, List<TaskParameter>> childrenByParent) {
    }
}

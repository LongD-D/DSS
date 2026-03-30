package dss.service.impl;

import dss.dto.AHPAnalysisRequestDto;
import dss.dto.AHPAnalysisResultDto;
import dss.model.entity.Decision;
import dss.model.entity.DecisionParameter;
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
        Map<String, List<TaskParameter>> grouped = parameters.stream()
                .collect(Collectors.groupingBy(
                        p -> Optional.ofNullable(p.getParentCriterion()).orElse("默认一级指标"),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        MatrixResult primaryResult = calculateMatrixResult(requestDto.getPrimaryMatrix());
        List<String> primaryNames = new ArrayList<>(grouped.keySet());

        Map<String, Double> finalCriteriaWeights = new LinkedHashMap<>();
        Map<String, AHPAnalysisResultDto.ConsistencyResultDto> consistency = new LinkedHashMap<>();
        consistency.put("一级指标", toConsistency(primaryResult));

        for (int i = 0; i < primaryNames.size(); i++) {
            String parent = primaryNames.get(i);
            List<TaskParameter> children = grouped.get(parent);
            List<List<Double>> matrix = requestDto.getSecondaryMatrices() == null
                    ? null : requestDto.getSecondaryMatrices().get(parent);

            MatrixResult secondaryResult = (matrix == null || matrix.isEmpty())
                    ? equalWeight(children.size())
                    : calculateMatrixResult(matrix);
            consistency.put("二级指标-" + parent, toConsistency(secondaryResult));

            double primaryWeight = i < primaryResult.weights.length ? primaryResult.weights[i] : 0.0;
            for (int j = 0; j < children.size(); j++) {
                TaskParameter parameter = children.get(j);
                double childWeight = j < secondaryResult.weights.length ? secondaryResult.weights[j] : 0.0;
                finalCriteriaWeights.put(parameter.getName(), primaryWeight * childWeight);
            }
        }

        double[][] decisionMatrix = buildDecisionMatrix(task, parameters);
        double[][] normalized = normalizeDecisionMatrix(parameters, decisionMatrix);
        double[] ahpScores = calculateScores(normalized, parameters, finalCriteriaWeights);

        Map<String, Double> expertScores = aggregateExpertScores(task.getDecisions());
        Map<String, Double> questionnaire = questionnaireDataService.getLatestDimensionAverage();

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
                .questionnaireDimensionScores(questionnaire)
                .ranking(ranking)
                .build();
    }

    private Map<String, Double> aggregateExpertScores(List<Decision> decisions) {
        Map<String, Double> expert = new LinkedHashMap<>();
        for (Decision decision : decisions) {
            expert.put(decision.getTitle(), decision.getRate() / 10.0);
        }
        return expert;
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
            for (int j = 0; j < size; j++) {
                Double value = listMatrix.get(i).get(j);
                matrix[i][j] = (value == null || value <= 0) ? 1.0 : value;
            }
        }
        return matrix;
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
}

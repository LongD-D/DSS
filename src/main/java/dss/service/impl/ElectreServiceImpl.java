package dss.service.impl;

import dss.model.entity.Decision;
import dss.model.entity.DecisionParameter;
import dss.model.entity.Task;
import dss.model.entity.TaskParameter;
import dss.model.entity.enums.OptimizationDirection;
import dss.service.ElectreService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@AllArgsConstructor
public class ElectreServiceImpl implements ElectreService {

    private static final double DEFAULT_C_THRESHOLD = 0.4;
    private static final double DEFAULT_D_THRESHOLD = 0.6;

    @Override
    public Map<String, Double> evaluateDecisionsELECTRE(Task task, double cThreshold, double dThreshold) {
        List<Decision> decisions = task.getDecisions();
        List<TaskParameter> criteria = task.getTaskParameters();

        int m = decisions.size();
        int n = criteria.size();

        double[][] matrix = new double[m][n];
        for (int i = 0; i < m; i++) {
            List<DecisionParameter> params = decisions.get(i).getDecisionParameters();
            for (int j = 0; j < n; j++) {
                matrix[i][j] = params.get(j).getValue();
            }
        }

        double[] weights = criteria.stream().mapToDouble(TaskParameter::getWeight).toArray();
        boolean[] isBenefit = new boolean[n];
        for (int i = 0; i < n; i++) {
            OptimizationDirection dir = criteria.get(i).getOptimizationDirection();
            isBenefit[i] = dir == null || dir == OptimizationDirection.MAXIMIZE;
        }

        System.out.println("\n决策矩阵:");
        printMatrix(matrix);

        double[][] concordance = calculateConcordance(matrix, weights, isBenefit);
        double[][] discordance = calculateDiscordance(matrix, isBenefit);

        System.out.println("\n一致性矩阵 C:");
        printMatrix(concordance);
        System.out.println("\n不一致矩阵 D:");
        printMatrix(discordance);

        System.out.println("\n阈值 C: " + cThreshold);
        System.out.println("阈值 D: " + dThreshold);

        boolean[][] dominance = new boolean[m][m];
        for (int a = 0; a < m; a++) {
            for (int b = 0; b < m; b++) {
                if (a == b) continue;
                if (concordance[a][b] >= cThreshold && discordance[a][b] <= dThreshold) {
                    dominance[a][b] = true;
                }
            }
        }

        System.out.println("\n支配矩阵:");
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < m; j++) {
                System.out.print((dominance[i][j] ? "1" : "0") + "\t");
            }
            System.out.println();
        }

        Set<Integer> kernel = new HashSet<>();
        for (int a = 0; a < m; a++) {
            boolean isDominated = false;
            for (int b = 0; b < m; b++) {
                if (dominance[b][a]) {
                    isDominated = true;
                    break;
                }
            }
            if (!isDominated) kernel.add(a);
        }

        Map<String, Double> result = new LinkedHashMap<>();
        for (int i = 0; i < m; i++) {
            result.put(decisions.get(i).getTitle(), kernel.contains(i) ? 1.0 : 0.0);
        }

        System.out.println("\n结果:");
        result.forEach((k, v) -> System.out.println(k + " = " + v));

        return result;
    }

    private double[][] calculateConcordance(double[][] matrix, double[] weights, boolean[] isBenefit) {
        int m = matrix.length;
        int n = matrix[0].length;
        double[][] concordance = new double[m][m];

        for (int a = 0; a < m; a++) {
            for (int b = 0; b < m; b++) {
                if (a == b) continue;
                double sum = 0.0;
                double weightSum = Arrays.stream(weights).sum();
                for (int j = 0; j < n; j++) {
                    boolean condition = isBenefit[j] ? matrix[a][j] >= matrix[b][j] : matrix[a][j] <= matrix[b][j];
                    if (condition) sum += weights[j];
                }
                concordance[a][b] = sum / weightSum;
            }
        }
        return concordance;
    }

    private double[][] calculateDiscordance(double[][] matrix, boolean[] isBenefit) {
        int m = matrix.length;
        int n = matrix[0].length;
        double[][] discordance = new double[m][m];

        for (int a = 0; a < m; a++) {
            for (int b = 0; b < m; b++) {
                if (a == b) continue;
                double maxDiff = 0;
                double denom = 0;
                for (int j = 0; j < n; j++) {
                    denom = Math.max(denom, Math.abs(matrix[a][j] - matrix[b][j]));
                }
                for (int j = 0; j < n; j++) {
                    boolean condition = isBenefit[j] ? matrix[a][j] < matrix[b][j] : matrix[a][j] > matrix[b][j];
                    if (condition) {
                        maxDiff = Math.max(maxDiff, Math.abs(matrix[a][j] - matrix[b][j]));
                    }
                }
                discordance[a][b] = denom == 0 ? 0 : maxDiff / denom;
            }
        }
        return discordance;
    }

    private double calculateAverage(double[][] matrix) {
        int m = matrix.length;
        double sum = 0;
        int count = 0;
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < m; j++) {
                if (i != j) {
                    sum += matrix[i][j];
                    count++;
                }
            }
        }
        return count == 0 ? 0.5 : sum / count;
    }

    private void printMatrix(double[][] matrix) {
        for (double[] row : matrix) {
            for (double val : row) {
                System.out.print(val + "\t");
            }
            System.out.println();
        }
    }
}

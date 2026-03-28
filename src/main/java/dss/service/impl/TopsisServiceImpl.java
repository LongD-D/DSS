package dss.service.impl;

import dss.model.entity.Decision;
import dss.model.entity.DecisionParameter;
import dss.model.entity.Task;
import dss.model.entity.TaskParameter;
import dss.model.entity.enums.OptimizationDirection;
import dss.service.TopsisService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@AllArgsConstructor
public class TopsisServiceImpl implements TopsisService {

    @Override
    public Map<String, Double> evaluateDecisionsTOPSIS(Task task, double[] weights) {
        List<Decision> decisions = task.getDecisions();
        List<TaskParameter> parameters = task.getTaskParameters();

        int m = decisions.size();
        int n = parameters.size();

        double[][] decisionMatrix = new double[m][n];

        for (int i = 0; i < m; i++) {
            List<DecisionParameter> params = decisions.get(i).getDecisionParameters();
            for (int j = 0; j < n; j++) {
                decisionMatrix[i][j] = params.get(j).getValue();
            }
        }

        System.out.println("--- Decision Matrix ---");
        printMatrix(decisionMatrix);

        // 归一化
        double[][] normalizedMatrix = new double[m][n];
        for (int j = 0; j < n; j++) {
            double sumSquares = 0.0;
            for (int i = 0; i < m; i++) {
                sumSquares += Math.pow(decisionMatrix[i][j], 2);
            }
            double norm = Math.sqrt(sumSquares);
            for (int i = 0; i < m; i++) {
                normalizedMatrix[i][j] = decisionMatrix[i][j] / (norm == 0 ? 1 : norm);
            }
        }

        System.out.println("--- Normalized Matrix ---");
        printMatrix(normalizedMatrix);

        // 加权归一化矩阵
        double[][] weightedMatrix = new double[m][n];
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                weightedMatrix[i][j] = normalizedMatrix[i][j] * weights[j];
            }
        }

        System.out.println("--- Weighted Matrix ---");
        printMatrix(weightedMatrix);

        // 根据 OptimizationDirection 计算理想解与负理想解
        double[] ideal = new double[n];
        double[] antiIdeal = new double[n];
        for (int j = 0; j < n; j++) {
            List<Double> values = new ArrayList<>();
            for (int i = 0; i < m; i++) {
                values.add(weightedMatrix[i][j]);
            }

            OptimizationDirection direction = parameters.get(j).getOptimizationDirection();
            if (direction == OptimizationDirection.MAXIMIZE) {
                ideal[j] = Collections.max(values);
                antiIdeal[j] = Collections.min(values);
            } else {
                ideal[j] = Collections.min(values);
                antiIdeal[j] = Collections.max(values);
            }
        }

        // 距离
        double[] distanceToIdeal = new double[m];
        double[] distanceToAntiIdeal = new double[m];
        for (int i = 0; i < m; i++) {
            double dPlus = 0.0, dMinus = 0.0;
            for (int j = 0; j < n; j++) {
                dPlus += Math.pow(weightedMatrix[i][j] - ideal[j], 2);
                dMinus += Math.pow(weightedMatrix[i][j] - antiIdeal[j], 2);
            }
            distanceToIdeal[i] = Math.sqrt(dPlus);
            distanceToAntiIdeal[i] = Math.sqrt(dMinus);
        }

        // 贴近系数
        Map<String, Double> result = new LinkedHashMap<>();
        for (int i = 0; i < m; i++) {
            double closeness = distanceToAntiIdeal[i] / (distanceToIdeal[i] + distanceToAntiIdeal[i]);
            result.put(decisions.get(i).getTitle(), closeness);
        }

        System.out.println("--- TOPSIS Results ---");
        result.forEach((k, v) -> System.out.println(k + ": " + v));

        return result;
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

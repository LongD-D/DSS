package dss.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AHPAnalysisResultDto {
    private Map<String, Double> criteriaWeights;
    private Map<String, ConsistencyResultDto> consistencyByLevel;
    private Map<String, Double> aggregatedExpertScores;
    private Map<String, Double> questionnaireDimensionScores;
    private List<RankedResultDto> ranking;
    /**
     * CALCULATED: 一致性通过并已计算排名
     * CONSISTENCY_NOT_PASSED: 一致性未通过，仅返回检验与权重结果
     */
    private String status;
    private boolean rankingCalculated;
    private String message;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConsistencyResultDto {
        private double ci;
        private double cr;
        private boolean consistent;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RankedResultDto {
        private String decisionTitle;
        private double ahpScore;
        private double expertScore;
        private double totalScore;
        private int rank;
    }
}

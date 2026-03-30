package dss.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class AHPAnalysisRequestDto {
    private List<List<Double>> primaryMatrix;
    private Map<String, List<List<Double>>> secondaryMatrices;
}

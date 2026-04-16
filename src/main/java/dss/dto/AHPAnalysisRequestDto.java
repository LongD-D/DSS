package dss.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class AHPAnalysisRequestDto {
    /**
     * 兼容旧版：一级矩阵（根节点）
     */
    private List<List<Double>> primaryMatrix;
    /**
     * 兼容旧版：二级矩阵（以父名称分组）
     */
    private Map<String, List<List<Double>>> secondaryMatrices;
    /**
     * 新版：以父节点ID提交子节点判断矩阵。
     * ROOT 表示根层（所有顶级节点）。
     */
    private Map<String, List<List<Double>>> nodeMatrices;
}

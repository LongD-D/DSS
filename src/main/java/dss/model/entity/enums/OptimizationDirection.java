package dss.model.entity.enums;

public enum OptimizationDirection {
    MINIMIZE("最小化"),
    MAXIMIZE("最大化");

    private final String label;

    OptimizationDirection(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
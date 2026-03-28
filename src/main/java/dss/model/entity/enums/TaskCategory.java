package dss.model.entity.enums;

public enum TaskCategory {
    ENERGETIC("能源"),
    ECONOMY("经济"),
    ECOLOGY("生态");

    private final String ukrainianLabel;

    TaskCategory(String ukrainianLabel) {
        this.ukrainianLabel = ukrainianLabel;
    }

    public String getLabel() {
        return ukrainianLabel;
    }
}

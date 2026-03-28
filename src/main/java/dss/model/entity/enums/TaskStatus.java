package dss.model.entity.enums;

public enum TaskStatus {
    NEW("新建"),
    SOLVED("已解决"),
    EMERGENCY("紧急");

    private final String ukrainianLabel;

    TaskStatus(String ukrainianLabel) {
        this.ukrainianLabel = ukrainianLabel;
    }

    public String getLabel() {
        return ukrainianLabel;
    }
}

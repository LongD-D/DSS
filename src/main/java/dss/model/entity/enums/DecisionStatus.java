package dss.model.entity.enums;

public enum DecisionStatus {
    PROPOSED("已提议"),
    APPROVED("已批准"),
    REJECTED("已拒绝");

    private final String ukrName;

    DecisionStatus(String ukrName) {
        this.ukrName = ukrName;
    }

    public String getUkrName() {
        return ukrName;
    }
}
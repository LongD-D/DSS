package dss.model.entity.enums;

public enum DecisionCategory {
    ECOLOGY("生态"),
    ENERGY("能源"),
    ECONOMY("经济");

    private final String ukrName;

    DecisionCategory(String ukrName) {
        this.ukrName = ukrName;
    }

    public String getUkrName() {

        return ukrName;
    }
}
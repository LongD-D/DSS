package dss.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DecisionImportErrorDto {
    private int row;
    private String reason;
}

package dss.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class DecisionImportResponseDto {
    private String message;
    private int successCount;
    private int failureCount;
    private List<DecisionImportErrorDto> errors;
}

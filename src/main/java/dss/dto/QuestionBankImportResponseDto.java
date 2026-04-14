package dss.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class QuestionBankImportResponseDto {
    private String message;
    private QuestionBankSummaryDto bank;
}

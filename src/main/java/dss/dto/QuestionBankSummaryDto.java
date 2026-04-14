package dss.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class QuestionBankSummaryDto {
    private Long id;
    private String name;
    private String sourceFileName;
    private Integer questionCount;
    private LocalDateTime importedAt;
}

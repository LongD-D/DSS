package dss.service;

import dss.dto.QuestionnaireAnswerInputDto;
import dss.dto.QuestionnaireSubmissionDto;
import dss.model.entity.User;

import dss.dto.QuestionBankSummaryDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface QuestionnaireDataService {
    void recordSubmission(User user, double overallAverage, List<QuestionnaireAnswerInputDto> answers, Map<String, Double> dimensionAverage);

    Map<String, Double> getLatestDimensionAverage();

    List<QuestionnaireSubmissionDto> getSubmissionHistory();

    void clearSubmissionHistory();
    List<QuestionBankSummaryDto> getAllQuestionBanks();

    QuestionBankSummaryDto importQuestionBank(MultipartFile file);

}

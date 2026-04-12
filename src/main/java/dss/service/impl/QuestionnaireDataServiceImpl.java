package dss.service.impl;

import dss.dto.QuestionnaireAnswerInputDto;
import dss.dto.QuestionnaireSubmissionDto;
import dss.model.entity.QuestionnaireAnswer;
import dss.model.entity.QuestionnaireDimensionScore;
import dss.model.entity.QuestionnaireSubmission;
import dss.model.entity.User;
import dss.repository.QuestionnaireSubmissionRepository;
import dss.service.QuestionnaireDataService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class QuestionnaireDataServiceImpl implements QuestionnaireDataService {

    private final QuestionnaireSubmissionRepository submissionRepository;

    @Override
    @Transactional
    public void recordSubmission(User user,
                                 double overallAverage,
                                 List<QuestionnaireAnswerInputDto> answers,
                                 Map<String, Double> dimensionAverage) {
        QuestionnaireSubmission submission = QuestionnaireSubmission.builder()
                .user(user)
                .submittedAt(LocalDateTime.now())
                .overallAverage(overallAverage)
                .build();

        List<QuestionnaireAnswer> answerEntities = answers.stream()
                .map(answer -> QuestionnaireAnswer.builder()
                        .submission(submission)
                        .questionId(answer.getQuestionId())
                        .dimension(answer.getDimension())
                        .questionText(answer.getQuestionText())
                        .score(answer.getScore())
                        .build())
                .toList();

        List<QuestionnaireDimensionScore> dimensionEntities = dimensionAverage.entrySet().stream()
                .map(entry -> QuestionnaireDimensionScore.builder()
                        .submission(submission)
                        .dimension(entry.getKey())
                        .averageScore(entry.getValue())
                        .build())
                .toList();

        submission.setAnswers(answerEntities);
        submission.setDimensionScores(dimensionEntities);
        submissionRepository.save(submission);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Double> getLatestDimensionAverage() {
        return submissionRepository.findTopByOrderBySubmittedAtDesc()
                .map(this::toDimensionMap)
                .orElseGet(LinkedHashMap::new);
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuestionnaireSubmissionDto> getSubmissionHistory() {
        return submissionRepository.findAllByOrderBySubmittedAtDesc().stream()
                .map(submission -> new QuestionnaireSubmissionDto(
                        submission.getSubmittedAt(),
                        toDimensionMap(submission)
                ))
                .toList();
    }

    @Override
    @Transactional
    public void clearSubmissionHistory() {
        submissionRepository.deleteAll();
    }

    private Map<String, Double> toDimensionMap(QuestionnaireSubmission submission) {
        return submission.getDimensionScores().stream()
                .collect(Collectors.toMap(
                        QuestionnaireDimensionScore::getDimension,
                        QuestionnaireDimensionScore::getAverageScore,
                        (a, b) -> a,
                        LinkedHashMap::new
                ));
    }
}

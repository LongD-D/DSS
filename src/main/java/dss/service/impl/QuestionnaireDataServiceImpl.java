package dss.service.impl;

import dss.dto.QuestionBankSummaryDto;
import dss.dto.QuestionnaireAnswerInputDto;
import dss.dto.QuestionnaireSubmissionDto;
import dss.model.entity.QuestionnaireAnswer;
import dss.model.entity.QuestionnaireDimensionScore;
import dss.model.entity.QuestionnaireQuestion;
import dss.model.entity.QuestionnaireQuestionBank;
import dss.model.entity.QuestionnaireSubmission;
import dss.model.entity.User;
import dss.repository.QuestionnaireQuestionBankRepository;
import dss.repository.QuestionnaireSubmissionRepository;
import dss.service.QuestionnaireDataService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class QuestionnaireDataServiceImpl implements QuestionnaireDataService {

    private static final String REQUIRED_CSV_HEADER = "dimension,question_text";

    private final QuestionnaireSubmissionRepository submissionRepository;
    private final QuestionnaireQuestionBankRepository questionBankRepository;

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

    @Override
    @Transactional(readOnly = true)
    public List<QuestionBankSummaryDto> getAllQuestionBanks() {
        return questionBankRepository.findAllByOrderByImportedAtDesc().stream()
                .map(this::toBankSummary)
                .toList();
    }

    @Override
    @Transactional
    public QuestionBankSummaryDto importQuestionBank(MultipartFile file) {
        validateCsvFile(file);

        String sourceFileName = file.getOriginalFilename() == null ? "question-bank.csv" : file.getOriginalFilename();
        String bankName = sourceFileName.replaceFirst("\\.[^.]+$", "") + "-" + LocalDateTime.now();

        List<QuestionnaireQuestion> questions = parseCsvQuestions(file);
        if (questions.isEmpty()) {
            throw new IllegalArgumentException("CSV 中未包含有效题目。请至少提供一行题目数据。");
        }

        QuestionnaireQuestionBank bank = QuestionnaireQuestionBank.builder()
                .name(bankName)
                .sourceFileName(sourceFileName)
                .importedAt(LocalDateTime.now())
                .questionCount(questions.size())
                .build();

        questions.forEach(question -> question.setBank(bank));
        bank.setQuestions(questions);

        QuestionnaireQuestionBank saved = questionBankRepository.save(bank);
        return toBankSummary(saved);
    }

    private void validateCsvFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("请上传 CSV 文件。");
        }

        String fileName = file.getOriginalFilename();
        if (fileName == null || !fileName.toLowerCase().endsWith(".csv")) {
            throw new IllegalArgumentException("仅支持 .csv 格式文件。");
        }
    }

    private List<QuestionnaireQuestion> parseCsvQuestions(MultipartFile file) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null || !REQUIRED_CSV_HEADER.equals(headerLine.trim().toLowerCase())) {
                throw new IllegalArgumentException("CSV 表头必须固定为: " + REQUIRED_CSV_HEADER);
            }

            List<QuestionnaireQuestion> questions = new ArrayList<>();
            String line;
            int lineNo = 1;
            while ((line = reader.readLine()) != null) {
                lineNo++;
                if (line.isBlank()) {
                    continue;
                }

                String[] parts = line.split(",", 2);
                if (parts.length != 2) {
                    throw new IllegalArgumentException("CSV 第 " + lineNo + " 行格式错误，必须包含 dimension,question_text 两列。");
                }

                String dimension = trimQuote(parts[0]);
                String questionText = trimQuote(parts[1]);
                if (dimension.isBlank() || questionText.isBlank()) {
                    throw new IllegalArgumentException("CSV 第 " + lineNo + " 行存在空值，请检查维度与题目文本。");
                }

                questions.add(QuestionnaireQuestion.builder()
                        .dimension(dimension)
                        .questionText(questionText)
                        .build());
            }

            return questions;
        } catch (IOException ex) {
            throw new IllegalArgumentException("读取 CSV 文件失败，请重试。", ex);
        }
    }

    private String trimQuote(String value) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.startsWith("\"") && trimmed.endsWith("\"") && trimmed.length() >= 2) {
            return trimmed.substring(1, trimmed.length() - 1).trim();
        }
        return trimmed;
    }

    private QuestionBankSummaryDto toBankSummary(QuestionnaireQuestionBank bank) {
        return new QuestionBankSummaryDto(
                bank.getId(),
                bank.getName(),
                bank.getSourceFileName(),
                bank.getQuestionCount(),
                bank.getImportedAt()
        );
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

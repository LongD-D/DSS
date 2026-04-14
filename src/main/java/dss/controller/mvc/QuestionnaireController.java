package dss.controller.mvc;

import dss.dto.QuestionnaireAnswerInputDto;
import dss.model.entity.QuestionnaireQuestion;
import dss.model.entity.User;
import dss.security.jwt.JwtService;
import dss.service.QuestionnaireDataService;
import dss.service.UserService;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import io.jsonwebtoken.JwtException;

@Controller
@AllArgsConstructor
public class QuestionnaireController {

    private final QuestionnaireDataService questionnaireDataService;
    private final UserService userService;
    private final JwtService jwtService;

    private static final int DEFAULT_DRAW_COUNT = 8;

    @GetMapping("/questionnaire")
    public String questionnaire(@RequestParam(value = "bankId", required = false) Long bankId, Model model) {
        List<QuestionItem> selectedQuestions = drawRandomQuestions(DEFAULT_DRAW_COUNT, bankId);
        model.addAttribute("questions", selectedQuestions);
        model.addAttribute("scoreOptions", List.of(1, 2, 3, 4, 5));
        if (selectedQuestions.isEmpty()) {
            model.addAttribute("errorMessage", "暂无可用题库数据，请管理员先导入题库CSV。");
        }
        return "questionnaire/questionnaire";
    }

    @PostMapping("/questionnaire/submit")
    public String submit(@RequestParam Map<String, String> formData, Model model, Authentication authentication) {
        User submitter = resolveSubmitter(authentication, formData.get("token"));
        if (submitter == null) {
            model.addAttribute("errorMessage", "提交失败：请先登录后再提交问卷。");
            model.addAttribute("questions", drawRandomQuestions(DEFAULT_DRAW_COUNT, null));
            model.addAttribute("scoreOptions", List.of(1, 2, 3, 4, 5));
            return "questionnaire/questionnaire";
        }

        List<QuestionAnswerResult> answers = new ArrayList<>();

        for (Map.Entry<String, String> entry : formData.entrySet()) {
            if (!entry.getKey().startsWith("score_")) {
                continue;
            }

            Long questionId = Long.parseLong(entry.getKey().substring("score_".length()));
            Integer score = Integer.parseInt(entry.getValue());
            QuestionItem question = questionnaireDataService.findQuestionById(questionId)
                    .map(this::toQuestionItem)
                    .orElse(null);

            if (question != null) {
                answers.add(new QuestionAnswerResult(question.getId(), question.getDimension(), question.getText(), score));
            }
        }

        if (answers.isEmpty()) {
            model.addAttribute("errorMessage", "提交失败：未找到有效题目数据，请刷新问卷后重试。");
            model.addAttribute("questions", drawRandomQuestions(DEFAULT_DRAW_COUNT, null));
            model.addAttribute("scoreOptions", List.of(1, 2, 3, 4, 5));
            return "questionnaire/questionnaire";
        }

        answers.sort(Comparator.comparing(QuestionAnswerResult::getDimension));

        double overallAverage = answers.stream()
                .mapToInt(QuestionAnswerResult::getScore)
                .average()
                .orElse(0.0);

        Map<String, Double> dimensionAverage = answers.stream()
                .collect(Collectors.groupingBy(
                        QuestionAnswerResult::getDimension,
                        LinkedHashMap::new,
                        Collectors.averagingInt(QuestionAnswerResult::getScore)
                ));

        var answersForStore = answers.stream()
                .map(answer -> new QuestionnaireAnswerInputDto(
                        answer.getQuestionId(),
                        answer.getDimension(),
                        answer.getQuestionText(),
                        answer.getScore()
                ))
                .toList();

        questionnaireDataService.recordSubmission(
                submitter,
                overallAverage,
                answersForStore,
                dimensionAverage
        );

        model.addAttribute("overallAverage", overallAverage);
        model.addAttribute("dimensionAverage", dimensionAverage);
        model.addAttribute("answers", answers);

        return "questionnaire/result";
    }

    private User resolveSubmitter(Authentication authentication, String rawToken) {
        if (authentication != null && authentication.getName() != null
                && !"anonymousUser".equals(authentication.getName())) {
            User user = userService.findUserByEmail(authentication.getName());
            if (user != null) {
                return user;
            }
        }

        if (rawToken == null || rawToken.isBlank()) {
            return null;
        }

        try {
            String email = jwtService.extractUsername(rawToken);
            return userService.findUserByEmail(email);
        } catch (JwtException | IllegalArgumentException ex) {
            return null;
        }
    }

    private List<QuestionItem> drawRandomQuestions(int count, Long selectedBankId) {
        List<QuestionItem> shuffled = questionnaireDataService.getEffectiveQuestions(Optional.ofNullable(selectedBankId))
                .stream()
                .map(this::toQuestionItem)
                .collect(Collectors.toCollection(ArrayList::new));

        for (int i = shuffled.size() - 1; i > 0; i--) {
            int j = ThreadLocalRandom.current().nextInt(i + 1);
            QuestionItem temp = shuffled.get(i);
            shuffled.set(i, shuffled.get(j));
            shuffled.set(j, temp);
        }
        return shuffled.subList(0, Math.min(count, shuffled.size()));
    }

    private QuestionItem toQuestionItem(QuestionnaireQuestion question) {
        return new QuestionItem(question.getId(), question.getDimension(), question.getQuestionText());
    }

    @Getter
    @AllArgsConstructor
    private static class QuestionItem {
        private Long id;
        private String dimension;
        private String text;
    }

    @Getter
    @AllArgsConstructor
    private static class QuestionAnswerResult {
        private Long questionId;
        private String dimension;
        private String questionText;
        private Integer score;
    }
}

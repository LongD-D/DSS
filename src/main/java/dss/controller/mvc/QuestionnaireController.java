package dss.controller.mvc;

import lombok.AllArgsConstructor;
import lombok.Getter;
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
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Controller
public class QuestionnaireController {

    private static final int DEFAULT_DRAW_COUNT = 8;

    private static final List<QuestionItem> QUESTION_BANK = List.of(
            new QuestionItem(1L, "技术成熟度", "该前沿技术在未来3年内具备可落地能力。"),
            new QuestionItem(2L, "技术成熟度", "该技术在现有产业链中具备较好的集成可行性。"),
            new QuestionItem(3L, "技术成熟度", "该技术关键组件的稳定性和可靠性较高。"),
            new QuestionItem(4L, "经济性", "该技术的初始投入在可接受范围内。"),
            new QuestionItem(5L, "经济性", "该技术在全生命周期内具有较高性价比。"),
            new QuestionItem(6L, "经济性", "该技术具备明确的商业化收益预期。"),
            new QuestionItem(7L, "生态影响", "该技术有助于降低资源消耗和污染排放。"),
            new QuestionItem(8L, "生态影响", "该技术在环保法规下具有明显优势。"),
            new QuestionItem(9L, "生态影响", "该技术推广后可能带来的生态风险可控。"),
            new QuestionItem(10L, "法规适配", "该技术与现行政策/法规总体兼容。"),
            new QuestionItem(11L, "法规适配", "该技术在合规审批流程中预期阻力较小。"),
            new QuestionItem(12L, "法规适配", "针对该技术的行业标准可在短期内形成。")
    );

    @GetMapping("/questionnaire")
    public String questionnaire(Model model) {
        List<QuestionItem> selectedQuestions = drawRandomQuestions(DEFAULT_DRAW_COUNT);
        model.addAttribute("questions", selectedQuestions);
        model.addAttribute("scoreOptions", List.of(1, 2, 3, 4, 5));
        return "questionnaire/questionnaire";
    }

    @PostMapping("/questionnaire/submit")
    public String submit(@RequestParam Map<String, String> formData, Model model) {
        List<QuestionAnswerResult> answers = new ArrayList<>();

        for (Map.Entry<String, String> entry : formData.entrySet()) {
            if (!entry.getKey().startsWith("score_")) {
                continue;
            }

            Long questionId = Long.parseLong(entry.getKey().substring("score_".length()));
            Integer score = Integer.parseInt(entry.getValue());
            QuestionItem question = findQuestion(questionId);

            if (question != null) {
                answers.add(new QuestionAnswerResult(question.getDimension(), question.getText(), score));
            }
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

        model.addAttribute("overallAverage", overallAverage);
        model.addAttribute("dimensionAverage", dimensionAverage);
        model.addAttribute("answers", answers);

        return "questionnaire/result";
    }

    private List<QuestionItem> drawRandomQuestions(int count) {
        List<QuestionItem> shuffled = new ArrayList<>(QUESTION_BANK);
        for (int i = shuffled.size() - 1; i > 0; i--) {
            int j = ThreadLocalRandom.current().nextInt(i + 1);
            QuestionItem temp = shuffled.get(i);
            shuffled.set(i, shuffled.get(j));
            shuffled.set(j, temp);
        }
        return shuffled.subList(0, Math.min(count, shuffled.size()));
    }

    private QuestionItem findQuestion(Long id) {
        return QUESTION_BANK.stream()
                .filter(q -> q.getId().equals(id))
                .findFirst()
                .orElse(null);
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
        private String dimension;
        private String questionText;
        private Integer score;
    }
}

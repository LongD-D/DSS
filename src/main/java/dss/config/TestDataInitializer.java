package dss.config;

import dss.dto.QuestionnaireAnswerInputDto;
import dss.model.entity.*;
import dss.model.entity.enums.*;
import dss.repository.*;
import dss.service.QuestionnaireDataService;
import lombok.AllArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@AllArgsConstructor
public class TestDataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final TaskRepository taskRepository;
    private final DecisionRepository decisionRepository;
    private final QuestionnaireSubmissionRepository questionnaireSubmissionRepository;
    private final QuestionnaireDataService questionnaireDataService;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        boolean hasDecisionTestData = decisionRepository.findAll().stream()
                .anyMatch(d -> d.getTitle() != null && d.getTitle().contains("【测试信息】"));
        boolean hasQuestionnaireTestData = questionnaireSubmissionRepository.findAll().stream()
                .anyMatch(s -> s.getDimensionScores().stream().anyMatch(d -> d.getDimension().contains("测试信息")));

        if (!hasDecisionTestData) {
            seedTechnicalAdminTestData();
        }
        if (!hasQuestionnaireTestData) {
            seedQuestionnaireAdminTestData();
        }
    }

    private void seedTechnicalAdminTestData() {
        Role analystRole = ensureRole("ANALYST");
        Role adminRole = ensureRole("ROLE_ADMIN");

        User analyst = ensureUser("test.analyst@dss.local", "测试分析员", analystRole);
        ensureUser("test.admin@dss.local", "测试管理员", adminRole);

        List<Task> createdTasks = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            Task task = new Task();
            task.setTitle("【测试信息】技术分析任务-" + i);
            task.setDescription("【测试信息】用于管理员后台展示的技术任务样例-" + i);
            task.setCategory(i % 3 == 0 ? TaskCategory.ECOLOGY : (i % 2 == 0 ? TaskCategory.ECONOMY : TaskCategory.ENERGETIC));
            task.setStatus(TaskStatus.NEW);
            task.setCreated(LocalDateTime.now().minusDays(5L - i));
            task.setUser(analyst);
            task.setTaskParameters(buildTaskParameters(task));
            createdTasks.add(taskRepository.save(task));
        }

        for (int i = 1; i <= 10; i++) {
            Task task = createdTasks.get((i - 1) % createdTasks.size());
            Decision decision = new Decision();
            decision.setTask(task);
            decision.setUser(analyst);
            decision.setTitle("【测试信息】候选技术方案-" + i);
            decision.setDescription("【测试信息】管理员技术后台测试记录，关联任务: " + task.getTitle());
            decision.setDecisionCategory(i % 3 == 0 ? DecisionCategory.ECOLOGY : (i % 2 == 0 ? DecisionCategory.ECONOMY : DecisionCategory.ENERGY));
            decision.setDecisionStatus(DecisionStatus.PROPOSED);
            decision.setCreated(LocalDateTime.now().minusHours(10L - i));
            decision.setScore(0.5 + (i * 0.03));
            decision.setScenarios(new ArrayList<>());
            decision.setDecisionParameters(new ArrayList<>());
            decision.setExpertEvaluations(new ArrayList<>());
            decisionRepository.save(decision);
        }
    }

    private List<TaskParameter> buildTaskParameters(Task task) {
        TaskParameter p1 = TaskParameter.builder()
                .task(task)
                .name("【测试信息】成本")
                .parentCriterion("经济性")
                .weight(0.4)
                .unit("万元")
                .optimizationDirection(OptimizationDirection.MINIMIZE)
                .build();

        TaskParameter p2 = TaskParameter.builder()
                .task(task)
                .name("【测试信息】效率")
                .parentCriterion("技术成熟度")
                .weight(0.6)
                .unit("%")
                .optimizationDirection(OptimizationDirection.MAXIMIZE)
                .build();

        return List.of(p1, p2);
    }

    private void seedQuestionnaireAdminTestData() {
        Role analystRole = ensureRole("ANALYST");
        User analyst = ensureUser("test.analyst@dss.local", "测试分析员", analystRole);

        for (int i = 1; i <= 10; i++) {
            List<QuestionnaireAnswerInputDto> answers = List.of(
                    new QuestionnaireAnswerInputDto(100L + i, "技术成熟度(测试信息)", "【测试信息】技术成熟度题目-" + i, 3 + (i % 3)),
                    new QuestionnaireAnswerInputDto(200L + i, "经济性(测试信息)", "【测试信息】经济性题目-" + i, 2 + (i % 4)),
                    new QuestionnaireAnswerInputDto(300L + i, "生态影响(测试信息)", "【测试信息】生态影响题目-" + i, 3),
                    new QuestionnaireAnswerInputDto(400L + i, "法规适配(测试信息)", "【测试信息】法规适配题目-" + i, 4)
            );

            Map<String, Double> dimensionAverage = Map.of(
                    "技术成熟度(测试信息)", 3.0 + (i % 3) * 0.2,
                    "经济性(测试信息)", 2.8 + (i % 4) * 0.2,
                    "生态影响(测试信息)", 3.4,
                    "法规适配(测试信息)", 3.8
            );

            questionnaireDataService.recordSubmission(analyst, 3.25 + (i % 2) * 0.1, answers, dimensionAverage);
        }
    }

    private Role ensureRole(String name) {
        Role role = roleRepository.findByName(name);
        if (role != null) {
            return role;
        }
        Role newRole = new Role();
        newRole.setName(name);
        return roleRepository.save(newRole);
    }

    private User ensureUser(String email, String name, Role role) {
        User user = userRepository.findByEmail(email);
        if (user != null) {
            return user;
        }
        User created = new User();
        created.setEmail(email);
        created.setName(name);
        created.setPhone_number("TEST-" + Math.abs(email.hashCode()));
        created.setPassword(passwordEncoder.encode("Test@123456"));
        created.setRoles(new ArrayList<>(List.of(role)));
        return userRepository.save(created);
    }
}

package dss.controller.rest;

import dss.dto.QuestionBankImportResponseDto;
import dss.dto.QuestionBankSummaryDto;
import dss.model.entity.User;
import dss.service.QuestionnaireDataService;
import dss.service.UserService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/questionnaire/admin")
@AllArgsConstructor
public class QuestionnaireAdminRestController {

    private final QuestionnaireDataService questionnaireDataService;
    private final UserService userService;

    @GetMapping("/submissions")
    public ResponseEntity<?> getSubmissionHistory(Authentication authentication) {
        if (!isAdmin(authentication)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(questionnaireDataService.getSubmissionHistory());
    }

    @DeleteMapping("/submissions")
    public ResponseEntity<Void> clearSubmissionHistory(Authentication authentication) {
        if (!isAdmin(authentication)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        questionnaireDataService.clearSubmissionHistory();
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/question-banks")
    public ResponseEntity<?> getQuestionBanks(Authentication authentication) {
        if (!isAdmin(authentication)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(questionnaireDataService.getAllQuestionBanks());
    }

    @PostMapping("/question-banks/import")
    public ResponseEntity<?> importQuestionBank(@RequestParam("file") MultipartFile file,
                                                Authentication authentication) {
        if (!isAdmin(authentication)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            QuestionBankSummaryDto created = questionnaireDataService.importQuestionBank(file);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new QuestionBankImportResponseDto("题库导入成功。", created));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    private boolean isAdmin(Authentication authentication) {
        User user = userService.findUserByEmail(authentication.getName());
        return user.getRoles().stream().anyMatch(role -> "ROLE_ADMIN".equals(role.getName()));
    }
}

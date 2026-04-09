package dss.controller.mvc;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class QuestionnaireAdminController {

    @GetMapping("/questionnaire/admin")
    public String questionnaireAdminPage() {
        return "questionnaire/admin";
    }
}

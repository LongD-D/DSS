package dss.controller.mvc;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class TechnologyAdminController {

    @GetMapping("/technologies/admin")
    public String technologyAdminPage() {
        return "decisions/admin";
    }
}

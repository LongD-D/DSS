package dss.repository;

import dss.model.entity.QuestionnaireQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionnaireQuestionRepository extends JpaRepository<QuestionnaireQuestion, Long> {
}

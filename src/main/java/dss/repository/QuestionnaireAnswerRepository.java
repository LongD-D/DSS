package dss.repository;

import dss.model.entity.QuestionnaireAnswer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionnaireAnswerRepository extends JpaRepository<QuestionnaireAnswer, Long> {
}

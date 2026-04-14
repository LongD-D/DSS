package dss.repository;

import dss.model.entity.QuestionnaireQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuestionnaireQuestionRepository extends JpaRepository<QuestionnaireQuestion, Long> {
    List<QuestionnaireQuestion> findAllByBankId(Long bankId);
}

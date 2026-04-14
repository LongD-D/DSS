package dss.repository;

import dss.model.entity.QuestionnaireQuestionBank;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface QuestionnaireQuestionBankRepository extends JpaRepository<QuestionnaireQuestionBank, Long> {
    List<QuestionnaireQuestionBank> findAllByOrderByImportedAtDesc();

    Optional<QuestionnaireQuestionBank> findTopByOrderByImportedAtDesc();
}

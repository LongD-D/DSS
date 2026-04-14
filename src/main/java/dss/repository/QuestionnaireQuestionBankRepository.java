package dss.repository;

import dss.model.entity.QuestionnaireQuestionBank;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuestionnaireQuestionBankRepository extends JpaRepository<QuestionnaireQuestionBank, Long> {
    List<QuestionnaireQuestionBank> findAllByOrderByImportedAtDesc();
}

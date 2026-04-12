package dss.repository;

import dss.model.entity.QuestionnaireSubmission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface QuestionnaireSubmissionRepository extends JpaRepository<QuestionnaireSubmission, Long> {
    Optional<QuestionnaireSubmission> findTopByOrderBySubmittedAtDesc();

    List<QuestionnaireSubmission> findAllByOrderBySubmittedAtDesc();
}

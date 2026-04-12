package dss.repository;

import dss.model.entity.Decision;
import dss.model.entity.Task;
import dss.model.entity.User;
import dss.model.entity.enums.DecisionCategory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DecisionRepository extends JpaRepository<Decision, Long> {

    boolean existsById(Long id);
    List<Decision> findAll();
    List<Decision> findAllByUser(User user);
    List<Decision> findAllByUser(User user, Pageable pageable);
    Decision save(Decision decision);
    Decision findById(long id);
    void deleteById(long id);
    List<Decision> findAllByTask(Task task);
    List<Decision> findAllByTaskUserOrUser(User taskUser, User user);

    List<Decision> findAllByTask(Task task, Pageable pageable);

    Decision findByIdAndUser(Long id, User user);

}

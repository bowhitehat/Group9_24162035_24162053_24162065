package com.group9.topicmanagement.repository;
import com.group9.topicmanagement.domain.evaluation.EvaluationCriterion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface EvaluationCriterionRepository extends JpaRepository<EvaluationCriterion, Long> {
    List<EvaluationCriterion> findByRegistrationPeriodIdOrderByDisplayOrderAsc(Long periodId);
    List<EvaluationCriterion> findByRegistrationPeriodIdAndIsActiveTrueOrderByDisplayOrderAsc(Long periodId);
}
package com.group9.topicmanagement.repository;
import com.group9.topicmanagement.model.evaluation.EvaluationScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface EvaluationScoreRepository extends JpaRepository<EvaluationScore, Long> {
    List<EvaluationScore> findByEvaluationId(Long evaluationId);

    @EntityGraph(attributePaths = {"criterion"})
    List<EvaluationScore> findByEvaluationIdIn(java.util.Collection<Long> evaluationIds);
}

package com.group9.topicmanagement.repository;
import com.group9.topicmanagement.domain.evaluation.EvaluationScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface EvaluationScoreRepository extends JpaRepository<EvaluationScore, Long> {
    List<EvaluationScore> findByEvaluationId(Long evaluationId);
}
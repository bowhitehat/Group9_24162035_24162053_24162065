package com.group9.topicmanagement.repository;
import com.group9.topicmanagement.domain.evaluation.Evaluation;
import com.group9.topicmanagement.domain.enums.EvaluationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface EvaluationRepository extends JpaRepository<Evaluation, Long> {
    List<Evaluation> findByTopicId(Long topicId);
    List<Evaluation> findByEvaluatorId(Long evaluatorId);
    Optional<Evaluation> findByTopicIdAndEvaluatorIdAndEvaluationType(Long topicId, Long evaluatorId, String evaluationType);
}
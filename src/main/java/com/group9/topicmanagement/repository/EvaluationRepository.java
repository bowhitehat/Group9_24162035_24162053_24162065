package com.group9.topicmanagement.repository;

import com.group9.topicmanagement.domain.enums.EvaluationStatus;
import com.group9.topicmanagement.domain.enums.EvaluationType;
import com.group9.topicmanagement.domain.evaluation.Evaluation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EvaluationRepository extends JpaRepository<Evaluation, Long> {
    List<Evaluation> findByTopicId(Long topicId);
    List<Evaluation> findByEvaluatorId(Long evaluatorId);
    Optional<Evaluation> findByTopicIdAndEvaluatorIdAndEvaluationType(Long topicId, Long evaluatorId, EvaluationType evaluationType);

    @Query("SELECT e FROM Evaluation e JOIN FETCH e.evaluator WHERE e.topic.id = :topicId")
    List<Evaluation> findByTopicIdWithEvaluator(@Param("topicId") Long topicId);

    long countByTopicIdAndStatusIn(Long topicId, List<EvaluationStatus> statuses);
    List<Evaluation> findByTopicIdAndEvaluationType(Long topicId, EvaluationType evaluationType);
}

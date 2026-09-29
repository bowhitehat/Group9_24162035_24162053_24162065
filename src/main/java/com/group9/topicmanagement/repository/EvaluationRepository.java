package com.group9.topicmanagement.repository;

import com.group9.topicmanagement.domain.enums.EvaluationStatus;
import com.group9.topicmanagement.domain.enums.EvaluationType;
import com.group9.topicmanagement.domain.evaluation.Evaluation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EvaluationRepository extends JpaRepository<Evaluation, Long> {
    @EntityGraph(attributePaths = {"evaluator"})
    List<Evaluation> findByTopicId(Long topicId);
    List<Evaluation> findByEvaluatorId(Long evaluatorId);
    Optional<Evaluation> findByTopicIdAndEvaluatorIdAndEvaluationType(Long topicId, Long evaluatorId, EvaluationType evaluationType);

    long countByTopicIdAndStatusIn(Long topicId, List<EvaluationStatus> statuses);
    List<Evaluation> findByTopicIdAndEvaluationType(Long topicId, EvaluationType evaluationType);
}

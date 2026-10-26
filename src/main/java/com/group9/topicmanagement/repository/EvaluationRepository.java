package com.group9.topicmanagement.repository;

import com.group9.topicmanagement.model.enums.EvaluationStatus;
import com.group9.topicmanagement.model.enums.EvaluationType;
import com.group9.topicmanagement.model.evaluation.Evaluation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

@Repository
public interface EvaluationRepository extends JpaRepository<Evaluation, Long> {
    @Query("select e.topic.id from Evaluation e where e.id = :id")
    Optional<Long> findTopicIdById(@Param("id") Long id);

    // Read the ballot only after the service owns the topic lock. A locking read
    // observes the latest committed state even with MySQL REPEATABLE_READ.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Evaluation> findLockedById(Long id);

    boolean existsByTopicId(Long topicId);
    boolean existsByCouncilMember_Council_IdAndTopic_IdInAndEvaluationType(
            Long councilId, List<Long> topicIds, EvaluationType evaluationType);
    @EntityGraph(attributePaths = {"evaluator"})
    List<Evaluation> findByTopicId(Long topicId);
    List<Evaluation> findByEvaluatorId(Long evaluatorId);
    List<Evaluation> findByReviewerAssignmentIdIn(List<Long> assignmentIds);
    Optional<Evaluation> findByTopicIdAndEvaluatorIdAndEvaluationType(Long topicId, Long evaluatorId, EvaluationType evaluationType);

    long countByTopicIdAndStatusIn(Long topicId, List<EvaluationStatus> statuses);
    List<Evaluation> findByTopicIdAndEvaluationType(Long topicId, EvaluationType evaluationType);
}

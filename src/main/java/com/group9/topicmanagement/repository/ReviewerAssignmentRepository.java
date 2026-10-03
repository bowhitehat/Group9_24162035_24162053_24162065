package com.group9.topicmanagement.repository;

import com.group9.topicmanagement.model.enums.ReviewerAssignmentStatus;
import com.group9.topicmanagement.model.evaluation.ReviewerAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewerAssignmentRepository extends JpaRepository<ReviewerAssignment, Long> {
    List<ReviewerAssignment> findByTopicId(Long topicId);
    @EntityGraph(attributePaths = {"topic", "topic.department", "topic.registrationPeriod", "reviewer"})
    List<ReviewerAssignment> findByReviewerId(Long reviewerId);
    boolean existsByTopicIdAndReviewerId(Long topicId, Long reviewerId);
    Optional<ReviewerAssignment> findByTopicIdAndReviewerId(Long topicId, Long reviewerId);

    @EntityGraph(attributePaths = {"topic", "topic.department", "topic.registrationPeriod", "reviewer"})
    List<ReviewerAssignment> findAllByOrderByIdAsc();
    long countByStatusIn(List<ReviewerAssignmentStatus> statuses);
}

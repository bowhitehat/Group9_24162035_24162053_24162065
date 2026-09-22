package com.group9.topicmanagement.repository;

import com.group9.topicmanagement.domain.enums.ReviewerAssignmentStatus;
import com.group9.topicmanagement.domain.evaluation.ReviewerAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewerAssignmentRepository extends JpaRepository<ReviewerAssignment, Long> {
    List<ReviewerAssignment> findByTopicId(Long topicId);
    List<ReviewerAssignment> findByReviewerId(Long reviewerId);
    boolean existsByTopicIdAndReviewerId(Long topicId, Long reviewerId);
    Optional<ReviewerAssignment> findByTopicIdAndReviewerId(Long topicId, Long reviewerId);

    @Query("SELECT a FROM ReviewerAssignment a JOIN FETCH a.topic t JOIN FETCH t.department JOIN FETCH a.reviewer LEFT JOIN FETCH t.registrationPeriod")
    List<ReviewerAssignment> findAllWithDetails();

    @Query("SELECT a FROM ReviewerAssignment a JOIN FETCH a.topic t JOIN FETCH a.reviewer WHERE a.reviewer.id = :reviewerId")
    List<ReviewerAssignment> findByReviewerIdWithTopic(@Param("reviewerId") Long reviewerId);

    @Query("SELECT COUNT(a) FROM ReviewerAssignment a WHERE a.status IN :statuses")
    long countByStatusIn(@Param("statuses") List<ReviewerAssignmentStatus> statuses);

    @Query("SELECT COUNT(DISTINCT r.topic.id) FROM TopicRegistration r WHERE r.status = 'APPROVED' AND NOT EXISTS (" +
            "SELECT 1 FROM ReviewerAssignment a WHERE a.topic.id = r.topic.id AND a.status <> 'CANCELLED')")
    long countApprovedTopicsWithoutReviewer();
}

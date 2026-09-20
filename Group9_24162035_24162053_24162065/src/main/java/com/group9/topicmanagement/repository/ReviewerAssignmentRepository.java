package com.group9.topicmanagement.repository;
import com.group9.topicmanagement.domain.evaluation.ReviewerAssignment;
import com.group9.topicmanagement.domain.enums.ReviewerAssignmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewerAssignmentRepository extends JpaRepository<ReviewerAssignment, Long> {
    List<ReviewerAssignment> findByTopicId(Long topicId);
    List<ReviewerAssignment> findByReviewerId(Long reviewerId);
    boolean existsByTopicIdAndReviewerId(Long topicId, Long reviewerId);
}
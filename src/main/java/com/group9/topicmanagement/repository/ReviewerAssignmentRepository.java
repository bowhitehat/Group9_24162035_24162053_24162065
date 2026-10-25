package com.group9.topicmanagement.repository;

import com.group9.topicmanagement.model.enums.ReviewerAssignmentStatus;
import com.group9.topicmanagement.model.evaluation.ReviewerAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

@Repository
public interface ReviewerAssignmentRepository extends JpaRepository<ReviewerAssignment, Long> {
    @Query("select a.topic.id from ReviewerAssignment a where a.id = :id")
    Optional<Long> findTopicIdById(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<ReviewerAssignment> findLockedById(Long id);

    List<ReviewerAssignment> findByTopicId(Long topicId);
    @EntityGraph(attributePaths = {"topic", "topic.department", "topic.registrationPeriod", "reviewer"})
    List<ReviewerAssignment> findByReviewerId(Long reviewerId);
    boolean existsByTopicIdAndReviewerId(Long topicId, Long reviewerId);
    boolean existsByTopicIdAndReviewerIdAndStatusNot(Long topicId, Long reviewerId, ReviewerAssignmentStatus status);

    @EntityGraph(attributePaths = {"topic", "reviewer"})
    @Query("""
        select distinct r from ReviewerAssignment r, CouncilAssignment a, CouncilMember m
        where r.topic.id in :topicIds and r.status <> :cancelled
          and a.topic = r.topic and a.council.status in :councilStatuses
          and m.council = a.council and m.member = r.reviewer
        """)
    List<ReviewerAssignment> findCouncilRoleConflicts(
            @Param("topicIds") List<Long> topicIds,
            @Param("councilStatuses") List<com.group9.topicmanagement.model.enums.CouncilStatus> councilStatuses,
            @Param("cancelled") ReviewerAssignmentStatus cancelled);
    Optional<ReviewerAssignment> findByTopicIdAndReviewerId(Long topicId, Long reviewerId);

    @EntityGraph(attributePaths = {"topic", "topic.department", "topic.registrationPeriod", "reviewer"})
    List<ReviewerAssignment> findAllByOrderByIdAsc();
    long countByStatusIn(List<ReviewerAssignmentStatus> statuses);

    // Effective overdue status is filtered in SQL before the UI materializes matching rows.
    @EntityGraph(attributePaths = {"topic", "topic.department", "topic.registrationPeriod", "reviewer"})
    @Query("""
        SELECT ra FROM ReviewerAssignment ra
        WHERE (:periodId IS NULL OR ra.topic.registrationPeriod.id = :periodId)
          AND (:departmentId IS NULL OR ra.topic.department.id = :departmentId)
          AND (:reviewerId IS NULL OR ra.reviewer.id = :reviewerId)
          AND (:status IS NULL OR
               (CASE WHEN ra.status IN ('ASSIGNED', 'IN_PROGRESS') AND ra.deadline < :now
                     THEN com.group9.topicmanagement.model.enums.ReviewerAssignmentStatus.OVERDUE
                     ELSE ra.status END) = :status)
          AND (:pendingScore = false OR ra.status IN ('ASSIGNED', 'IN_PROGRESS', 'OVERDUE'))
        ORDER BY ra.id ASC
        """)
    List<ReviewerAssignment> findWithFilters(
        @Param("periodId") Long periodId,
        @Param("departmentId") Long departmentId,
        @Param("reviewerId") Long reviewerId,
        @Param("status") ReviewerAssignmentStatus status,
        @Param("pendingScore") boolean pendingScore,
        @Param("now") LocalDateTime now);
}

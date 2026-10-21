package com.group9.topicmanagement.repository;

import com.group9.topicmanagement.model.enums.TopicResultStatus;
import com.group9.topicmanagement.model.evaluation.TopicResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Collection;

@Repository
public interface TopicResultRepository extends JpaRepository<TopicResult, Long> {
    boolean existsByTopicId(Long topicId);
    @EntityGraph(attributePaths = {"topic", "topic.registrationPeriod", "confirmer", "publisher"})
    Optional<TopicResult> findByTopicId(Long topicId);

    @EntityGraph(attributePaths = {"topic", "topic.registrationPeriod", "councilAssignment", "councilAssignment.council"})
    List<TopicResult> findAllByOrderByIdAsc();

    @EntityGraph(attributePaths = {"topic", "topic.registrationPeriod", "councilAssignment", "councilAssignment.council"})
    List<TopicResult> findByCouncilAssignment_IdIn(Collection<Long> assignmentIds);

    long countByStatus(TopicResultStatus status);

    @EntityGraph(attributePaths = {"topic", "topic.registrationPeriod", "councilAssignment", "councilAssignment.council"})
    @Query("""
        SELECT r FROM TopicResult r
        WHERE r.status = 'PUBLISHED'
          AND EXISTS (
              SELECT 1 FROM TopicRegistration reg
              JOIN reg.studentGroup.members m
              WHERE reg.topic.id = r.topic.id
                AND reg.status = 'APPROVED'
                AND m.member.id = :studentId
          )
        ORDER BY r.id ASC
        """)
    List<TopicResult> findPublishedResultsForStudent(@Param("studentId") Long studentId);
}

package com.group9.topicmanagement.repository;

import com.group9.topicmanagement.domain.topic.Topic;
import com.group9.topicmanagement.domain.topic.TopicStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TopicRepository extends JpaRepository<Topic, Long> {

    @Override
    @EntityGraph(attributePaths = {"department", "registrationPeriod", "proposer", "advisors"})
    Optional<Topic> findById(Long id);

    @EntityGraph(attributePaths = {"department", "registrationPeriod", "proposer", "advisors"})
    @Query("SELECT t FROM Topic t WHERE t.id = :id")
    Optional<Topic> findByIdWithDetails(@Param("id") Long id);
    
    @EntityGraph(attributePaths = {"department", "registrationPeriod", "proposer"})
    Page<Topic> findByProposerId(Long proposerId, Pageable pageable);

    @EntityGraph(attributePaths = {"department", "registrationPeriod", "proposer"})
    @Query("SELECT DISTINCT t FROM Topic t LEFT JOIN t.advisors advisor WHERE " +
           "(t.proposer.id = :proposerId OR advisor.id = :proposerId " +
           "OR EXISTS (SELECT ra.id FROM ReviewerAssignment ra WHERE ra.topic.id = t.id AND ra.reviewer.id = :proposerId AND ra.status <> com.group9.topicmanagement.domain.enums.ReviewerAssignmentStatus.CANCELLED) " +
           "OR EXISTS (SELECT cm.id FROM CouncilMember cm WHERE cm.member.id = :proposerId AND cm.council.status <> com.group9.topicmanagement.domain.enums.CouncilStatus.CANCELLED " +
           "AND cm.council.id IN (SELECT ca.council.id FROM CouncilAssignment ca WHERE ca.topic.id = t.id))) " +
           "AND (:periodId IS NULL OR t.registrationPeriod.id = :periodId) " +
           "AND (:departmentId IS NULL OR t.department.id = :departmentId) " +
           "AND (:status IS NULL OR t.status = :status) " +
           "AND (:keyword IS NULL OR LOWER(t.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(t.code) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Topic> findByProposerWithFilters(@Param("proposerId") Long proposerId,
                                          @Param("periodId") Long periodId,
                                          @Param("departmentId") Long departmentId,
                                          @Param("status") TopicStatus status,
                                          @Param("keyword") String keyword,
                                          Pageable pageable);

    @Query("SELECT CASE WHEN COUNT(DISTINCT t.id) > 0 THEN true ELSE false END FROM Topic t LEFT JOIN t.advisors advisor WHERE t.id = :topicId AND " +
           "(t.proposer.id = :lecturerId OR advisor.id = :lecturerId " +
           "OR EXISTS (SELECT ra.id FROM ReviewerAssignment ra WHERE ra.topic.id = t.id AND ra.reviewer.id = :lecturerId AND ra.status <> com.group9.topicmanagement.domain.enums.ReviewerAssignmentStatus.CANCELLED) " +
           "OR EXISTS (SELECT cm.id FROM CouncilMember cm WHERE cm.member.id = :lecturerId AND cm.council.status <> com.group9.topicmanagement.domain.enums.CouncilStatus.CANCELLED " +
           "AND cm.council.id IN (SELECT ca.council.id FROM CouncilAssignment ca WHERE ca.topic.id = t.id)))")
    boolean existsAccessibleToLecturer(@Param("topicId") Long topicId, @Param("lecturerId") Long lecturerId);

    @EntityGraph(attributePaths = {"department", "registrationPeriod", "proposer"})
    @Query("SELECT t FROM Topic t JOIN t.advisors a WHERE a.id = :advisorId")
    Page<Topic> findByAdvisorsId(@Param("advisorId") Long advisorId, Pageable pageable);

    @EntityGraph(attributePaths = {"department", "registrationPeriod", "proposer"})
    @Query("SELECT t FROM Topic t WHERE t.registrationPeriod.id = :periodId " +
           "AND (:departmentId IS NULL OR t.department.id = :departmentId) " +
           "AND (:status IS NULL OR t.status = :status) " +
           "AND (:keyword IS NULL OR LOWER(t.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(t.code) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Topic> findWithFilters(@Param("periodId") Long periodId,
                                @Param("departmentId") Long departmentId,
                                @Param("status") TopicStatus status,
                                @Param("keyword") String keyword,
                                Pageable pageable);

    @EntityGraph(attributePaths = {"department", "registrationPeriod", "proposer"})
    @Query("SELECT t FROM Topic t WHERE t.registrationPeriod.id = :periodId " +
           "AND t.status = 'PUBLISHED' " +
           "AND (:departmentId IS NULL OR t.department.id = :departmentId) " +
           "AND (:keyword IS NULL OR LOWER(t.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(t.code) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Topic> findAvailableTopicsForStudents(@Param("periodId") Long periodId, 
                                               @Param("departmentId") Long departmentId,
                                               @Param("keyword") String keyword, 
                                               Pageable pageable);
}

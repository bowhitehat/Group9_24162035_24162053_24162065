package com.group9.topicmanagement.repository;

import com.group9.topicmanagement.model.topic.Topic;
import com.group9.topicmanagement.model.topic.TopicStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.Optional;
import java.util.List;
import java.util.Collection;

public interface TopicRepository extends JpaRepository<Topic, Long> {

    // Serialize score edits and result confirmation/publication for the same topic.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Topic> findLockedById(Long id);

    boolean existsByCodeIgnoreCaseAndRegistrationPeriodId(String code, Long registrationPeriodId);
    boolean existsByCodeIgnoreCaseAndRegistrationPeriodIdAndIdNot(String code, Long registrationPeriodId, Long id);

    @Override
    @EntityGraph(attributePaths = {"department", "registrationPeriod", "proposer", "advisors"})
    Optional<Topic> findById(Long id);

    @EntityGraph(attributePaths = {"department", "registrationPeriod", "proposer"})
    Page<Topic> findByProposerId(Long proposerId, Pageable pageable);

    @EntityGraph(attributePaths = {"department", "registrationPeriod", "proposer"})
    Page<Topic> findDistinctByAdvisors_Id(Long advisorId, Pageable pageable);

    // Collection hydration is deliberately separate from pagination, which stays in SQL.
    @EntityGraph(attributePaths = {"department", "registrationPeriod", "proposer", "advisors"})
    List<Topic> findDetailsByIdIn(Collection<Long> ids);

    // Filter with all optional params pushed to DB
    @EntityGraph(attributePaths = {"department", "registrationPeriod", "proposer"})
    @Query("""
        SELECT DISTINCT t FROM Topic t
        WHERE (:periodId IS NULL OR t.registrationPeriod.id = :periodId)
          AND (:departmentId IS NULL OR t.department.id = :departmentId)
          AND (:status IS NULL OR t.status = :status)
          AND (:keyword IS NULL OR LOWER(t.title) LIKE LOWER(CONCAT('%',:keyword,'%')) ESCAPE '!'
               OR LOWER(t.code) LIKE LOWER(CONCAT('%',:keyword,'%')) ESCAPE '!')
        ORDER BY t.id DESC
        """)
    Page<Topic> findWithFilters(
        @Param("periodId") Long periodId,
        @Param("departmentId") Long departmentId,
        @Param("status") TopicStatus status,
        @Param("keyword") String keyword,
        Pageable pageable);

    // Keep the same access scope as the detail view without per-topic permission queries.
    @EntityGraph(attributePaths = {"department", "registrationPeriod", "proposer"})
    @Query("""
        SELECT t FROM Topic t
        WHERE (:periodId IS NULL OR t.registrationPeriod.id = :periodId)
          AND (:departmentId IS NULL OR t.department.id = :departmentId)
          AND (:status IS NULL OR t.status = :status)
          AND (:keyword IS NULL OR LOWER(t.title) LIKE LOWER(CONCAT('%',:keyword,'%')) ESCAPE '!'
               OR LOWER(t.code) LIKE LOWER(CONCAT('%',:keyword,'%')) ESCAPE '!')
          AND (t.proposer.id = :lecturerId
            OR EXISTS (SELECT adv.id FROM Topic ta JOIN ta.advisors adv
                       WHERE ta.id = t.id AND adv.id = :lecturerId)
            OR EXISTS (SELECT ra.id FROM ReviewerAssignment ra WHERE ra.topic.id = t.id
                       AND ra.reviewer.id = :lecturerId AND ra.status <> 'CANCELLED')
            OR EXISTS (SELECT ca.id FROM CouncilAssignment ca, CouncilMember cm
                       WHERE ca.topic.id = t.id AND ca.council.status <> 'CANCELLED'
                       AND cm.council.id = ca.council.id AND cm.member.id = :lecturerId))
        ORDER BY t.id DESC
        """)
    Page<Topic> findWithFiltersForLecturer(
        @Param("periodId") Long periodId,
        @Param("departmentId") Long departmentId,
        @Param("status") TopicStatus status,
        @Param("keyword") String keyword,
        @Param("lecturerId") Long lecturerId,
        Pageable pageable);

    long countByStatus(TopicStatus status);
}

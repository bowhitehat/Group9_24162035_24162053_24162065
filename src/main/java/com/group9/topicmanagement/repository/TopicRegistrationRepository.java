package com.group9.topicmanagement.repository;

import com.group9.topicmanagement.model.enums.RegistrationStatus;
import com.group9.topicmanagement.model.registration.TopicRegistration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;
import java.util.Collection;

public interface TopicRegistrationRepository extends JpaRepository<TopicRegistration, Long> {
    // Serialize version allocation for a single registration without locking joined users/groups.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<TopicRegistration> findForUploadLockedById(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"topic", "studentGroup", "studentGroup.leader", "studentGroup.members", "studentGroup.members.member", "registrationPeriod"})
    Optional<TopicRegistration> findLockedById(Long id);

    boolean existsByTopic_Id(Long topicId);
    boolean existsByStudentGroup_IdAndStatus(Long groupId, RegistrationStatus status);

    @EntityGraph(attributePaths = {"registrationPeriod"})
    List<TopicRegistration> findDistinctByStudentGroup_Members_Member_UsernameIgnoreCaseOrderByIdDesc(String username);

    @Override
    @EntityGraph(attributePaths = {
            "topic", "topic.department", "topic.proposer", "topic.advisors",
            "studentGroup", "studentGroup.leader", "studentGroup.members", "studentGroup.members.member",
            "registrationPeriod", "approver"
    })
    Optional<TopicRegistration> findById(Long id);
    
    @EntityGraph(attributePaths = {"topic", "studentGroup", "studentGroup.leader", "registrationPeriod"})
    Optional<TopicRegistration> findByStudentGroupIdAndRegistrationPeriodId(Long groupId, Long periodId);
    
    Optional<TopicRegistration> findFirstReferenceByTopic_IdAndStatus(Long topicId, RegistrationStatus status);

    // Do not combine a first/limit query with a collection fetch.
    default Optional<TopicRegistration> findFirstByTopic_IdAndStatus(Long topicId, RegistrationStatus status) {
        return findFirstReferenceByTopic_IdAndStatus(topicId, status).flatMap(r -> findById(r.getId()));
    }

    // Locking reads are current reads on MySQL/InnoDB, even under REPEATABLE_READ.
    // The topic row is locked first by the service, so this query observes an approval
    // committed by the preceding transaction instead of reusing an older snapshot.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<TopicRegistration> findFirstLockedByTopic_IdAndStatus(Long topicId, RegistrationStatus status);

    Optional<TopicRegistration> findFirstByTopic_IdAndRegistrationPeriod_IdAndStatus(Long topicId, Long periodId,
                                                                                      RegistrationStatus status);

    @EntityGraph(attributePaths = {
            "topic", "topic.department", "studentGroup", "studentGroup.leader",
            "studentGroup.members", "studentGroup.members.member", "registrationPeriod"
    })
    List<TopicRegistration> findAllByOrderByIdDesc();

    // The reviewer form reads period type/status/deadline after the service transaction closes.
    @EntityGraph(attributePaths = {"topic", "registrationPeriod", "studentGroup", "studentGroup.members", "studentGroup.members.member"})
    List<TopicRegistration> findByStatusOrderByIdAsc(RegistrationStatus status);

    long countByStatus(RegistrationStatus status);

    @EntityGraph(attributePaths = {"topic", "studentGroup", "studentGroup.leader", "registrationPeriod"})
    List<TopicRegistration> findByStudentGroup_Leader_UsernameIgnoreCaseAndStatusOrderByIdDesc(
            String username, RegistrationStatus status);

    // General filter (for faculty/admin – no username scope)
    @EntityGraph(attributePaths = {
        "topic", "topic.department", "studentGroup", "studentGroup.leader",
        "registrationPeriod", "approver"
    })
    @Query("""
        SELECT r FROM TopicRegistration r
        WHERE (:periodId IS NULL OR r.registrationPeriod.id = :periodId)
          AND (:departmentId IS NULL OR r.topic.department.id = :departmentId)
          AND (:status IS NULL OR r.status = :status)
          AND (:keyword IS NULL OR LOWER(r.topic.title) LIKE LOWER(CONCAT('%',:keyword,'%')) ESCAPE '!')
        ORDER BY r.id DESC
        """)
    Page<TopicRegistration> findWithFilters(
        @Param("periodId") Long periodId,
        @Param("departmentId") Long departmentId,
        @Param("status") RegistrationStatus status,
        @Param("keyword") String keyword,
        Pageable pageable);

    // Student-scoped filter (by member username)
    @EntityGraph(attributePaths = {
        "topic", "topic.department", "studentGroup", "studentGroup.leader",
        "registrationPeriod", "approver"
    })
    @Query("""
        SELECT r FROM TopicRegistration r
        WHERE EXISTS (SELECT m.id FROM GroupMember m WHERE m.group.id = r.studentGroup.id
                      AND LOWER(m.member.username) = LOWER(:username))
          AND (:periodId IS NULL OR r.registrationPeriod.id = :periodId)
          AND (:status IS NULL OR r.status = :status)
          AND (:keyword IS NULL OR LOWER(r.topic.title) LIKE LOWER(CONCAT('%',:keyword,'%')) ESCAPE '!')
        ORDER BY r.id DESC
        """)
    Page<TopicRegistration> findWithFiltersForStudent(
        @Param("username") String username,
        @Param("periodId") Long periodId,
        @Param("status") RegistrationStatus status,
        @Param("keyword") String keyword,
        Pageable pageable);

    @EntityGraph(attributePaths = {"topic", "topic.department", "studentGroup", "studentGroup.leader",
            "studentGroup.members", "studentGroup.members.member", "registrationPeriod", "approver"})
    List<TopicRegistration> findDetailsByIdIn(Collection<Long> ids);

    @EntityGraph(attributePaths = {"topic", "topic.registrationPeriod", "registrationPeriod", "studentGroup"})
    @Query("""
        SELECT r FROM TopicRegistration r WHERE r.status = 'APPROVED'
          AND NOT EXISTS (SELECT ra.id FROM ReviewerAssignment ra
                          WHERE ra.topic.id = r.topic.id AND ra.status <> 'CANCELLED')
        ORDER BY r.id ASC
        """)
    List<TopicRegistration> findApprovedWithoutReviewer();

    // Count topics that have no active (non-CANCELLED) reviewer assignment
    @Query("""
        SELECT COUNT(r) FROM TopicRegistration r
        WHERE r.status = 'APPROVED'
          AND NOT EXISTS (
              SELECT 1 FROM ReviewerAssignment ra
              WHERE ra.topic.id = r.topic.id
                AND ra.status <> 'CANCELLED'
          )
        """)
    long countApprovedWithoutReviewer();
}

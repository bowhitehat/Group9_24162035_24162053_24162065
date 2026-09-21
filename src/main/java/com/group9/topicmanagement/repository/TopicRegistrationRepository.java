package com.group9.topicmanagement.repository;

import com.group9.topicmanagement.domain.enums.RegistrationStatus;
import com.group9.topicmanagement.domain.registration.TopicRegistration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TopicRegistrationRepository extends JpaRepository<TopicRegistration, Long> {
    
    Optional<TopicRegistration> findByStudentGroupIdAndRegistrationPeriodId(Long groupId, Long periodId);
    
    @Query("SELECT r FROM TopicRegistration r WHERE r.topic.id = :topicId AND r.status = 'APPROVED'")
    Optional<TopicRegistration> findApprovedRegistrationForTopic(@Param("topicId") Long topicId);

    @Query("SELECT r FROM TopicRegistration r WHERE r.topic.id = :topicId AND r.registrationPeriod.id = :periodId AND r.status = 'APPROVED'")
    Optional<TopicRegistration> findApprovedRegistrationForTopicAndPeriod(@Param("topicId") Long topicId, @Param("periodId") Long periodId);

    @Query("SELECT r FROM TopicRegistration r WHERE r.registrationPeriod.id = :periodId " +
           "AND (:departmentId IS NULL OR r.topic.department.id = :departmentId) " +
           "AND (:status IS NULL OR r.status = :status) " +
           "AND (:keyword IS NULL OR LOWER(r.topic.title) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<TopicRegistration> findWithFilters(@Param("periodId") Long periodId,
                                            @Param("departmentId") Long departmentId,
                                            @Param("status") RegistrationStatus status,
                                            @Param("keyword") String keyword,
                                            Pageable pageable);

    @Query("SELECT DISTINCT r FROM TopicRegistration r JOIN r.studentGroup.members gm " +
           "WHERE gm.member.username = :username " +
           "AND (:periodId IS NULL OR r.registrationPeriod.id = :periodId) " +
           "AND (:status IS NULL OR r.status = :status) " +
           "AND (:keyword IS NULL OR LOWER(r.topic.title) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<TopicRegistration> findForStudent(@Param("username") String username,
                                           @Param("periodId") Long periodId,
                                           @Param("status") RegistrationStatus status,
                                           @Param("keyword") String keyword,
                                           Pageable pageable);

    @Query("SELECT r FROM TopicRegistration r JOIN FETCH r.topic JOIN FETCH r.studentGroup WHERE r.status = :status")
    List<TopicRegistration> findByStatusWithTopicAndGroup(@Param("status") RegistrationStatus status);

    long countByStatus(RegistrationStatus status);
}

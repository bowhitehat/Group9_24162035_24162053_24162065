package com.group9.topicmanagement.repository;

import com.group9.topicmanagement.domain.enums.RegistrationStatus;
import com.group9.topicmanagement.domain.registration.TopicRegistration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
}

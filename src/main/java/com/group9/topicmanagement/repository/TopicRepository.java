package com.group9.topicmanagement.repository;

import com.group9.topicmanagement.domain.topic.Topic;
import com.group9.topicmanagement.domain.topic.TopicStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TopicRepository extends JpaRepository<Topic, Long> {
    
    Page<Topic> findByProposerId(Long proposerId, Pageable pageable);

    @Query("SELECT t FROM Topic t JOIN t.advisors a WHERE a.id = :advisorId")
    Page<Topic> findByAdvisorsId(@Param("advisorId") Long advisorId, Pageable pageable);

    @Query("SELECT t FROM Topic t WHERE t.registrationPeriod.id = :periodId " +
           "AND (:departmentId IS NULL OR t.department.id = :departmentId) " +
           "AND (:status IS NULL OR t.status = :status) " +
           "AND (:keyword IS NULL OR LOWER(t.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(t.code) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Topic> findWithFilters(@Param("periodId") Long periodId,
                                @Param("departmentId") Long departmentId,
                                @Param("status") TopicStatus status,
                                @Param("keyword") String keyword,
                                Pageable pageable);

    @Query("SELECT t FROM Topic t WHERE t.registrationPeriod.id = :periodId " +
           "AND (t.status = 'APPROVED' OR t.status = 'PUBLISHED') " +
           "AND (:keyword IS NULL OR LOWER(t.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(t.code) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Topic> findAvailableTopicsForStudents(@Param("periodId") Long periodId, 
                                               @Param("keyword") String keyword, 
                                               Pageable pageable);
}

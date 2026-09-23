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
    
    @EntityGraph(attributePaths = {"department", "registrationPeriod", "proposer"})
    Page<Topic> findByProposerId(Long proposerId, Pageable pageable);

    @EntityGraph(attributePaths = {"department", "registrationPeriod", "proposer"})
    @Query("SELECT t FROM Topic t WHERE t.proposer.id = :proposerId " +
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

package com.group9.topicmanagement.repository;

import com.group9.topicmanagement.domain.council.Council;
import com.group9.topicmanagement.domain.enums.CouncilStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CouncilRepository extends JpaRepository<Council, Long> {
    List<Council> findByRegistrationPeriodId(Long periodId);

    @Query("SELECT DISTINCT c FROM Council c JOIN FETCH c.registrationPeriod")
    List<Council> findAllWithPeriod();

    @Query("SELECT c FROM Council c JOIN FETCH c.registrationPeriod WHERE c.id = :id")
    Optional<Council> findByIdWithPeriod(@Param("id") Long id);

    long countByStatus(CouncilStatus status);
}

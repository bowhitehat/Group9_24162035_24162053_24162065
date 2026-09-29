package com.group9.topicmanagement.repository;

import com.group9.topicmanagement.domain.council.Council;
import com.group9.topicmanagement.domain.enums.CouncilStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CouncilRepository extends JpaRepository<Council, Long> {
    @EntityGraph(attributePaths = {"registrationPeriod"})
    List<Council> findByRegistrationPeriodId(Long periodId);
    @EntityGraph(attributePaths = {"registrationPeriod"})
    List<Council> findAllByOrderByIdAsc();
    @Override
    @EntityGraph(attributePaths = {"registrationPeriod"})
    Optional<Council> findById(Long id);

    long countByStatus(CouncilStatus status);
}

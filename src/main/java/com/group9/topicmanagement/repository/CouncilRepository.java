package com.group9.topicmanagement.repository;

import com.group9.topicmanagement.model.council.Council;
import com.group9.topicmanagement.model.enums.CouncilStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
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

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"registrationPeriod"})
    Optional<Council> findLockedById(Long id);

    @EntityGraph(attributePaths = {"registrationPeriod"})
    @Query("select c from Council c where exists (select m.id from CouncilMember m "
            + "where m.council = c and m.member.id = :userId) order by c.id")
    List<Council> findForMember(@Param("userId") Long userId);

    long countByStatus(CouncilStatus status);
}

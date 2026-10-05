package com.group9.topicmanagement.repository;

import com.group9.topicmanagement.model.topic.Topic;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

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

    @EntityGraph(attributePaths = {"department", "registrationPeriod", "proposer", "advisors"})
    Page<Topic> findDistinctByAdvisors_Id(Long advisorId, Pageable pageable);

    @EntityGraph(attributePaths = {"department", "registrationPeriod", "proposer", "advisors"})
    List<Topic> findAllByOrderByIdDesc();
}

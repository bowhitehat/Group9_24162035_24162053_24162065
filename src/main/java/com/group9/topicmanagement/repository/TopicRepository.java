package com.group9.topicmanagement.repository;

import com.group9.topicmanagement.domain.topic.Topic;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;
import java.util.Optional;

public interface TopicRepository extends JpaRepository<Topic, Long> {

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

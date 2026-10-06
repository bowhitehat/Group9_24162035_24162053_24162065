package com.group9.topicmanagement.repository;

import com.group9.topicmanagement.model.enums.TopicResultStatus;
import com.group9.topicmanagement.model.evaluation.TopicResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TopicResultRepository extends JpaRepository<TopicResult, Long> {
    @EntityGraph(attributePaths = {"topic", "topic.registrationPeriod", "confirmer", "publisher"})
    Optional<TopicResult> findByTopicId(Long topicId);

    @EntityGraph(attributePaths = {"topic", "topic.registrationPeriod", "councilAssignment", "councilAssignment.council"})
    List<TopicResult> findAllByOrderByIdAsc();

    long countByStatus(TopicResultStatus status);
}

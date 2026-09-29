package com.group9.topicmanagement.repository;

import com.group9.topicmanagement.domain.enums.TopicResultStatus;
import com.group9.topicmanagement.domain.evaluation.TopicResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TopicResultRepository extends JpaRepository<TopicResult, Long> {
    @EntityGraph(attributePaths = {"topic", "confirmer", "publisher"})
    Optional<TopicResult> findByTopicId(Long topicId);

    @EntityGraph(attributePaths = {"topic"})
    List<TopicResult> findAllByOrderByIdAsc();

    long countByStatus(TopicResultStatus status);
}

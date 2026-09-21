package com.group9.topicmanagement.repository;

import com.group9.topicmanagement.domain.enums.TopicResultStatus;
import com.group9.topicmanagement.domain.evaluation.TopicResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TopicResultRepository extends JpaRepository<TopicResult, Long> {
    Optional<TopicResult> findByTopicId(Long topicId);

    @Query("SELECT r FROM TopicResult r JOIN FETCH r.topic")
    List<TopicResult> findAllWithTopic();

    long countByStatus(TopicResultStatus status);
}

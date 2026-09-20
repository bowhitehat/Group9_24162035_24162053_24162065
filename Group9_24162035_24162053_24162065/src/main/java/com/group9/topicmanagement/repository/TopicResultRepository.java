package com.group9.topicmanagement.repository;
import com.group9.topicmanagement.domain.evaluation.TopicResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface TopicResultRepository extends JpaRepository<TopicResult, Long> {
    Optional<TopicResult> findByTopicId(Long topicId);
}
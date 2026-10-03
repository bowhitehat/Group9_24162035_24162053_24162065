package com.group9.topicmanagement.repository;

import com.group9.topicmanagement.model.council.CouncilAssignment;
import com.group9.topicmanagement.model.enums.CouncilStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CouncilAssignmentRepository extends JpaRepository<CouncilAssignment, Long> {
    @EntityGraph(attributePaths = {"topic"})
    List<CouncilAssignment> findByCouncilId(Long councilId);
    @EntityGraph(attributePaths = {"council", "council.registrationPeriod"})
    List<CouncilAssignment> findByTopicId(Long topicId);
    boolean existsByCouncilIdAndTopicId(Long councilId, Long topicId);

    @EntityGraph(attributePaths = {"council"})
    List<CouncilAssignment> findByTopicIdAndCouncil_StatusIn(Long topicId, List<CouncilStatus> statuses);

    @EntityGraph(attributePaths = {"topic", "council"})
    Optional<CouncilAssignment> findFirstByTopicIdOrderByIdDesc(Long topicId);

    @EntityGraph(attributePaths = {"topic", "council"})
    List<CouncilAssignment> findAllByOrderByIdAsc();
}

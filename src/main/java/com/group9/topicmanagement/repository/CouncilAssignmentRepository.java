package com.group9.topicmanagement.repository;

import com.group9.topicmanagement.model.council.CouncilAssignment;
import com.group9.topicmanagement.model.enums.CouncilStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    @EntityGraph(attributePaths = {"topic", "council"})
    @Query("""
            select a from CouncilAssignment a join a.council c
            where a.topic.id = :topicId and c.status in :statuses
            order by case when c.status = com.group9.topicmanagement.model.enums.CouncilStatus.ACTIVE
                then 0 else 1 end, a.id desc
            """)
    List<CouncilAssignment> findByTopicIdAndCouncil_StatusIn(
            @Param("topicId") Long topicId, @Param("statuses") List<CouncilStatus> statuses);

    @EntityGraph(attributePaths = {"topic", "council"})
    Optional<CouncilAssignment> findFirstByTopicIdOrderByIdDesc(Long topicId);

    @EntityGraph(attributePaths = {"topic", "council"})
    List<CouncilAssignment> findAllByOrderByIdAsc();
}

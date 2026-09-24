package com.group9.topicmanagement.repository;

import com.group9.topicmanagement.domain.council.CouncilAssignment;
import com.group9.topicmanagement.domain.enums.CouncilStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CouncilAssignmentRepository extends JpaRepository<CouncilAssignment, Long> {
    List<CouncilAssignment> findByCouncilId(Long councilId);
    List<CouncilAssignment> findByTopicId(Long topicId);
    boolean existsByCouncilIdAndTopicId(Long councilId, Long topicId);

    @Query("SELECT a FROM CouncilAssignment a JOIN FETCH a.topic WHERE a.council.id = :councilId")
    List<CouncilAssignment> findByCouncilIdWithTopic(@Param("councilId") Long councilId);

    @Query("SELECT a FROM CouncilAssignment a JOIN FETCH a.council c JOIN FETCH c.registrationPeriod WHERE a.topic.id = :topicId")
    List<CouncilAssignment> findByTopicIdWithCouncil(@Param("topicId") Long topicId);

    @Query("SELECT a FROM CouncilAssignment a JOIN FETCH a.council c WHERE a.topic.id = :topicId AND c.status IN :statuses")
    List<CouncilAssignment> findByTopicIdAndCouncilStatusIn(@Param("topicId") Long topicId,
                                                            @Param("statuses") List<CouncilStatus> statuses);

    @EntityGraph(attributePaths = {"topic", "council"})
    Optional<CouncilAssignment> findFirstByTopicIdOrderByIdDesc(Long topicId);

    @Query("SELECT a FROM CouncilAssignment a JOIN FETCH a.topic JOIN FETCH a.council")
    List<CouncilAssignment> findAllWithTopicAndCouncil();
}

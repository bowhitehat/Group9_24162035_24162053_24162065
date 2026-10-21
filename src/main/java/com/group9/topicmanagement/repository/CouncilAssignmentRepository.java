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

    // Select exactly the same current assignment as currentAssignment(): ACTIVE first,
    // otherwise latest COMPLETED, otherwise latest historical assignment. Only then
    // apply visibility; membership of an old council must not expose the new council.
    @EntityGraph(attributePaths = {"topic", "topic.registrationPeriod", "council"})
    @Query("""
            select a from CouncilAssignment a
            where a.council.status <> 'CANCELLED'
              and not exists (
                  select b.id from CouncilAssignment b where b.topic = a.topic and (
                      (case when b.council.status = 'ACTIVE' then 0
                            when b.council.status = 'COMPLETED' then 1 else 2 end)
                      < (case when a.council.status = 'ACTIVE' then 0
                              when a.council.status = 'COMPLETED' then 1 else 2 end)
                      or ((case when b.council.status = 'ACTIVE' then 0
                                when b.council.status = 'COMPLETED' then 1 else 2 end)
                          = (case when a.council.status = 'ACTIVE' then 0
                                  when a.council.status = 'COMPLETED' then 1 else 2 end)
                          and b.id > a.id)
                  )
              )
              and (:manager = true or exists (
                  select m.id from CouncilMember m
                  where m.council = a.council and m.member.id = :userId
              ))
            order by (select min(h.id) from CouncilAssignment h where h.topic = a.topic)
            """)
    List<CouncilAssignment> findCurrentVisibleAssignments(@Param("manager") boolean manager,
                                                         @Param("userId") Long userId);
}

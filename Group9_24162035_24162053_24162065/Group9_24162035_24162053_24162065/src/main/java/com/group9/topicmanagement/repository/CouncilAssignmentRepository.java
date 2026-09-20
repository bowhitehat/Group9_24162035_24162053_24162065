package com.group9.topicmanagement.repository;
import com.group9.topicmanagement.domain.council.CouncilAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface CouncilAssignmentRepository extends JpaRepository<CouncilAssignment, Long> {
    List<CouncilAssignment> findByCouncilId(Long councilId);
    Optional<CouncilAssignment> findByTopicId(Long topicId);
    boolean existsByCouncilIdAndTopicId(Long councilId, Long topicId);
}